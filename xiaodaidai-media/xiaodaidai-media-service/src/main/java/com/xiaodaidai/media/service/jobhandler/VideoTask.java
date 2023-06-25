package com.xiaodaidai.media.service.jobhandler;

import com.xiaodaidai.base.utils.Mp4VideoUtil;
import com.xiaodaidai.media.model.po.MediaProcess;
import com.xiaodaidai.media.service.MediaFileProcessService;
import com.xiaodaidai.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class VideoTask {
    @Autowired
    MediaFileProcessService mediaFileProcessService;

    @Autowired
    MediaFileService mediaFileService;
    //ffmpegpath的路径
    @Value("${videoprocess.ffmpegpath}")
    private String ffmpeg_path;
    /**
     * 视频处理任务
     */
    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();   //执行器的序号，从0开始
        int shardTotal = XxlJobHelper.getShardTotal();   //执行器总数

        //确定cpu的核心数
        int processors = Runtime.getRuntime().availableProcessors();
        //查询待处理任务
        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
        log.debug("取到的视频处理任务数：{}",mediaProcessList.size());
        if (mediaProcessList.size()<=0){
            return;
        }

        //任务数量
        int size = mediaProcessList.size();
        //创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        //使用计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        mediaProcessList.forEach(mediaProcess -> {
            //将任务加入线程池
            executorService.execute(()->{
                try {


                    //任务id
                    Long taskid = mediaProcess.getId();
                    //文件id就是md5
                    String fileId = mediaProcess.getFileId();
                    //开启任务
                    boolean startTask = mediaFileProcessService.startTask(taskid);
                    if (!startTask) {
                        log.debug("抢占任务失败,任务id：{}", taskid);
                        return;
                    }
                    //桶
                    String bucket = mediaProcess.getBucket();
                    String objectName = mediaProcess.getFilePath();
                    //执行视频转码
                    //下载minio视频到本地
                    File file = mediaFileService.downloadFileFromMinIO(bucket, objectName);
                    if (file == null) {
                        log.debug("下载视频出错,任务id：{}，bucket:{},objectName:{}", taskid, bucket, objectName);
                        //保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskid, "3", fileId, null, "下载视频到本地失败");
                        return;
                    }
                    //源avi视频的路径
                    String video_path = file.getAbsolutePath();

                    //转换后mp4文件的名称
                    String mp4_name = fileId + ".mp4";
                    //转换后mp4文件的路径
                    //先创建一个临时文件，作为转换后的文件
                    File mp4File = null;
                    try {
                        mp4File = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        log.debug("创建临时文件异常", e.getMessage());
                        //保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskid, "3", fileId, null, "创建临时文件异常");
                        return;
                    }
                    String mp4_path = mp4File.getAbsolutePath();
                    //创建工具类对象
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpeg_path, video_path, mp4_name, mp4_path);
                    //开始视频转换，成功将返回success
                    String result = videoUtil.generateMp4();
                    if (!result.equals("success")) {
                        log.debug("视频转码失败，原因:{},bucket:{},obkectName:{}", result, bucket, objectName);
                        mediaFileProcessService.saveProcessFinishStatus(taskid, "3", fileId, null, "视频转码失败");
                        return;
                    }
                    //mp4文件的url
                    String filePathByMd5 = getFilePathByMd5(fileId, ".mp4");
                    //上传到minio
                    boolean b = mediaFileService.addMediaFilesToMinIO(mp4_path, "video/mp4", bucket, filePathByMd5);
                    if (!b) {
                        log.debug("上传mp4到minio失败");
                        mediaFileProcessService.saveProcessFinishStatus(taskid, "3", fileId, null, "上传mp4到minio失败");
                        return;
                    }
                    //保存任务处理成功的结果
                    mediaFileProcessService.saveProcessFinishStatus(taskid, "2", fileId, filePathByMd5, "下载视频到本地失败");

                }finally {
                    //计数器减去1
                    countDownLatch.countDown();
                }
            });
        });

        //阻塞,指定最大限度等待时间,阻塞最多等待一定的时间后就解除阻塞
        countDownLatch.await(30,TimeUnit.MINUTES);
    }
    /**
     * 得到合并后的文件的地址
     * @param fileMd5 文件id即md5值
     * @param fileExt 文件扩展名
     * @return
     */
    private String getFilePathByMd5(String fileMd5,String fileExt){
        return   fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +fileMd5 +fileExt;
    }

}
