package com.sohu.smc.md.cache.anno;

import java.lang.annotation.*;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/7
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface MdBatchCache {

    /**
     * 批量查询主参数在参数列表中的index
     * @return
     */
    int index() default 0;

    /**
     * 用于生产cache key的spel表达式
     * @return
     */
    String key();
}
