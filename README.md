# mybatis-helper

[![Build Status](http://img.shields.io/travis/WillTong/mybatis-helper.svg?branch=master)](http://img.shields.io/travis/WillTong/mybatis-helper.svg?branch=master)
[![Dependency Status](https://img.shields.io/versioneye/d/WillTong/mybatis-helper.svg)](https://img.shields.io/versioneye/d/WillTong/mybatis-helper.svg)
[![License](https://img.shields.io/github/license/WillTong/mybatis-helper.svg)](LICENSE)

mybatis的工具类。    

* [核心(mybatis-helper-core)](#核心(mybatis-helper-core))
* [分页(mybatis-helper-page)](#分页)
* [数据范围权限(mybatis-helper-datascope)](#数据范围权限)
* [通用字段填充(mybatis-helper-commonfield)](#通用字段填充)
* [操作日志(mybatis-helper-dolog)](#操作日志)

## 核心(mybatis-helper-core)
#### 1.概述  
此模块是mybatis-helper框架的核心模块，是必须要引入的。它控制拦截器的加载、运行并提供一些工具类。
#### 2.sql拦截器 SqlInterceptor  
它是mybatis的StatementHandler类中的prepare方法的拦截器，是mybatis-helper修改sql的入口类。
#### 3.springboot指定mybatis配置文件 
```yaml
mybatis:
  configLocation: classpath:/mybatis-config.xml
```
#### 4.如何配置 mybatis-config.xml
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <plugins>
        <plugin interceptor="com.github.mybatis.helper.page.PageSqlInterceptor">
        </plugin>
        <plugin interceptor="com.github.mybatis.helper.datascope.DataScopeSqlInterceptor">
        </plugin>
        <plugin interceptor="com.github.mybatis.helper.dolog.CommonFieldSqlInterceptor">
        </plugin>
        <plugin interceptor="com.github.mybatis.helper.dolog.DoLogSqlInterceptor">
        </plugin>
    </plugins>
</configuration>
```
这样就分别加载了mybatis的四个功能。   
#### 5.plugin节点配置
```xml
<property name="include" value="com.bbb,com.xxx.A,com.xxx.B.select"></property>
```
指定哪些包、类、方法生效，例如上图是com.bbb包下、com.xxx.A类下、com.xxx.B.select方法都生效。   
如果指定了这个节点，则除此之外的都不生效
```xml
<property name="exclude" value="com.bbb,com.xxx.A,com.xxx.B.select"></property>
```
指定哪些包、类、方法不生效，例如上图是com.bbb包下、com.xxx.A类下、com.xxx.B.select方法都不生效   
如果指定了这个节点，则除此之外的都生效
```xml
<property name="paramName" value="userId"></property>
```
指定可以通过MybatisThreadHelper.putVariable("userId")来给相应的拦截器传值。在下面介绍各个拦截器的时候会说明它们的默认值。
```xml
<property name="defaultSettingClass" value="com.xxx.SysApplication"></property>
```
指定拦截器默认的配置注解所在的类。如果不配置则使用默认配置。在下面介绍各个拦截器的时候会说明它们的配置信息。
#### 6.对于springboot可以使用defaultSettingClass来指定默认配置为application启动类。   

## 分页(mybatis-helper-page)
#### 1.概述  
如果mapper方法中有com.github.mybatis.helper.page.PagingBounds（继承于RowBounds）对象入参并且是查询语句，则开启分页拦截器。分页拦截器首先通过生成的查询总数的sql查询总条目，然后将总条目存入传入的PagingBounds对象。
#### 2.例子
```java
List<ProviderTest> providerTestList=providerTestMapper.selectByExample(example,new PagingBounds(11,10));
```
selectByExample是mybatis自动生成的方法本身是不带分页的，通过分页拦截器之后将会加入分页语句改造成分页方法。
#### 3.配置
分页插件目前只有一个配置pageCountSqlParserType。如果是Inner则生成分页查询的时候是把select和from之间的内容改为count(1)。如果是Outer则将原有sql用括号括起来在外面加入select count(1) from
```java
@PageSettings(pageCountSqlParserType = PageCountSqlParserType.OUTER)
```
这个注解可以加在类上或方法上。如果都加则以方法上的配置为准。

## 数据范围权限(mybatis-helper-datascope)
#### 1.概述  
此模块的作用是根据登录用户的权限控制参数来动态添加查询语句的where条件，从而控制用户数据查看范围。
#### 2.传入权限对象
```java
MybatisThreadHelper.putVariable("dataScope",DataScope);
```
在执行查询之前先传入权限对象，默认的是dataScope，可以通过param来指定。值必须是Map<String,Object[]>类型。如果Object数组只有一个值那么是where a=1这种形式，如果是多个值则是where a in (1,2)。如果MybatisThreadHelper里面没有dataScope或者值是null，那么则不触发数据权限过滤器。
#### 3.配置  
DataScopeSettings注解可以加在类上或方法上。如果都加则以方法上的配置为准。
activeScopeFields配置可以自定义dataScope生效的字段。例如
```java
@DataScopeSettings(activeScopeFields={@ActiveScopeField("DEPT_ID"),
        @ActiveScopeField(value = "PROJECT_ID",columnName = "ID")})
public interface SysProjectMapper
```
这个配置的含义是让dataScope中key为DEPT_ID和PROJECT_ID的条件生效并且将PROJECT_ID改为ID。如果map的结构是[{"DEPT_ID":"1"},{"PROJECT_ID":"2"},{"WORK_ID":"3"}]则最后生成的sql是where DEPT_ID=1 and ID=2   
- dataScopeSqlStyle配置：如果是inner可以指定使用直接在where后拼上and，如果是outter则在外面套上select T.* from (xxx) T where 语句。注：inner只能支持简单的sql拼接，建议使用outter。   
- outerSqlStyleSettings配置：dataScopeSqlStyle为outter时的配置。select属性是覆盖select T.* from (xxx) where 语句中的T.*。   

## 通用字段填充(mybatis-helper-commonfield)
#### 1.概述  
如果需要在插入或者更新的时候固定填写操作人和操作时间等字段可以使用这个模块。它通过过滤器修改插入或更新语句，在insert和update的后面增加操作人和操作时间，所以它需要传入操作人的id。
#### 2.传入用户id   
```java
MybatisThreadHelper.putVariable("userId",userId);
```
在执行插入或更新之前先传入用户id，默认的是userId，可以通过param来指定。userId可以是数字类型也可以是字符类型。如果MybatisThreadHelper里面没有userId或者值是null，那么则不触发。
#### 3.配置  
CommonFieldSettings注解可以加在类上或方法上。如果都加则以方法上的配置为准。   
- insertPersionColumn指定创建人的列名，默认为CREATED_BY   
- insertDateColumn指定创建时间的列名，默认为CREATED_DATE
- updatePersionColumn指定更新人的列名，默认为LAST_UPD_BY
- updateDateColumn指定更新时间的列名，默认为LAST_UPD_DATE
#### 4.执行过程
如果为插入则会在sql语句加入所有四个字段，如果为更新则会加入updatePersionColumn和updateDateColumn。

## 操作日志(mybatis-helper-dolog)
#### 1.概述
本模块的作用是在dml操作的时候把改动前和改动后的数据保存起来。
#### 2.传入用户id   
```java
MybatisThreadHelper.putVariable("userId",userId);
```
在执行插入或更新之前先传入用户id，默认的是userId，可以通过param来指定。userId可以是数字类型也可以是字符类型。如果MybatisThreadHelper里面没有userId或者值是null，那么则不触发。
#### 3.配置
DoLogSettings注解可以加在类上或方法上。如果都加则以方法上的配置为准。   
```java
public @interface DoLogSettings {
    /**
     * 日志表主键生成策略
     * * @return
     */
    PrimarykeyGenerationStrategy primarykeyGenerationStrategy() default PrimarykeyGenerationStrategy.INCREAMENT;

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
```


