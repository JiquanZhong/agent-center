package com.diit.ds;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "com.diit.ds.mapper")
public class DiitMicroserviceDSApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiitMicroserviceDSApplication.class, args);
    }

}
