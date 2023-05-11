package com.xuecheng.base.model;

import lombok.Data;
import lombok.ToString;

/**
 * 分页查询参数
 */
@Data
@ToString
public class PageParams {
    //当前页码
    private Long pageNo =1L;
    //每页显示记录数
    private Long pageSize =30L;

    public PageParams() {
    }

    public PageParams(Long pageNo, Long pageSize) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }
}
