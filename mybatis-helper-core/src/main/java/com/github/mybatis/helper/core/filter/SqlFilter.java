package com.github.mybatis.helper.core.filter;

import com.github.mybatis.helper.core.ExecuteHelper;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.RowBounds;

/**
 * sql拦截器
 * <p>用于规范sql拦截器写法</p>
 * @author will
 */
public interface SqlFilter {
    /**
     * 执行sql拦截
     * @param originalSql 原始sql
     * @param filterParam 拦截器参数
     * @param executeHelper sql执行工具类
     * @param mappedStatement
     * @param boundSql
     * @param rowBounds
     * @return
     */
    String doSqlFilter(String originalSql,Object filterParam, ExecuteHelper executeHelper, MappedStatement mappedStatement, BoundSql boundSql, RowBounds rowBounds);

    /**
     * 指定拦截器参数
     * @return
     */
    String getFilterParamName();

    /**
     * 指定拦截sql语句类型
     * @return
     */
    SqlCommandType[] getSqlCommandType();

    /**
     * MappedStatementId在列表内则执行
     * @return
     */
    String[] getIncludeMappedStatementIds();

    /**
     * MappedStatementId在列表内则不执行
     * @return
     */
    String[] getExcludeMappedStatementIds();
}
