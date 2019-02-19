package com.github.mybatis.helper.page.dialect.helper;

import org.apache.ibatis.session.RowBounds;

/**
 * mysql断言.
 * @author will
 */
public class MySqlDialect implements Dialect {

    @Override
    public String buildPageSql(String sql, RowBounds rowBounds) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append(sql);
        sqlBuilder.append(" limit ").append(rowBounds.getOffset()).append(",").append(rowBounds.getLimit());
        return sqlBuilder.toString();
    }
}
