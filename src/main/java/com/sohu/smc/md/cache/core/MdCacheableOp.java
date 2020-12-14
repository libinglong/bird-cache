package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCacheable;
import com.sohu.smc.md.cache.spring.CacheProperty;
import com.sohu.smc.md.cache.spring.SpelParseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
@Slf4j
public class MdCacheableOp {

    private final Expression keyExpr;
    private final String cacheSpaceName;
    private final CacheManager cacheManager;
    private Cache cache;
    private Cache secondaryCache;
    private final CacheProperty cacheProperty;
    private final boolean usingOtherDcWhenMissing;
    private final Duration timeout = Duration.of(200, ChronoUnit.MILLIS);

    public MdCacheableOp(MdCacheable mdCacheable, String cacheSpaceName, CacheManager cacheManager, CacheProperty cacheProperty,
                         SpelParseService spelParseService) {
        this.cacheSpaceName = cacheSpaceName;
        this.cacheManager = cacheManager;
        this.cacheProperty = cacheProperty;
        this.keyExpr = spelParseService.getExpression(mdCacheable.key());
        this.usingOtherDcWhenMissing = mdCacheable.usingOtherDcWhenMissing();
        init();
    }

    public Mono<Object> processCacheableOp(InvocationContext invocationContext) {
        Object key = OpHelper.getKey(invocationContext, this, keyExpr);
        Entry oriEntry = new Entry();
        Mono<?> processCache = Mono.just(oriEntry)
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
                });
        if (usingOtherDcWhenMissing){
            processCache = processCache
                    .then(Mono.just(oriEntry))
                    .filter(entry -> NullValue.MISS_NULL.equals(entry.getValue()))
                    .doOnNext(entry -> log.debug("fallback to request other dc"))
                    .map(Entry::getCachedKeyObj)
                    .flatMap(this::getFromSecondaryCache)
                    .zipWith(Mono.just(oriEntry))
                    .doOnNext(tuple2 -> {
                        CacheValue t1 = tuple2.getT1();
                        Entry entry = tuple2.getT2();
                        entry.setValue(t1.getV());
                        if (!NullValue.MISS_NULL.equals(t1.getV())){
                            entry.setFromOtherDc(true);
                            entry.setPttl(t1.getPttl());
                        }
                    });
        }
        return processCache
                .then(Mono.just(oriEntry))
                .filter(entry -> NullValue.MISS_NULL.equals(entry.getValue()))
                .doOnNext(o -> log.debug("fallback the actual method invoke"))
                .flatMap(entry -> doInvoke(invocationContext).doOnNext(entry::setValue))
                .then(Mono.just(oriEntry))
                .filter(Entry::isNeedCache)
                .flatMap(entry -> {
                    if (!entry.isFromOtherDc()){
                        return cache.set(entry.getCachedKeyObj(), entry.getValue(), cacheProperty.getExpireTime());
                    }
                    if (entry.getPttl() > 0){
                        return cache.set(entry.getCachedKeyObj(), entry.getValue(), entry.getPttl());
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

    private Mono<CacheValue> getFromSecondaryCache(Object key){
        return secondaryCache.get(key)
                .zipWith(secondaryCache.pttl(key))
                .map(tuple2 -> new CacheValue(tuple2.getT2(), tuple2.getT1()))
                .timeout(timeout)
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<Object> doInvoke(InvocationContext invocationContext){
        return invocationContext.doInvoke()
                .defaultIfEmpty(NullValue.REAL_NULL);
    }

    public void init() {
        this.cache = cacheManager.getCache(cacheSpaceName);
        if (usingOtherDcWhenMissing && !(cacheManager instanceof SyncCacheManager)){
            throw new RuntimeException("using SyncCacheManager when usingOtherDcWhenMissing is true");
        }
        if (cacheManager instanceof SyncCacheManager){
            this.secondaryCache = ((SyncCacheManager)cacheManager).getSecondaryCache(cacheSpaceName);
        }
    }
}
