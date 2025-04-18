package com.diit.system.enums;

import lombok.Getter;

@Getter
public enum RoleEnum {
    
    ADMIN("admin", "管理员"),
    NORMAL("normal", "普通用户");

    private final String code;
    private final String name;

    RoleEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
} 