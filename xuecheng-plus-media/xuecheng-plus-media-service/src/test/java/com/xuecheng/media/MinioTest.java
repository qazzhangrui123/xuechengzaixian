package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MinioTest {

    MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://172.21.228.122:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();
    //上传
    @Test
    public void FileLoaderTest(){

        try {
            //通过扩展名获取资源类型mimeType
            ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".mp4");
            String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
            if (extensionMatch!=null){
                mimeType = extensionMatch.getMimeType();
            }
            //上传文件的参数信息
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket("testbucket")  //桶
//                    .object("triple.js")   //对象名,在桶下存储文件
                    .object("test/greeting.js")
                    .contentType(mimeType)   //设置媒体文件类型
                    .filename("/home/liulaoban/greeting.js")   //指定本地文件路径
                    .build();
            //上传文件
            minioClient.uploadObject(testbucket);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //删除文件
    @Test
    public void delFileLoaderTest(){

        try {
            RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket("testbucket").object("triple.js").build();
            //上传文件
            minioClient.removeObject(removeObjectArgs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //查询文件   从minio中下载
    @Test
    public void test_getFile(){
        try {
            GetObjectArgs testbucket = GetObjectArgs.builder().bucket("testbucket").object("test/triple.js").build();
            FilterInputStream fileInputStream = minioClient.getObject(testbucket);
            //指定输出流
            FileOutputStream filterOutputStream = new FileOutputStream(new File("/home/liulaoban/111.js"));
            IOUtils.copy(fileInputStream,filterOutputStream);
            //校验文件的完整性，对文件的内容进行md5
            String source_md5 = DigestUtils.md5DigestAsHex(new FileInputStream("/home/liulaoban/triple.js")); //minio中文件的md5
            String local_md5 = DigestUtils.md5DigestAsHex(new FileInputStream(new File("/home/liulaoban/111.js")));
            if (source_md5.equals(local_md5)){
                System.out.println("下载成功");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //将分块文件上传到minio
    @Test
    public void uploadChunk() throws Exception{
        for (int i = 0; i < 4; i++) {
            //上传文件的参数信息
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket("testbucket")  //桶
                    .object("chunk/"+i)
                    .filename("/media/liulaoban/新加卷/chunk/"+i)   //指定本地文件路径
                    .build();
            minioClient.uploadObject(testbucket);
            System.out.println("上传分块"+ i + "成功");
        }

    }
    //调用minio接口合并分块
    @Test
    public void testMerge() throws Exception{
//        List<ComposeSource> sources = new ArrayList<>();
//        for (int i = 0; i < 2; i++) {
//            //指定分块文件的信息
//            ComposeSource testbucket = ComposeSource.builder().bucket("testbucket").object("chunk/" + i).build();
//            sources.add(testbucket);
//        }
        List<ComposeSource> testbucket1 = Stream.iterate(0, i -> ++i).limit(4)
                .map(i -> ComposeSource
                        .builder()
                        .bucket("testbucket")
                        .object("chunk/" + i)
                        .build())
                .collect(Collectors.toList());

        //指定合并后的objectname等信息
        ComposeObjectArgs testbucket = ComposeObjectArgs.builder()
                .bucket("testbucket")
                .object("merge01.mp4")
                .sources(testbucket1)   //指定源文件
                .build();
        minioClient.composeObject(testbucket);
    }
    //批量清理分块文件
}
