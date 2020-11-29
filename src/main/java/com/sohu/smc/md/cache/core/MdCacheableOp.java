package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCacheable;
import com.sohu.smc.md.cache.spring.CacheConfig;
import com.sohu.smc.md.cache.spring.SpelParseService;
import org.springframework.expression.Expression;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
public class MdCacheableOp {

    private final Expression keyExpr;
    private final Cache cache;
    private final CacheConfig cacheConfig;

    public MdCacheableOp(MdCacheable mdCacheable, Cache cache, CacheConfig cacheConfig,
                         SpelParseService spelParseService) {
        this.cache = cache;
        this.cacheConfig = cacheConfig;
        this.keyExpr = spelParseService.getExpression(mdCacheable.key());
    }

    /**
     *
     * @param key key
     * @throws RuntimeException e
     * @return 返回null表示缓存没有命中,如果命中,返回{@link NullValue#NULL}表示缓存的值为null
     */
    public Mono<ValueWrapper> getCacheValue(Object key) throws RuntimeException {
        return cache.get(key)
                .map(ValueWrapper::wrap);
    }

    public Mono<Object> processCacheableOp(InvocationContext invocationContext) {
        Object key = OpHelper.getKey(invocationContext, this, keyExpr);
        return getCacheValue(key)
                .filter(Objects::nonNull)
                .map(ValueWrapper::get)
                .switchIfEmpty(invocationContext.doInvoke())
                .flatMap(o -> cache.set(key, o, cacheConfig.getDefaultExpireTime()));
    }

}
