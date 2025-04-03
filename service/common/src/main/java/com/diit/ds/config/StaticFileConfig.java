package com.diit.ds.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class StaticFileConfig implements WebMvcConfigurer {
    
    @Value("${file.preview-path:preview/}")
    private String previewPath;
    
    @Value("${file.preview-mapping:/preview/}")
    private String previewMapping;
    
    /**
     * 配置静态资源映射
     * @param registry 资源处理注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保预览路径是绝对路径
        File previewDir = new File(previewPath);
        if (!previewDir.isAbsolute()) {
            // 如果是相对路径,转换为绝对路径
            previewDir = new File(System.getProperty("user.dir"), previewPath);
        }
        
        // 确保目录存在
        if (!previewDir.exists()) {
            previewDir.mkdirs();
        }
        
        String absolutePreviewPath = previewDir.getAbsolutePath().replace("\\", "/");
        if (!absolutePreviewPath.endsWith("/")) {
            absolutePreviewPath += "/";
        }
        
        // 配置preview文件夹的内容映射
        registry.addResourceHandler(previewMapping + "**")
                .addResourceLocations("file:/" + absolutePreviewPath);
                
        // 确保Swagger文档正常访问
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        
        // 添加原有的静态资源映射，避免覆盖默认配置
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("classpath:/public/")
                .addResourceLocations("classpath:/resources/")
                .addResourceLocations("classpath:/META-INF/resources/");
    }
}
