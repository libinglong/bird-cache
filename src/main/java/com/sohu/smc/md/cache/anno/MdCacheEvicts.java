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
public @interface MdCacheEvicts {

    MdCacheEvict[] value();

}
