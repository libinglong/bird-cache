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
     * 特别需要注意的是,#obj表示list中的一个元素
     * 例如#obj.name + #p1表示list中的每个元素的名字加上第一个参数,作为缓存的key
     * @return
     */
    String key();
    /**
     * 批量查询的时候,由于返回的列表未必是和入参一一按序对应,因此返回结果中必须包含映射关系的key表达式
     * 使用该表达式对返回值计算的结果和{@link MdBatchCache#key()}对入参的计算结果应该是相同的
     * @return
     */
    String retKey();

    /**
     * 是否将其他机房作为次级数据来源
     */
    boolean usingOtherDcWhenMissing() default false;
}
