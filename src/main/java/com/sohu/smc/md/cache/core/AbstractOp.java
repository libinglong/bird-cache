package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.spring.CacheProperties;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.Annotation;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/10
 */
public class AbstractOp<A extends Annotation> {

    @Autowired
    protected CacheProperties cacheProperties;

    protected MetaData<A> metaData;
    protected Cache cache;

    public AbstractOp(MetaData<A> metaData, Cache cache) {
        this.metaData = metaData;
        this.cache = cache;
    }
}
