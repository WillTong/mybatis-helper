package com.github.mybatis.helper.datascope;

import com.github.mybatis.helper.core.sql.ExecuteHelper;
import com.github.mybatis.helper.core.sql.SqlInterceptor;
import com.github.mybatis.helper.datascope.annotation.ActiveScopeField;
import com.github.mybatis.helper.datascope.annotation.DataScopeSettings;
import com.github.mybatis.helper.datascope.annotation.DataScopeSqlStyle;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    originalSql = DataScopeSqlHelper.selectInnerDataScope(originalSql, dataScopeList);
                }else{
                    String selectItem=activeSettings.outerSqlStyleSettings().select()==null?"T.*":activeSettings.outerSqlStyleSettings().select();
                    originalSql = DataScopeSqlHelper.selectOuterDataScope(originalSql, dataScopeList, selectItem);
                }
            }else if(mappedStatement.getSqlCommandType()==SqlCommandType.UPDATE){
                originalSql = DataScopeSqlHelper.updateInnerDataScope(originalSql, dataScopeList);
            }else{
                originalSql = DataScopeSqlHelper.deleteInnerDataScope(originalSql, dataScopeList);
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
}
