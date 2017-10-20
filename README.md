# mybatis-helper

[![Build Status](http://img.shields.io/travis/WillTong/mybatis-helper.svg?branch=master)](http://img.shields.io/travis/WillTong/mybatis-helper.svg?branch=master)
[![codecov](https://codecov.io/github/WillTong/mybatis-helper/coverage.svg?branch=master)](https://codecov.io/github/WillTong/mybatis-helper?branch=master)
[![Dependency Status](https://img.shields.io/versioneye/d/WillTong/mybatis-helper.svg)](https://img.shields.io/versioneye/d/WillTong/mybatis-helper.svg)
[![License](https://img.shields.io/github/license/WillTong/mybatis-helper.svg)](LICENSE)

mybatis的工具类，主要用来填充分页功能和数据权限功能。

* [分页](#分页)
* [数据权限](#数据权限)

## 分页
- 实体类继承com.github.mybatis.helper.BaseModel。
```java
public class BaseModel implements Serializable {
    public Integer rows = Integer.valueOf(10);
    public Integer page = Integer.valueOf(1);
    private int begin;
    private int end;
    private String sort;
    private String order;
    private int total;
    private Map<String, String[]> dataAuthority;
}
```
分页后会返回total。也就是总数据。

然后建立实体，例如：
```java
public class Example extends BaseModel{
    
}
```
- 在mybatis的mapper代码中加入Page注解
```java
public interface ExampleMapper {
    @Page
    List<Example> query(Example example);
}
```
##数据权限
- 同样实体类继承BaseModel
- 在查询的实体中放入dataAuthority。map集合中的key是表的列名，value是关联数组。最后生成的sql会in这个数组，来实现数据权限的过滤。
例如：
```java
public interface ExampleMapper {

    @Authority
    List<Example> query(Example example);
}
```
