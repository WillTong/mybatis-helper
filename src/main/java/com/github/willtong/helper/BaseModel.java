package com.github.willtong.helper;

import java.io.Serializable;
import java.util.Map;

public class BaseModel implements Serializable {
    public Integer rows = 10;// 每页显示记录数
    public Integer page = 1;// 下一页 页号
    private int begin;
    private int end;
    private String sort;//排序字段
    private String order;//排序方式 可以是 'asc' 或者 'desc'，默认值是 'asc'。
    private int total;//总记录数
    private Map<String,String[]> dataAuthority; //权限控制

    public Integer getRows() {
        return rows;
    }

    public void setRows(Integer rows) {
        this.rows = rows;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public Map<String, String[]> getDataAuthority() {
        return dataAuthority;
    }

    public void setDataAuthority(Map<String, String[]> dataAuthority) {
        this.dataAuthority = dataAuthority;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }
}
