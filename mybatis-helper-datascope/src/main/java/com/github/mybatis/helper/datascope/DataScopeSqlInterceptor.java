package com.github.mybatis.helper.datascope;

import com.github.mybatis.helper.core.MybatisUtils;
import com.github.mybatis.helper.core.sql.ExecuteHelper;
import com.github.mybatis.helper.core.sql.SqlInterceptor;
import com.github.mybatis.helper.datascope.annotation.ActiveScopeField;
import com.github.mybatis.helper.datascope.annotation.DataScopeSettings;
import com.github.mybatis.helper.datascope.annotation.DataScopeSqlStyle;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.*;

/**
 * 数据权限拦截器
 * <p>通过传入参数自动在sql拼上查询条件</p>
 * <p>拦截器传入参数类型为：Map<String,Object[]></p>
 * @author will
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
@DataScopeSettings
public class DataScopeSqlInterceptor extends SqlInterceptor {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DataScopeSqlInterceptor() {
        super();
        paramName="dataScope";
    }

    @Override
    public String doSqlFilter(String originalSql, Object filterParam, MappedStatement mappedStatement, RowBounds rowBounds, BoundSql boundSql, ExecuteHelper executeHelper) {
        try{
            Map<String,Object[]> dataScopeMap=new HashMap<>();
            DataScopeSettings activeSettings=getSetting(mappedStatement.getId());
            if(activeSettings.activeScopeFields()==null||activeSettings.activeScopeFields().length==0){
                dataScopeMap=(Map<String,Object[]>)filterParam;
            }else{
                for(ActiveScopeField activeScopeField:activeSettings.activeScopeFields()){
                    Map<String,Object[]> filterParamMap=(Map<String,Object[]>)filterParam;
                    String key=activeScopeField.value();
                    if(filterParamMap.containsKey(key)){
                        if(activeScopeField.columnName().length()==0){
                            dataScopeMap.put(key,filterParamMap.get(activeScopeField.value()));
                        }else{
                            dataScopeMap.put(activeScopeField.columnName(),filterParamMap.get(activeScopeField.value()));
                        }
                    }
                }
            }
            if(dataScopeMap.size()==0){
                return originalSql;
            }
            //增加查询条件
            if(activeSettings.dataScopeSqlStyle()== DataScopeSqlStyle.INNER){
                originalSql = innerDataScope(originalSql, dataScopeMap);
            }else{
                originalSql = outerDataScope(originalSql, dataScopeMap, activeSettings);
            }
        }catch(Exception e){
            logger.error("数据范围解析错误！sql：{}，原因：{}",originalSql,e);
        }
        return originalSql;
    }

    /**
     * 通用配置和自定义配置合并
     * @param mappedStatementId
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     */
    private DataScopeSettings generateSettings(String mappedStatementId) {
        DataScopeSettings myDataScopeFilterSettings = null;
        try {
            Class clazz = MybatisUtils.getClass(mappedStatementId);
            Method method = MybatisUtils.getMethod(mappedStatementId);
            if (clazz.isAnnotationPresent(DataScopeSettings.class)) {
                myDataScopeFilterSettings = (DataScopeSettings) clazz.getAnnotation(DataScopeSettings.class);
            } else {
                myDataScopeFilterSettings = (DataScopeSettings) method.getAnnotation(DataScopeSettings.class);
            }
        } catch (NoSuchMethodException | ClassNotFoundException e) {

        }
        if (myDataScopeFilterSettings == null) {
            return this.getClass().getAnnotation(DataScopeSettings.class);
        } else {
            return myDataScopeFilterSettings;
        }
    }

    @Override
    public boolean isContainsSqlCommandType(SqlCommandType sqlCommandType) {
        return sqlCommandType==SqlCommandType.SELECT;
    }

    private String outerDataScope(String originalSql, Map<String, Object[]> dataScopeMap,DataScopeSettings activeSettings) {
        StringBuilder sqlBuilder = new StringBuilder();
        for(Map.Entry<String,Object[]> entry:dataScopeMap.entrySet()) {
            sqlBuilder.append(this.addFeild(entry.getKey(),entry.getValue()));
        }
        if(sqlBuilder.length()!=0){
            String selectItem=activeSettings.outerSqlStyleSettings().select()==null?"T.*":activeSettings.outerSqlStyleSettings().select();
            sqlBuilder.insert(0,"select "+selectItem+" from ("+originalSql+") T where");
            if(sqlBuilder.toString().endsWith("and")){
                sqlBuilder.delete(sqlBuilder.length()-3,sqlBuilder.length());
            }
            originalSql=sqlBuilder.toString();
        }
        return originalSql;
    }

    private String addFeild(String column,Object[] values){
        String feildSplit=",";
        StringBuffer sqlBuilder=new StringBuffer();
        sqlBuilder.append(" ");
        sqlBuilder.append(column);
        if(values.length ==1){
            sqlBuilder.append("=").append(addValue(values[0]));
        }else{
            sqlBuilder.append(" in (");
            for(Object value:values){
                sqlBuilder.append(addValue(value)).append(feildSplit);
            }
            if(sqlBuilder.toString().endsWith(feildSplit)){
                sqlBuilder.delete(sqlBuilder.length()-1,sqlBuilder.length());
            }
            sqlBuilder.append(")");
        }
        sqlBuilder.append(" and");
        return sqlBuilder.toString();
    }

    private String addValue(Object value){
        if(value instanceof String){
            return "'"+value+"'";
        }else{
            return value.toString();
        }
    }



    private String innerDataScope(String originalSql, Map<String, Object[]> dataScopeMap){
        try{
            Statement stmt = CCJSqlParserUtil.parse(originalSql);
            if (stmt instanceof Select) {
                Select select = (Select) stmt;
                PlainSelect ps = (PlainSelect) select.getSelectBody();
                TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
                List<String> tableList = tablesNamesFinder.getTableList(select);
                if (tableList.size() == 0) {
                    return select.toString();
                }
                for (Map.Entry<String, Object[]> dataScope : dataScopeMap.entrySet()) {
                    if(dataScope.getValue()==null||dataScope.getValue().length==0){
                        continue;
                    }
                    String column=dataScope.getKey();
                    Object[] values=dataScope.getValue();
                    if (ps.getWhere() != null) {
                        String aliasName;
                        aliasName = getTableAlias(stmt, column);
                        aliasName=aliasName==null?"":aliasName+".";
                        column=aliasName+column;
                        if(((PlainSelect)((Select)stmt).getSelectBody()).getWhere().toString().indexOf(column)==-1){
                            AndExpression andExpression = addAndExpression(stmt, ps.getWhere(), column,values);
                            // form 和 join 中加载的表
                            if (andExpression != null) {
                                ps.setWhere(andExpression);
                            } else {
                                //子查询中的表
                                findSubSelect(stmt, ps.getWhere(), column,values);
                            }
                        }
                    } else {
                        ps.setWhere(addEqualsTo(stmt, column,values));
                    }
                }
                return select.toString();
            }
        }catch(Exception e){
            logger.error("sql解析错误！",e);
        }
        return originalSql;
    }

    /**
     * 多条件情况下，使用AndExpression给where条件加上tenantid条件
     *
     * @param where
     * @return
     * @throws Exception
     */
    private AndExpression addAndExpression(Statement stmt, Expression where, String column, Object[] value) throws Exception {
        Expression expression = addEqualsTo(stmt ,column,value);
        if (expression != null) {
            return new AndExpression(expression, where);
        } else {
            return null;
        }
    }

    /**
     * 创建一个 EqualsTo相同判断 条件
     *
     * @param stmt  查询对象
     * @return “A=B” 单个where条件表达式
     * @throws Exception
     */
    private Expression addEqualsTo(Statement stmt,String column,Object[] value) throws Exception {
        if(value.length>1){
            InExpression in=new InExpression();
            in.setLeftExpression(new Column(column));
            List<Expression> expList=new ArrayList<>();
            for(Object v:value){
                if(v instanceof String){
                    expList.add(new StringValue(v.toString()));
                }else if(v instanceof Date){
                    expList.add(new DateValue(v.toString()));
                }else if(v instanceof Double){
                    expList.add(new DoubleValue(v.toString()));
                }else{
                    expList.add(new LongValue(v.toString()));
                }
            }
            ExpressionList expressionList=new ExpressionList();
            expressionList.setExpressions(expList);
            in.setRightItemsList(expressionList);
            return in;
        }else{
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(new Column(column));
            if(value[0] instanceof String){
                equalsTo.setRightExpression(new StringValue(value[0].toString()));
            }else if(value[0] instanceof Date){
                equalsTo.setRightExpression(new DateValue(value[0].toString()));
            }else if(value[0] instanceof Double){
                equalsTo.setRightExpression(new DoubleValue(value[0].toString()));
            }else{
                equalsTo.setRightExpression(new LongValue(value[0].toString()));
            }
            return equalsTo;
        }
    }
    /**
     * 获取sql送指定表的别名你，没有别名则返回原表名 如果表名不存在返回null
     * 【仅查询from和join 不含 IN 子查询中的表 】
     *
     * @param stmt
     * @return
     */
    private String getTableAlias(Statement stmt, String column) {
        String alias = null;

        if (stmt instanceof Select) {
            Select select = (Select) stmt;

            PlainSelect ps = (PlainSelect) select.getSelectBody();
            //select中是否可以找到别名
            for(SelectItem selectItem:ps.getSelectItems()){
                if(((SelectExpressionItem)selectItem).getExpression() instanceof Column){
                    Column c=(Column) (((SelectExpressionItem)selectItem).getExpression());
                    if(c.getColumnName().equalsIgnoreCase(column)){
                        return c.getTable().getName();
                    }
                }
            }
            // 判断主表的别名
            if(ps.getFromItem().getAlias()!=null){
                return ps.getFromItem().getAlias().getName();
            }
        }
        return alias;
    }

    /**
     * 递归处理 子查询中的tenantid-where
     *
     * @param stmt  sql查询对象
     * @param where 当前sql的where条件 where为AndExpression或OrExpression的实例，解析其中的rightExpression，然后检查leftExpression是否为空，
     *              不为空则是AndExpression或OrExpression，再次解析其中的rightExpression
     *              注意tenantid-where是加在子查询上的
     */
    private void findSubSelect(Statement stmt, Expression where,String column,Object[] value) throws Exception {

        // and 表达式
        if (where instanceof AndExpression) {
            AndExpression andExpression = (AndExpression) where;
            if (andExpression.getRightExpression() instanceof SubSelect) {
                SubSelect subSelect = (SubSelect) andExpression.getRightExpression();
                doSelect(stmt, subSelect,column,value);
            }
            if (andExpression.getLeftExpression() != null) {
                findSubSelect(stmt, andExpression.getLeftExpression(),column,value);
            }
        } else if (where instanceof OrExpression) {
            //  or表达式
            OrExpression orExpression = (OrExpression) where;
            if (orExpression.getRightExpression() instanceof SubSelect) {
                SubSelect subSelect = (SubSelect) orExpression.getRightExpression();
                doSelect(stmt, subSelect,column,value);
            }
            if (orExpression.getLeftExpression() != null) {
                findSubSelect(stmt, orExpression.getLeftExpression(),column,value);
            }
        }
    }

    /**
     * 处理select 和 subSelect
     *
     * @param stmt   查询对象
     * @param select
     * @return
     * @throws Exception
     */
    private Expression doSelect(Statement stmt, Expression select,String column,Object[] value) throws Exception {
        PlainSelect ps = null;
        boolean hasSubSelect = false;

        if (select instanceof SubSelect) {
            ps = (PlainSelect) ((SubSelect) select).getSelectBody();
        }
        if (select instanceof Select) {
            ps = (PlainSelect) ((Select) select).getSelectBody();
        }

        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        List<String> tableList = tablesNamesFinder.getTableList(select);
        if (tableList.size() == 0) {
            return select;
        }
        for (String table : tableList) {
            // sql 包含 where 条件的情况 使用 addAndExpression 连接 已有的条件和新条件
            if (ps.getWhere() == null) {
                AndExpression where = addAndExpression(stmt, ps.getWhere(),column,value);
                // form 和 join 中加载的表
                if (where != null) {
                    ps.setWhere(where);
                } else {
                    // 如果在Statement中不存在这个表名，则存在于子查询中
                    hasSubSelect = true;
                }
            } else {
                // sql 不含 where条件 新建一个EqualsTo设置为where条件
                Expression expression = addEqualsTo(stmt,column,value);
                ps.setWhere(expression);
            }
        }

        if (hasSubSelect) {
            //子查询中的表
            findSubSelect(stmt, ps.getWhere(),column,value);
        }
        return select;
    }
}
