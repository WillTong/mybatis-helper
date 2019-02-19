package com.github.mybatis.helper.core;

import com.github.mybatis.helper.core.filter.SqlFilter;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;

/**
 * mybatis拦截.
 * @author will
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class StatementHandlerInterceptor extends AbstractSqlInterceptor{

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        //获取入参
        Connection connection=(Connection)invocation.getArgs()[0];
        StatementHandler delegate = (StatementHandler) ReflectUtil.getFieldValue(statementHandler, "delegate");
        MappedStatement mappedStatement = (MappedStatement) ReflectUtil.getFieldValue(delegate, "mappedStatement");
        RowBounds rowBounds = (RowBounds) ReflectUtil.getFieldValue(delegate, "rowBounds");
        BoundSql boundSql = delegate.getBoundSql();
        String sql=boundSql.getSql();
        ExecuteHelper executeHelper=ExecuteHelper.build(connection,mappedStatement,boundSql);
        String mappedStatementId=mappedStatement.getId();
        //遍历过滤器
        for(SqlFilter sqlFilter : sqlFilterList){
            //判断MappedStatementId是否在执行之列
            if(isMatchMappedStatementId(mappedStatementId, sqlFilter)){
                for(SqlCommandType sqlCommandType:sqlFilter.getSqlCommandType()){
                    if(sqlCommandType==mappedStatement.getSqlCommandType()){
                        if(sqlFilter.getFilterParamName()==null){
                            sql= sqlFilter.doSqlFilter(sql,null,executeHelper,mappedStatement,boundSql,rowBounds);
                        }else{
                            if(MybatisThreadHelper.containsVariableKey(sqlFilter.getFilterParamName())){
                                sql= sqlFilter.doSqlFilter(sql,MybatisThreadHelper.getVariable(sqlFilter.getFilterParamName()),executeHelper,mappedStatement,boundSql,rowBounds);
                            }
                        }
                        break;
                    }
                }
            }
        }
        
        ReflectUtil.setFieldValue(boundSql, "sql", sql);
        return invocation.proceed();
    }

    private boolean isMatchMappedStatementId(String mappedStatementId, SqlFilter sqlFilter) {
        int include=MybatisUtils.matchMappedStatementId(mappedStatementId,sqlFilter.getIncludeMappedStatementIds());
        int exclude=MybatisUtils.matchMappedStatementId(mappedStatementId,sqlFilter.getExcludeMappedStatementIds());
        return include>exclude;
    }
}
