package com.github.mybatis.helper.datascope;

public class ActiveScopeFieldSettings {
    private String value;
    private String columnName;

    public ActiveScopeFieldSettings() {
    }

    public ActiveScopeFieldSettings(String value, String columnName) {
        this.value = value;
        this.columnName = columnName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
}
