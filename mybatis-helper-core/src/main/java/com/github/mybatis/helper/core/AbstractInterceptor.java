package com.github.mybatis.helper.core;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * sql拦截器接口
 * @author will
 */
public abstract class AbstractInterceptor implements Interceptor {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    protected String[] includeMapperIds;
    protected String[] excludeMapperIds;
    protected String paramName;
    protected Map<String, Annotation> settings;
    protected Annotation defaultSetting;
    protected Class settingsAnnotation;

    public AbstractInterceptor(){
        settings=new HashMap<>();
        settingsAnnotation=this.getSettingsAnnotation();
    }


    public void setDefaultSetting(Class clazz){
        if(clazz.isAnnotationPresent(settingsAnnotation)) {
            defaultSetting=clazz.getAnnotation(settingsAnnotation);
        }else{
            defaultSetting=this.getClass().getAnnotation(settingsAnnotation);
        }
    }

    public void setAllSettings(Collection<Class<?>> classList){
        for(Class clazz:classList) {
            for (Method method : clazz.getMethods()) {
                String mappedStatementId=String.join(".",clazz.getName(),method.getName());
                if(method.isAnnotationPresent(this.settingsAnnotation)){
                    settings.put(mappedStatementId, method.getAnnotation(settingsAnnotation));
                }else if(clazz.isAnnotationPresent(this.settingsAnnotation)){
                    settings.put(mappedStatementId, clazz.getAnnotation(settingsAnnotation));
                }else{
                    settings.put(mappedStatementId, defaultSetting);
                }
            }
        }
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
        if(properties.getProperty("include")!=null){
            this.includeMapperIds=properties.getProperty("include").split(",");
        }else{
            this.includeMapperIds=new String[]{"*"};
        }
        if(properties.getProperty("exclude")!=null){
            this.excludeMapperIds=properties.getProperty("exclude").split(",");
        }
        if(properties.get("paramName")!=null){
            this.paramName=properties.get("paramName").toString();
        }
        if(properties.get("defaultSettingClass")!=null){
            try {
                this.setDefaultSetting(Class.forName(properties.get("defaultSettingClass").toString()));
            } catch (ClassNotFoundException e) {
                this.setDefaultSetting(this.getClass());
            }
        }else{
            this.setDefaultSetting(this.getClass());
        }
    }

    protected boolean isMatchMappedStatementId(String mappedStatementId) {
        int include=MybatisUtils.matchMappedStatementId(mappedStatementId,includeMapperIds);
        int exclude=MybatisUtils.matchMappedStatementId(mappedStatementId,excludeMapperIds);
        return include>exclude;
    }

    protected <T extends Annotation> T getSetting(String mappedStatementId){
        if(settings.containsKey(mappedStatementId)){
            return (T)settings.get(mappedStatementId);
        }else{
            return (T)defaultSetting;
        }
    }

    public Class getSettingsAnnotation(){
        for(Annotation annotation:this.getClass().getAnnotations()){
            if(annotation instanceof Intercepts){
                continue;
            }
            return annotation.annotationType();
        }
        logger.error("没有指定默认配置！");
        return null;
    }
}