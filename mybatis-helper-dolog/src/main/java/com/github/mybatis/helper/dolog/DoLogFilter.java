package com.github.mybatis.helper.dolog;

import com.alibaba.fastjson.JSONObject;
import com.github.mybatis.helper.core.ExecuteHelper;
import com.github.mybatis.helper.core.MybatisUtils;
import com.github.mybatis.helper.core.ReflectUtil;
import com.github.mybatis.helper.core.filter.SqlFilter;
import com.github.mybatis.helper.dolog.annotation.DoLogSettings;
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
import net.sf.jsqlparser.util.SelectUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SelectionKey;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 创建或更新时自动插入日志表
 * @author will
 */
public class DoLogFilter implements SqlFilter {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private String filterParamName;
    private Settings settings;
    private String[] includeMapperIds;
    private String[] excludeMapperIds;
    private SqlCommandType[] sqlCommandTypes;
    private static final SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private DoLogFilter(String filterParamName,Settings settings){
        this.filterParamName=filterParamName;
        this.settings=settings;
        this.sqlCommandTypes=new SqlCommandType[]{SqlCommandType.INSERT,SqlCommandType.UPDATE,SqlCommandType.DELETE};
    }

    public static DoLogFilter build(String filterParamName,Settings settings){
        return new DoLogFilter(filterParamName,settings);
    }

    public DoLogFilter setIncludeMapperIds(String[] includeMapperIds){
        this.includeMapperIds=includeMapperIds;
        return this;
    }

    public DoLogFilter setExcludeMapperIds(String[] excludeMapperIds){
        this.excludeMapperIds=excludeMapperIds;
        return this;
    }

    public DoLogFilter setSqlCommandTypes(SqlCommandType[] sqlCommandTypes){
        this.sqlCommandTypes=sqlCommandTypes;
        return this;
    }

    @Override
    public String doSqlFilter(String originalSql,Object filterParam, ExecuteHelper executeHelper, MappedStatement mappedStatement, BoundSql boundSql, RowBounds rowBounds) {
        try{
            Settings activeSettings=generateSettings(mappedStatement.getId());
            Object parameter=boundSql.getParameterObject();
            if(parameter instanceof MapperMethod.ParamMap){
                throw new Exception("目前只支持单个入参！");
            }

            List<ParameterMapping> parameterMappingList=boundSql.getParameterMappings();

            Insert insert=new Insert();
            List<Column> columnList=new ArrayList<>();
            List<Expression> expressions=new ArrayList<>();

            if(activeSettings.getLogTypeColumn()!=null){
                columnList.add(new Column(activeSettings.getLogTypeColumn()));
                expressions.add(new StringValue(mappedStatement.getSqlCommandType().name()));
            }

            if(activeSettings.getLogPersionColumn()!=null){
                columnList.add(new Column(activeSettings.getLogPersionColumn()));
                if(filterParam instanceof String){
                    expressions.add(new StringValue(filterParam.toString()));
                }else{
                    expressions.add(new LongValue(filterParam.toString()));
                }
            }

            if(activeSettings.getLogDateColumn()!=null){
                columnList.add(new Column(activeSettings.getLogDateColumn()));
                expressions.add(new TimestampValue(" "+simpleDateFormat.format(new Date())+" "));
            }
            if(mappedStatement.getSqlCommandType()==SqlCommandType.INSERT){
                //插入
                Insert statement=(Insert) CCJSqlParserUtil.parse(originalSql);
                String recordTableName=statement.getTable().getName();

                String logTableName=generateTableName(activeSettings, recordTableName);
                insert.setTable(new Table(logTableName));

                if(activeSettings.getLogPrimaryKeyColumn()!=null){
                    columnList.add(new Column(activeSettings.getLogPrimaryKeyColumn()));
                    if(activeSettings.getPrimarykeyGenerationStrategy()!=null){
                        if(activeSettings.getPrimarykeyGenerationStrategy()==PrimarykeyGenerationStrategy.SEQUENCE){
                            expressions.add(new HexValue("SEQ_"+logTableName+".NEXTVAL"));
                        }
                    }
                }

                if(activeSettings.getLogRecordTableNameColumn()!=null){
                    columnList.add(new Column(activeSettings.getLogRecordTableNameColumn()));
                    expressions.add(new StringValue(recordTableName));
                }

                if(activeSettings.getLogRecordPrimaryKeyColumn()!=null){
                    columnList.add(new Column(activeSettings.getLogRecordPrimaryKeyColumn()));
                    int index=0;
                    for(int i=0;i<statement.getColumns().size();i++){
                        Column column=statement.getColumns().get(i);
                        Expression expression=((ExpressionList) statement.getItemsList()).getExpressions().get(i);
                        if(column.getColumnName().equals(activeSettings.getRecordPrimaryKey())){
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

                if(activeSettings.getLogContentColumn()!=null){
                    columnList.add(new Column(activeSettings.getLogContentColumn()));
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

                if(activeSettings.getLogPrimaryKeyColumn()!=null){
                    columnList.add(new Column(activeSettings.getLogPrimaryKeyColumn()));
                    if(activeSettings.getPrimarykeyGenerationStrategy()!=null){
                        if(activeSettings.getPrimarykeyGenerationStrategy()==PrimarykeyGenerationStrategy.SEQUENCE){
                            expressions.add(new HexValue("SEQ_"+logTableName+".NEXTVAL"));
                        }
                    }
                }

                if(activeSettings.getLogRecordTableNameColumn()!=null){
                    columnList.add(new Column(activeSettings.getLogRecordTableNameColumn()));
                    expressions.add(new StringValue(recordTableName));
                }

                Expression recordPrimaryKeyExpression=null;
                if(activeSettings.getLogRecordPrimaryKeyColumn()!=null){
                    columnList.add(new Column(activeSettings.getLogRecordPrimaryKeyColumn()));
                    ParameterMapping parameterMapping=parameterMappingList.get(parameterMappingList.size()-1);
                    if(parameterMapping.getJavaType().getName().equals(String.class.getName())){
                        recordPrimaryKeyExpression=new StringValue(ReflectUtil.getFieldValue(parameter,parameterMapping.getProperty()).toString());
                        expressions.add(recordPrimaryKeyExpression);
                    }else {
                        recordPrimaryKeyExpression=new LongValue(ReflectUtil.getFieldValue(parameter,parameterMapping.getProperty()).toString());
                        expressions.add(recordPrimaryKeyExpression);
                    }
                }

                if(activeSettings.getLogContentColumn()!=null){
                    columnList.add(new Column(activeSettings.getLogContentColumn()));
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

                if(activeSettings.getLogRecordContentColumn()!=null&&activeSettings.getLogRecordPrimaryKeyColumn()!=null){
                    columnList.add(new Column(activeSettings.getLogRecordContentColumn()));
                    List<SelectItem> selectColumnList=new ArrayList<>();
                    for(int i=0;i<statement.getColumns().size();i++){
                        selectColumnList.add(new SelectExpressionItem(statement.getColumns().get(i)));
                    }
                    PlainSelect plainSelect=new PlainSelect();
                    plainSelect.setSelectItems(selectColumnList);
                    plainSelect.setFromItem(new Table(recordTableName));
                    EqualsTo equalsTo=new EqualsTo();
                    equalsTo.setLeftExpression(new Column(settings.getRecordPrimaryKey()));
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

                if(activeSettings.getLogPrimaryKeyColumn()!=null){
                    columnList.add(new Column(activeSettings.getLogPrimaryKeyColumn()));
                    if(activeSettings.getPrimarykeyGenerationStrategy()!=null){
                        if(activeSettings.getPrimarykeyGenerationStrategy()==PrimarykeyGenerationStrategy.SEQUENCE){
                            expressions.add(new HexValue("SEQ_"+logTableName+".NEXTVAL"));
                        }
                    }
                }

                if(activeSettings.getLogRecordTableNameColumn()!=null){
                    columnList.add(new Column(activeSettings.getLogRecordTableNameColumn()));
                    expressions.add(new StringValue(recordTableName));
                }

                Expression recordPrimaryKeyExpression=null;
                if(activeSettings.getLogRecordPrimaryKeyColumn()!=null){
                    columnList.add(new Column(activeSettings.getLogRecordPrimaryKeyColumn()));
                    if(parameter instanceof String){
                        recordPrimaryKeyExpression=new StringValue(parameter.toString());
                        expressions.add(recordPrimaryKeyExpression);
                    }else{
                        recordPrimaryKeyExpression=new LongValue(parameter.toString());
                        expressions.add(recordPrimaryKeyExpression);
                    }
                }

                if(activeSettings.getLogRecordContentColumn()!=null&&activeSettings.getLogRecordPrimaryKeyColumn()!=null){
                    columnList.add(new Column(activeSettings.getLogRecordContentColumn()));
                    PlainSelect plainSelect=new PlainSelect();
                    plainSelect.addSelectItems(new AllColumns());
                    plainSelect.setFromItem(new Table(recordTableName));
                    EqualsTo equalsTo=new EqualsTo();
                    equalsTo.setLeftExpression(new Column(settings.getRecordPrimaryKey()));
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
            logger.error("无法生成操作日志！");
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
    private Settings generateSettings(String mappedStatementId) throws ClassNotFoundException {
        Class clazz=MybatisUtils.getClass(mappedStatementId);
        if(clazz.isAnnotationPresent(DoLogSettings.class)){
            DoLogSettings doLogSettings=(DoLogSettings)clazz.getAnnotation(DoLogSettings.class);
            return new Settings() {
                @Override
                public String getRecordPrimaryKey() {
                    if(doLogSettings.recordPrimaryKey().length()==0){
                        return settings.getRecordPrimaryKey();
                    }else{
                        return doLogSettings.recordPrimaryKey();
                    }
                }

                @Override
                public String getLogTableNamePrefix() {
                    if(doLogSettings.logTableNamePrefix().length()==0){
                        return settings.getLogTableNamePrefix();
                    }else{
                        return doLogSettings.logTableNamePrefix();
                    }
                }

                @Override
                public String getLogTableNamePostfix() {
                    if(doLogSettings.logTableNamePostfix().length()==0){
                        return settings.getLogTableNamePostfix();
                    }else{
                        return doLogSettings.logTableNamePostfix();
                    }
                }

                @Override
                public String getLogTableName() {
                    if(doLogSettings.logTableName().length()==0){
                        return settings.getLogTableName();
                    }else{
                        return doLogSettings.logTableName();
                    }
                }

                @Override
                public String getLogPrimaryKeyColumn() {
                    if(doLogSettings.logPrimaryKeyColumn().length()==0){
                        return settings.getLogPrimaryKeyColumn();
                    }else{
                        return doLogSettings.logPrimaryKeyColumn();
                    }
                }

                @Override
                public PrimarykeyGenerationStrategy getPrimarykeyGenerationStrategy() {
                    return settings.getPrimarykeyGenerationStrategy();
                }

                @Override
                public String getLogRecordPrimaryKeyColumn() {
                    if(doLogSettings.logRecordPrimaryKeyColumn().length()==0){
                        return settings.getLogRecordPrimaryKeyColumn();
                    }else{
                        return doLogSettings.logRecordPrimaryKeyColumn();
                    }
                }

                @Override
                public String getLogRecordTableNameColumn() {
                    if(doLogSettings.logRecordTableNameColumn().length()==0){
                        return settings.getLogRecordTableNameColumn();
                    }else{
                        return doLogSettings.logRecordTableNameColumn();
                    }
                }

                @Override
                public String getLogContentColumn() {
                    if(doLogSettings.logContentColumn().length()==0){
                        return settings.getLogContentColumn();
                    }else{
                        return doLogSettings.logContentColumn();
                    }
                }

                @Override
                public String getLogRecordContentColumn() {
                    if(doLogSettings.logRecordContentColumn().length()==0){
                        return settings.getLogRecordContentColumn();
                    }else{
                        return doLogSettings.logRecordContentColumn();
                    }
                }

                @Override
                public String getLogTypeColumn() {
                    if(doLogSettings.logTypeColumn().length()==0){
                        return settings.getLogTypeColumn();
                    }else{
                        return doLogSettings.logTypeColumn();
                    }
                }

                @Override
                public String getLogPersionColumn() {
                    if(doLogSettings.logPersionColumn().length()==0){
                        return settings.getLogPersionColumn();
                    }else{
                        return doLogSettings.logPersionColumn();
                    }
                }

                @Override
                public String getLogDateColumn() {
                    if(doLogSettings.logDateColumn().length()==0){
                        return settings.getLogDateColumn();
                    }else{
                        return doLogSettings.logDateColumn();
                    }
                }
            };
        }else{
            return settings;
        }
    }

    private String generateTableName(Settings settings, String recordTableName) {
        if(settings.getLogTableName()!=null){
            return settings.getLogTableName();
        }
        StringBuilder tableNameBuilder=new StringBuilder();
        if(settings.getLogTableNamePrefix()!=null){
            tableNameBuilder.append(settings.getLogTableNamePrefix());
        }
        tableNameBuilder.append(recordTableName);
        if(settings.getLogTableNamePostfix()!=null){
            tableNameBuilder.append(settings.getLogTableNamePostfix());
        }
        return tableNameBuilder.toString();
    }

    @Override
    public String getFilterParamName() {
        return filterParamName;
    }

    @Override
    public SqlCommandType[] getSqlCommandType() {
        return sqlCommandTypes;
    }

    @Override
    public String[] getIncludeMappedStatementIds() {
        return includeMapperIds;
    }

    @Override
    public String[] getExcludeMappedStatementIds() {
        return excludeMapperIds;
    }

    public interface Settings {
        /**
         * 业务表主键名
         * @return
         */
        String getRecordPrimaryKey();
        /**
         * 日志表前缀（后面加表名）
         * @return
         */
        String getLogTableNamePrefix();
        /**
         * 日志表后缀（前面加表名）
         * @return
         */
        String getLogTableNamePostfix();
        /**
         * 直接指定日志表
         * @return
         */
        String getLogTableName();
        /**
         * 日志表主键名
         * @return
         */
        String getLogPrimaryKeyColumn();
        /**
         * 日志表主键策略
         * @return
         */
        PrimarykeyGenerationStrategy getPrimarykeyGenerationStrategy();
        /**
         * 业务表主键存储字段
         * @return
         */
        String getLogRecordPrimaryKeyColumn();
        /**
         * 业务表表名存储字段
         * @return
         */
        String getLogRecordTableNameColumn();
        /**
         * 业务表更新之后内容存储字段
         * @return
         */
        String getLogContentColumn();
        /**
         * 业务表更新之前内容存储字段
         * @return
         */
        String getLogRecordContentColumn();
        /**
         * 业务表操作类型存储字段
         * @return
         */
        String getLogTypeColumn();
        /**
         * 业务表操作人存储字段
         * @return
         */
        String getLogPersionColumn();
        /**
         * 业务表操作时间存储字段
         * @return
         */
        String getLogDateColumn();
    }

    public enum PrimarykeyGenerationStrategy{
        SEQUENCE;
    }
}
