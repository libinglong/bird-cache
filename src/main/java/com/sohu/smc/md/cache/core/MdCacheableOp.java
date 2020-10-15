package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCacheable;
import com.sohu.smc.md.cache.spring.CacheConfig;
import com.sohu.smc.md.cache.spring.SpelParseService;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
public class MdCacheableOp extends AbstractKeyOp<MdCacheable> {

    public MdCacheableOp(MetaData<MdCacheable> metaData, Cache cache, CacheConfig cacheConfig,
                          SpelParseService spelParseService) {
        super(metaData, cache, cacheConfig, spelParseService);
    }

    /**
     *
     * @param key key
     * @throws RuntimeException e
     * @return 返回null表示缓存没有命中,如果命中,返回{@link NullValue#NULL}表示缓存的值为null
     */
    public ValueWrapper getCacheValue(Object key) throws RuntimeException {
        return wrapper(cache.get(key));
    }

    @Override
    protected String getKeyExpr() {
        return metaData.getAnno()
                .key();
    }

    public Object processCacheableOp(InvocationContext invocationContext) throws Throwable {
        Object key = getKey(invocationContext);
        ValueWrapper valueWrapper = getCacheValue(key);
        if (valueWrapper == null){
            Object result = invocationContext.doInvoke();
            cache.set(key, result, getExpiredTime());
            return result;
        }
        return valueWrapper.get();
    }

}
