package com.scienjus.mrc.annotation;

import java.lang.annotation.*;

/**
 * @author ScienJus
 * @date 2016/1/28.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Counter {

    String name() default "";

    int expire() default 0;
}
