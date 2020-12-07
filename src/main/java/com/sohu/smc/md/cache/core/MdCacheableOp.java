package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCacheable;
import com.sohu.smc.md.cache.spring.CacheConfig;
import com.sohu.smc.md.cache.spring.SpelParseService;
import org.springframework.expression.Expression;
import reactor.core.publisher.Mono;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
public class MdCacheableOp {

    private final Expression keyExpr;
    private final Cache cache;
    private final Cache secondaryCache;
    private final CacheConfig cacheConfig;
    private final boolean usingOtherDcWhenMissing;

    public MdCacheableOp(MdCacheable mdCacheable, Cache cache, Cache secondaryCache, CacheConfig cacheConfig,
                         SpelParseService spelParseService) {
        this.cache = cache;
        this.secondaryCache = secondaryCache;
        this.cacheConfig = cacheConfig;
        this.keyExpr = spelParseService.getExpression(mdCacheable.key());
        this.usingOtherDcWhenMissing = mdCacheable.usingOtherDcWhenMissing();
    }

    public Mono<Object> processCacheableOp(InvocationContext invocationContext) {
        Object key = OpHelper.getKey(invocationContext, this, keyExpr);
        Mono<Object> fallback = invocationContext.doInvoke()
                .defaultIfEmpty(NullValue.NULL);
        if (usingOtherDcWhenMissing){
            fallback = secondaryCache.get(key)
                    .switchIfEmpty(fallback);
        }
        fallback = fallback.flatMap(o -> cache.set(key, o, cacheConfig.getDefaultExpireTime()).then(Mono.just(o)));
        return cache.get(key)
                .switchIfEmpty(fallback);
    }

}
