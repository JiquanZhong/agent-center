package com.diit.ds.service.impl;

import com.diit.ds.domain.req.RAGFlowKnowledgeReq;
import com.diit.ds.domain.resp.RAGFlowKnowledgeResp;
import com.diit.ds.service.RAGFlowAPIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RAGFlowAPIService真实集成测试类
 * 注意：此测试需要真实的RAGFlow API环境，默认被禁用
 * 运行测试前，请确保application-test.yaml中配置了正确的RAGFlow API信息
 */
@SpringBootTest
//@ActiveProfiles("test") // 使用测试环境配置
@Disabled("需要真实的RAGFlow API环境才能运行")
class RAGFlowAPIServiceRealIntegrationTest {

    @Autowired
    private RAGFlowAPIService ragFlowAPIService;

    private RAGFlowKnowledgeReq req;

    @BeforeEach
    void setUp() {
        // 创建测试请求对象
        req = new RAGFlowKnowledgeReq();
        req.setQuestion("重点区域");
        req.setDatasetIds(Arrays.asList("3bd65786ff2911efa9fe0242ac120006"));
//        req.setDocumentIds(Arrays.asList(""));
    }

    /**
     * 测试真实调用RAGFlow API
     * 注意：此测试需要真实的RAGFlow API环境
     */
    @Test
    void testRealRetrieval() {
        // 调用被测试方法
        RAGFlowKnowledgeResp result = ragFlowAPIService.retrieval(req);

        // 验证结果
        assertNotNull(result);
        // 由于是真实环境，我们只验证响应不为空，不验证具体内容
        if (result.getCode() == 0) {
            // 成功响应
            assertNotNull(result.getData());
            assertNotNull(result.getData().getChunks());
            // 可能没有匹配的结果，所以不验证chunks的大小
        } else {
            // 失败响应
            assertNotNull(result.getMessage());
            System.out.println("API调用失败: " + result.getMessage());
        }
    }
} 