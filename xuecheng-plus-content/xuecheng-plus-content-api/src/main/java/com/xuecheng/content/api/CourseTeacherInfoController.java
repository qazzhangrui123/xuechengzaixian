package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherInfoService;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CourseTeacherInfoController {
    @Autowired
    CourseTeacherInfoService courseTeacherInfoService;
    //查询师资信息
    @ApiOperation("查询师资信息")
    @GetMapping("/courseTeacher/list/{courseId}")
    public List<CourseTeacher> getTreeNodes(@PathVariable Long courseId){
        return courseTeacherInfoService.findCourseTeacherInfo(courseId);
    }
}
