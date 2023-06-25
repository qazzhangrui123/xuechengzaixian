package com.xiaodaidai.content;

import com.xiaodaidai.content.mapper.CourseCategoryMapper;
import com.xiaodaidai.content.model.dto.CourseCategoryTreeDto;
import com.xiaodaidai.content.service.CourseCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class CourseCategoryServiceTests {
    @Autowired
    CourseCategoryService courseCategoryService;

    @Test
    public void testCourseCategoryService(){
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryService.queryTreeNodes("1");
        System.out.println(courseCategoryTreeDtos);
    }
}
