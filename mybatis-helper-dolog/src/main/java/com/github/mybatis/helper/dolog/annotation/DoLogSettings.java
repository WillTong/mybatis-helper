package com.github.mybatis.helper.dolog.annotation;

import com.github.mybatis.helper.dolog.DoLogFilter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface DoLogSettings {
    /**
     * 业务表主键列
     * @return
     */
    String recordPrimaryKey() default "";

    /**
     * 日志表前缀（后面加表名）
     * @return
     */
    String logTableNamePrefix() default "";

    /**
     * 日志表后缀（前面加表名）
     * @return
     */
    String logTableNamePostfix() default "";

    /**
     * 直接指定日志表
     * @return
     */
    String logTableName() default "";

    /**
     * 日志表主键列
     * @return
     */
    String logPrimaryKeyColumn() default "";

    /**
     * 业务表主键存储字段
     * @return
     */
    String logRecordPrimaryKeyColumn() default "";

    /**
     * 业务表表名存储字段
     * @return
     */
    String logRecordTableNameColumn() default "";

    /**
     * 业务表更新之后内容存储字段
     * @return
     */
    String logContentColumn() default "";

    /**
     * 业务表更新之前内容存储字段
     * @return
     */
    String logRecordContentColumn() default "";

    /**
     * 业务表操作类型存储字段
     * @return
     */
    String logTypeColumn() default "";

    /**
     * 业务表操作人存储字段
     * @return
     */
    String logPersionColumn() default "";

    /**
     * 业务表操作时间存储字段
     * @return
     */
    String logDateColumn() default "";
}
