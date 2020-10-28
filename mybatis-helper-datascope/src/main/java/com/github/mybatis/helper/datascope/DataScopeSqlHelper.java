package com.github.mybatis.helper.datascope;

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
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DataScopeSqlHelper {

    private static Logger logger = LoggerFactory.getLogger(DataScopeSqlHelper.class);

    /**
     * 外部拼接sql查询
     * @param originalSql
     * @param dataScopeList
     * @return
     */
    public static String selectOuterDataScope(String originalSql, List<Map<String, Object[]>> dataScopeList, String selectItem) throws JSQLParserException {
        if(dataScopeList.size()>0){
            originalSql="SELECT "+selectItem+" FROM ("+originalSql+") T ";
            originalSql=selectInnerDataScope(originalSql,dataScopeList);
        }
        return originalSql;
    }

    /**
     * 内部拼接sql查询
     * @param originalSql
     * @param dataScopeList
     * @return
     */
    public static String selectInnerDataScope(String originalSql, List<Map<String, Object[]>> dataScopeList){
        try {
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
        } catch (Exception e) {
            logger.error("数据范围解析错误！sql：{}，原因：{}",originalSql,e);
        }
        return originalSql;
    }

    /**
     * 更新语句增加可见权限
     * @param originalSql
     * @param dataScopeList
     * @return
     */
    public static String updateInnerDataScope(String originalSql, List<Map<String, Object[]>> dataScopeList) {
        try {
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
        } catch (Exception e) {
            logger.error("数据范围解析错误！sql：{}，原因：{}",originalSql,e);
        }
        return originalSql;
    }

    /**
     * 删除语句增加可见权限
     * @param originalSql
     * @param dataScopeList
     * @return
     */
    public static String deleteInnerDataScope(String originalSql, List<Map<String, Object[]>> dataScopeList){
        try{
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
        } catch (Exception e) {
            logger.error("数据范围解析错误！sql：{}，原因：{}",originalSql,e);
        }
        return originalSql;
    }

    private static List<Map<String, Object[]>> convertAlias(Statement stmt,List<Map<String, Object[]>> dataScopeList){
        List<Map<String, Object[]>> aliasDataScopeList=new ArrayList<>();
        for(Map<String, Object[]> dataScopeMap:dataScopeList){
            Map<String, Object[]> aliasDataScopeMap=new HashMap();
            for (Map.Entry<String, Object[]> dataScope : dataScopeMap.entrySet()) {
                if (dataScope.getValue() == null || dataScope.getValue().length == 0) {
                    continue;
                }
                String column = dataScope.getKey();
                if(column.indexOf(".")==-1){
                    String alias=getTableAlias(stmt, column);
                    if(alias!=null){
                        column=String.join(".",alias,column);
                    }
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
    private static String getTableAlias(Statement stmt, String column) {
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

    private static Expression getDataScopeExpression(List<Map<String, Object[]>> dataScopeList){
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
    private static Expression addConditionExpression(String column, Object[] value) {
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

    private static Expression convertValueExpression(Object value){
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
