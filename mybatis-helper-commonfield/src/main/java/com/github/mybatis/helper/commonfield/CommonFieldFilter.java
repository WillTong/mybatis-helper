package com.github.mybatis.helper.commonfield;

import com.github.mybatis.helper.commonfield.annotation.CommonFieldFilterSettings;
import com.github.mybatis.helper.core.ExecuteHelper;
import com.github.mybatis.helper.core.MybatisUtils;
import com.github.mybatis.helper.core.filter.SqlFilter;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 查询或更新时自动写入创建人创建时间，更新人更新时间
 * @author will
 */
public class CommonFieldFilter implements SqlFilter {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private Settings settings;
    private String filterParamName;
    private String[] includeMapperIds;
    private String[] excludeMapperIds;
    private SqlCommandType[] sqlCommandTypes;

    private CommonFieldFilter(String filterParamName,Settings settings){
        this.settings=settings;
        this.filterParamName=filterParamName;
        sqlCommandTypes=new SqlCommandType[]{SqlCommandType.INSERT,SqlCommandType.UPDATE};
    }

    public static CommonFieldFilter build(String filterParamName,Settings settings){
        return new CommonFieldFilter(filterParamName,settings);
    }

    public CommonFieldFilter setIncludeMapperIds(String[] includeMapperIds){
        this.includeMapperIds=includeMapperIds;
        return this;
    }

    public CommonFieldFilter setExcludeMapperIds(String[] excludeMapperIds){
        this.excludeMapperIds=excludeMapperIds;
        return this;
    }

    public CommonFieldFilter setSqlCommandTypes(SqlCommandType[] sqlCommandTypes){
        this.sqlCommandTypes=sqlCommandTypes;
        return this;
    }

    @Override
    public String doSqlFilter(String originalSql, Object filterParam, ExecuteHelper executeHelper, MappedStatement mappedStatement, BoundSql boundSql, RowBounds rowBounds) {
        try{
            Settings activeSettings=generateSettings(mappedStatement.getId());
            if(mappedStatement.getSqlCommandType()==SqlCommandType.UPDATE){
                Update update = (Update) CCJSqlParserUtil.parse(originalSql);
                boolean isHasUp=false;
                boolean isHasUd=false;
                for(Column column:update.getColumns()){
                    if(!isHasUp&&column.getColumnName().equals(activeSettings.getUpdatePersionColumn())){
                        isHasUp=true;
                        continue;
                    }
                    if(!isHasUd&&column.getColumnName().equals(activeSettings.getUpdateDateColumn())){
                        isHasUd=true;
                    }
                }
                if(!isHasUp){
                    update.getColumns().add(new Column(activeSettings.getUpdatePersionColumn()));
                    update.getExpressions().add(new LongValue(Long.parseLong(filterParam.toString())));
                }
                if(!isHasUd){
                    update.getColumns().add(new Column(activeSettings.getUpdateDateColumn()));
                    update.getExpressions().add(new TimestampValue(" "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+" "));
                }
                return update.toString();
            }else if (mappedStatement.getSqlCommandType()==SqlCommandType.INSERT){
                Insert insert = (Insert) CCJSqlParserUtil.parse(originalSql);
                List<Expression> insertExpressionList = ((ExpressionList) insert.getItemsList()).getExpressions();
                LongValue persion=new LongValue(Long.parseLong(filterParam.toString()));
                TimestampValue today=new TimestampValue(" "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+" ");
                boolean isHasIp=false;
                boolean isHasId=false;
                boolean isHasUp=false;
                boolean isHasUd=false;
                for(Column column:insert.getColumns()){
                    if(!isHasUp&&column.getColumnName().equals(activeSettings.getUpdatePersionColumn())){
                        isHasUp=true;
                        continue;
                    }
                    if(!isHasUd&&column.getColumnName().equals(activeSettings.getUpdateDateColumn())){
                        isHasUd=true;
                        continue;
                    }
                    if(!isHasIp&&column.getColumnName().equals(activeSettings.getInsertPersionColumn())){
                        isHasIp=true;
                        continue;
                    }
                    if(!isHasId&&column.getColumnName().equals(activeSettings.getInsertDateColumn())){
                        isHasId=true;
                    }

                }
                if(!isHasIp){
                    insert.getColumns().add(new Column(activeSettings.getInsertPersionColumn()));
                    insertExpressionList.add(persion);
                }
                if(!isHasId){
                    insert.getColumns().add(new Column(activeSettings.getInsertDateColumn()));
                    insertExpressionList.add(today);
                }
                if(activeSettings.isInsertAllUpdate()){
                    if(!isHasUp){
                        insert.getColumns().add(new Column(activeSettings.getUpdatePersionColumn()));
                        insertExpressionList.add(new LongValue(Long.parseLong(filterParam.toString())));
                    }
                    if(!isHasUd){
                        insert.getColumns().add(new Column(activeSettings.getUpdateDateColumn()));
                        insertExpressionList.add(new TimestampValue(" "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+" "));
                    }
                }
                return insert.toString();
            }
        }catch (Exception e){
            logger.error("通用字段保存错误！");
        }
        return null;
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
        if(clazz.isAnnotationPresent(CommonFieldFilterSettings.class)){
            CommonFieldFilterSettings commonFieldFilterSettings=(CommonFieldFilterSettings)clazz.getAnnotation(CommonFieldFilterSettings.class);
            return new Settings() {
                @Override
                public String getInsertPersionColumn() {
                    if(commonFieldFilterSettings.insertPersionColumn().length()==0){
                        return settings.getInsertPersionColumn();
                    }else{
                        return commonFieldFilterSettings.insertPersionColumn();
                    }
                }

                @Override
                public String getInsertDateColumn() {
                    if(commonFieldFilterSettings.insertDateColumn().length()==0){
                        return settings.getInsertDateColumn();
                    }else{
                        return commonFieldFilterSettings.insertDateColumn();
                    }
                }

                @Override
                public String getUpdatePersionColumn() {
                    if(commonFieldFilterSettings.updatePersionColumn().length()==0){
                        return settings.getUpdatePersionColumn();
                    }else{
                        return commonFieldFilterSettings.updatePersionColumn();
                    }
                }

                @Override
                public String getUpdateDateColumn() {
                    if(commonFieldFilterSettings.updateDateColumn().length()==0){
                        return settings.getUpdateDateColumn();
                    }else{
                        return commonFieldFilterSettings.updateDateColumn();
                    }
                }

                @Override
                public boolean isInsertAllUpdate() {
                    return settings.isInsertAllUpdate();
                }
            };
        }else{
            return settings;
        }
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
         * 新增操作人存储字段
         * @return
         */
        String getInsertPersionColumn();
        /**
         * 新增操作日期存储字段
         * @return
         */
        String getInsertDateColumn();
        /**
         * 修改操作人存储字段
         * @return
         */
        String getUpdatePersionColumn();
        /**
         * 修改操作日期存储字段
         * @return
         */
        String getUpdateDateColumn();
        /**
         * 是否在新增时自动填入更新人信息
         * @return
         */
        boolean isInsertAllUpdate();
    }
}
