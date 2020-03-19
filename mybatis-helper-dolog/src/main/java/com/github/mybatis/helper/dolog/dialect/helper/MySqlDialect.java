package com.github.mybatis.helper.dolog.dialect.helper;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;

/**
 * mysql断言.
 * @author will
 */
public class MySqlDialect implements Dialect {
    @Override
    public Expression buildNowExpression() {
        return new Column("SYSDATE()");
    }

    @Override
    public Expression buildUUIDExpression() {
        return new Column("uuid()");
    }
}
