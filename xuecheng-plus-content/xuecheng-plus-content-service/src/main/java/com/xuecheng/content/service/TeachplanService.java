package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

/**
 * 课程计划管理相关接口
 */
public interface TeachplanService {
    /**
     * 根据id查询课程计划
     * @param courseId  课程计划
     * @return
     */
    public List<TeachplanDto> findTeachplanTree(Long courseId);

    /**
     * 新增/修改/保存课程计划
     * @param saveTeachplanDto
     */
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto);

    /**
     * 删除课程计划
     * @param courseId
     */
    public void delTeachplan(Long courseId);

    /**
     * 课程计划下移
     * @param courseId
     */
    public void movedownTeachplan(Long courseId);

    /**
     * 课程计划上移
     * @param courseId
     */
    public void moveupTeachplan(Long courseId);
}
