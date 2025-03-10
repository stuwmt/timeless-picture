package com.timeless.timelesspicturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 作用在方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时有效
public @interface AuthCheck {
    /**
     * 必须具有某个角色
     */
    String mustRole() default "";
}
