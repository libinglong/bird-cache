package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdBatchCache;
import com.sohu.smc.md.cache.spel.ParamEvaluationContext;
import com.sohu.smc.md.cache.spring.CacheConfig;
import com.sohu.smc.md.cache.spring.SpelParseService;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
@Slf4j
public class MdBatchCacheOp {

    private final Expression listExpr;
    private final Expression retKeyExpr;
    private final Expression keyExpr;
    private final Cache cache;
    private final Cache secondaryCache;
    private final CacheConfig cacheConfig;
    private final int listIndex;
    private final boolean usingOtherDcWhenMissing;
    private final Duration timeout = Duration.of(200, ChronoUnit.MILLIS);

    public MdBatchCacheOp(MdBatchCache mdBatchCache, Cache cache, Cache secondaryCache, CacheConfig cacheConfig,
                          SpelParseService spelParseService) {
        this.cache = cache;
        this.secondaryCache = secondaryCache;
        this.cacheConfig = cacheConfig;
        this.keyExpr = spelParseService.getExpression(mdBatchCache.key());
        this.listExpr = spelParseService.getExpression("#p" + mdBatchCache.index());
        this.retKeyExpr = spelParseService.getExpression(mdBatchCache.retKey());
        this.listIndex = mdBatchCache.index();
        this.usingOtherDcWhenMissing = mdBatchCache.usingOtherDcWhenMissing();
    }

    public List<Entry> getEntries(MethodInvocation methodInvocation){
        EvaluationContext context = new ParamEvaluationContext(methodInvocation.getArguments());
        List<?> list = listExpr.getValue(context, List.class);
        List<Entry> entries = new ArrayList<>();
        for (Object o : list){
            EvaluationContext ctx = new ParamEvaluationContext(methodInvocation.getArguments());
            ctx.setVariable("obj", o);
            Object keyObj = keyExpr.getValue(ctx);
            Entry entry = new Entry();
            entry.setOriginKeyObj(o);
            entry.setCachedKeyObj(keyObj);
            entries.add(entry);
        }
        return entries;
    }


    public Mono<List<Object>> processBatchCacheOp(InvocationContext invocationContext) {
        MethodInvocation methodInvocation = invocationContext.getMethodInvocation();
        List<Entry> entries = getEntries(methodInvocation);
        Mono<?> processCache = Mono.justOrEmpty(entries)
                .flatMapMany(Flux::fromIterable)
                .map(Entry::getCachedKeyObj)
                .collectList()
                .flatMap(this::getCacheMap)
                .zipWith(Mono.justOrEmpty(entries))
                .doOnNext(tuple -> {
                    Map<?, ?> map = tuple.getT1();
                    List<Entry> entries1 = tuple.getT2();
                    entries1.forEach(entry -> {
                        Object o = map.get(entry.getCachedKeyObj());
                        entry.setValue(o);
                        if (NullValue.MISS_NULL.equals(o)){
                            entry.setNeedCache(true);
                        }
                    });
                });
        if (usingOtherDcWhenMissing){
            processCache = processCache
                    .thenMany(Flux.fromIterable(entries))
                    .filter(entry -> NullValue.MISS_NULL.equals(entry.getValue()))
                    .map(Entry::getCachedKeyObj)
                    .collectList()
                    .filter(objects -> !objects.isEmpty())
                    .doOnNext(objects -> log.debug("fallback to request other dc"))
                    .flatMap(this::getSecondaryCacheMap)
                    .timeout(timeout)
                    .onErrorResume(throwable -> Mono.empty())
                    .zipWith(Mono.justOrEmpty(entries))
                    .doOnNext(tuple -> {
                        Map<Object, CacheValue> map = tuple.getT1();
                        List<Entry> entries1 = tuple.getT2();
                        entries1.forEach(entry -> {
                            CacheValue cacheValue = map.get(entry.getCachedKeyObj());
                            if (cacheValue !=null && !NullValue.MISS_NULL.equals(cacheValue.getV())){
                                entry.setValue(cacheValue.getV());
                                entry.setPttl(cacheValue.getPttl());
                                entry.setFromOtherDc(true);
                            }
                        });
                    });
        }
        return processCache
                .thenMany(Flux.fromIterable(entries))
                .filter(entry -> NullValue.MISS_NULL.equals(entry.getValue()))
                .map(Entry::getOriginKeyObj)
                .collectList()
                .filter(objects -> !objects.isEmpty())
                .doOnNext(o -> log.debug("fallback to actual method invoke"))
                .flatMap(objects -> {
                    invocationContext.getMethodInvocation()
                            .getArguments()[listIndex] = objects;
                    return invocationContext.doInvoke()
                            .map(ret -> (List<?>) ret)
                            .doOnNext(this::check)
                            .flatMapMany(Flux::fromIterable)
                            .collectMap(e -> {
                                if (e instanceof NullValue) {
                                    return ((NullValue) e).get();
                                }
                                ParamEvaluationContext context = new ParamEvaluationContext(methodInvocation.getArguments());
                                context.setVariable("obj", e);
                                return retKeyExpr.getValue(context);
                            }, o -> (Object)o)
                            .map(kvs -> {
                                objects.forEach(o -> {
                                    if (!kvs.containsKey(o)){
                                        kvs.put(o, NullValue.REAL_NULL);
                                    }
                                });
                                return kvs;
                            });
                })
                .zipWith(Mono.justOrEmpty(entries))
                .doOnNext(tuple -> {
                    Map<Object, ?> map = tuple.getT1();
                    List<Entry> entries1 = tuple.getT2();
                    entries1.forEach(entry -> {
                        Object o = map.get(entry.getCachedKeyObj());
                        if (o != null) {
                            entry.setValue(o);
                        }
                    });
                })
                .thenMany(Flux.fromIterable(entries))
                .filter(Entry::isNeedCache)
                .flatMap(entry -> {
                    if (!entry.isFromOtherDc()){
                        return cache.set(entry.getCachedKeyObj(), entry.getValue(), cacheConfig.getDefaultExpireTime());
                    }
                    if (entry.getPttl() > 0){
                        return cache.set(entry.getCachedKeyObj(), entry.getValue(), entry.getPttl());
                    }
                    return cache.set(entry.getCachedKeyObj(), entry.getValue());
                })
                .thenMany(Flux.fromIterable(entries))
                .map(Entry::getValue)
                .collectList()
                .map(objects -> {
                    List<Object> ret = new ArrayList<>();
                    objects.forEach(o -> {
                        if (o instanceof NullValue){
                            ret.add(null);
                        } else {
                            ret.add(o);
                        }
                    });
                    return ret;
                });
    }

    private void check(List<?> list){
        list.forEach(o -> {
            if (o == null){
                throw new RuntimeException("MdBatchCache don't support null element in return list");
            }
        });
    }

    private Mono<Map<Object, Object>> getCacheMap(List<Object> keys){
        return cache.get(keys)
                .zipWith(Flux.fromIterable(keys))
                .collectMap(Tuple2::getT2, Tuple2::getT1);
    }

    private Mono<Map<Object, CacheValue>> getSecondaryCacheMap(List<Object> keys){
        Flux<CacheValue> cvs = Flux.fromIterable(keys)
                .flatMap(secondaryCache::pttl)
                .zipWith(secondaryCache.get(keys))
                .map(tuple2 -> new CacheValue(tuple2.getT1(), tuple2.getT2()));
        return Flux.fromIterable(keys)
                .zipWith(cvs)
                .collectMap(Tuple2::getT1, Tuple2::getT2);
    }

}
