package com.github.mybatis.helper.core;

import com.github.mybatis.helper.core.filter.SqlFilter;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * sql拦截器接口
 * @author will
 */
public class AbstractSqlInterceptor implements Interceptor {

    protected List<SqlFilter> sqlFilterList;

    public AbstractSqlInterceptor(){
        this.sqlFilterList = new ArrayList<>();
    }

    public void addFilter(SqlFilter sqlFilter) {
        sqlFilterList.add(sqlFilter);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        return null;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
