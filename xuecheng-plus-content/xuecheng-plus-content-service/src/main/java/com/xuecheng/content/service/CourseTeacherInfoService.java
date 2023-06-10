package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

/**
 * 师资管理接口
 */
public interface CourseTeacherInfoService {
    /**
     * 查询教师列表
     * @param courseId    课程id
     * @return
     */
    public List<CourseTeacher> findCourseTeacherInfo(Long courseId);
}
