package com.xiaodaidai.content.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "search",fallbackFactory = SearchServiceClientfallbackFactory.class)
public interface SearchServiceClient {
    @PostMapping("/search/index/course")
    public Boolean add(@RequestBody CourseIndex courseIndex) ;
}
