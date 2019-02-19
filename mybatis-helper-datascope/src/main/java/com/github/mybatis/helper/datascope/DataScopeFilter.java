package com.github.mybatis.helper.datascope;

import com.github.mybatis.helper.core.ExecuteHelper;
import com.github.mybatis.helper.core.MybatisUtils;
import com.github.mybatis.helper.core.filter.SqlFilter;
import com.github.mybatis.helper.datascope.annotation.DataScopeFilterSettings;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据权限拦截器
 * <p>通过传入参数自动在sql拼上查询条件</p>
 * <p>拦截器传入参数类型为：Map<String,Object[]></p>
 * @author will
 */
public class DataScopeFilter implements SqlFilter {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private Settings settings;
    private String filterParamName;
    private String[] includeMapperIds;
    private String[] excludeMapperIds;

    private DataScopeFilter(String filterParamName,Settings settings){
        this.settings=settings;
        this.filterParamName=filterParamName;
    }

    public static DataScopeFilter build(String filterParamName,Settings settings){
        return new DataScopeFilter(filterParamName,settings);
    }

    public DataScopeFilter setIncludeMapperIds(String[] includeMapperIds){
        this.includeMapperIds=includeMapperIds;
        return this;
    }

    public DataScopeFilter setExcludeMapperIds(String[] excludeMapperIds){
        this.excludeMapperIds=excludeMapperIds;
        return this;
    }

    @Override
    public String doSqlFilter(String originalSql,Object filterParam, ExecuteHelper executeHelper, MappedStatement mappedStatement, BoundSql boundSql, RowBounds rowBounds) {
        try{
            Map<String,Object[]> dataScopeMap=new HashMap<>();
            Settings activeSettings=generateSettings(mappedStatement.getId());
            if(activeSettings.getActiveScopeFields()==null||activeSettings.getActiveScopeFields().length==0){
                dataScopeMap=(Map<String,Object[]>)filterParam;
            }else{
                for(ActiveScopeFieldSettings activeScopeField:activeSettings.getActiveScopeFields()){
                    Map<String,Object[]> filterParamMap=(Map<String,Object[]>)filterParam;
                    String key=activeScopeField.getValue();
                    if(filterParamMap.containsKey(key)){
                        if(activeScopeField.getColumnName().length()==0){
                            dataScopeMap.put(key,filterParamMap.get(activeScopeField.getValue()));
                        }else{
                            dataScopeMap.put(activeScopeField.getColumnName(),filterParamMap.get(activeScopeField.getValue()));
                        }
                    }
                }
            }
            if(dataScopeMap.size()==0){
                return originalSql;
            }
            //增加查询条件
            StringBuilder sqlBuilder = new StringBuilder();
            for(Map.Entry<String,Object[]> entry:dataScopeMap.entrySet()) {
                sqlBuilder.append(this.addFeild(entry.getKey(),entry.getValue()));
            }
            if(sqlBuilder.length()!=0){
                String selectItem=activeSettings.getSelect()==null?"T.*":activeSettings.getSelect();
                sqlBuilder.insert(0,"select "+selectItem+" from ("+originalSql+") T where");
                if(sqlBuilder.toString().endsWith("and")){
                    sqlBuilder.delete(sqlBuilder.length()-3,sqlBuilder.length());
                }
                originalSql=sqlBuilder.toString();
            }
        }catch(Exception e){
            logger.error("数据范围解析错误！");
        }
        return originalSql;
    }

    @Override
    public String getFilterParamName() {
        return filterParamName;
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

    /**
     * 通用配置和自定义配置合并
     * @param mappedStatementId
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     */
    private Settings generateSettings(String mappedStatementId) throws ClassNotFoundException, NoSuchMethodException {
        Class clazz=MybatisUtils.getClass(mappedStatementId);
        Method method=MybatisUtils.getMethod(mappedStatementId);
        DataScopeFilterSettings myDataScopeFilterSettings=null;
        if(clazz.isAnnotationPresent(DataScopeFilterSettings.class)){
            myDataScopeFilterSettings=(DataScopeFilterSettings)clazz.getAnnotation(DataScopeFilterSettings.class);
        }else{
            myDataScopeFilterSettings=(DataScopeFilterSettings)method.getAnnotation(DataScopeFilterSettings.class);
        }
        if(myDataScopeFilterSettings!=null){
            DataScopeFilterSettings dataScopeFilterSettings=myDataScopeFilterSettings;
            return new Settings() {

                @Override
                public ActiveScopeFieldSettings[] getActiveScopeFields() {
                    if(dataScopeFilterSettings.activeScopeFields().length==0){
                        return settings.getActiveScopeFields();
                    }else{
                        return Arrays.stream(dataScopeFilterSettings.activeScopeFields())
                                .map(e->new ActiveScopeFieldSettings(e.value(),e.columnName()))
                                .toArray(ActiveScopeFieldSettings[]::new);
                    }
                }

                @Override
                public String getSelect() {
                    if(dataScopeFilterSettings.select()==null){
                        return "T.*";
                    }else{
                        return dataScopeFilterSettings.select();
                    }
                }
            };
        }else{
            return settings;
        }
    }

    public interface Settings{
        /**
         * 自定义dataScope生效的字段
         * @return
         */
        ActiveScopeFieldSettings[] getActiveScopeFields();
        /**
         * 自定义外层select语句
         * @return
         */
        String getSelect();
    }
}
