package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseTeacher;
import org.springframework.web.bind.annotation.PathVariable;

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

    /**
     * 新增/修改教师信息
     * @param companyId
     * @param courseTeacher
     * @return
     */
    public List<CourseTeacher> insertCourseTeacher(Long companyId, CourseTeacher courseTeacher);

    /**
     * 删除教师信息
     * @param courseId  课程id
     * @param id    教师id
     * @return
     */
    public void delCourseTeacher(Long courseId, Long id);


}
