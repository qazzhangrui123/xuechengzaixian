package com.xiaodaidai.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaodaidai.base.model.PageParams;
import com.xiaodaidai.base.model.PageResult;
import com.xiaodaidai.content.mapper.CourseBaseMapper;
import com.xiaodaidai.content.model.dto.QueryCourseParamsDto;
import com.xiaodaidai.content.model.po.CourseBase;
import com.xiaodaidai.content.service.CourseBaseInfoService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class CourseBaseInfoServiceTests {
    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @Test
    public void testCourseBaseMapper(){

        QueryCourseParamsDto queryCourseParamsDto = new QueryCourseParamsDto();
        queryCourseParamsDto.setCourseName("java");  //课程名称查询条件
        queryCourseParamsDto.setAuditStatus("202004");

        //分页参数对象
        PageParams pageParams = new PageParams();
        pageParams.setPageNo(1L);
        pageParams.setPageSize(2L);


        PageResult<CourseBase> courseBasePageResult = courseBaseInfoService.queryCourseBaseList(null,pageParams,queryCourseParamsDto);
        System.out.println(courseBasePageResult);
    }
}
