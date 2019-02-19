package com.github.mybatis.helper.page;

import org.apache.ibatis.session.RowBounds;

public class PagingBounds extends RowBounds {
    private long total;
    private int offset;
    private int limit;

    public PagingBounds() {
        this.offset = NO_ROW_OFFSET;
        this.limit = NO_ROW_LIMIT;
    }

    public PagingBounds(int offset, int limit) {
        this.offset = offset;
        this.limit = limit;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    public void setDefault() {
        this.offset = NO_ROW_OFFSET;
        this.limit = NO_ROW_LIMIT;
    }

    public int getSelectCount() {
        return limit + offset;
    }
}
