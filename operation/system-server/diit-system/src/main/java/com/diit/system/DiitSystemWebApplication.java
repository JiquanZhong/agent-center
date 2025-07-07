package com.diit.system;

import com.diit.bpm.autoconfigure.system.SystemSwaggerConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

@SpringBootApplication
@MapperScan({"com.diit.**.mapper"})
@ComponentScan({"com.diit.base","com.diit.system","com.diit.cache","com.diit.file","com.diit.encrypt"})
@EnableSwagger2WebMvc
@Import({SystemSwaggerConfig.class})
public class DiitSystemWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiitSystemWebApplication.class, args);
    }

}
