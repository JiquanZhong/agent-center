package com.diit.ds.annotation;

import com.diit.ds.condition.AuthTypeCondition;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(SSOAuthTypeCondition.InnerCondition.class)
public @interface SSOAuthTypeCondition {
    class InnerCondition extends AuthTypeCondition {
        public InnerCondition() {
            super("SSO");
        }
    }
} 