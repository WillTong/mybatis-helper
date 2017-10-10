package com.github.mybatis.helper.dialect.helper;

import com.github.mybatis.helper.BaseModel;

/**
 * oracle断言.
 * @author will
 */
public class OracleDialect implements Dialect {

    @Override
    public String buildPageSql(String sql, BaseModel baseModel) {
        StringBuilder sqlBuilder = new StringBuilder();
        baseModel.setBegin((baseModel.getPage() - 1) * baseModel.getRows());
        baseModel.setEnd(baseModel.getPage() * baseModel.getRows());
        sqlBuilder.append("select * from (select h.*,rownum rn from (");
        sqlBuilder.append(sql);
        sqlBuilder.append(" ) h where rownum <=").append(baseModel.getEnd()).append(") where rn> ").append(baseModel.getBegin());
        return sqlBuilder.toString();
    }
}
