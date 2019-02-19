package com.github.mybatis.helper.page;

import com.github.mybatis.helper.core.ExecuteHelper;
import com.github.mybatis.helper.core.MybatisUtils;
import com.github.mybatis.helper.core.filter.SqlFilter;
import com.github.mybatis.helper.page.annotation.PageCountSqlParserType;
import com.github.mybatis.helper.page.annotation.PageFilterSettings;
import com.github.mybatis.helper.page.dialect.DialectHandler;
import com.github.mybatis.helper.page.dialect.helper.Dialect;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 分页拦截器
 * @author will
 */
public class PageSqlFilter implements SqlFilter {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private Settings settings;
    private String[] includeMapperIds;
    private String[] excludeMapperIds;
    private Dialect dialect;

    private PageSqlFilter(Settings settings){
        this.settings=settings;
    }

    public static PageSqlFilter build(Settings settings){
        return new PageSqlFilter(settings);
    }

    public PageSqlFilter setIncludeMapperIds(String[] includeMapperIds){
        this.includeMapperIds=includeMapperIds;
        return this;
    }

    public PageSqlFilter setExcludeMapperIds(String[] excludeMapperIds){
        this.excludeMapperIds=excludeMapperIds;
        return this;
    }

    @Override
    public String doSqlFilter(String originalSql,Object filterParam, ExecuteHelper executeHelper, MappedStatement mappedStatement, BoundSql boundSql, RowBounds rowBounds) {
        if(rowBounds.getLimit()!=new RowBounds().getLimit()) {
            if(dialect==null){
                dialect = DialectHandler.getDialect(mappedStatement);
            }
            Long count = null;
            try {
            Settings activeSettings=generateSettings(mappedStatement.getId());
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
                logger.error("SQL无法解析");
            }
            return originalSql;
        }else{
            return originalSql;
        }

    }

    /**
     * 创建数量查询sql
     * @param sql
     * @param settings
     * @return
     * @throws JSQLParserException
     */
    private String buildCountSql(String sql,Settings settings) throws JSQLParserException {
        if(settings.getPageCountSqlParserType()==PageCountSqlParserType.INNER){
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

    @Override
    public String getFilterParamName() {
        return null;
    }

    @Override
    public SqlCommandType[] getSqlCommandType() {
        return new SqlCommandType[]{SqlCommandType.SELECT};
    }

    @Override
    public String[] getIncludeMappedStatementIds() {
        return includeMapperIds;
    }

    @Override
    public String[] getExcludeMappedStatementIds() {
        return excludeMapperIds;
    }

    /**
     * 通用配置和自定义配置合并
     * @param mappedStatementId
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     */
    private Settings generateSettings(String mappedStatementId) throws ClassNotFoundException {
        Class clazz= MybatisUtils.getClass(mappedStatementId);
        if(clazz.isAnnotationPresent(PageFilterSettings.class)){
            PageFilterSettings pageFilterSettings=(PageFilterSettings)clazz.getAnnotation(PageFilterSettings.class);
            return new Settings() {

                @Override
                public PageCountSqlParserType getPageCountSqlParserType() {
                    if(pageFilterSettings.pageCountSqlParserType()==null){
                        return settings.getPageCountSqlParserType();
                    }else{
                        return pageFilterSettings.pageCountSqlParserType();
                    }
                }
            };
        }else{
            return settings;
        }
    }

    public interface Settings {
        PageCountSqlParserType getPageCountSqlParserType();
    }
}
