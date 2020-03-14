package com.github.mybatis.helper.datascope.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.METHOD})
public @interface DataScopeSettings {
    /**
     * 自定义dataScope生效的字段
     * @return
     */
    ActiveScopeField[] activeScopeFields() default {};

    /**
     * sql风格
     * @return
     */
    DataScopeSqlStyle dataScopeSqlStyle() default DataScopeSqlStyle.OUTER;

    /**
     * 外部sql配置
     * @return
     */
    OuterSqlStyleSettings outerSqlStyleSettings() default @OuterSqlStyleSettings;

    /**
     * 内部sql配置
     * @return
     */
    InnerSqlStyleSettings innerSqlStyleSettings() default @InnerSqlStyleSettings;
}
