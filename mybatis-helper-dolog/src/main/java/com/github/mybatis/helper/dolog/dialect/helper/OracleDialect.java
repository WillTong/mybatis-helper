package com.github.mybatis.helper.dolog.dialect.helper;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;

/**
 * oracle断言.
 * @author will
 */
public class OracleDialect implements Dialect {

    @Override
    public Expression buildNowExpression() {
        return new Column("SYSDATE");
    }

    @Override
    public Expression buildUUIDExpression() {
        return new Column("sys_guid()");
    }
}
