package com.sohu.smc.anno;

import java.lang.annotation.*;

/**
 * 一个方法只允许有一个MdCacheable
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/7
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface MdCacheable {

    /**
     * 用于生产cache key的spel表达式
     * @return
     */
    String key();

}
