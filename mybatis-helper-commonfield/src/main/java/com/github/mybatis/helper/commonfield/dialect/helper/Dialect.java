package com.github.mybatis.helper.commonfield.dialect.helper;

import net.sf.jsqlparser.expression.Expression;
import org.apache.ibatis.session.RowBounds;

/**
 * 断言.
 * @author will
 */
public interface Dialect {
    Expression buildNowExpression();
}
