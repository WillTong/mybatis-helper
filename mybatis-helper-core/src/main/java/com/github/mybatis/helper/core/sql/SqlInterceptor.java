package com.github.mybatis.helper.core.sql;

import com.github.mybatis.helper.core.AbstractInterceptor;
import com.github.mybatis.helper.core.MybatisThreadHelper;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

/**
 * mybatis拦截.
 * @author will
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public abstract class SqlInterceptor extends AbstractInterceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Connection connection=(Connection)invocation.getArgs()[0];
        MetaObject metaStatementHandler = SystemMetaObject.forObject(invocation.getTarget());
        while (metaStatementHandler.hasGetter("h")) {
            metaStatementHandler = SystemMetaObject.forObject(metaStatementHandler.getValue("h"));
            metaStatementHandler = SystemMetaObject.forObject(metaStatementHandler.getValue("target"));
        }
        MappedStatement mappedStatement=(MappedStatement) metaStatementHandler.getValue("delegate.mappedStatement");
        RowBounds rowBounds=(RowBounds) metaStatementHandler.getValue("delegate.rowBounds");
        BoundSql boundSql=(BoundSql)metaStatementHandler.getValue("delegate.boundSql");
        Object filterParam=null;
        if(paramName!=null&&paramName.length()!=0){
            filterParam= MybatisThreadHelper.getVariable(paramName);
            if(filterParam==null){
                return  invocation.proceed();
            }
        }
        ExecuteHelper executeHelper=ExecuteHelper.build(connection,mappedStatement,boundSql);
        //初始化配置
        if(settings.isEmpty()){
            setAllSettings(mappedStatement.getConfiguration().getMapperRegistry().getMappers());
        }
        if(isMatchMappedStatementId(mappedStatement.getId())&&isContainsSqlCommandType(mappedStatement.getSqlCommandType())){
            metaStatementHandler.setValue("delegate.boundSql.sql",doSqlFilter(boundSql.getSql(),filterParam,mappedStatement,rowBounds,boundSql,executeHelper));
        }
        return invocation.proceed();
    }

     /**
     * 执行sql拦截
     * @param originalSql 原始sql
     * @return
     */
     public abstract String doSqlFilter(String originalSql,Object filterParam,MappedStatement mappedStatement,RowBounds rowBounds,BoundSql boundSql,ExecuteHelper executeHelper);

    /**
     * 指定拦截sql语句类型
     * @return
     */
    public abstract boolean isContainsSqlCommandType(SqlCommandType sqlCommandType);
}
