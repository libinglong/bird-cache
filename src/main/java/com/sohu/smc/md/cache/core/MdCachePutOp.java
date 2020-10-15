package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCachePut;
import com.sohu.smc.md.cache.spring.CacheProperties;
import com.sohu.smc.md.cache.spring.SpelParseService;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
public class MdCachePutOp extends AbstractKeyOp<MdCachePut> {

    public MdCachePutOp(MetaData<MdCachePut> metaData, Cache cache, CacheProperties cacheProperties,
                          SpelParseService spelParseService) {
        super(metaData, cache, cacheProperties, spelParseService);
    }

    public void delayInvalid(InvocationContext invocationContext) throws RuntimeException {
        cache.expire(getKey(invocationContext), getDelayInvalidTime());
    }

    public void set(InvocationContext invocationContext, Object value){
        cache.set(getKey(invocationContext), value, getExpiredTime());
    }

    @Override
    protected String getKeyExpr() {
        return metaData.getAnno()
                .key();
    }
}
