package com.xuecheng.content;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 测试远程调用媒资服务
 */
@SpringBootTest
public class FeignUploadTest {
    @Autowired
    MediaServiceClient mediaServiceClient;
    @Test
    public void test() throws Exception {
        //将file转成MultipartFile
        File file = new File("/media/liulaoban/新加卷/cutFilter300/120.html");
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        //远程调用得到返回值
        String upload = mediaServiceClient.upload(multipartFile, "course/120.html");
        if (upload==null){
            System.out.println("走了降级逻辑");
        }
    }
}
