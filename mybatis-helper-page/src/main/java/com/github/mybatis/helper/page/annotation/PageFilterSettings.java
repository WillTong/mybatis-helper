package com.github.mybatis.helper.page.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface PageFilterSettings {
    /**
     * 如果是OUTER则在外面套一层（）写分页语句，如果是INNER则在里面直接拼上
     * @return
     */
    PageCountSqlParserType pageCountSqlParserType() default PageCountSqlParserType.INNER;
}
