package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XuechengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;

    /**
     * 根据id查询课程计划
     * @param courseId  课程计划
     * @return
     */
    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(courseId);
        return teachplanDtos;
    }
    /**
     * 新增/修改/保存课程计划
     * @param saveTeachplanDto
     */
    @Transactional
    @Override
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {
        //通过课程计划id判断新增还是修改
        Long teachplanId = saveTeachplanDto.getId();
        if (teachplanId==null){
            //新增
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            //确定排序字段，找到它的同级节点个数，排序字段为个数加一
            Long parentid = saveTeachplanDto.getParentid();
            Long courseId = saveTeachplanDto.getCourseId();
            int count = getTeachplanCount(parentid,courseId);
            teachplan.setOrderby(count);
            int insert = teachplanMapper.insert(teachplan);
            if (insert<=0)
                XuechengPlusException.cast("插入失败");
        }else {
            //修改
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);
            //将参数复制到teachplan
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            teachplanMapper.updateById(teachplan);
        }
    }

    /**
     * 删除课程计划
     * @param courseId
     */
    @Override
    public void delTeachplan(Long courseId) {
        Teachplan teachplan = teachplanMapper.selectById(courseId);
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(courseId);
        if (teachplan.getParentid()==0&&teachplanDtos.size()==0){
            //为大章节且大单节下没有小章节时可以正常删除
            teachplanMapper.deleteById(courseId);
        }else if (teachplan.getParentid()==0&&teachplanDtos.size()>0){
            //删除大章节，大章节下有小章节时不允许删除
            XuechengPlusException.cast("课程计划信息还有子级信息，无法操作");
        }else if (teachplan.getParentid()!=0){
            //删除小章节，同时将关联的信息进行删除
            //获取关联信息
            LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TeachplanMedia::getTeachplanId,courseId);
            //删除关联信息
            teachplanMediaMapper.delete(queryWrapper);
            //删除小章节
            teachplanMapper.deleteById(courseId);
        }
    }

    /**
     * 课程计划下移
     * @param courseId
     */
    @Override
    public void movedownTeachplan(Long courseId) {
        moveplan(courseId,false);
    }

    /**
     * 课程计划上移
     * @param courseId
     */
    @Override
    public void moveupTeachplan(Long courseId) {
        moveplan(courseId,true);
    }

    @Override
    @Transactional
    public void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {
        //先删除原有记录，根据课程计划id删除它所绑定的媒资
        teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>()
                .eq(TeachplanMedia::getTeachplanId,bindTeachplanMediaDto.getTeachplanId()));
        //课程计划id
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if (teachplan==null)
            XuechengPlusException.cast("课程计划不存在");
        Long courseId = teachplan.getCourseId();
        //再添加新的记录
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        BeanUtils.copyProperties(bindTeachplanMediaDto,teachplanMedia);
        teachplanMedia.setCourseId(courseId);
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        teachplanMediaMapper.insert(teachplanMedia);
    }

    private void moveplan(Long courseId,Boolean UpOrDown){
        //如果UpOrDown为false则为下移
        //否则则为上移

        //获取父节点的课程id
        Teachplan teachplan = teachplanMapper.selectById(courseId);
        Long parentid = teachplan.getParentid();
        //查询所有的的课程计划
        List<TeachplanDto> teachplanTree = teachplanMapper.selectTreeNodes(teachplan.getCourseId());
        //判断是大章节移动还是小章节移动
        if (parentid!=0){
            //小章节移动
            for (int i = 0; i < teachplanTree.size(); i++) {
                TeachplanDto teachplanDto = teachplanTree.get(i);
                if (teachplanDto.getId().equals(parentid)){
                    teachplanTree = teachplanTree.get(i).getTeachPlanTreeNodes();
                    break;
                }
            }
        }
        //获取当前计划的排序字段
        Integer preorderby = teachplan.getOrderby();
        //如果UpOrDown为false，则判断当前计划是否为最后一个
        if (preorderby==teachplanTree.size()&&!UpOrDown){
            //如果为最后一个则不变
            XuechengPlusException.cast("当前计划已经为最后一个计划啦，没法再往下啦");
        }else if (preorderby==1&&UpOrDown){
            //如果UpOrDown为true，则判断当前计划是否为第一个
            XuechengPlusException.cast("当前计划已经为第一个计划啦，没法再往上啦，你干嘛～～");
        }else {
            Teachplan teach;
            int m=-1,temp=0;
            if (!UpOrDown){
                //否则，与其下一个计划进行交换
                //获取到下一个计划
                for (int i = 0; i < teachplanTree.size(); i++) {
                    if (teachplanTree.get(i).getOrderby()>preorderby){
                        if (m==-1||teachplanTree.get(i).getOrderby()<temp){
                            m=i;
                            temp = teachplanTree.get(i).getOrderby();
                        }
                    }
                }
            }else{
                for (int i = 0; i < teachplanTree.size(); i++) {
                    if (teachplanTree.get(i).getOrderby()<preorderby){
                        if (m==-1||teachplanTree.get(i).getOrderby()>temp){
                            m=i;
                            temp = teachplanTree.get(i).getOrderby();
                        }
                    }
                }
            }
            //交换排序字段
            teach = teachplanTree.get(m);
            teach.setOrderby(preorderby);
            teachplan.setOrderby(temp);
            teachplanMapper.updateById(teachplan);
            teachplanMapper.updateById(teach);
        }
    }

    private int getTeachplanCount(Long parentid,Long courseId){
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,courseId).eq(Teachplan::getParentid,parentid);
        return teachplanMapper.selectCount(queryWrapper)+1;
    }



}

