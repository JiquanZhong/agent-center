package com.diit.ds.service.impl;

import com.diit.ds.config.RAGFlowConfig;
import com.diit.ds.domain.req.RAGFlowDatasetCreateReq;
import com.diit.ds.domain.req.RAGFlowDatasetDeleteReq;
import com.diit.ds.domain.req.RAGFlowDatasetListReq;
import com.diit.ds.domain.req.RAGFlowDatasetUpdateReq;
import com.diit.ds.domain.req.RAGFlowKnowledgeReq;
import com.diit.ds.domain.resp.RAGFlowDatasetCreateResp;
import com.diit.ds.domain.resp.RAGFlowDatasetDeleteResp;
import com.diit.ds.domain.resp.RAGFlowDatasetListResp;
import com.diit.ds.domain.resp.RAGFlowDatasetUpdateResp;
import com.diit.ds.domain.resp.RAGFlowKnowledgeResp;
import com.diit.ds.service.RAGFlowDBAPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * RAGFlowAPI调用实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGFlowDBAPIServiceImpl implements RAGFlowDBAPIService {
    private static final String API_PREFIX = "/api/v1";
    private static final String RETRIEVAL_ENDPOINT = "/retrieval";
    private static final String DATASETS_ENDPOINT = "/datasets";

    private final RAGFlowConfig ragFlowConfig;
    private final RestTemplate restTemplate;

    @Override
    public RAGFlowKnowledgeResp retrieval(RAGFlowKnowledgeReq req) {
        req.setRerankId(ragFlowConfig.getRerankId());

        try {
            // 构建请求URL
            String url = ragFlowConfig.getBaseUrl() + API_PREFIX + RETRIEVAL_ENDPOINT;
            log.info("RAGFlow API请求URL: {}", url);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", ragFlowConfig.getApiKey());

            // 创建HTTP请求实体
            HttpEntity<RAGFlowKnowledgeReq> requestEntity = new HttpEntity<>(req, headers);

            // 发送POST请求
            ResponseEntity<RAGFlowKnowledgeResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    RAGFlowKnowledgeResp.class
            );

            // 获取响应结果
            RAGFlowKnowledgeResp response = responseEntity.getBody();
            log.info("RAGFlow API响应状态码: {}", response != null ? response.getCode() : "null");

            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow API失败", e);
            // 创建一个错误响应
            RAGFlowKnowledgeResp errorResp = new RAGFlowKnowledgeResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow API失败: " + e.getMessage());
            return errorResp;
        }
    }

    @Override
    public RAGFlowDatasetCreateResp createDataset(RAGFlowDatasetCreateReq req) {
        try {
            // 根据chunk_method类型配置不同的parser_config
            configureParserConfigByChunkMethod(req);
            if (ragFlowConfig.getEmbeddingsId() != null && !ragFlowConfig.getEmbeddingsId().isEmpty()) {
                req.setEmbeddingModel(ragFlowConfig.getEmbeddingsId());
            } else {
                req.setEmbeddingModel(null);
            }

            // 构建请求URL
            String url = ragFlowConfig.getBaseUrl() + API_PREFIX + DATASETS_ENDPOINT;
            log.info("RAGFlow 创建数据集API请求URL: {}", url);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", ragFlowConfig.getApiKey());

            // 创建HTTP请求实体
            HttpEntity<RAGFlowDatasetCreateReq> requestEntity = new HttpEntity<>(req, headers);

            // 发送POST请求
            ResponseEntity<RAGFlowDatasetCreateResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    RAGFlowDatasetCreateResp.class
            );

            // 获取响应结果
            RAGFlowDatasetCreateResp response = responseEntity.getBody();

            if (response != null && response.getCode() == 0) {
                log.info("RAGFlow 创建数据集API响应数据集: {}", response.getData());
            } else {
                log.error("RAGFlow 创建数据集API响应错误: {}", response != null ? response.getMessage() : "null");
            }

            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow 创建数据集API失败", e);
            // 创建一个错误响应
            RAGFlowDatasetCreateResp errorResp = new RAGFlowDatasetCreateResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow 创建数据集API失败: " + e.getMessage());
            return errorResp;
        }
    }

    @Override
    public RAGFlowDatasetDeleteResp deleteDatasets(RAGFlowDatasetDeleteReq req) {
        try {
            // 构建请求URL
            String url = ragFlowConfig.getBaseUrl() + API_PREFIX + DATASETS_ENDPOINT;
            log.info("RAGFlow 删除数据集API请求URL: {}", url);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", ragFlowConfig.getApiKey());

            // 创建HTTP请求实体
            HttpEntity<RAGFlowDatasetDeleteReq> requestEntity = new HttpEntity<>(req, headers);

            // 发送DELETE请求
            ResponseEntity<RAGFlowDatasetDeleteResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    RAGFlowDatasetDeleteResp.class
            );

            // 获取响应结果
            RAGFlowDatasetDeleteResp response = responseEntity.getBody();

            if (response != null && response.getCode() == 0) {
                log.info("RAGFlow 删除数据集API成功执行");
            } else {
                log.error("RAGFlow 删除数据集API响应错误: {}", response != null ? response.getMessage() : "null");
            }

            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow 删除数据集API失败", e);
            // 创建一个错误响应
            RAGFlowDatasetDeleteResp errorResp = new RAGFlowDatasetDeleteResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow 删除数据集API失败: " + e.getMessage());
            return errorResp;
        }
    }

    @Override
    public RAGFlowDatasetUpdateResp updateDataset(String datasetId, RAGFlowDatasetUpdateReq req) {
        try {
            // 构建请求URL
            String url = ragFlowConfig.getBaseUrl() + API_PREFIX + DATASETS_ENDPOINT + "/" + datasetId;
            log.info("RAGFlow 更新数据集API请求URL: {}", url);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", ragFlowConfig.getApiKey());

            // 创建HTTP请求实体
            HttpEntity<RAGFlowDatasetUpdateReq> requestEntity = new HttpEntity<>(req, headers);

            // 发送PUT请求
            ResponseEntity<RAGFlowDatasetUpdateResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    RAGFlowDatasetUpdateResp.class
            );

            // 获取响应结果
            RAGFlowDatasetUpdateResp response = responseEntity.getBody();

            if (response != null && response.getCode() == 0) {
                log.info("RAGFlow 更新数据集API成功执行");
            } else {
                log.error("RAGFlow 更新数据集API响应错误: {}", response != null ? response.getMessage() : "null");
            }

            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow 更新数据集API失败", e);
            // 创建一个错误响应
            RAGFlowDatasetUpdateResp errorResp = new RAGFlowDatasetUpdateResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow 更新数据集API失败: " + e.getMessage());
            return errorResp;
        }
    }

    @Override
    public RAGFlowDatasetListResp listDatasets(RAGFlowDatasetListReq req) {
        try {
            // 构建带查询参数的URL
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(
                    ragFlowConfig.getBaseUrl() + API_PREFIX + DATASETS_ENDPOINT);

            // 添加查询参数
            if (req.getPage() != null) {
                builder.queryParam("page", req.getPage());
            }
            if (req.getPageSize() != null) {
                builder.queryParam("page_size", req.getPageSize());
            }
            if (req.getOrderby() != null) {
                builder.queryParam("orderby", req.getOrderby());
            }
            if (req.getDesc() != null) {
                builder.queryParam("desc", req.getDesc());
            }
            if (req.getName() != null) {
                builder.queryParam("name", req.getName());
            }
            if (req.getId() != null) {
                builder.queryParam("id", req.getId());
            }

            String url = builder.build().toUriString();
            log.info("RAGFlow 获取数据集列表API请求URL: {}", url);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", ragFlowConfig.getApiKey());

            // 创建HTTP请求实体
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // 发送GET请求
            ResponseEntity<RAGFlowDatasetListResp> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    RAGFlowDatasetListResp.class
            );

            // 获取响应结果
            RAGFlowDatasetListResp response = responseEntity.getBody();

            if (response != null && response.getCode() == 0) {
                log.info("RAGFlow 获取数据集列表API成功执行，获取到{}个数据集",
                        response.getData() != null ? response.getData().size() : 0);
            } else {
                log.error("RAGFlow 获取数据集列表API响应错误: {}", response != null ? response.getMessage() : "null");
            }

            return response;
        } catch (Exception e) {
            log.error("调用RAGFlow 获取数据集列表API失败", e);
            // 创建一个错误响应
            RAGFlowDatasetListResp errorResp = new RAGFlowDatasetListResp();
            errorResp.setCode(500);
            errorResp.setMessage("调用RAGFlow 获取数据集列表API失败: " + e.getMessage());
            return errorResp;
        }
    }

    /**
     * 根据分块方法（chunk_method）配置解析器设置（parser_config）
     *
     * @param req 创建数据集请求
     */
    private void configureParserConfigByChunkMethod(RAGFlowDatasetCreateReq req) {
        String chunkMethod = req.getChunkMethod();
        RAGFlowDatasetCreateReq.ParserConfig parserConfig = req.getParserConfig();

        if (parserConfig == null) {
            parserConfig = new RAGFlowDatasetCreateReq.ParserConfig();
            req.setParserConfig(parserConfig);
        }

        // 确保Raptor配置存在
        if (parserConfig.getRaptor() == null) {
            RAGFlowDatasetCreateReq.ParserConfig.RaptorConfig raptorConfig =
                    new RAGFlowDatasetCreateReq.ParserConfig.RaptorConfig();
            raptorConfig.setUseRaptor(false);
            parserConfig.setRaptor(raptorConfig);
        }

        // 确保GraphRAG配置存在
        if (parserConfig.getGraphrag() == null) {
            RAGFlowDatasetCreateReq.ParserConfig.GraphRAGConfig graphRAGConfig =
                    new RAGFlowDatasetCreateReq.ParserConfig.GraphRAGConfig();
            graphRAGConfig.setUseGraphRAG(false);
            parserConfig.setGraphrag(graphRAGConfig);
        }
//
//        // 根据不同分块方法配置不同的parser_config
//        switch (chunkMethod) {
//            case "naive":
//                // 通用类型
//                configureNaiveParserConfig(parserConfig);
//                break;
//            case "manual":
//                // 法律类型
//                configureSimpleParserConfig(parserConfig);
//                break;
//            case "paper":
//                // 论文类型
//                configureSimpleParserConfig(parserConfig);
//                break;
//            case "book":
//                // 书籍类型
//                configureSimpleParserConfig(parserConfig);
//                break;
//            case "qa":
//                // 问答对类型 - 仅保留raptor和graphrag配置
//                configureQAParserConfig(parserConfig);
//                break;
//            default:
//                // 默认使用通用配置
//                configureNaiveParserConfig(parserConfig);
//                break;
//        }

        log.info("根据chunk_method[{}]配置parser_config完成", chunkMethod);
    }

    /**
     * 配置通用类型的解析器设置
     */
    private void configureNaiveParserConfig(RAGFlowDatasetCreateReq.ParserConfig parserConfig) {
        // 通用类型需要保留所有字段
        parserConfig.setLayoutRecognize("DeepDOC");
        parserConfig.setChunkTokenNum(512);
        parserConfig.setDelimiter("\\n!?;。；！？");
        parserConfig.setAutoKeywords(5);
        parserConfig.setAutoQuestions(2);
        parserConfig.setHtml4excel(false);
        parserConfig.setTaskPageSize(12);
    }

    /**
     * 配置简单类型的解析器设置（适用于manual、paper、book、laws等）
     */
    private void configureSimpleParserConfig(RAGFlowDatasetCreateReq.ParserConfig parserConfig) {
        // 这些类型只需要部分字段
        parserConfig.setLayoutRecognize("DeepDOC");
        parserConfig.setAutoKeywords(5);
        parserConfig.setAutoQuestions(2);

        // 清除不需要的字段
        parserConfig.setChunkTokenNum(null);
        parserConfig.setDelimiter(null);
        parserConfig.setHtml4excel(null);
        parserConfig.setTaskPageSize(null);
    }

    /**
     * 配置问答对类型的解析器设置
     */
    private void configureQAParserConfig(RAGFlowDatasetCreateReq.ParserConfig parserConfig) {
        // 问答对类型需要清除大部分字段，只保留必要的配置
        parserConfig.setLayoutRecognize(null);
        parserConfig.setAutoKeywords(null);
        parserConfig.setAutoQuestions(null);
        parserConfig.setChunkTokenNum(null);
        parserConfig.setDelimiter(null);
        parserConfig.setHtml4excel(null);
        parserConfig.setTaskPageSize(null);
    }

    /**
     * 创建通用类型数据集
     *
     * @param name        数据集名称
     * @param description 数据集描述
     * @param permission  权限设置，默认为"me"
     * @return 创建的数据集信息
     */
    public RAGFlowDatasetCreateResp createGeneralDataset(String name, String description, String permission, String delimiter, Integer chunkTokenNum, Integer autoKeywords, Integer autoQuestions) {
        RAGFlowDatasetCreateReq req = new RAGFlowDatasetCreateReq();
        req.setName(name);
        req.setDescription(description);
        req.setPermission(permission != null ? permission : "team");
        req.setChunkMethod("naive");

        if (Strings.isNotBlank(delimiter)) {
            req.getParserConfig().setDelimiter(delimiter);
        }
        if (chunkTokenNum != null) {
            req.getParserConfig().setChunkTokenNum(chunkTokenNum);
        }
        if (autoKeywords != null) {
            req.getParserConfig().setAutoKeywords(autoKeywords);
        }
        if (autoQuestions != null) {
            req.getParserConfig().setAutoQuestions(autoQuestions);
        }

        return createDataset(req);
    }

    /**
     * 创建法律类型数据集
     *
     * @param name        数据集名称
     * @param description 数据集描述
     * @param permission  权限设置，默认为"me"
     * @return 创建的数据集信息
     */
    public RAGFlowDatasetCreateResp createLawsDataset(String name, String description, String permission, String delimiter, Integer chunkTokenNum, Integer autoKeywords, Integer autoQuestions) {
        RAGFlowDatasetCreateReq req = new RAGFlowDatasetCreateReq();
        req.setName(name);
        req.setDescription(description);
        req.setPermission(permission != null ? permission : "team");
        req.setChunkMethod("manual");

        if (Strings.isNotBlank(delimiter)) {
            req.getParserConfig().setDelimiter(delimiter);
        }
        if (chunkTokenNum != null) {
            req.getParserConfig().setChunkTokenNum(chunkTokenNum);
        }
        if (autoKeywords != null) {
            req.getParserConfig().setAutoKeywords(autoKeywords);
        }
        if (autoQuestions != null) {
            req.getParserConfig().setAutoQuestions(autoQuestions);
        }

        return createDataset(req);
    }

    /**
     * 创建论文类型数据集
     *
     * @param name        数据集名称
     * @param description 数据集描述
     * @param permission  权限设置，默认为"me"
     * @return 创建的数据集信息
     */
    public RAGFlowDatasetCreateResp createPaperDataset(String name, String description, String permission, String delimiter, Integer chunkTokenNum, Integer autoKeywords, Integer autoQuestions) {
        RAGFlowDatasetCreateReq req = new RAGFlowDatasetCreateReq();
        req.setName(name);
        req.setDescription(description);
        req.setPermission(permission != null ? permission : "team");
        req.setChunkMethod("paper");

        if (Strings.isNotBlank(delimiter)) {
            req.getParserConfig().setDelimiter(delimiter);
        }
        if (chunkTokenNum != null) {
            req.getParserConfig().setChunkTokenNum(chunkTokenNum);
        }
        if (autoKeywords != null) {
            req.getParserConfig().setAutoKeywords(autoKeywords);
        }
        if (autoQuestions != null) {
            req.getParserConfig().setAutoQuestions(autoQuestions);
        }

        return createDataset(req);
    }

    /**
     * 创建书籍类型数据集
     *
     * @param name        数据集名称
     * @param description 数据集描述
     * @param permission  权限设置，默认为"me"
     * @return 创建的数据集信息
     */
    public RAGFlowDatasetCreateResp createBookDataset(String name, String description, String permission, String delimiter, Integer chunkTokenNum, Integer autoKeywords, Integer autoQuestions) {
        RAGFlowDatasetCreateReq req = new RAGFlowDatasetCreateReq();
        req.setName(name);
        req.setDescription(description);
        req.setPermission(permission != null ? permission : "team");
        req.setChunkMethod("book");

        if (Strings.isNotBlank(delimiter)) {
            req.getParserConfig().setDelimiter(delimiter);
        }
        if (chunkTokenNum != null) {
            req.getParserConfig().setChunkTokenNum(chunkTokenNum);
        }
        if (autoKeywords != null) {
            req.getParserConfig().setAutoKeywords(autoKeywords);
        }
        if (autoQuestions != null) {
            req.getParserConfig().setAutoQuestions(autoQuestions);
        }

        return createDataset(req);
    }

    /**
     * 创建问答对类型数据集
     *
     * @param name        数据集名称
     * @param description 数据集描述
     * @param permission  权限设置，默认为"me"
     * @return 创建的数据集信息
     */
    public RAGFlowDatasetCreateResp createQADataset(String name, String description, String permission, String delimiter, Integer chunkTokenNum, Integer autoKeywords, Integer autoQuestions) {
        RAGFlowDatasetCreateReq req = new RAGFlowDatasetCreateReq();
        req.setName(name);
        req.setDescription(description);
        req.setPermission(permission != null ? permission : "team");
        req.setChunkMethod("qa");

        if (Strings.isNotBlank(delimiter)) {
            req.getParserConfig().setDelimiter(delimiter);
        }
        if (chunkTokenNum != null) {
            req.getParserConfig().setChunkTokenNum(chunkTokenNum);
        }
        if (autoKeywords != null) {
            req.getParserConfig().setAutoKeywords(autoKeywords);
        }
        if (autoQuestions != null) {
            req.getParserConfig().setAutoQuestions(autoQuestions);
        }

        return createDataset(req);
    }
}
