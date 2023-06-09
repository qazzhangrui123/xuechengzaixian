package com.xiaodaidai.work.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaodaidai.base.model.PageParams;
import com.xiaodaidai.base.model.PageResult;
import com.xiaodaidai.work.model.po.Work;
import com.xiaodaidai.work.service.WorkService;
import com.xiaodaidai.work.mapper.WorkMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author itcast
 */
@Slf4j
@Service
public class WorkServiceImpl implements WorkService {

    @Autowired
    WorkMapper workMapper;

    /**
     * 作业分页信息查询
     * @param pageParams
     * @return
     */
    @Override
    public PageResult<Work> queryWorkInfo(Long companyId,PageParams pageParams) {
        //拼装查询条件
        LambdaQueryWrapper<Work> queryWrapper = new LambdaQueryWrapper<>();
        //根据机构id拼装查询条件
        queryWrapper.eq(Work::getCompanyId,companyId);

        //创建page分页参数对象，参数：当前页码，每页记录数
        Page<Work> page = new Page<>(pageParams.getPageNo(),pageParams.getPageSize());
        //开始进行分页查询
        Page<Work> pageResult = workMapper.selectPage(page, queryWrapper);
        //数据列表
        List<Work> items = pageResult.getRecords();
        //总记录数
        long total = pageResult.getTotal();

        PageResult<Work> result = new PageResult<>(items,total,pageParams.getPageNo(),pageParams.getPageSize());
        return result;
    }

}
