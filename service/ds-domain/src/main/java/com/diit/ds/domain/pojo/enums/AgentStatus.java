package com.diit.ds.domain.pojo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yjxbz
 */

@Getter
@AllArgsConstructor
public enum AgentStatus {
    CREATED("CREATED", "创建完成"),
    PUBLISHED("PUBLISHED", "已发布"),
    REJECTED("REJECTED", "审核未通过"),
    REVIEWING("REVIEWING", "审核中"),
    PASS("PASS", "审核通过"),
    OFFSHELF("OFFSHELF", "已下架")
    ;
    private final String value;

    private final String code;
}
