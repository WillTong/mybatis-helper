package com.github.mybatis.helper.dolog.annotation;

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
    String recordPrimaryKey() default "ID";

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
    String logPrimaryKeyColumn() default "ID";

    /**
     * 业务表主键存储字段
     * @return
     */
    String logRecordPrimaryKeyColumn() default "RECORD_ID";

    /**
     * 业务表表名存储字段
     * @return
     */
    String logRecordTableNameColumn() default "RECORD_TABLE_NAME";

    /**
     * 业务表更新之后内容存储字段
     * @return
     */
    String logContentColumn() default "LOG_CONTENT";

    /**
     * 业务表更新之前内容存储字段
     * @return
     */
    String logRecordContentColumn() default "RECORD_CONTENT";

    /**
     * 业务表操作类型存储字段
     * @return
     */
    String logTypeColumn() default "LOG_TYPE";

    /**
     * 业务表操作人存储字段
     * @return
     */
    String logPersonColumn() default "LOG_PERSON";

    /**
     * 业务表操作时间存储字段
     * @return
     */
    String logDateColumn() default "LOG_DATE";
}
