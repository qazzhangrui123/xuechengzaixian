package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XuechengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {
    @Autowired
    CourseBaseMapper courseBaseMapper;


    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    /**
     * 课程分页查询
     * @param pageParams
     * @param queryCourseParamsDto
     * @return
     */
    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {

        //拼装查询条件
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //根据名称模糊查询 course_base.name like '%值%'
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()),CourseBase::getName,queryCourseParamsDto.getCourseName());
        //根据课程的审核状态查询 course_base.audit_status = ?
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()), CourseBase::getAuditStatus, queryCourseParamsDto.getAuditStatus());

        //创建page分页参数对象，参数：当前页码，每页记录数
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(),pageParams.getPageSize());
        //开始进行分页查询
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        //数据列表
        List<CourseBase> items = pageResult.getRecords();
        //总记录数
        long total = pageResult.getTotal();

        PageResult<CourseBase> result = new PageResult<>(items,total,pageParams.getPageNo(),pageParams.getPageSize());
        return result;
    }


    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {
        //参数合法性校验
//        if (StringUtils.isBlank(dto.getName())) {
////            throw new RuntimeException("课程名称为空");
//            XuechengPlusException.cast("课程名称为空");
//        }
//
//        if (StringUtils.isBlank(dto.getMt())) {
//            throw new RuntimeException("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getSt())) {
//            throw new RuntimeException("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getGrade())) {
//            throw new RuntimeException("课程等级为空");
//        }
//
//        if (StringUtils.isBlank(dto.getTeachmode())) {
//            throw new RuntimeException("教育模式为空");
//        }
//
//        if (StringUtils.isBlank(dto.getUsers())) {
//            throw new RuntimeException("适应人群为空");
//        }
//
//        if (StringUtils.isBlank(dto.getCharge())) {
//            throw new RuntimeException("收费规则为空");
//        }
        //向课程基本信息表course_base写入数据
        CourseBase courseBase = new CourseBase();
        //将传入的页面参数放到courseBase对象
//        courseBase.setName(dto.getName());
//        courseBase.setDescription(dto.getDescription());
        //上边从原始对象中get拿数据向新对象set，比较复杂
        BeanUtils.copyProperties(dto,courseBase);  //只要属性名称一致就可以拷贝
        courseBase.setCompanyId(companyId);
        courseBase.setCreateDate(LocalDateTime.now());
        //审核状态默认为未提交
        courseBase.setAuditStatus("202002");
        //发布状态为未发布
        courseBase.setStatus("203001");
        //插入数据库
        int insert = courseBaseMapper.insert(courseBase);
        if (insert<=0)
            throw new RuntimeException("插入失败");
        //向课程营销系course_market写入数据
        CourseMarket courseMarket = new CourseMarket();
        //将页面输入的数据拷贝到courseMarket
        BeanUtils.copyProperties(dto,courseMarket);
        //主键：课程id
        Long id = courseBase.getId();
        courseMarket.setId(id);

        //保存营销信息
        int a = saveCourseMarket(courseMarket);
        //从数据库查出课程的详细信息,包括两部分
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(id);

        return courseBaseInfo;
    }


    //查询课程信息
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId){
        //从课程基本信息表查询
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase==null)
            return null;
        //从课程营销表查询
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //组装在一起
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        if (courseMarket!=null)
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);
        //通过CourseCategoryMapper查询分类信息，把分类名称放在courseBaseInfoDto对象里
        CourseCategory courseCategory = courseCategoryMapper.selectById(courseBaseInfoDto.getSt());
        courseBaseInfoDto.setMtName(courseCategory.getName());
        CourseCategory mt = courseCategoryMapper.selectById(courseBaseInfoDto.getMt());
        courseBaseInfoDto.setMtName(mt.getName());

        return courseBaseInfoDto;
    }

    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {
        //拿到课程id
        Long courseId = editCourseDto.getId();
        //查询课程信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase==null){
            XuechengPlusException.cast("课程不存在");
        }
        //数据合法性校验
        //根据具体的业务逻辑校验
        //本机构只能修改本机构的课程
        if (!companyId.equals(courseBase.getCompanyId())){
            XuechengPlusException.cast("本机构只能修改本机构的课程");
        }
        //封装数据
        BeanUtils.copyProperties(editCourseDto,courseBase);
        //修改时间
        courseBase.setCreateDate(LocalDateTime.now());
        //更新数据库
        int i = courseBaseMapper.updateById(courseBase);
        if (i<=0)
            XuechengPlusException.cast("修改课程失败");
        //更新营销信息
        //查询营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        BeanUtils.copyProperties(editCourseDto,courseMarket);
        int aa = courseMarketMapper.updateById(courseMarket);
        if (aa<=0)
            XuechengPlusException.cast("修改营销信息失败");
        //查询课程信息
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    //单独写一个方法，保存营销信息，逻辑：存在则更新，不存在则添加
    private int saveCourseMarket(CourseMarket courseMarket){
        //参数合法性校验
        String charge = courseMarket.getCharge();
        if (StringUtils.isEmpty(charge)){
            throw new RuntimeException("收费规则为空");
        }
        //如果课程收费，价格没有填写
        if (charge.equals("201001")){
            if (courseMarket.getPrice()==null||courseMarket.getPrice()<=0)
//                throw new RuntimeException("课程价格不能为空且大于0");
                XuechengPlusException.cast("课程价格不能为空且大于0");
        }
        //从数据库查询营销信息，存在则更新，不存在则添加
        Long id = courseMarket.getId();
        CourseMarket market = courseMarketMapper.selectById(id);
        if (market==null){
//            插入数据库
            int insert = courseMarketMapper.insert(courseMarket);
            return insert;
        }else {
            BeanUtils.copyProperties(courseMarket,market);
            market.setId(courseMarket.getId());
            //更新
            courseMarketMapper.updateById(market);
        }
        return 0;
    }
}
