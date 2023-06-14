package com.xuecheng.media;

import org.junit.jupiter.api.Test;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 测试大文件上传方法
 */
public class BigFileTest {
    //分块测试
    @Test
    public void checkChunk() throws Exception {
        //源文件
        File sourceFile = new File("/media/liulaoban/新加卷/cutFilter300/example.mp4");
        //分块文件存储路径
        String chunkFilePath = "/media/liulaoban/新加卷/chunk/";
        //分块文件大小
        int chunSize = 1024*1024*5;   //5MB
        //分块文件个数
        int chunkNum = (int)Math.ceil(sourceFile.length()*1.0/chunSize);
        //使用流从源文件读书巨，向分块文件中写数据
        RandomAccessFile raf_r = new RandomAccessFile(sourceFile, "r");
        //缓存区
        byte[] bytes = new byte[1024];
        for (int i = 0; i < chunkNum; i++) {
            File chunkFile = new File(chunkFilePath + i);
            //分块文件写入流
            RandomAccessFile raf_rw = new RandomAccessFile(chunkFile, "rw");
            int len=-1;

            while ((len = raf_r.read(bytes))!=-1){
                raf_rw.write(bytes,0,len);
                if (chunkFile.length()>=chunSize)
                    break;
            }
            raf_rw.close();
        }
        raf_r.close();
    }

    //将分块进行合并
    @Test
    public void testMerge() throws  Exception{
        //块文件目录
        File chunkFolder = new File("/media/liulaoban/新加卷/chunk/");
        //源文件
        File sourceFile = new File("/media/liulaoban/新加卷/cutFilter300/example.mp4");
        //合并后的文件
        File mergeFile = new File("/media/liulaoban/新加卷/cutFilter300/example_eeee.mp4");

        //取出所有分块文件
        File[] files = chunkFolder.listFiles();
        //将数组转成list
        List<File> fileList = Arrays.asList(files);
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName())-Integer.parseInt(o2.getName());
            }
        });
        //分块文件写入流
        RandomAccessFile raf_rw = new RandomAccessFile(mergeFile, "rw");
        //缓存区
        byte[] bytes = new byte[1024];
        //遍历分块文件
        for (int i = 0; i < fileList.size(); i++) {
            RandomAccessFile raf_r = new RandomAccessFile(fileList.get(i), "r");
            int len=-1;
            while ((len=raf_r.read(bytes))!=-1)
                raf_rw.write(bytes,0,len);
            raf_r.close();
        }
        raf_rw.close();

        //合并文件完成，对合并的文件进行校验
        FileInputStream fileInputStream = new FileInputStream(mergeFile);
        FileInputStream fileInputStream1 = new FileInputStream(sourceFile);
        String s = DigestUtils.md5DigestAsHex(fileInputStream);
        String s1 = DigestUtils.md5DigestAsHex(fileInputStream1);
        if (s.equals(s1))
            System.out.println("合并成功");
    }


}
