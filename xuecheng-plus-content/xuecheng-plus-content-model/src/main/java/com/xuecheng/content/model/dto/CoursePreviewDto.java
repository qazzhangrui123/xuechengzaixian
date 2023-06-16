package com.xuecheng.content.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 课程预览
 */
@Data
public class CoursePreviewDto {

    //课程基本信息l,课程营销信息
    private CourseBaseInfoDto courseBase;

    //课程计划信息
    public List<TeachplanDto>  teachplans;

    //课程师资信息...
}
