package com.github.mybatis.helper.commonfield.dialect.helper;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import org.apache.ibatis.session.RowBounds;

/**
 * oracle断言.
 * @author will
 */
public class OracleDialect implements Dialect {

    @Override
    public Expression buildNowExpression() {
        return new Column("SYSDATE");
    }
}
