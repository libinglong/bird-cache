package com.sohu.smc.anno;

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
@Repeatable(MdCachePuts.class)
public @interface MdCachePut {


    /**
     * 表示只是更新缓存,不涉及到数据库之类持久化的操作.此场景下,只需要保证双机房数据一致性即可.
     * @return
     */
    boolean cacheOnly() default false;
}
