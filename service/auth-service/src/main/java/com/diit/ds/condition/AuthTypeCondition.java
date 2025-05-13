package com.diit.ds.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class AuthTypeCondition implements Condition {
    private final String expectedAuthType;

    public AuthTypeCondition(String expectedAuthType) {
        this.expectedAuthType = expectedAuthType;
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String authType = context.getEnvironment().getProperty("diit.security.auth-type", "JWT");
        return expectedAuthType.equalsIgnoreCase(authType);
    }
} 