package com.xiaodaidai.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.xiaodaidai.base.exception.CommonError;
import com.xiaodaidai.base.exception.XuechengPlusException;
import com.xiaodaidai.content.config.MultipartSupportConfig;
import com.xiaodaidai.content.feignclient.MediaServiceClient;
import com.xiaodaidai.content.mapper.CourseBaseMapper;
import com.xiaodaidai.content.mapper.CourseMarketMapper;
import com.xiaodaidai.content.mapper.CoursePublishMapper;
import com.xiaodaidai.content.mapper.CoursePublishPreMapper;
import com.xiaodaidai.content.model.dto.CourseBaseInfoDto;
import com.xiaodaidai.content.model.dto.CoursePreviewDto;
import com.xiaodaidai.content.model.dto.TeachplanDto;
import com.xiaodaidai.content.model.po.CourseBase;
import com.xiaodaidai.content.model.po.CourseMarket;
import com.xiaodaidai.content.model.po.CoursePublish;
import com.xiaodaidai.content.model.po.CoursePublishPre;
import com.xiaodaidai.content.service.CourseBaseInfoService;
import com.xiaodaidai.content.service.CoursePublishService;
import com.xiaodaidai.content.service.TeachplanService;
import com.xiaodaidai.messagesdk.model.po.MqMessage;
import com.xiaodaidai.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 课程发布相关接口
 */
@Slf4j
@Service
public class CoursePublishServiceImpl implements CoursePublishService {
    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @Autowired
    TeachplanService teachplanService;

    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;

    @Autowired
    CourseBaseMapper courseBaseMapper;
    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CoursePublishMapper coursePublishMapper;

    @Autowired
    MediaServiceClient mediaServiceClient;

    @Autowired
    MqMessageService mqMessageService;

    @Autowired
    RedisTemplate redisTemplate;

    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        //课程基本信息，营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        //课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        coursePreviewDto.setCourseBase(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplanTree);
        return coursePreviewDto;
    }

    @Override
    @Transactional
    public void commitAudit(Long companyId, Long courseId) {
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if (courseBaseInfo == null) {
            XuechengPlusException.cast("课程找不到呀");
        }
        //校验：本机构只能提交本机构的课程

        //todo:

        //审核状态
        String auditStatus = courseBaseInfo.getAuditStatus();
        //如果课程的审核状态为已提交则不允许提交
        if (auditStatus.equals("202003")) {
            XuechengPlusException.cast("课程已提交轻等待审核");
        }
        //课程的图片\计划信息没有填写也不允许提交
        String pic = courseBaseInfo.getPic();
        if (StringUtils.isEmpty(pic)) {
            XuechengPlusException.cast("请上传课程图片");
        }
        //课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        if (teachplanTree == null || teachplanTree.size() == 0) {
            XuechengPlusException.cast("请编写课程计划");
        }
        //查询到课程基本信息、营销信息、计划等信息插入到课程预发布表
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfo, coursePublishPre);
        //设置机构id
        coursePublishPre.setCompanyId(companyId);
        //营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //转json
        String courseMarketJson = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(courseMarketJson);
        //计划信息
        //转json
        String teachplanTreeJson = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachplan(teachplanTreeJson);
        //修改状态为已提交
        coursePublishPre.setStatus("202003");
        //提交时间
        coursePublishPre.setCreateDate(LocalDateTime.now());
        //查询预发布表，如果有记录则更新，没有则插入
        CoursePublishPre pre = coursePublishPreMapper.selectById(courseId);
        if (pre == null) {
            coursePublishPreMapper.insert(coursePublishPre);
        } else {
            coursePublishPreMapper.updateById(coursePublishPre);
        }
        //更新课程基本信息表的审核状态为已提交
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setAuditStatus("202003");//审核状态为已提交
        courseBaseMapper.updateById(courseBase);
    }

    @Override
    public void publish(Long companyId, Long courseId) {
        //查询预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null)
            XuechengPlusException.cast("课程没有审核记录，无法发布");
        //状态
        String status = coursePublishPre.getStatus();
        //课程如果没有审核通过不允许发布
        if (!status.equals("202004")) {
            XuechengPlusException.cast("课程没有审核通过不允许发布");
        }
        //向课程发布表写入数据
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre, coursePublish);
        //先查询课程发布表
        CoursePublish select = coursePublishMapper.selectById(courseId);
        if (select == null) {
            //向消息表写入数据
            coursePublishMapper.insert(coursePublish);
        } else {
            coursePublishMapper.updateById(coursePublish);
        }
        //向消息表写入数据
        saveCoursePublishMessage(courseId);

        //将预发布表数据删除
        coursePublishPreMapper.deleteById(coursePublishPre);

    }

    @Override
    public File generateCourseHtml(Long courseId) {
        //配置freemarker
        Configuration configuration = new Configuration(Configuration.getVersion());
        //最终返回的静态文件
        File htmlfile = null;
        //加载模板
        //选指定模板路径,classpath下templates下
        //得到classpath路径
        ClassPathResource templates = new ClassPathResource("templates");
        try {
            configuration.setDirectoryForTemplateLoading(templates.getFile());

            //设置字符编码
            configuration.setDefaultEncoding("utf-8");

            //指定模板文件名称
            Template template = configuration.getTemplate("course_template.ftl");

            //准备数据
            CoursePreviewDto coursePreviewInfo = this.getCoursePreviewInfo(courseId);

            Map<String, Object> map = new HashMap<>();
            map.put("model", coursePreviewInfo);

            //静态化
            //参数1：模板，参数2：数据模型
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
            System.out.println(content);
            //将静态化内容输出到文件中
            InputStream inputStream = IOUtils.toInputStream(content);
            htmlfile = File.createTempFile("coursepublish",".html");
            //输出流
            FileOutputStream outputStream = new FileOutputStream(htmlfile);
            IOUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            log.error("页面静态化报错，课程id:{}",courseId,e);
            e.printStackTrace();
        }
        return htmlfile;
    }

    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        try {
            //将file转成MultipartFile
            MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
            //远程调用得到返回值
            String upload = mediaServiceClient.upload(multipartFile, "course/"+courseId+".html");
            if (upload==null){
                log.debug("远程调用走降级逻辑，得到上传的结果为null，课程id：{}",courseId);
                XuechengPlusException.cast("上传静态文件过程中存在异常");
            }
        } catch (Exception e) {
            e.printStackTrace();
            XuechengPlusException.cast("上传静态文件过程中存在异常");
        }

    }

    /**
     * @param courseId 课程id
     * @return void
     * @description 保存消息表记录
     */
    private void saveCoursePublishMessage(Long courseId) {
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if (mqMessage == null) {
            XuechengPlusException.cast(CommonError.UNKOWN_ERROR);
        }
    }

    /**
     * 根据课程id查询课程发布信息
     * @param courseId
     * @return
     */
    public CoursePublish getCoursePublish(Long courseId){
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        return coursePublish ;
    }

    @Override
    public CoursePublish getCoursePublishCache(Long courseId) {
        //查询缓存
        Object  jsonObj = redisTemplate.opsForValue().get("course:" + courseId);
        if(jsonObj!=null){
            String jsonString = jsonObj.toString();
            CoursePublish coursePublish = JSON.parseObject(jsonString, CoursePublish.class);
            return coursePublish;
        }else{
            //调用redis的方法，执行setnx命令，谁执行成功谁拿到所
            Boolean lock01 = redisTemplate.opsForValue().setIfAbsent("lock01", "01");
            synchronized(this){
                jsonObj = redisTemplate.opsForValue().get("course:" + courseId);
                if(jsonObj!=null){
                    String jsonString = jsonObj.toString();
                    CoursePublish coursePublish = JSON.parseObject(jsonString, CoursePublish.class);
                    return coursePublish;
                }
                System.out.println("=========从数据库查询==========");
                //从数据库查询
                CoursePublish coursePublish = getCoursePublish(courseId);
                //设置过期时间300秒
                redisTemplate.opsForValue().set("course:" + courseId, JSON.toJSONString(coursePublish),300, TimeUnit.SECONDS);
                return coursePublish;
            }
        }
    }
}
