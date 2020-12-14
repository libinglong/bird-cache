package com.sohu.smc.md.cache.anno;

import com.sohu.smc.md.cache.spring.CacheProperty;

import java.lang.annotation.*;

/**
 *
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/12/13
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Prop {

    /**
     * 属性的名称
     * @see CacheProperty
     * @return
     */
    String name();

    /**
     *
     * @return
     */
    String value();


}
