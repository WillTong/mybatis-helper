package com.github.mybatis.helper.dolog;

import com.alibaba.fastjson.JSONObject;
import com.github.mybatis.helper.core.ReflectUtil;
import com.github.mybatis.helper.core.sql.ExecuteHelper;
import com.github.mybatis.helper.core.sql.SqlInterceptor;
import com.github.mybatis.helper.dolog.annotation.DoLogSettings;
import com.github.mybatis.helper.dolog.annotation.PrimarykeyGenerationStrategy;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 创建或更新时自动插入日志表
 * @author will
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
@DoLogSettings
public class DoLogSqlInterceptor extends SqlInterceptor {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DoLogSqlInterceptor() {
        super();
        paramName="userId";
    }

    private static final SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String doSqlFilter(String originalSql, Object filterParam, MappedStatement mappedStatement, RowBounds rowBounds, BoundSql boundSql, ExecuteHelper executeHelper) {
        try{

            DoLogSettings activeSettings=getSetting(mappedStatement.getId());
            Object parameter=boundSql.getParameterObject();
            if(parameter instanceof MapperMethod.ParamMap){
                throw new Exception("目前只支持单个入参！");
            }

            List<ParameterMapping> parameterMappingList=boundSql.getParameterMappings();

            Insert insert=new Insert();
            List<Column> columnList=new ArrayList<>();
            List<Expression> expressions=new ArrayList<>();

            if(activeSettings.logTypeColumn()!=null){
                columnList.add(new Column(activeSettings.logTypeColumn()));
                expressions.add(new StringValue(mappedStatement.getSqlCommandType().name()));
            }

            if(activeSettings.logPersionColumn()!=null){
                columnList.add(new Column(activeSettings.logPersionColumn()));
                if(filterParam instanceof String){
                    expressions.add(new StringValue(filterParam.toString()));
                }else{
                    expressions.add(new LongValue(filterParam.toString()));
                }
            }

            if(activeSettings.logDateColumn()!=null){
                columnList.add(new Column(activeSettings.logDateColumn()));
                expressions.add(new TimestampValue(" "+simpleDateFormat.format(new Date())+" "));
            }
            if(mappedStatement.getSqlCommandType()==SqlCommandType.INSERT){
                //插入
                Insert statement=(Insert) CCJSqlParserUtil.parse(originalSql);
                String recordTableName=statement.getTable().getName();

                String logTableName=generateTableName(activeSettings, recordTableName);
                insert.setTable(new Table(logTableName));

                if(activeSettings.logPrimaryKeyColumn()!=null){
                    columnList.add(new Column(activeSettings.logPrimaryKeyColumn()));
                    if(activeSettings.primarykeyGenerationStrategy()!=null){
                        if(activeSettings.primarykeyGenerationStrategy()==PrimarykeyGenerationStrategy.SEQUENCE){
                            expressions.add(new HexValue("SEQ_"+logTableName+".NEXTVAL"));
                        }
                    }
                }

                if(activeSettings.logRecordTableNameColumn()!=null){
                    columnList.add(new Column(activeSettings.logRecordTableNameColumn()));
                    expressions.add(new StringValue(recordTableName));
                }

                if(activeSettings.logRecordPrimaryKeyColumn()!=null){
                    columnList.add(new Column(activeSettings.logRecordPrimaryKeyColumn()));
                    int index=0;
                    for(int i=0;i<statement.getColumns().size();i++){
                        Column column=statement.getColumns().get(i);
                        Expression expression=((ExpressionList) statement.getItemsList()).getExpressions().get(i);
                        if(column.getColumnName().equals(activeSettings.recordPrimaryKey())){
                            if(expression instanceof JdbcParameter) {
                                if(parameterMappingList.get(index).getJavaType().getName().equals(String.class.getName())){
                                    expressions.add(new StringValue(ReflectUtil.getFieldValue(parameter,parameterMappingList.get(index).getProperty()).toString()));
                                }else {
                                    expressions.add(new LongValue(ReflectUtil.getFieldValue(parameter,parameterMappingList.get(index).getProperty()).toString()));
                                }
                            }else {
                                expressions.add(expression);
                            }
                            break;
                        }
                        if(expression instanceof JdbcParameter) {
                            index++;
                        }
                    }
                }

                if(activeSettings.logContentColumn()!=null){
                    columnList.add(new Column(activeSettings.logContentColumn()));
                    Map<String,Object> jsonMap=new HashMap<>();
                    int index=0;
                    for(int i=0;i<statement.getColumns().size();i++){
                        Column column=statement.getColumns().get(i);
                        Expression expression=((ExpressionList) statement.getItemsList()).getExpressions().get(i);
                        if(expression instanceof JdbcParameter){
                            jsonMap.put(column.getColumnName(),ReflectUtil.getFieldValue(parameter,parameterMappingList.get(index).getProperty()));
                            index++;
                        }else{
                            jsonMap.put(column.getColumnName(),expression.toString());
                        }
                    }
                    expressions.add(new StringValue(JSONObject.toJSONString(jsonMap)));
                }
            }else if(mappedStatement.getSqlCommandType()==SqlCommandType.UPDATE){
                //更新
                Update statement=(Update) CCJSqlParserUtil.parse(originalSql);
                String recordTableName=statement.getTables().get(0).getName();

                String logTableName=generateTableName(activeSettings,recordTableName);
                insert.setTable(new Table(logTableName));

                if(activeSettings.logPrimaryKeyColumn()!=null){
                    columnList.add(new Column(activeSettings.logPrimaryKeyColumn()));
                    if(activeSettings.primarykeyGenerationStrategy()!=null){
                        if(activeSettings.primarykeyGenerationStrategy()== PrimarykeyGenerationStrategy.SEQUENCE){
                            expressions.add(new HexValue("SEQ_"+logTableName+".NEXTVAL"));
                        }
                    }
                }

                if(activeSettings.logRecordTableNameColumn()!=null){
                    columnList.add(new Column(activeSettings.logRecordTableNameColumn()));
                    expressions.add(new StringValue(recordTableName));
                }

                Expression recordPrimaryKeyExpression=null;
                if(activeSettings.logRecordPrimaryKeyColumn()!=null){
                    columnList.add(new Column(activeSettings.logRecordPrimaryKeyColumn()));
                    ParameterMapping parameterMapping=parameterMappingList.get(parameterMappingList.size()-1);
                    if(parameterMapping.getJavaType().getName().equals(String.class.getName())){
                        recordPrimaryKeyExpression=new StringValue(ReflectUtil.getFieldValue(parameter,parameterMapping.getProperty()).toString());
                        expressions.add(recordPrimaryKeyExpression);
                    }else {
                        recordPrimaryKeyExpression=new LongValue(ReflectUtil.getFieldValue(parameter,parameterMapping.getProperty()).toString());
                        expressions.add(recordPrimaryKeyExpression);
                    }
                }

                if(activeSettings.logContentColumn()!=null){
                    columnList.add(new Column(activeSettings.logContentColumn()));
                    Map<String,Object> jsonMap=new HashMap<>();
                    int index=0;
                    for(int i=0;i<statement.getColumns().size();i++){
                        Column column=statement.getColumns().get(i);
                        Expression expression=statement.getExpressions().get(i);
                        if(expression instanceof JdbcParameter){
                            jsonMap.put(column.getColumnName(),ReflectUtil.getFieldValue(parameter,parameterMappingList.get(index).getProperty()));
                            index++;
                        }else{
                            jsonMap.put(column.getColumnName(),expression.toString());
                        }
                    }
                    expressions.add(new StringValue(JSONObject.toJSONString(jsonMap)));
                }

                if(activeSettings.logRecordContentColumn()!=null&&activeSettings.logRecordPrimaryKeyColumn()!=null){
                    columnList.add(new Column(activeSettings.logRecordContentColumn()));
                    List<SelectItem> selectColumnList=new ArrayList<>();
                    for(int i=0;i<statement.getColumns().size();i++){
                        selectColumnList.add(new SelectExpressionItem(statement.getColumns().get(i)));
                    }
                    PlainSelect plainSelect=new PlainSelect();
                    plainSelect.setSelectItems(selectColumnList);
                    plainSelect.setFromItem(new Table(recordTableName));
                    EqualsTo equalsTo=new EqualsTo();
                    equalsTo.setLeftExpression(new Column(activeSettings.recordPrimaryKey()));
                    equalsTo.setRightExpression(recordPrimaryKeyExpression);
                    plainSelect.setWhere(equalsTo);
                    Select select = new Select();
                    select.setSelectBody(plainSelect);
                    Map<String,Object> jsonMap=executeHelper.exeQuery(new ExecuteHelper.Action() {
                        @Override
                        public Map<String,Object> doAction(ResultSet resultSet) throws SQLException {
                            Map<String,Object> jsonMap=new HashMap<>();
                            if(resultSet.next()) {
                                for(Column column:statement.getColumns()){
                                    jsonMap.put(column.toString(),resultSet.getObject(column.toString()));
                                }
                            }
                            return jsonMap;
                        }
                    },select.toString(),false);
                    expressions.add(new StringValue(JSONObject.toJSONString(jsonMap)));
                }
            }else{
                //删除
                Delete statement=(Delete) CCJSqlParserUtil.parse(originalSql);
                String recordTableName=statement.getTable().getName();

                String logTableName=generateTableName(activeSettings,recordTableName);
                insert.setTable(new Table(logTableName));

                if(activeSettings.logPrimaryKeyColumn()!=null){
                    columnList.add(new Column(activeSettings.logPrimaryKeyColumn()));
                    if(activeSettings.primarykeyGenerationStrategy()!=null){
                        if(activeSettings.primarykeyGenerationStrategy()==PrimarykeyGenerationStrategy.SEQUENCE){
                            expressions.add(new HexValue("SEQ_"+logTableName+".NEXTVAL"));
                        }
                    }
                }

                if(activeSettings.logRecordTableNameColumn()!=null){
                    columnList.add(new Column(activeSettings.logRecordTableNameColumn()));
                    expressions.add(new StringValue(recordTableName));
                }

                Expression recordPrimaryKeyExpression=null;
                if(activeSettings.logRecordPrimaryKeyColumn()!=null){
                    columnList.add(new Column(activeSettings.logRecordPrimaryKeyColumn()));
                    if(parameter instanceof String){
                        recordPrimaryKeyExpression=new StringValue(parameter.toString());
                        expressions.add(recordPrimaryKeyExpression);
                    }else{
                        recordPrimaryKeyExpression=new LongValue(parameter.toString());
                        expressions.add(recordPrimaryKeyExpression);
                    }
                }

                if(activeSettings.logRecordContentColumn()!=null&&activeSettings.logRecordPrimaryKeyColumn()!=null){
                    columnList.add(new Column(activeSettings.logRecordContentColumn()));
                    PlainSelect plainSelect=new PlainSelect();
                    plainSelect.addSelectItems(new AllColumns());
                    plainSelect.setFromItem(new Table(recordTableName));
                    EqualsTo equalsTo=new EqualsTo();
                    equalsTo.setLeftExpression(new Column(activeSettings.recordPrimaryKey()));
                    equalsTo.setRightExpression(recordPrimaryKeyExpression);
                    plainSelect.setWhere(equalsTo);
                    Select select = new Select();
                    select.setSelectBody(plainSelect);
                    Map<String,Object> jsonMap=executeHelper.exeQuery(new ExecuteHelper.Action() {
                        @Override
                        public Map<String,Object> doAction(ResultSet resultSet) throws SQLException {
                            Map<String,Object> jsonMap=new HashMap<>();
                            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                            if(resultSet.next()) {
                                for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                                    if(resultSet.getObject(i) instanceof java.sql.Date){
                                        jsonMap.put(resultSetMetaData.getColumnName(i),simpleDateFormat.format(new Date(((java.sql.Timestamp)resultSet.getTimestamp(i)).getTime())));
                                    }else{
                                        jsonMap.put(resultSetMetaData.getColumnName(i),resultSet.getObject(i));
                                    }
                                }
                            }
                            return jsonMap;
                        }
                    },select.toString(),false);
                    expressions.add(new StringValue(JSONObject.toJSONString(jsonMap)));
                }
            }

            insert.setColumns(columnList);
            ExpressionList expressionList=new ExpressionList();
            expressionList.setExpressions(expressions);
            insert.setItemsList(expressionList);

            executeHelper.exeUpdate(insert.toString(),false);
        }catch (Exception e){
            logger.error("无法生成操作日志，sql：{}，原因：{}",originalSql,e);
        }
        return originalSql;
    }

    private String generateTableName(DoLogSettings settings, String recordTableName) {
        if(settings.logTableName()!=null){
            return settings.logTableName();
        }
        StringBuilder tableNameBuilder=new StringBuilder();
        if(settings.logTableNamePrefix()!=null){
            tableNameBuilder.append(settings.logTableNamePrefix());
        }
        tableNameBuilder.append(recordTableName);
        if(settings.logTableNamePostfix()!=null){
            tableNameBuilder.append(settings.logTableNamePostfix());
        }
        return tableNameBuilder.toString();
    }

    @Override
    public boolean isContainsSqlCommandType(SqlCommandType sqlCommandType) {
        return sqlCommandType==SqlCommandType.DELETE||sqlCommandType==SqlCommandType.UPDATE||sqlCommandType==SqlCommandType.INSERT;
    }
}
