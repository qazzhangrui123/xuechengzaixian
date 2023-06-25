package com.xiaodaidai.content.api;


import com.xiaodaidai.base.exception.ValidationGroups;
import com.xiaodaidai.base.model.PageParams;
import com.xiaodaidai.base.model.PageResult;
import com.xiaodaidai.content.model.dto.AddCourseDto;
import com.xiaodaidai.content.model.dto.CourseBaseInfoDto;
import com.xiaodaidai.content.model.dto.EditCourseDto;
import com.xiaodaidai.content.model.dto.QueryCourseParamsDto;
import com.xiaodaidai.content.model.po.CourseBase;
import com.xiaodaidai.content.service.CourseBaseInfoService;
import com.xiaodaidai.content.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Api(value = "课程信息管理接口",tags = "课程信息管理接口")
@RestController//相当于@Controller + @responseBody
public class CourseBaseInfoController {

    @Autowired
    CourseBaseInfoService courseBaseInfoService;
    @ApiOperation("课程查询接口")
    @PreAuthorize("hasAuthority('xc_teachmanager_course_list')")  //指定权限标识符
    @PostMapping("/course/list")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto){
        //当前登录用户
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        //用户所属机构id
        Long companyId = null;
        if (!StringUtils.isEmpty(user.getCompanyId()))
            companyId = Long.parseLong(user.getCompanyId());
        PageResult<CourseBase> courseBasePageResult = courseBaseInfoService.queryCourseBaseList(companyId,pageParams, queryCourseParamsDto);
        return courseBasePageResult;
    }

    /**
     * 新增课程
     * @param addCourseDto
     * @return
     */
    @ApiOperation("新增课程")
    @PostMapping("/course")
    public CourseBaseInfoDto createCourseBase(@RequestBody @Validated(ValidationGroups.Insert.class) AddCourseDto addCourseDto){
        //获取到用户所属机构id
        //机构id，由于认证系统没有上线暂时硬编码
        Long companyId = 1232141425L;
        CourseBaseInfoDto courseBase = courseBaseInfoService.createCourseBase(companyId, addCourseDto);
        return courseBase;
    }


    /**
     * 根据课程id查询课程
     * @param courseId
     * @return
     */
    @ApiOperation("根据课程id查询课程")
    @GetMapping("/course/{courseId}")
    public CourseBaseInfoDto getCourseBaseById(@PathVariable Long courseId){
        //获取当前用户身份
//        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        System.out.println(principal);
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        System.out.println(user.getUsername());
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    @ApiOperation("修改课程")
    @PutMapping("/course")
    public CourseBaseInfoDto modifyCourseBase(@RequestBody @Validated(ValidationGroups.Update.class) EditCourseDto editCourseDto){
        //机构id，由于认证系统没有上线暂时硬编码
        Long companyId = 1232141425L;
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.updateCourseBase(companyId, editCourseDto);
        return courseBaseInfoDto;
    }


    @ApiOperation("删除课程")
    @DeleteMapping("/course/{courseId}")
    public void delCourseBase(@PathVariable Long courseId){
        courseBaseInfoService.delCourseBase(courseId);
    }
}
