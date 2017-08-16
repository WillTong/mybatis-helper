package com.github.mybatis.helper;

import com.github.mybatis.helper.annotation.Authority;
import com.github.mybatis.helper.annotation.Page;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.Map;

/**
 * mybatis拦截.
 * @author will
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class MybatisHelper extends AbsStatementHandlerInterceptor {

    @Override
    protected Object doIntercept(Invocation invocation, MetaObject metaStatementHandler, MappedStatement mappedStatement, Method method) throws Throwable {
        Authority authority=method.getAnnotation(Authority.class);
        if(authority!= null){
            BoundSql boundSql = (BoundSql) metaStatementHandler.getValue("delegate.boundSql");
            BaseModel baseModel = (BaseModel) boundSql.getParameterObject();
            if(baseModel.getDataAuthority()!=null&&!baseModel.getDataAuthority().isEmpty()){
                if(authority.value()!=null&&authority.value().length!=0){
                    for(String exclude:authority.value()){
                        if(baseModel.getDataAuthority().containsKey(exclude)){
                            baseModel.getDataAuthority().remove(exclude);
                        }
                    }
                }
                //排除之后再验证是否有值
                if(!baseModel.getDataAuthority().isEmpty()){
                    metaStatementHandler.setValue("delegate.boundSql.sql", buildAuthoritySql(boundSql.getSql(), baseModel.getDataAuthority()));
                }
            }
        }
        if (method.getAnnotation(Page.class) != null) {
            BoundSql boundSql = (BoundSql) metaStatementHandler.getValue("delegate.boundSql");
            BaseModel baseModel = (BaseModel) boundSql.getParameterObject();
            if (baseModel.getRows() > 0) {
                baseModel.setBegin((baseModel.getPage() - 1) * baseModel.getRows());
                if(baseModel.getTotal()==0){
                    int totalCount = countTotal((Connection) invocation.getArgs()[0], mappedStatement, boundSql);
                    baseModel.setTotal(totalCount);
                }
                DataSource dataSource = mappedStatement.getConfiguration().getEnvironment().getDataSource();
                String dbType = dataSource.getConnection().getMetaData().getDriverName();
                metaStatementHandler.setValue("delegate.boundSql.sql", buildPageSql(boundSql.getSql(), baseModel,dbType));
            }
        }
        return invocation.proceed();
    }

    /**
     * 计算总记录数
     *
     * @param connection
     * @param mappedStatement
     * @param boundSql
     * @return
     * @throws Throwable
     */
    private int countTotal(Connection connection, MappedStatement mappedStatement, BoundSql boundSql) throws Throwable {
        String countSql = buildCountSql(boundSql.getSql());
        PreparedStatement countStmt = null;
        ResultSet rs = null;
        try {
            countStmt = connection.prepareStatement(countSql);
            BoundSql countBS = new BoundSql(mappedStatement.getConfiguration(), countSql, boundSql.getParameterMappings(), boundSql.getParameterObject());
            ParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement, countBS.getParameterObject(), countBS);
            parameterHandler.setParameters(countStmt);
            rs = countStmt.executeQuery();
            int totalCount = 0;
            if (rs.next()) {
                totalCount = rs.getInt(1);
            }
            return totalCount;
        } finally {
            rs.close();
            countStmt.close();
        }
    }

    /**
     * 创建统计总记录数sql
     *
     * @param sql
     * @return
     */
    private String buildCountSql(String sql) {
        StringBuilder sqlBuilder = new StringBuilder("select count(1) from (").append(sql).append(") as total ");
        return sqlBuilder.toString();
    }

    /**
     * 创建分页sql
     *
     * @param sql
     * @param baseModel
     * @return
     */
    private String buildPageSql(String sql, BaseModel baseModel, String dbType) {
        StringBuilder sqlBuilder = new StringBuilder();
        baseModel.setBegin((baseModel.getPage() - 1) * baseModel.getRows());
        baseModel.setEnd(baseModel.getPage() * baseModel.getRows());
        //默认oracle数据库
        if(dbType == null || "".equals(dbType) || dbType.toLowerCase().contains("oracle")){
            sqlBuilder.append("select * from(select h.*,rownum rn from (");
            sqlBuilder.append(sql);
            sqlBuilder.append(" ) h where rownum &lt;=").append(baseModel.getEnd()).append(") where rn> ").append(baseModel.getBegin());
        }else{
            if(dbType.toLowerCase().contains("mysql")){
                sqlBuilder.append(sql);
                sqlBuilder.append(" limit ").append(baseModel.getBegin()).append(",").append(baseModel.getEnd());
            }else {
                //todo 其他情况继续判断添加
                sqlBuilder.append(sql);
                sqlBuilder.append(" limit ").append(baseModel.getRows()).append(" offset ").append(baseModel.getBegin());
            }
        }
        return sqlBuilder.toString();
    }



    private String buildAuthoritySql(String sql,Map<String, String[]> dataAuthority){
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("select T.* from (").append(sql).append(") T where");
        Iterator iter = dataAuthority.entrySet().iterator();
        while (iter.hasNext()) {
            sqlBuilder.append(" ");
            Map.Entry<String,String[]> entry = (Map.Entry<String,String[]>) iter.next();
            sqlBuilder.append(entry.getKey());
            String[] values=entry.getValue();
            if(values.length==1){
                sqlBuilder.append("=").append("'").append(values[0]).append("'");
            }else{
                sqlBuilder.append(" in (");
                for(String value:values){
                    sqlBuilder.append("'").append(value).append("',");
                }
                if(sqlBuilder.toString().endsWith(",")){
                    sqlBuilder.delete(sqlBuilder.length()-1,sqlBuilder.length());
                }
                sqlBuilder.append(")");
            }
            sqlBuilder.append(" and");
        }
        if(sqlBuilder.toString().endsWith("and")){
            sqlBuilder.delete(sqlBuilder.length()-3,sqlBuilder.length());
        }
        return sqlBuilder.toString();
    }
}
