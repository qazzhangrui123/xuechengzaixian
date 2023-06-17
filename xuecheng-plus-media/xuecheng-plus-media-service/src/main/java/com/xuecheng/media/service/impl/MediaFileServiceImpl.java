package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XuechengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Service
@Slf4j
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MinioClient minioClient;

    //存储普通文件
    @Value("${minio.bucket.files}")
    private String bucket_mediafiles;

    //存储视频
    @Value("${minio.bucket.videofiles}")
    private String bucket_videofiles;


    @Autowired
    MediaProcessMapper mediaProcessMapper;
    @Autowired
    MediaFileService currentProxy;

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    //通过扩展名获取资源类型mimeType
    private String getMimeType(String extension){
        if(extension==null)
            extension = "";
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        //通用mimeType，字节流
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if(extensionMatch!=null){
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }
    //将文件上传到minio

    /**
     *
     * @param localFilePath
     * @param mimeType
     * @param bucket
     * @param objectName
     * @return
     */
    public boolean addMediaFilesToMinIO(String localFilePath,String mimeType,String bucket, String objectName) {
        try {
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .filename(localFilePath)
                    .contentType(mimeType)
                    .build();
            minioClient.uploadObject(testbucket);
            log.debug("上传文件到minio成功,bucket:{},objectName:{}",bucket,objectName);
            System.out.println("上传成功");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件到minio出错,bucket:{},objectName:{},错误原因:{}",bucket,objectName,e.getMessage(),e);
            XuechengPlusException.cast("上传文件到文件系统失败");
        }
        return false;
    }

    @Override
    public MediaFiles getFileById(String mediaId) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(mediaId);
        return mediaFiles;
    }


    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String LocalFilePath,String objectName) {
        //文件名
        String filename = uploadFileParamsDto.getFilename();
        //扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        //获取到mimeType
        String mimeType = getMimeType(extension);
        //获取文件保存路径
        String defaultFolderPath = getDefaultFolderPath();
        //文件的md5值
        String fileMd5 = getFileMd5(new File(LocalFilePath));
        if (StringUtils.isEmpty(objectName))
            //使用默认年月日存储
            objectName = defaultFolderPath+fileMd5+extension;

        //上传文件的参数信息
        //上传文件到minio
        boolean result = addMediaFilesToMinIO(LocalFilePath, mimeType, bucket_mediafiles, objectName);
        if (!result)
            XuechengPlusException.cast("上传文件失败");


        //将文件信息保存到数据库
        //入库文件信息
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_mediafiles, objectName);
        if (mediaFiles==null)
            XuechengPlusException.cast("文件上传后保存信息失败");


        //准备返回的对象
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles,uploadFileResultDto);
        return uploadFileResultDto;
    }

    /**
     * @description 将文件信息添加到文件表
     * @param companyId  机构id
     * @param fileMd5  文件md5值
     * @param uploadFileParamsDto  上传文件的信息
     * @param bucket  桶
     * @param objectName 对象名称
     * @return com.xuecheng.media.model.po.MediaFiles
     * @author Mr.M
     * @date 2022/10/12 21:22
     */
    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId,String fileMd5,UploadFileParamsDto uploadFileParamsDto,String bucket,String objectName){
        //将文件信息保存到数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles==null) {
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            //文件id
            mediaFiles.setId(fileMd5);
            //机构id
            mediaFiles.setCompanyId(companyId);
            //桶
            mediaFiles.setBucket(bucket);
            //file_path
            mediaFiles.setFilePath(objectName);
            //file_id
            mediaFiles.setFileId(fileMd5);
            //url
            mediaFiles.setUrl("/"+bucket+"/"+objectName);
            //上传时间
            mediaFiles.setCreateDate(LocalDateTime.now());
            //状态
            mediaFiles.setStatus("1");
            //审核状态
            mediaFiles.setAuditStatus("002003");
            //插入数据库
            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert<=0){
                log.debug("向数据库保存文件失败，bucket:{},objectName:{}",bucket,objectName);
                return null;
            }
            //记录待处理任务

            addWaitingTask(mediaFiles);
            //向MediaProcess插入记录

            return mediaFiles;
        }
        return mediaFiles;
    }


    /**
     * 添加待处理任务
     * @param mediaFiles 媒资文件信息
     */
    private void addWaitingTask(MediaFiles mediaFiles){
        //获取mimeType
        //文件名称
        String filename = mediaFiles.getFilename();
        //文件扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        String mimeType = getMimeType(extension);
        //通过mimeType 判断如果是avi视频，写入待处理任务
        if (mimeType.equals("video/x-msvideo")){
            //如果是avi视频，写入待处理任务
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles,mediaProcess);
            //状态为未处理
            mediaProcess.setStatus("1");
            mediaProcess.setCreateDate(LocalDateTime.now());
            mediaProcess.setFailCount(0);  //失败次数，默认为0
            mediaProcess.setUrl(null);
            mediaProcessMapper.insert(mediaProcess);
        }

    }

    //检查目标文件是否已在minio中
    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        //先查询数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles!=null){
            //如果数据库存在再查询minio
            GetObjectArgs testbucket = GetObjectArgs
                    .builder()
                    .bucket(mediaFiles.getBucket())    //桶
                    .object(mediaFiles.getFilePath())  //对象名
                    .build();
            //查询远程服务获取流对象
            try {
                FilterInputStream fileInputStream = minioClient.getObject(testbucket);
                if (fileInputStream!=null){
                    //文件已存在
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //文件不存在
        return RestResponse.success(false);
    }

    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex){
        //分块存储路径：md5前两位为两个目录，chunk存储分块文件
        //根据md5得到分块文件所在目录的路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //如果数据库存在再查询minio
        GetObjectArgs testbucket = GetObjectArgs
                .builder()
                .bucket(bucket_videofiles)    //桶
                .object(chunkFileFolderPath + chunkIndex)  //对象名
                .build();
        //查询远程服务获取流对象
        try {
            FilterInputStream fileInputStream = minioClient.getObject(testbucket);
            if (fileInputStream!=null){
                //文件已存在
                return RestResponse.success(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //文件不存在
        return RestResponse.success(false);
    }

    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {
        //分块文件路径
        String chunkFilePath = getChunkFileFolderPath(fileMd5) + chunk;
        //获取mimeType
        String mimeType = getMimeType(null);
        //将分块上传到minio
        boolean res = addMediaFilesToMinIO(localChunkFilePath, mimeType, bucket_videofiles, chunkFilePath);
        if (!res){
            return RestResponse.validfail(false,"上传分块文件失败");
        }
        //上传成功
        return RestResponse.success(true);
    }

    @Override
    @Transactional
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        //分块文件所在目录
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //找到分块文件调用minio的sdk进行文件合并
        List<ComposeSource> sourceList = Stream.iterate(0, i -> ++i).limit(chunkTotal)
                .map(i -> ComposeSource
                        .builder()
                        .bucket(bucket_videofiles)
                        .object(chunkFileFolderPath+i)
                        .build())
                .collect(Collectors.toList());
        //源文件名称
        String filename = uploadFileParamsDto.getFilename();
        //扩展名
        String extesion = filename.substring(filename.lastIndexOf("."));
        String mergeFilePath = getFilePathByMd5(fileMd5,extesion);
        //指定合并后的objectname等信息
        ComposeObjectArgs testbucket = ComposeObjectArgs.builder()
                .bucket(bucket_videofiles)
                .object(mergeFilePath)
                .sources(sourceList)   //指定源文件
                .build();
        try {
            ObjectWriteResponse response = minioClient.composeObject(testbucket);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("合并文件出错，bucket：{},objectName:{},所悟信息:{}",bucket_videofiles,mergeFilePath,e.getMessage());
            return RestResponse.validfail(false,"合并文件异常");
        }
        //校验合并后的和源文件是否一致，视频上传才成功
        //先下载合并后的文件
        File file = downloadFileFromMinIO(bucket_videofiles, mergeFilePath);
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            //计算合并后文件的md5
            String mergeMd5 = DigestUtils.md5Hex(fileInputStream);
            if (!mergeMd5.equals(fileMd5)){
                //比较两个md5
                log.error("校验合并文件md5值不一致，原始文件：{},合并文件:{}",fileMd5,mergeMd5);
                return RestResponse.validfail(false,"文件校验失败");
            }
            //文件大小
            uploadFileParamsDto.setFileSize(file.length());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //将文件信息入库
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_videofiles, mergeFilePath);
        if (mediaFiles==null){
            return RestResponse.validfail(false,"文件入库失败");
        }
        //清理分块文件
        clearChunkFiles(chunkFileFolderPath,chunkTotal);
        return RestResponse.success(true);
    }

    /**
     * 清除分块文件
     * @param chunkFileFolderPath 分块文件路径
     * @param chunkTotal 分块文件总数
     */
    private void clearChunkFiles(String chunkFileFolderPath,int chunkTotal){
        try {
            List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                    .limit(chunkTotal)
                    .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
                    .collect(Collectors.toList());

            RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket(bucket_videofiles).objects(deleteObjects).build();
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
            results.forEach(r->{
                DeleteError deleteError = null;
                try {
                    deleteError = r.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("清楚分块文件失败,objectname:{}",deleteError.objectName(),e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("清楚分块文件失败,chunkFileFolderPath:{}",chunkFileFolderPath,e);
        }
    }

    //得到分块文件的目录
    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
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

    //获取文件的md5
    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            String fileMd5 = DigestUtils.md5Hex(fileInputStream);
            return fileMd5;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 从minio下载文件
     * @param bucket 桶
     * @param objectName 对象名称
     * @return 下载后的文件
     */
    public File downloadFileFromMinIO(String bucket,String objectName){
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try{
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile=File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream,outputStream);
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(outputStream!=null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    //获取文件默认存储目录路径 年/月/日
    private String getDefaultFolderPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String folder = sdf.format(new Date()).replace("-", "/")+"/";
        return folder;
    }
}
