package com.xiaodaidai.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaodaidai.content.model.dto.CourseCategoryTreeDto;
import com.xiaodaidai.content.model.po.CourseCategory;

import java.util.List;

/**
 * <p>
 * 课程分类 Mapper 接口
 * 从数据库中查询数据
 * </p>
 *
 * @author itcast
 */
public interface CourseCategoryMapper extends BaseMapper<CourseCategory> {
    //使用递归查询分类
    public List<CourseCategoryTreeDto> selectTreeNodes(String id);

}
