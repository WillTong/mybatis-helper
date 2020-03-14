package com.github.mybatis.helper.commonfield.dialect.helper;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import org.apache.ibatis.session.RowBounds;

/**
 * mysql断言.
 * @author will
 */
public class MySqlDialect implements Dialect {
    @Override
    public Expression buildNowExpression() {
        return new Column("SYSDATE()");
    }
}
