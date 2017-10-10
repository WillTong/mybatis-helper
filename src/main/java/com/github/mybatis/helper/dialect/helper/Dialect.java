package com.github.mybatis.helper.dialect.helper;

import com.github.mybatis.helper.BaseModel;

/**
 * 断言.
 * @author will
 */
public interface Dialect {
    String buildPageSql(String sql, BaseModel baseModel);
}
