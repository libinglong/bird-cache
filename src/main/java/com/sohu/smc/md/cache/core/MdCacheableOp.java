package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCacheable;
import com.sohu.smc.md.cache.spring.CacheProperty;
import com.sohu.smc.md.cache.spring.SpelParseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import reactor.core.publisher.Mono;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
@Slf4j
public class MdCacheableOp {

    private final Expression keyExpr;
    private final Cache cache;
    private final CacheProperty cacheProperty;

    public MdCacheableOp(MdCacheable mdCacheable, String cacheSpaceName, CacheManager cacheManager, CacheProperty cacheProperty,
                         SpelParseService spelParseService) {
        this.cacheProperty = cacheProperty;
        this.keyExpr = spelParseService.getExpression(mdCacheable.key());
        this.cache = cacheManager.getCache(cacheSpaceName);
    }

    public Mono<Object> processCacheableOp(InvocationContext invocationContext) {
        Object key = OpHelper.getKey(invocationContext, this, keyExpr);
        Entry oriEntry = new Entry();
        return Mono.just(oriEntry)
                .doOnNext(entry -> entry.setCachedKeyObj(key))
                .flatMap(entry -> cache.get(entry.getCachedKeyObj()))
                .zipWith(Mono.just(oriEntry))
                .doOnNext(tuple2 -> {
                    Object v = tuple2.getT1();
                    Entry entry = tuple2.getT2();
                    entry.setValue(v);
                    if (NullValue.MISS_NULL.equals(v)) {
                        entry.setNeedCache(true);
                    }
                })
                .then(Mono.just(oriEntry))
                .filter(entry -> NullValue.MISS_NULL.equals(entry.getValue()))
                .doOnNext(o -> log.debug("fallback the actual method invoke"))
                .flatMap(entry -> doInvoke(invocationContext).doOnNext(entry::setValue))
                .then(Mono.just(oriEntry))
                .filter(Entry::isNeedCache)
                .flatMap(entry -> {
                    if (cacheProperty.getExpireTime() > 0){
                        return cache.set(entry.getCachedKeyObj(), entry.getValue(), cacheProperty.getExpireTime());
                    }
                    return cache.set(entry.getCachedKeyObj(), entry.getValue());
                })
                .then(Mono.just(oriEntry))
                .map(Entry::getValue)
                .flatMap(o -> {
                    if (o instanceof NullValue){
                        return Mono.empty();
                    }
                    return Mono.just(o);
                });
    }

    private Mono<Object> doInvoke(InvocationContext invocationContext){
        return invocationContext.doInvoke()
                .defaultIfEmpty(NullValue.REAL_NULL);
    }
}
