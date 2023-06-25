package com.xiaodaidai.content.model.dto;

import com.xiaodaidai.content.model.po.Teachplan;
import com.xiaodaidai.content.model.po.TeachplanMedia;
import lombok.Data;

import java.util.List;

/**
 * 课程计划信息模型类
 */
@Data
public class TeachplanDto extends Teachplan {
    //与媒资关联的信息
    private TeachplanMedia teachplanMedia;

    //小章节list
    private List<TeachplanDto> teachPlanTreeNodes;
}
