package com.xiaodaidai.content;

import com.xiaodaidai.content.mapper.TeachplanMapper;
import com.xiaodaidai.content.model.dto.TeachplanDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class TeachplanMapperTests {

    @Autowired
    TeachplanMapper teachplanMapper;

    @Test
    public void setTeachplanMapper(){
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(117L);
        System.out.println(teachplanDtos);
    }
}
