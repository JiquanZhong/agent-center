package com.diit.ds.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.diit.ds.rag.domain.resp.DifyChatConversationResp;
import com.diit.ds.domain.pojo.dto.ConversationDTO;
import com.diit.ds.chat.service.ConversationService;
import com.diit.ds.auth.util.DifyAuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dify监控面板", description = "Dify监控面板API")
@RequiredArgsConstructor
public class DashboardController {

    private final DifyAuthUtil difyAuthUtil;
    private final RestTemplate restTemplate;
    private final ConversationService conversationService;
    
    @Value("${dify.api.base-url}")
    private String baseUrl;
    
    @Value("${dify.api.app-id}")
    private String appId;
    
    @Value("${dify.api.chat-conversations-path}")
    private String chatConversationsPath;

    /**
     * 获取Dify的jwt token
     *
     * @return Jwt token
     */
    @Deprecated
    @Operation(summary = "获取Jwt", description = "获取Dify的jwt token")
    @PostMapping("/getJwtToken")
    public String retrieveKnowledge() {
        String accessToken = difyAuthUtil.getAccessToken();
        log.info("获取到的jwt token: {}", accessToken);
        return accessToken;
    }
    
    /**
     * 获取聊天会话列表 (远程API调用)
     *
     * @param page 页码
     * @param limit 每页数量
     * @param start 开始时间
     * @param end 结束时间
     * @param sortBy 排序方式
     * @param annotationStatus 标注状态
     * @return 聊天会话列表
     */
    @Operation(summary = "获取聊天会话列表(远程API)", description = "通过远程API获取Dify的聊天会话列表")
    @GetMapping("/remote-chat-conversations")
    public DifyChatConversationResp getRemoteChatConversations(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(name = "sort_by", defaultValue = "-created_at") String sortBy,
            @RequestParam(name = "annotation_status", defaultValue = "all") String annotationStatus) {
        
        try {
            // 获取访问令牌
            String accessToken = difyAuthUtil.getAccessToken();
            if (accessToken.isEmpty()) {
                log.error("获取Dify访问令牌失败");
                return new DifyChatConversationResp();
            }
            
            // 构建请求URL
            String fullPath = chatConversationsPath.replace("{appId}", appId);
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + fullPath)
                    .queryParam("page", page)
                    .queryParam("limit", limit)
                    .queryParam("sort_by", sortBy)
                    .queryParam("annotation_status", annotationStatus)
                    .toUriString();
            
            // 添加可选参数
            if (start != null && !start.isEmpty()) {
                url = url + "&start=" + start;
            }
            if (end != null && !end.isEmpty()) {
                url = url + "&end=" + end;
            }
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);
            
            // 构建请求
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            
            log.debug("发送获取聊天会话列表请求: {}", url);
            ResponseEntity<DifyChatConversationResp> responseEntity = 
                    restTemplate.exchange(url, HttpMethod.GET, requestEntity, DifyChatConversationResp.class);
            
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                return responseEntity.getBody();
            } else {
                log.error("获取聊天会话列表失败, 状态码: {}", responseEntity.getStatusCode());
                return new DifyChatConversationResp();
            }
        } catch (Exception e) {
            log.error("获取聊天会话列表异常: {}", e.getMessage(), e);
            return new DifyChatConversationResp();
        }
    }

    /**
     * 获取对话列表 (本地数据库)
     *
     * @param page 页码
     * @param limit 每页数量
     * @param appId 应用ID
     * @param start 开始时间
     * @param end 结束时间
     * @param sortBy 排序方式
     * @param annotationStatus 标注状态
     * @return 对话列表
     */
    @Operation(summary = "获取对话列表(本地数据库)", description = "从数据库获取用户的对话历史记录")
    @GetMapping("/chat-conversations")
    public DifyChatConversationResp getConversations(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd+HH:mm") LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd+HH:mm") LocalDateTime end,
            @RequestParam(name = "sort_by", defaultValue = "-created_at") String sortBy,
            @RequestParam(name = "annotation_status", defaultValue = "all") String annotationStatus,
            @RequestParam(required = false) String systemName,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String dialogTitle){
        
        // 转换appId字符串为UUID
        UUID appUuid = null;
        if (appId != null && !appId.isEmpty()) {
            try {
                appUuid = UUID.fromString(appId);
            } catch (IllegalArgumentException e) {
                log.error("无效的应用ID格式: {}", appId);
            }
        } else if (this.appId != null && !this.appId.isEmpty()) {
            // 使用配置的默认应用ID
            try {
                appUuid = UUID.fromString(this.appId);
            } catch (IllegalArgumentException e) {
                log.error("无效的默认应用ID格式: {}", this.appId);
            }
        }
        
        try {
            IPage<ConversationDTO> conversationPage = conversationService.getConversationPage(page, limit, appUuid, start, end, sortBy, annotationStatus, systemName, userName, dialogTitle);
            
            // 转换为DifyChatConversationResp对象
            DifyChatConversationResp response = new DifyChatConversationResp();
            response.setPage(page);
            response.setLimit(limit);
            response.setTotal((int) conversationPage.getTotal());
            response.setHasMore(conversationPage.getCurrent() < conversationPage.getPages());
            
            List<DifyChatConversationResp.ConversationData> dataList = conversationPage.getRecords().stream()
                    .map(this::convertToConversationData)
                    .toList();
            
            response.setData(dataList);
            return response;
        } catch (Exception e) {
            log.error("获取会话列表异常: {}", e.getMessage(), e);
            return new DifyChatConversationResp();
        }
    }
    
    /**
     * 将Conversation实体转换为ConversationData响应对象
     *
     * @param conversation 会话实体
     * @return 会话数据响应对象
     */
    private DifyChatConversationResp.ConversationData convertToConversationData(ConversationDTO conversation) {
        DifyChatConversationResp.ConversationData data = new DifyChatConversationResp.ConversationData();
        
        data.setId(conversation.getId());
        data.setStatus(conversation.getStatus());
        data.setFromSource(conversation.getFromSource());
        data.setFromEndUserId(conversation.getFromEndUserId());
        data.setFromAccountId(conversation.getFromAccountId());
        data.setName(conversation.getName());
        data.setSummary(conversation.getSummary());
        data.setFromEndUserSessionId(conversation.getFromEndUserSessionId());
        data.setSystemName(conversation.getSystemName());

        // 时间戳转换
        if (conversation.getReadAt() != null) {
            data.setReadAt(conversation.getReadAt().toEpochSecond(java.time.ZoneOffset.UTC));
        }
        if (conversation.getCreatedAt() != null) {
            data.setCreatedAt(conversation.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC));
        }
        if (conversation.getUpdatedAt() != null) {
            data.setUpdatedAt(conversation.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC));
        }
        
        // 模型配置
        DifyChatConversationResp.ModelConfig modelConfig = new DifyChatConversationResp.ModelConfig();
        modelConfig.setModel(conversation.getModelId());
        data.setModelConfig(modelConfig);
        
        // 消息统计
        data.setMessageCount(conversation.getDialogueCount());
        
        // 用户反馈统计（默认值）
        DifyChatConversationResp.FeedbackStats userFeedbackStats = new DifyChatConversationResp.FeedbackStats();
        userFeedbackStats.setLike(0);
        userFeedbackStats.setDislike(0);
        data.setUserFeedbackStats(userFeedbackStats);
        
        // 管理员反馈统计（默认值）
        DifyChatConversationResp.FeedbackStats adminFeedbackStats = new DifyChatConversationResp.FeedbackStats();
        adminFeedbackStats.setLike(0);
        adminFeedbackStats.setDislike(0);
        data.setAdminFeedbackStats(adminFeedbackStats);
        
        // 状态统计（默认值）
        DifyChatConversationResp.StatusCount statusCount = new DifyChatConversationResp.StatusCount();
        statusCount.setSuccess(0);
        statusCount.setFailed(0);
        statusCount.setPartialSuccess(0);
        data.setStatusCount(statusCount);
        
        return data;
    }
}
