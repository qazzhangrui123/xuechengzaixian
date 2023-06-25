package com.xiaodaidai.content.api;

import com.xiaodaidai.content.model.dto.CourseCategoryTreeDto;
import com.xiaodaidai.content.service.CourseCategoryService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CourseCategoryController {
    @Autowired
    CourseCategoryService courseCategoryService;
    @GetMapping("/course-category/tree-nodes")
    public List<CourseCategoryTreeDto> queryTreeNodes(){

        return courseCategoryService.queryTreeNodes("1");
    }



}
