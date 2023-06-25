package com.xiaodaidai.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaodaidai.base.exception.XuechengPlusException;
import com.xiaodaidai.content.mapper.CourseBaseMapper;
import com.xiaodaidai.content.mapper.CourseTeacherMapper;
import com.xiaodaidai.content.model.po.CourseBase;
import com.xiaodaidai.content.model.po.CourseTeacher;
import com.xiaodaidai.content.service.CourseTeacherInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseTeacherInfoImpl implements CourseTeacherInfoService {
    @Autowired
    CourseTeacherMapper courseTeacherMapper;

    @Autowired
    CourseBaseMapper courseBaseMapper;
    /**
     * 查询教师列表
     * @param courseId
     * @return
     */
    @Override
    public List<CourseTeacher> findCourseTeacherInfo(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId);
        return courseTeacherMapper.selectList(queryWrapper);
    }

    /**
     * 新增教师信息
     * @param companyId
     * @param courseTeacher
     * @return
     */
    @Override
    public List<CourseTeacher> insertCourseTeacher(Long companyId, CourseTeacher courseTeacher) {
        //只允许向机构自己的课程中添加老师、删除老师。
        //获取课程id
        Long courseId = courseTeacher.getCourseId();
        //根据课程id查询课程信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        //数据合法性校验
        //根据具体的业务逻辑校验
        //本机构只能修改本机构的课程
        if (!courseBase.getCompanyId().equals(companyId)){
            XuechengPlusException.cast("本机构只能向本机构的课程添加教师，越界啦");
        }
        if (courseTeacher.getId()!=null){
            int update = courseTeacherMapper.updateById(courseTeacher);
            if (update<=0)
                XuechengPlusException.cast("更新教师失败，自己找问题");
        }else {
            int insert = courseTeacherMapper.insert(courseTeacher);
            if (insert<=0)
                XuechengPlusException.cast("插入教师失败，自己找问题");
        }

        return findCourseTeacherInfo(courseId);
    }


    /**
     * 删除教师信息
     * @param courseId  课程id
     * @param id    教师id
     */
    @Override
    public void delCourseTeacher(Long courseId, Long id) {
        //删除对应教师
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId).eq(CourseTeacher::getId,id);
        courseTeacherMapper.delete(queryWrapper);
    }


}
