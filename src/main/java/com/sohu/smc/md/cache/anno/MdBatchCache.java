package com.sohu.smc.md.cache.anno;

import java.lang.annotation.*;

/**
 * 该注解要求返回值和参数必须是List,且参数和返回值的按顺序一一映射.如果无法按顺序映射,则缓存会出现错误
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
     * #p+index表示第index个参数,如#p0,#p1
     * 特别的,#obj表示list中的一个元素
     * @return
     */
    String key();
}
