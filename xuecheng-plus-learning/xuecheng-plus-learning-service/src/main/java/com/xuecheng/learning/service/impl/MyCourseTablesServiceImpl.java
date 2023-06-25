package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XuechengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.CoursePublish;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 选课相关的接口实现
 */
@Slf4j
@Service
public class MyCourseTablesServiceImpl implements MyCourseTablesService {
    @Autowired
    XcChooseCourseMapper xcChooseCourseMapper;

    @Autowired
    XcCourseTablesMapper xcCourseTablesMapper;

    @Autowired
    ContentServiceClient contentServiceClient;

    @Override
    @Transactional
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        //查询课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        //课程收费标准
        String charge = coursepublish.getCharge();
        //选课记录
        XcChooseCourse chooseCourse = null;
        if ("201000".equals(charge)) {//课程免费
            //添加免费课程
            chooseCourse = addFreeCoruse(userId, coursepublish);
            //添加到我的课程表
            XcCourseTables xcCourseTables = addCourseTabls(chooseCourse);
        } else {
            //添加收费课程
            chooseCourse = addChargeCoruse(userId, coursepublish);
        }
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(chooseCourse,xcChooseCourseDto);
        //获取学习资格
        XcCourseTablesDto xcCourseTablesDto = getLearningStatus(userId, courseId);
        xcChooseCourseDto.setLearnStatus(xcCourseTablesDto.getLearnStatus());
        return xcChooseCourseDto;
    }

    //学习资格状态 [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}
    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        //查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        if(xcCourseTables==null){
            XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
            //没有选课或选课后没有支付
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        BeanUtils.copyProperties(xcCourseTables,xcCourseTablesDto);
        //是否过期,true过期，false未过期
        boolean isExpires = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if(!isExpires){
            //正常学习
            xcCourseTablesDto.setLearnStatus("702001");
            return xcCourseTablesDto;
        }else{
            //已过期
            xcCourseTablesDto.setLearnStatus("702003");
            return xcCourseTablesDto;
        }

    }

    @Override
    public boolean saveChooseCourseStauts(String chooseCourseId) {
        //根据选课id查询选课表
        XcChooseCourse xcChooseCourse = xcChooseCourseMapper.selectById(chooseCourseId);
        if (xcChooseCourse==null){
            log.debug("接收购买课程的消息，根据选课id从数据库找不到选课记录.选课id：{}",chooseCourseId);
            return false;
        }
        //选课状态
        String status = xcChooseCourse.getStatus();
        //只有当未支付时才更新为已支付
        if (status.equals("701002")){
            //更新选课记录的状态为支付成功
            xcChooseCourse.setStatus("701001");
            int update = xcChooseCourseMapper.updateById(xcChooseCourse);
            if (update<=0){
                log.debug("添加选课记录失败:{}",xcChooseCourse);
                XuechengPlusException.cast("添加选课记录失败");
            }
            //向我的课程表插入记录
            XcCourseTables xcCourseTables = addCourseTabls(xcChooseCourse);
            return true;
        }
        return false;
    }

    @Override
    public PageResult<XcCourseTables> mycourestables(MyCourseTableParams params) {
        //用户id
        String userId = params.getUserId();
        //当前页码
        int pageNo = params.getPage();
        //每页记录数
        int size = params.getSize();
        Page<XcCourseTables> xcCourseTablesPage = new Page<>(pageNo,size);
        LambdaQueryWrapper<XcCourseTables> lambdaQueryWrapper = new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, userId);
        //查询数据
        Page<XcCourseTables> result = xcCourseTablesMapper.selectPage(xcCourseTablesPage, lambdaQueryWrapper);
        //数据列表
        List<XcCourseTables> records = result.getRecords();
        long total = result.getTotal();
        //List<T> items, long counts, long page, long pageSize
        PageResult xcCourseTablesPageResult = new PageResult(records,total,pageNo,size);
        return xcCourseTablesPageResult;
    }

    //添加免费课程,免费课程加入选课记录表、我的课程表
    public XcChooseCourse addFreeCoruse(String userId, CoursePublish coursepublish) {
        Long coursepublishId = coursepublish.getId();
        //如果存在免费的选课记录且选课状态为成功，直接返回
        LambdaQueryWrapper<XcChooseCourse> xcChooseCourseLambdaQueryWrapper = new LambdaQueryWrapper<>();
        xcChooseCourseLambdaQueryWrapper.eq(XcChooseCourse::getUserId,userId)
                .eq(XcChooseCourse::getCourseId,coursepublishId)
                .eq(XcChooseCourse::getOrderType,"700001")   //免费课程
                .eq(XcChooseCourse::getStatus,"701001");    //选课成功
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(xcChooseCourseLambdaQueryWrapper);
        if (xcChooseCourses.size()>0){
            return xcChooseCourses.get(0);
        }
        //添加选课记录信息
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(0f);//免费课程价格为0
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");//免费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701001");//选课成功

        xcChooseCourse.setValidDays(365);//免费课程默认365
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));
        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if (insert<=0){
            XuechengPlusException.cast("添加选课记录失败");
        }
        return xcChooseCourse;
    }

    //添加收费课程
    public XcChooseCourse addChargeCoruse(String userId,CoursePublish coursepublish){
        Long coursepublishId = coursepublish.getId();
        //如果存在免费的选课记录且选课状态为成功，直接返回
        LambdaQueryWrapper<XcChooseCourse> xcChooseCourseLambdaQueryWrapper = new LambdaQueryWrapper<>();
        xcChooseCourseLambdaQueryWrapper.eq(XcChooseCourse::getUserId,userId)
                .eq(XcChooseCourse::getCourseId,coursepublishId)
                .eq(XcChooseCourse::getOrderType,"700002")   //收费课程
                .eq(XcChooseCourse::getStatus,"701002");    //等待支付
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(xcChooseCourseLambdaQueryWrapper);
        if (xcChooseCourses.size()>0){
            return xcChooseCourses.get(0);
        }
        //向选课记录表写数据
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublishId);
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setOrderType("700002");   //收费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setValidDays(365);    //有效期
        xcChooseCourse.setStatus("701002");  //等待支付
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());  //有效期的开始时间
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(xcChooseCourse.getValidDays()));     //有效期的结束时间

        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if (insert<=0){
            XuechengPlusException.cast("添加选课记录失败");
        }
        return xcChooseCourse;
    }
    //添加到我的课程表
    public XcCourseTables addCourseTabls(XcChooseCourse xcChooseCourse){
        //选课记录完成且未过期可以添加课程到课程表
        String status = xcChooseCourse.getStatus();
        if (!"701001".equals(status)){
            XuechengPlusException.cast("选课未成功，无法添加到课程表");
        }
        //查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if(xcCourseTables!=null){
            return xcCourseTables;
        }
        XcCourseTables xcCourseTablesNew = new XcCourseTables();
        xcCourseTablesNew.setChooseCourseId(xcChooseCourse.getId());
        xcCourseTablesNew.setUserId(xcChooseCourse.getUserId());
        xcCourseTablesNew.setCourseId(xcChooseCourse.getCourseId());
        xcCourseTablesNew.setCompanyId(xcChooseCourse.getCompanyId());
        xcCourseTablesNew.setCourseName(xcChooseCourse.getCourseName());
        xcCourseTablesNew.setCreateDate(LocalDateTime.now());
        xcCourseTablesNew.setValidtimeStart(xcChooseCourse.getValidtimeStart());
        xcCourseTablesNew.setValidtimeEnd(xcChooseCourse.getValidtimeEnd());
        xcCourseTablesNew.setCourseType(xcChooseCourse.getOrderType());
        xcCourseTablesMapper.insert(xcCourseTablesNew);

        return xcCourseTablesNew;
    }

    /**
     * @description 根据课程和用户查询我的课程表中某一门课程
     * @param userId
     * @param courseId
     * @return com.xuecheng.learning.model.po.XcCourseTables
     * @author Mr.M
     * @date 2022/10/2 17:07
     */
    public XcCourseTables getXcCourseTables(String userId,Long courseId){
        XcCourseTables xcCourseTables = xcCourseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, userId).eq(XcCourseTables::getCourseId, courseId));
        return xcCourseTables;

    }
}
