package com.xiaodaidai.content;

import com.xiaodaidai.content.model.dto.CoursePreviewDto;
import com.xiaodaidai.content.service.CoursePublishService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试freemarker页面静态化方法
 */
@SpringBootTest
public class FreemarkerTest {

    @Autowired
    CoursePublishService coursePublishService;

    @Test
    public void testGenerateHtmlByTemplate() throws Exception{
        //配置freemarker
        Configuration configuration = new Configuration(Configuration.getVersion());
        //加载模板
        //选指定模板路径,classpath下templates下
        //得到classpath路径
        ClassPathResource templates = new ClassPathResource("templates");
        configuration.setDirectoryForTemplateLoading(templates.getFile());
        //设置字符编码
        configuration.setDefaultEncoding("utf-8");

        //指定模板文件名称
        Template template = configuration.getTemplate("course_template.ftl");

        //准备数据
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(2L);

        Map<String, Object> map = new HashMap<>();
        map.put("model", coursePreviewInfo);

        //静态化
        //参数1：模板，参数2：数据模型
        String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
        System.out.println(content);
        //将静态化内容输出到文件中
        InputStream inputStream = IOUtils.toInputStream(content);
        //输出流
        FileOutputStream outputStream = new FileOutputStream("/media/liulaoban/新加卷/cutFilter300/120.html");
        IOUtils.copy(inputStream, outputStream);
    }
}
