package com.github.mybatis.helper.dolog.dialect.helper;

import net.sf.jsqlparser.expression.Expression;

/**
 * 断言.
 * @author will
 */
public interface Dialect {
    Expression buildNowExpression();

    Expression buildUUIDExpression();
}
