package com.xuecheng.work.model.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author itcast
 */
@Data
@TableName("work")
public class Work implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 机构ID
     */
    private Long companyId;

    /**
     * 机构名称
     */
    private String companyName;

    private String bindCourse;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createDate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime changeDate;

    private String question;

    private Long status;

    private String titile;

    private String username;

    private Long userNum;


}
