package com.xuecheng.work.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.work.model.po.Work;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author itcast
 * @since 2023-06-23
 */
public interface WorkService  {

    /**
     * 查询作业信息
     * @param pageParams
     * @return
     */
    public PageResult<Work> queryWorkInfo(Long companyId,PageParams pageParams);

}
