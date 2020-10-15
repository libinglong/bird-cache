package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.spring.CacheConfig;
import com.sohu.smc.md.cache.spring.SpelParseService;

import java.lang.annotation.Annotation;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/10
 */
public class AbstractOp<A extends Annotation> {

    protected CacheConfig cacheConfig;
    protected SpelParseService spelParseService;
    protected MetaData<A> metaData;
    protected Cache cache;

    public AbstractOp(MetaData<A> metaData, Cache cache, CacheConfig cacheConfig, SpelParseService spelParseService) {
        this.metaData = metaData;
        this.cache = cache;
        this.cacheConfig = cacheConfig;
        this.spelParseService = spelParseService;
    }
}
