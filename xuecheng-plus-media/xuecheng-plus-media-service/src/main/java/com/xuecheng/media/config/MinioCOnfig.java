package com.xuecheng.media.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

@Configuration
public class MinioCOnfig {

    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.accessKey}")
    private String accessKey;
    @Value("${minio.secretKey}")
    private String secretKey;

    @Bean
    public MinioClient minioClient(){
        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint(endpoint)
                        .credentials("minioadmin", "minioadmin")
                        .build();
        return minioClient;

    }

}
