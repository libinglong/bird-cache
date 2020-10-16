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
@Repeatable(MdCacheEvicts.class)
public @interface MdCacheEvict {

    /**
     * 用于生产cache key的spel表达式
     * #p+index表示第index个参数,如#p0,#p1
     * @return
     */
    String key();

}
