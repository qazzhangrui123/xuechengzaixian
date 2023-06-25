package com.xiaodaidai.content.service;

import com.xiaodaidai.content.model.dto.CoursePreviewDto;
import com.xiaodaidai.content.model.po.CoursePublish;

import java.io.File;

/**
 * 课程发布相关的接口
 */
public interface CoursePublishService {
    /**
     * @description 获取课程预览信息
     * @param courseId 课程id
     * @return com.xuecheng.content.model.dto.CoursePreviewDto
     * @author Mr.M
     */
    public CoursePreviewDto getCoursePreviewInfo(Long courseId);

    /**
     * @description 提交审核
     * @param courseId  课程id
     * @return void
     * @author Mr.M
     */
    public void commitAudit(Long companyId,Long courseId);

    /**
     * 课程发布
     * @param companyId
     * @param courseId
     */
    public void publish(Long companyId,Long courseId);

    /**
     * @description 课程静态化
     * @param courseId  课程id
     * @return File 静态化文件
     */
    public File generateCourseHtml(Long courseId);
    /**
     * @description 上传课程静态化页面
     * @param file  静态化文件
     * @return void
     */
    public void  uploadCourseHtml(Long courseId,File file);


    /**
     * 查询课程发布信息
     * @param courseId
     * @return
     */
    public CoursePublish getCoursePublish(Long courseId);

    /**
     * @description 查询缓存中的课程信息
     * @param courseId
     * @return com.xuecheng.content.model.po.CoursePublish
     */
    public CoursePublish getCoursePublishCache(Long courseId);
}
