package com.xiaodaidai.learning.api;

import com.xiaodaidai.base.model.RestResponse;
import com.xiaodaidai.learning.service.LearningService;
import com.xiaodaidai.learning.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Mr.M
 * @version 1.0
 * @description 我的学习接口
 * @date 2022/10/27 8:59
 */
@Api(value = "学习过程管理接口", tags = "学习过程管理接口")
@Slf4j
@RestController
public class MyLearningController {


    @Autowired
    LearningService learningService;
    /**
     *
     * @param courseId   课程id
     * @param teachplanId  课程计划id
     * @param mediaId   媒资文件id
     * @return
     */
    @ApiOperation("获取视频")
    @GetMapping("/open/learn/getvideo/{courseId}/{teachplanId}/{mediaId}")
    public RestResponse<String> getvideo(@PathVariable("courseId") Long courseId,@PathVariable("teachplanId") Long teachplanId, @PathVariable("mediaId") String mediaId){
        //判断学习资格
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        String userId = user.getId();
        //获取视频
        RestResponse<String> video = learningService.getVideo(userId, courseId, teachplanId, mediaId);
        //有学习资格了远程调用媒资服务查询视频的播放地址
        return video;
    }
}
