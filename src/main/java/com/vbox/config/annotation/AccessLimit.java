package com.vbox.config.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AccessLimit {
    int seconds() default 60;
    int maxCount();
}

