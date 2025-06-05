package com.diit.ds.auth.annotation;

import com.diit.ds.auth.condition.AuthTypeCondition;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(CTSSOAuthTypeCondition.InnerCondition.class)
public @interface CTSSOAuthTypeCondition {
    class InnerCondition extends AuthTypeCondition {
        public InnerCondition() {
            super("CTSSO");
        }
    }
} 