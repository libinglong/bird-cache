package com.sohu.smc.md.cache.anno;

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
     * #p+index表示第index个参数,如#p0,#p1
     * @return
     */
    String key();

}
