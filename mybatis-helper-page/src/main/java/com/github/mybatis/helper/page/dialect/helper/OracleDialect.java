package com.github.mybatis.helper.page.dialect.helper;

import org.apache.ibatis.session.RowBounds;

/**
 * oracle断言.
 * @author will
 */
public class OracleDialect implements Dialect {

    @Override
    public String buildPageSql(String sql, RowBounds rowBounds) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("select * from (select h.*,rownum rn from (");
        sqlBuilder.append(sql);
        sqlBuilder.append(" ) h where rownum <=").append(rowBounds.getOffset()+rowBounds.getLimit()).append(") where rn> ").append(rowBounds.getOffset());
        return sqlBuilder.toString();
    }
}
