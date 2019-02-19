package com.github.mybatis.helper.page.dialect.helper;

import org.apache.ibatis.session.RowBounds;

/**
 * 断言.
 * @author will
 */
public interface Dialect {
    /**
     * 生成分页sql
     * @param sql 原sql
     * @param rowBounds 分页参数
     * @return 生成后的参数
     */
    String buildPageSql(String sql, RowBounds rowBounds);
}
