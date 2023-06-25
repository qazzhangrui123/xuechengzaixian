package com.xiaodaidai.content.service.impl;

import com.xiaodaidai.content.mapper.CourseCategoryMapper;
import com.xiaodaidai.content.model.dto.CourseCategoryTreeDto;
import com.xiaodaidai.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {
    @Autowired
    CourseCategoryMapper courseCategoryMapper;
    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //调用mapper递归查询出分类信息
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);
        //找到每个节点的子节点，最终封装成List<CourseCategoryTreeDto>
        //先将list转成map，key就是节点的id，value就是CourseCategoryTreeDto对象，目的为了方便从map获取节点
        //filter(item->!id.equals(item.getId()))把根节点排除
        Map<String, CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtos.stream().filter(item->!id.equals(item.getId())).collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));
        //定义一个结果list
        ArrayList<CourseCategoryTreeDto> courseCategoryList = new ArrayList<>();
        //从头遍历List<CourseCategoryTreeDto> ，边遍历边找子节点放在父节点的childrenTreeNodes
        courseCategoryTreeDtos.stream().filter(item->!id.equals(item.getId())).forEach(item->{
            if (item.getParentid().equals(id)){
                //向list中写入元素
                courseCategoryList.add(item);
            }
            CourseCategoryTreeDto courseCategoryParent = mapTemp.get(item.getParentid());
            if (courseCategoryParent!=null){
                if (courseCategoryParent.getChildrenTreeNodes()==null){
                    //如果该父节点的属性为空，则初始化
                    courseCategoryParent.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }
                //找到每个子节点，放在父节点的childrenTreeNodes中
                courseCategoryParent.getChildrenTreeNodes().add(item);
            }

        });
        return courseCategoryList;
    }
}
