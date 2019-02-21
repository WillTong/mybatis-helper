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
- 概述  
此模块是mybatis-helper框架的控制模块，是必须要引入的。它控制拦截器的加载、运行并提供一些工具类。
- mybatis拦截器 StatementHandlerInterceptor 
它是mybatis的StatementHandler类中的prepare方法的拦截器，是mybatis-helper的入口类。它通过运行继承于SqlFilter的过滤器来实现各个功能模块过滤器的加载。
- springboot中如何加载过滤器
```java
@Bean
public StatementHandlerInterceptor mybatisHelper(){
    //初始化拦截器
    StatementHandlerInterceptor statementHandlerInterceptor=new StatementHandlerInterceptor();
    //添加一个DataScopeFilter过滤器，并且将MybatisThreadHelper里面的dataScope的值作为过滤器的入参
    statementHandlerInterceptor.addFilter(DataScopeFilter.build("dataScope", new DataScopeFilter.Settings() {
        //过滤器的全局配置（详见各过滤器注释），如果要添加特殊配置可以使用注解添加在dao的接口上面。
        @Override
        public ActiveScopeFieldSettings[] getActiveScopeFields() {
            return null;
        }
        @Override
        public String getSelect() {
            return null;
        }
    //通过IncludeMapperIds和ExcludeMapperIds来匹配mapper.xml的mappedStatementId控制过滤器的加载。
    }).setIncludeMapperIds(new String[]{"*"})
            .setExcludeMapperIds(new String[]{"com.xxx.dao.ProviderTestMapper","com.xxx.dao.AcGroupUserMapper.selectGroupUserInfo"}));
}
```

## 分页(mybatis-helper-page)
- 概述  
如果mapper方法中有PagingBounds（继承于RowBounds）入参并且是查询语句，则开启分页拦截器。分页拦截器首先通过生成的查询总数的sql查询总条目，然后将总条目存入PagingBounds。最后把修改后的分页语句返回。
- 如何使用
首先在springbootapplication添加配置
```java
statementHandlerInterceptor.addFilter(PageSqlFilter.build(new PageSqlFilter.Settings() {

            @Override
            public PageCountSqlParserType getPageCountSqlParserType() {
                return PageCountSqlParserType.INNER;
            }
        }).setIncludeMapperIds(new String[]{"*"})
);
```
在查询的时候可以这么写
```java
List<ProviderTest> providerTestList=providerTestMapper.selectByExample(example,new PagingBounds(11,10));
```
selectByExample是mybatis自动生成的方法本身是不带分页的，通过分页拦截器之后将会加入分页语句改造成分页方法。
- 配置
分页插件目前只有一个配置pageCountSqlParserType。如果是Inner则生成分页查询的时候是把select和from之间的内容改为count(1)。如果是Outer则将原有sql用括号括起来在外面加入select count(1) from

## 数据范围权限(mybatis-helper-datascope)
- 概述  
此模块的作用是根据登录用户的权限控制参数来动态添加查询语句的where条件，从而控制用户数据查看范围。
- 如何使用  
首先在SpringBootApplication中添加DataScopeFilter。DataScopeFilter.build("dataScope",xxx)中dataScope是MybatisThreadHelper的参数名，值必须是Map<String,Object[]>类型。如果Object数组只有一个值那么是where a=1这种形式，如果是多个值则是where a in (1,2)。如果MybatisThreadHelper里面没有dataScope或者值是null，那么则不触发数据权限过滤器。
- 配置  
activeScopeFields配置可以自定义dataScope种生效的字段。例如
```java
@DataScopeFilterSettings(activeScopeFields={@ActiveScopeField("DEPT_ID"),
        @ActiveScopeField(value = "PROJECT_ID",columnName = "ID")})
public interface SysProjectMapper
```
这个配置的含义是让dataScope中key为DEPT_ID和PROJECT_ID的条件生效并且将PROJECT_ID改为ID。如果map的结构是[{"DEPT_ID":"1"},{"PROJECT_ID":"2",{"WORK_ID":"3"}},]则最后生成的sql是where DEPT_ID=1 and ID=2

## 通用字段填充(mybatis-helper-commonfield)
- 概述  
如果需要在插入或者更新的时候固定填写操作人和操作时间等字段可以使用这个模块。它通过过滤器修改插入或更新语句，在insert和update的后面增加操作人和操作时间，所以它需要传入操作人的id。
- 如何使用   
```java
statementHandlerInterceptor.addFilter(
    CommonFieldFilter.build("suId", new CommonFieldFilter.Settings() {
        @Override
        public String getInsertPersionColumn() {
            return "CREATED_BY";
        }

        @Override
        public String getInsertDateColumn() {
            return "CREATED_DATE";
        }

        @Override
        public String getUpdatePersionColumn() {
            return "LAST_UPD_BY";
        }

        @Override
        public String getUpdateDateColumn() {
            return "LAST_UPD_DATE";
        }

        @Override
        public boolean isInsertAllUpdate() {
            return true;
        }
    }).setIncludeMapperIds(new String[]{"*"})
);
```
如上图需要在MybatisThreadHelper的suId里面放入用户id。然后再配置各个存储字段的名称。
- 配置  
```java
public interface Settings {
    /**
     * 新增操作人存储字段
     * @return
     */
    String getInsertPersionColumn();
    /**
     * 新增操作日期存储字段
     * @return
     */
    String getInsertDateColumn();
    /**
     * 修改操作人存储字段
     * @return
     */
    String getUpdatePersionColumn();
    /**
     * 修改操作日期存储字段
     * @return
     */
    String getUpdateDateColumn();
    /**
     * 是否在新增时自动填入更新人信息
     * @return
     */
    boolean isInsertAllUpdate();
}
```
## 操作日志(mybatis-helper-dolog)
- 概述
本模块的作用是在dml操作的时候把改动前和改动后的数据保存起来。
- 如何使用
```java
statementHandlerInterceptor.addFilter(
    DoLogFilter.build("suId",new DoLogFilter.Settings() {
        @Override
        public String getRecordPrimaryKey() {
            return "ID";
        }

        @Override
        public String getLogTableNamePrefix() {
            return null;
        }

        @Override
        public String getLogTableNamePostfix() {
            return "_LOG";
        }

        @Override
        public String getLogTableName() {
            return null;
        }

        @Override
        public String getLogPrimaryKeyColumn() {
            return "ID";
        }

        @Override
        public DoLogFilter.PrimarykeyGenerationStrategy getPrimarykeyGenerationStrategy() {
            return DoLogFilter.PrimarykeyGenerationStrategy.SEQUENCE;
        }

        @Override
        public String getLogRecordPrimaryKeyColumn() {
            return "OP_ID";
        }

        @Override
        public String getLogRecordTableNameColumn() {
            return "OP_TABLE";
        }

        @Override
        public String getLogContentColumn() {
            return "OP_CONTENT";
        }

        @Override
        public String getLogRecordContentColumn() {
            return "OP_RECORD_CONTENT";
        }

        @Override
        public String getLogTypeColumn() {
            return "OP_TYPE";
        }

        @Override
        public String getLogPersionColumn() {
            return "OP_BY";
        }

        @Override
        public String getLogDateColumn() {
            return "OP_DATE";
        }
    }).setExcludeMapperIds(new String[]{"*"}).setIncludeMapperIds(new String[]{
            "com.haier.lecc.cm.dao.ProviderTestMapper"
    })
);
```
同通用字段填充，需要在MybatisThreadHelper的suId里面放入用户id。
- 配置
```java
public interface Settings {
    /**
     * 业务表主键名
     * @return
     */
    String getRecordPrimaryKey();
    /**
     * 日志表前缀（后面加表名）
     * @return
     */
    String getLogTableNamePrefix();
    /**
     * 日志表后缀（前面加表名）
     * @return
     */
    String getLogTableNamePostfix();
    /**
     * 直接指定日志表
     * @return
     */
    String getLogTableName();
    /**
     * 日志表主键名
     * @return
     */
    String getLogPrimaryKeyColumn();
    /**
     * 日志表主键策略
     * @return
     */
    PrimarykeyGenerationStrategy getPrimarykeyGenerationStrategy();
    /**
     * 业务表主键存储字段
     * @return
     */
    String getLogRecordPrimaryKeyColumn();
    /**
     * 业务表表名存储字段
     * @return
     */
    String getLogRecordTableNameColumn();
    /**
     * 业务表更新之后内容存储字段
     * @return
     */
    String getLogContentColumn();
    /**
     * 业务表更新之前内容存储字段
     * @return
     */
    String getLogRecordContentColumn();
    /**
     * 业务表操作类型存储字段
     * @return
     */
    String getLogTypeColumn();
    /**
     * 业务表操作人存储字段
     * @return
     */
    String getLogPersionColumn();
    /**
     * 业务表操作时间存储字段
     * @return
     */
    String getLogDateColumn();
}
```


