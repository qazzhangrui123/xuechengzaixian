package com.xiaodaidai.content.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {
    //拿到了熔断异常信息throwable
    @Override
    public MediaServiceClient create(Throwable throwable) {
        return new MediaServiceClient() {
            //发生熔断，上传服务调用此方法执行降级逻辑
            @Override
            public String upload(MultipartFile filedata, String objectName) throws Exception {
                log.debug("远程调用上传文件的接口发生熔断：{}",throwable.toString(),throwable);
                return null;
            }
        };
    }
}
