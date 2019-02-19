package com.github.mybatis.helper.page.dialect.helper;

import org.apache.ibatis.session.RowBounds;

/**
 * postgre断言.
 * @author will
 */
public class PostgreSqlDialect implements Dialect {

    @Override
    public String buildPageSql(String sql,RowBounds rowBounds) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append(sql);
        sqlBuilder.append(" limit ").append(rowBounds.getLimit()).append(" offset ").append(rowBounds.getOffset());
        return sqlBuilder.toString();
    }
}
