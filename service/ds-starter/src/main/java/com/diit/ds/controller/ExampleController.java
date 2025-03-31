package com.diit.ds.controller;

import com.diit.ds.annotation.NotNeedAuth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/example")
@Tag(name = "示例接口", description = "展示JWT认证的例子")
@RequiredArgsConstructor
public class ExampleController {

    /**
     * 公开接口，不需要认证
     */
    @Operation(summary = "公开接口", description = "无需JWT认证的公开接口")
    @GetMapping("/public")
    @NotNeedAuth
    public String publicEndpoint() {
        return "这是一个公开的接口，无需JWT认证";
    }

    /**
     * 受保护接口，需要认证
     */
    @Operation(summary = "受保护接口", description = "需要JWT认证的接口")
    @GetMapping("/protected")
    public String protectedEndpoint() {
        return "这是一个受保护的接口，需要有效的JWT认证才能访问";
    }
} 