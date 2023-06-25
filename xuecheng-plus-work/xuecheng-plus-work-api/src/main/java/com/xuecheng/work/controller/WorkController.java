package com.xuecheng.work.controller;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.work.model.po.Work;
import com.xuecheng.work.service.WorkService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author itcast
 */
@Slf4j
@RestController
@Api(value = "作业信息管理接口",tags = "作业信息管理接口")
public class WorkController {

    @Autowired
    private WorkService workService;

    @ApiOperation("作业查询接口")
    @PostMapping("/work/list")
    public PageResult<Work> list(PageParams pageParams){
        //机构id，由于认证系统没有上线暂时硬编码
        Long companyId = 1232141425L;
        PageResult<Work> workPageResult = workService.queryWorkInfo(companyId, pageParams);
        return workPageResult;
    }
}
