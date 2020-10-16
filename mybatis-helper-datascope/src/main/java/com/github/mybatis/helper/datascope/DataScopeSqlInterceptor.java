package com.github.mybatis.helper.datascope;

import com.github.mybatis.helper.core.MybatisUtils;
import com.github.mybatis.helper.core.sql.ExecuteHelper;
import com.github.mybatis.helper.core.sql.SqlInterceptor;
import com.github.mybatis.helper.datascope.annotation.ActiveScopeField;
import com.github.mybatis.helper.datascope.annotation.DataScopeSettings;
import com.github.mybatis.helper.datascope.annotation.DataScopeSqlStyle;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
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
 * 通过传入参数自动在sql拼上查询条件
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
            List<Map<String,Object[]>> dataScopeParamList=new ArrayList<>();
            List<Map<String,Object[]>> dataScopeList=new ArrayList<>();
            DataScopeSettings activeSettings=getSetting(mappedStatement.getId());
            if(filterParam instanceof List){
                dataScopeParamList=(List<Map<String,Object[]>>)filterParam;
            }else{
                dataScopeParamList.add((Map<String,Object[]>)filterParam);
            }
            //字段修改
            if(activeSettings.activeScopeFields()==null||activeSettings.activeScopeFields().length==0){
                dataScopeList=dataScopeParamList;
            }else{
                for(Map<String,Object[]> param:dataScopeParamList){
                    Map<String,Object[]> dataScopeMap=new HashMap<>();
                    for(int i=activeSettings.activeScopeFields().length-1;i>=0;i--){
                        ActiveScopeField activeScopeField=activeSettings.activeScopeFields()[i];
                        String key=activeScopeField.value();
                        if(param.containsKey(key)){
                            if(activeScopeField.columnName().length()==0){
                                dataScopeMap.put(key,param.get(activeScopeField.value()));
                            }else{
                                dataScopeMap.put(activeScopeField.columnName(),param.get(activeScopeField.value()));
                            }
                            if(activeSettings.onlyUseSmallScope()){
                                break;
                            }
                        }
                    }
                    if(dataScopeMap.size()>0){
                        dataScopeList.add(dataScopeMap);
                    }
                }
            }
            if(dataScopeList.size()==0){
                return originalSql;
            }
            //增加查询条件
            if(mappedStatement.getSqlCommandType()==SqlCommandType.SELECT){
                if(activeSettings.dataScopeSqlStyle()== DataScopeSqlStyle.INNER){
                    originalSql = innerDataScope(originalSql, dataScopeList);
                }else{
                    originalSql = outerDataScope(originalSql, dataScopeList, activeSettings);
                }
            }else if(mappedStatement.getSqlCommandType()==SqlCommandType.UPDATE){
                originalSql = updateDataScope(originalSql, dataScopeList);
            }else{
                originalSql = deleteDataScope(originalSql, dataScopeList);
            }
        }catch(Exception e){
            logger.error("数据范围解析错误！sql：{}，原因：{}",originalSql,e);
        }
        return originalSql;
    }

    @Override
    public boolean isContainsSqlCommandType(SqlCommandType sqlCommandType) {
        return sqlCommandType==SqlCommandType.SELECT||sqlCommandType==SqlCommandType.UPDATE||sqlCommandType==SqlCommandType.DELETE;
    }

    /**
     * 外部拼接sql查询
     * @param originalSql
     * @param dataScopeList
     * @return
     */
    private String outerDataScope(String originalSql, List<Map<String, Object[]>> dataScopeList,DataScopeSettings activeSettings) throws JSQLParserException {
        if(dataScopeList.size()>0){
            String selectItem=activeSettings.outerSqlStyleSettings().select()==null?"T.*":activeSettings.outerSqlStyleSettings().select();
            originalSql="SELECT "+selectItem+" FROM ("+originalSql+") T ";
            originalSql=selectDataScope(originalSql,dataScopeList);
        }
        return originalSql;
    }

    /**
     * 内部拼接sql查询
     * @param originalSql
     * @param dataScopeList
     * @return
     * @throws JSQLParserException
     */
    private String innerDataScope(String originalSql, List<Map<String, Object[]>> dataScopeList) throws JSQLParserException {
        return selectDataScope(originalSql,dataScopeList);
    }

    private String selectDataScope(String originalSql, List<Map<String, Object[]>> dataScopeList) throws JSQLParserException {
        Statement stmt = CCJSqlParserUtil.parse(originalSql);
        if(dataScopeList.size()>0) {
            if (stmt instanceof Select) {
                Select select = (Select) stmt;
                PlainSelect ps = (PlainSelect) select.getSelectBody();
                dataScopeList=convertAlias(stmt,dataScopeList);
                Expression dataScopeExpression=getDataScopeExpression(dataScopeList);
                if(ps.getWhere()!=null) {
                    dataScopeExpression=new AndExpression(ps.getWhere(),dataScopeExpression);
                }
                ps.setWhere(dataScopeExpression);
                originalSql=select.toString();
            }
        }
        return originalSql;
    }

    /**
     * 更新语句增加可见权限
     * @param originalSql
     * @param dataScopeList
     * @return
     * @throws JSQLParserException
     */
    private String updateDataScope(String originalSql, List<Map<String, Object[]>> dataScopeList) throws JSQLParserException {
        Statement stmt = CCJSqlParserUtil.parse(originalSql);
        if (stmt instanceof Update) {
            if(dataScopeList.size()>0){
                Update update = (Update) stmt;
                Expression dataScopeExpression=getDataScopeExpression(dataScopeList);
                if(update.getWhere()!=null) {
                    dataScopeExpression=new AndExpression(update.getWhere(),dataScopeExpression);
                }
                update.setWhere(dataScopeExpression);
            }
        }
        return stmt.toString();
    }

    /**
     * 删除语句增加可见权限
     * @param originalSql
     * @param dataScopeList
     * @return
     * @throws JSQLParserException
     */
    private String deleteDataScope(String originalSql, List<Map<String, Object[]>> dataScopeList) throws JSQLParserException {
        Statement stmt = CCJSqlParserUtil.parse(originalSql);
        if (stmt instanceof Delete) {
            if(dataScopeList.size()>0){
                Delete delete = (Delete) stmt;
                Expression dataScopeExpression=getDataScopeExpression(dataScopeList);
                if(delete.getWhere()!=null) {
                    dataScopeExpression=new AndExpression(delete.getWhere(),dataScopeExpression);
                }
                delete.setWhere(dataScopeExpression);
            }
        }
        return stmt.toString();
    }

    private List<Map<String, Object[]>> convertAlias(Statement stmt,List<Map<String, Object[]>> dataScopeList){
        List<Map<String, Object[]>> aliasDataScopeList=new ArrayList<>();
        for(Map<String, Object[]> dataScopeMap:dataScopeList){
            Map<String, Object[]> aliasDataScopeMap=new HashMap();
            for (Map.Entry<String, Object[]> dataScope : dataScopeMap.entrySet()) {
                if (dataScope.getValue() == null || dataScope.getValue().length == 0) {
                    continue;
                }
                String column = dataScope.getKey();
                String alias=getTableAlias(stmt, column);
                if(alias!=null){
                    column=String.join(".",alias,column);
                }
                aliasDataScopeMap.put(column,dataScope.getValue());
            }
            aliasDataScopeList.add(aliasDataScopeMap);
        }
        return aliasDataScopeList;
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
                if(selectItem instanceof SelectExpressionItem){
                    if(((SelectExpressionItem)selectItem).getExpression() instanceof Column){
                        Column c=(Column) (((SelectExpressionItem)selectItem).getExpression());
                        if(c.getColumnName().equalsIgnoreCase(column)){
                            return c.getTable().getName();
                        }
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

    private Expression getDataScopeExpression(List<Map<String, Object[]>> dataScopeList){
        Expression allExpression=null;
        for(int i=0;i<dataScopeList.size();i++){
            Map<String, Object[]> dataScopeMap=dataScopeList.get(i);
            Expression oneExpression=null;
            int index=0;
            for (Map.Entry<String, Object[]> dataScope : dataScopeMap.entrySet()) {
                if(dataScope.getValue()==null||dataScope.getValue().length==0){
                    continue;
                }
                String column = dataScope.getKey();
                Object[] values = dataScope.getValue();
                Expression expression= addConditionExpression(column,values);
                if(index==0){
                    oneExpression=expression;
                }else{
                    oneExpression=new AndExpression(oneExpression,expression);
                }
                index++;
            }
            if(dataScopeMap.size()>1){
                oneExpression=new Parenthesis(oneExpression);
            }
            if(i==0){
                allExpression=oneExpression;
            }else{
                allExpression=new OrExpression(allExpression,oneExpression);
            }
        }
        if(dataScopeList.size()>1){
            return new Parenthesis(allExpression);
        }else{
            return allExpression;
        }
    }

    /**
     * 创建一个 EqualsTo相同判断 条件
     *
     * @return “A=B” 单个where条件表达式
     */
    private Expression addConditionExpression(String column, Object[] value) {
        if(value.length>1){
            InExpression in=new InExpression();
            in.setLeftExpression(new Column(column));
            List<Expression> expList=new ArrayList<>();
            for(Object v:value){
                expList.add(convertValueExpression(v));
            }
            ExpressionList expressionList=new ExpressionList();
            expressionList.setExpressions(expList);
            in.setRightItemsList(expressionList);
            return in;
        }else{
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(new Column(column));
            equalsTo.setRightExpression(convertValueExpression(value[0]));
            return equalsTo;
        }
    }

    private Expression convertValueExpression(Object value){
        if(value instanceof String){
            return new StringValue(value.toString());
        }else if(value instanceof Date){
            return new DateValue(value.toString());
        }else if(value instanceof Double){
            return new DoubleValue(value.toString());
        }else{
            return new LongValue(value.toString());
        }
    }
}
