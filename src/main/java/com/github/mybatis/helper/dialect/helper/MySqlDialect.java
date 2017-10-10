package com.github.mybatis.helper.dialect.helper;

import com.github.mybatis.helper.BaseModel;

/**
 * mysql断言.
 * @author will
 */
public class MySqlDialect implements Dialect {

    @Override
    public String buildPageSql(String sql, BaseModel baseModel) {
        StringBuilder sqlBuilder = new StringBuilder();
        baseModel.setBegin((baseModel.getPage() - 1) * baseModel.getRows());
        baseModel.setEnd(baseModel.getPage() * baseModel.getRows());
        sqlBuilder.append(sql);
        sqlBuilder.append(" limit ").append(baseModel.getBegin()).append(",").append(baseModel.getEnd());
        return sqlBuilder.toString();
    }
}
