package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCacheable;
import com.sohu.smc.md.cache.spring.CacheConfig;
import com.sohu.smc.md.cache.spring.SpelParseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
@Slf4j
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
        AtomicBoolean needCache = new AtomicBoolean(true);
        Mono<Object> processCache = cache.get(key)
                .doOnNext(o -> needCache.set(false));
        if (usingOtherDcWhenMissing){
            processCache = processCache
                    .switchIfEmpty(getFromSecondaryCache(key));
        }
        Mono<Object> ret = processCache
                .switchIfEmpty(doInvoke(invocationContext))
                .cache();
        return ret.filter(o -> needCache.get())
                .flatMap(o -> cache.set(key, o, cacheConfig.getDefaultExpireTime()))
                .then(ret);
    }

    private Mono<Object> getFromSecondaryCache(Object key){
        return Mono.fromRunnable(() -> log.debug("fallback to request other dc"))
                .then(secondaryCache.get(key));
    }

    private Mono<Object> doInvoke(InvocationContext invocationContext){
        return Mono.fromRunnable(() -> log.debug("fallback the actual method invoke"))
                .then(invocationContext.doInvoke())
                .defaultIfEmpty(NullValue.REAL_NULL);
    }

}
