package com.xuecheng.learning.service.impl;

import com.xuecheng.base.model.RestResponse;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTablesService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LearningServiceImpl implements LearningService {

    @Autowired
    MyCourseTablesService myCourseTablesService;

    @Autowired
    ContentServiceClient contentServiceClient;

    @Autowired
    MediaServiceClient mediaServiceClient;
    @Override
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId) {
        //查询课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish==null){
            return RestResponse.validfail("课程不存在");
        }
        //远程调用内容管理服务根据课程计划id（teachplanid）去查询课程计划信息，如果is_preview为1表示支持试学
        //也可以从coursepublish对象中解析出课程计划信息去判断是否支持试学
        //todo:自己做

        //用户已登录
        //获取学习资格
        if (StringUtils.isNotEmpty(userId)){
            //通过我的课程表查询用户的学习资格
            XcCourseTablesDto xcCourseTablesDto = myCourseTablesService.getLearningStatus(userId, courseId);
            String learnStatus = xcCourseTablesDto.getLearnStatus();
            if (learnStatus.equals("702002")){
                return RestResponse.validfail("无法学习，因为没有选课或选课后没有支付");
            }else if (learnStatus.equals("702003")){
                return RestResponse.validfail("已过期");
            }else {
                ///有资格学习，要返回视频的播放地址
                //远程调用媒资服务获取视频的播放地址
                RestResponse<String> playUrlByMediaId = mediaServiceClient.getPlayUrlByMediaId(mediaId);
                return playUrlByMediaId;
            }
        }else {
            //用户没有登录
            //取出收费规则
            String charge = coursepublish.getCharge();
            if (charge.equals("201000")){
                ///有资格学习，要返回视频的播放地址
                RestResponse<String> playUrlByMediaId = mediaServiceClient.getPlayUrlByMediaId(mediaId);
                return playUrlByMediaId;
            }

        }
        return RestResponse.validfail("该课程没有选课");
    }
}
