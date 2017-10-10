package com.github.mybatis.helper.dialect.helper;

import com.github.mybatis.helper.BaseModel;

/**
 * postgre断言.
 * @author will
 */
public class PostgreSqlDialect implements Dialect {

    @Override
    public String buildPageSql(String sql, BaseModel baseModel) {
        StringBuilder sqlBuilder = new StringBuilder();
        baseModel.setBegin((baseModel.getPage() - 1) * baseModel.getRows());
        baseModel.setEnd(baseModel.getPage() * baseModel.getRows());
        sqlBuilder.append(sql);
        sqlBuilder.append(" limit ").append(baseModel.getRows()).append(" offset ").append(baseModel.getBegin());
        return sqlBuilder.toString();
    }
}
