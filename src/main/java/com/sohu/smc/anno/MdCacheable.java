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
@Repeatable(MdCacheables.class)
public @interface MdCacheable {

    /**
     * 批量查询,如果缓存miss,需要指定使用哪个方法加载数据.
     * 方法的签名必须是
     * @return 方法名称
     */
    String valueLoadMethod() default "";

}
