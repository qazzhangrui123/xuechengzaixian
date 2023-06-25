package com.xiaodaidai.content.api;

import com.xiaodaidai.base.exception.ValidationGroups;
import com.xiaodaidai.content.model.dto.AddCourseDto;
import com.xiaodaidai.content.model.dto.CourseBaseInfoDto;
import com.xiaodaidai.content.model.dto.TeachplanDto;
import com.xiaodaidai.content.model.po.CourseTeacher;
import com.xiaodaidai.content.service.CourseTeacherInfoService;
import com.xiaodaidai.content.service.TeachplanService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 新增/修改教师
     * @param courseTeacher
     * @return
     */
    @ApiOperation("新增教师")
    @PostMapping("/courseTeacher")
    public List<CourseTeacher> createCourseBase(@RequestBody @Validated(ValidationGroups.Insert.class) CourseTeacher courseTeacher){
        //获取到用户所属机构id
        //机构id，由于认证系统没有上线暂时硬编码
        Long companyId = 1232141425L;
        return courseTeacherInfoService.insertCourseTeacher(companyId,courseTeacher);
    }

    /**
     * 删除教师
     * @param id
     * @param courseId
     * @return
     */
    @ApiOperation("删除教师")
    @DeleteMapping("/courseTeacher/course/{courseId}/{id}")
    public void deleteCourseBase(@PathVariable Long courseId,@PathVariable Long id){
        courseTeacherInfoService.delCourseTeacher(courseId,id);
    }

}
