package com.github.willtong.helper;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Method;
import java.util.Properties;

public abstract class AbsStatementHandlerInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaStatementHandler = SystemMetaObject.forObject(statementHandler);

        while (metaStatementHandler.hasGetter("h")) {
            metaStatementHandler = SystemMetaObject.forObject(metaStatementHandler.getValue("h"));
        }
        while (metaStatementHandler.hasGetter("target")) {
            metaStatementHandler = SystemMetaObject.forObject(metaStatementHandler.getValue("target"));
        }

        MappedStatement mappedStatement = (MappedStatement) metaStatementHandler.getValue("delegate.mappedStatement");

        try {
            String selectId = mappedStatement.getId();
            String className = selectId.substring(0, selectId.lastIndexOf("."));
            String methodName = selectId.substring(selectId.lastIndexOf(".") + 1);
            Method method = Class.forName(className).getMethod(methodName);
            return doIntercept(invocation, metaStatementHandler, mappedStatement, method);
        } catch (Exception e) {
            return invocation.proceed();
        }
    }

    protected abstract Object doIntercept(Invocation invocation, MetaObject metaStatementHandler, MappedStatement mappedStatement, Method method) throws Throwable;

    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
