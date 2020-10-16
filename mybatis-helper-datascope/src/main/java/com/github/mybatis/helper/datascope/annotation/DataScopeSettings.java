package com.github.mybatis.helper.datascope.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.METHOD})
public @interface DataScopeSettings {
    boolean onlyUseSmallScope() default true;

    /**
     * 自定义dataScope生效的字段
     * @return activeScopeFields
     */
    ActiveScopeField[] activeScopeFields() default {};

    /**
     * sql风格
     * @return dataScopeSqlStyle
     */
    DataScopeSqlStyle dataScopeSqlStyle() default DataScopeSqlStyle.OUTER;

    /**
     * 外部sql配置
     * @return outerSqlStyleSettings
     */
    OuterSqlStyleSettings outerSqlStyleSettings() default @OuterSqlStyleSettings;

    /**
     * 内部sql配置
     * @return innerSqlStyleSettings
     */
    InnerSqlStyleSettings innerSqlStyleSettings() default @InnerSqlStyleSettings;
}
