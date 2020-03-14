package com.github.mybatis.helper.page;

import com.github.mybatis.helper.core.sql.ExecuteHelper;
import com.github.mybatis.helper.core.sql.SqlInterceptor;
import com.github.mybatis.helper.page.annotation.PageCountSqlParserType;
import com.github.mybatis.helper.page.annotation.PageSettings;
import com.github.mybatis.helper.page.dialect.DialectHandler;
import com.github.mybatis.helper.page.dialect.helper.Dialect;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 分页拦截器
 * @author will
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
@PageSettings
public class PageSqlInterceptor extends SqlInterceptor {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private Dialect dialect;

    @Override
    public String doSqlFilter(String originalSql,Object filterParam,MappedStatement mappedStatement,RowBounds rowBounds,BoundSql boundSql,ExecuteHelper executeHelper) {
        if(rowBounds.getLimit()!=new RowBounds().getLimit()) {
            if(dialect==null){
                dialect = DialectHandler.getDialect(mappedStatement);
            }
            Long count = null;
            try {
                PageSettings activeSettings=getSetting(mappedStatement.getId());
                count = executeHelper.exeQuery(new ExecuteHelper.Action() {
                @Override
                public Long doAction(ResultSet resultSet) throws SQLException {
                    Long totalRecord =0L;
                    if(resultSet.next()) {
                        totalRecord = resultSet.getLong(1);
                    }
                    return totalRecord;
                }
            },buildCountSql(originalSql,activeSettings),true);
            PagingBounds pagingBounds=(PagingBounds)rowBounds;
            String sql=dialect.buildPageSql(originalSql, rowBounds);
            pagingBounds.setDefault();
            pagingBounds.setTotal(count);
            return sql;
            } catch (Exception e) {
                logger.error("SQL无法解析，sql：{}，原因：{}",originalSql,e);
            }
            return originalSql;
        }else{
            return originalSql;
        }

    }

    @Override
    public boolean isContainsSqlCommandType(SqlCommandType sqlCommandType) {
        return sqlCommandType==SqlCommandType.SELECT;
    }

    /**
     * 创建数量查询sql
     * @param sql
     * @param settings
     * @return
     * @throws JSQLParserException
     */
    private String buildCountSql(String sql, PageSettings settings) throws JSQLParserException {
        if(settings.pageCountSqlParserType()==PageCountSqlParserType.INNER){
            Select select = (Select) CCJSqlParserUtil.parse(sql);
            PlainSelect plainSelect=(PlainSelect)select.getSelectBody();
            plainSelect.getSelectItems().clear();
            plainSelect.getSelectItems().add(new SelectExpressionItem(new Column("count(1)")));
            return select.toString();
        }else{
            StringBuilder sqlBuilder = new StringBuilder("select count(1) from (").append(sql).append(")");
            return sqlBuilder.toString();
        }
    }
}
