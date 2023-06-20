package com.xuecheng.content.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;


public interface CourseBaseInfoService {
    /**
     * 课程分页查询
     * @param companyId
     * @param pageParams
     * @param courseParamsDto
     * @return
     */
    public PageResult<CourseBase>  queryCourseBaseList(Long companyId,PageParams pageParams, QueryCourseParamsDto courseParamsDto);

    /**
     * 新增课程
     * @param companyId  机构id
     * @param addCourseDto 课程信息
     * @return  课程详细信息
     */
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto);


    /**
     * 根据课程id查询课程信息
     * @param courseId
     * @return
     */
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId);

    /**
     * 修改课程信息
     * @param companyId 机构id
     * @param editCourseDto 课程信息
     * @return
     */
    public CourseBaseInfoDto updateCourseBase(Long companyId,EditCourseDto editCourseDto);

    /**
     * 删除指定课程信息
     * @param courseId
     */
    public void delCourseBase(Long courseId);
}
