package com.diit.ds.rag.domain.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DifyChatConversationResp {
    private Integer page;
    private Integer limit;
    private Integer total;
    @JsonProperty("has_more")
    private Boolean hasMore;
    private List<ConversationData> data;

    @Data
    public static class ConversationData {
        private String id;
        private String status;
        @JsonProperty("from_source")
        private String fromSource;
        @JsonProperty("from_end_user_id")
        private String fromEndUserId;
        @JsonProperty("from_end_user_session_id")
        private String fromEndUserSessionId;
        @JsonProperty("from_account_id")
        private String fromAccountId;
        @JsonProperty("from_account_name")
        private String fromAccountName;
        private String name;
        private String summary;
        @JsonProperty("read_at")
        private Long readAt;
        @JsonProperty("created_at")
        private Long createdAt;
        @JsonProperty("updated_at")
        private Long updatedAt;
        private Boolean annotated;
        @JsonProperty("model_config")
        private ModelConfig modelConfig;
        @JsonProperty("message_count")
        private Integer messageCount;
        @JsonProperty("user_feedback_stats")
        private FeedbackStats userFeedbackStats;
        @JsonProperty("admin_feedback_stats")
        private FeedbackStats adminFeedbackStats;
        @JsonProperty("status_count")
        private StatusCount statusCount;
        @JsonProperty("system_name")
        private String systemName;
    }

    @Data
    public static class ModelConfig {
        private String model;
        @JsonProperty("pre_prompt")
        private String prePrompt;
    }

    @Data
    public static class FeedbackStats {
        private Integer like;
        private Integer dislike;
    }

    @Data
    public static class StatusCount {
        private Integer success;
        private Integer failed;
        @JsonProperty("partial_success")
        private Integer partialSuccess;
    }
} 