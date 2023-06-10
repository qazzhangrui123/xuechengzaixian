package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseTeacherInfoImpl implements CourseTeacherInfoService {
    @Autowired
    CourseTeacherMapper courseTeacherMapper;
    /**
     * 查询教师列表
     * @param courseId
     * @return
     */
    @Override
    public List<CourseTeacher> findCourseTeacherInfo(Long courseId) {
        //只允许向机构自己的课程中添加老师、删除老师。
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId);
        return courseTeacherMapper.selectList(queryWrapper);
    }
}
