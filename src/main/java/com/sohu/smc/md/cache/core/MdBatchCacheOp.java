package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdBatchCache;
import com.sohu.smc.md.cache.spel.ParamEvaluationContext;
import com.sohu.smc.md.cache.spring.CacheConfig;
import com.sohu.smc.md.cache.spring.SpelParseService;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
public class MdBatchCacheOp {

    private final Expression listExpr;
    private final Expression retKeyExpr;
    private final Expression keyExpr;
    private final Cache cache;
    private final Cache secondaryCache;
    private final CacheConfig cacheConfig;
    private final int listIndex;
    private final boolean usingOtherDcWhenMissing;

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
                        entry.setValue(map.get(entry.getCachedKeyObj()));
                        entry.setNeedCache(false);
                    });
                });
        if (usingOtherDcWhenMissing){
            processCache = processCache
                    .thenMany(Flux.fromIterable(entries))
                    .filter(entry -> entry.getValue() instanceof NullValue)
                    .map(Entry::getCachedKeyObj)
                    .collectList()
                    .flatMap(this::getSecondaryCacheMap)
                    .zipWith(Mono.justOrEmpty(entries))
                    .doOnNext(tuple -> {
                        Map<?, ?> map = tuple.getT1();
                        List<Entry> entries1 = tuple.getT2();
                        entries1.forEach(entry -> entry.setValue(map.get(entry.getCachedKeyObj())));
                    });
        }
        return processCache
                .thenMany(Flux.fromIterable(entries))
                .map(Entry::getOriginKeyObj)
                .collectList()
                .flatMap(objects -> {
                    invocationContext.getMethodInvocation()
                            .getArguments()[listIndex] = objects;
                    return invocationContext.doInvoke();
                })
                .map(o -> (List<?>)o)
                .map(this::replaceNull)
                .flatMapMany(Flux::fromIterable)
                .collectMap(o -> {
                    if (o instanceof NullValue){
                        return ((NullValue) o).getKey();
                    }
                    ParamEvaluationContext context = new ParamEvaluationContext(methodInvocation.getArguments());
                    context.setVariable("obj", o);
                    return retKeyExpr.getValue(context);
                })
                .zipWith(Mono.justOrEmpty(entries))
                .doOnNext(tuple -> {
                    Map<Object, ?> map = tuple.getT1();
                    List<Entry> entries1 = tuple.getT2();
                    entries1.forEach(entry -> {
                        Object o = map.get(entry.getCachedKeyObj());
                        entry.setValue(o);
                    });
                })
                .then(Mono.justOrEmpty(entries))
                .flatMap(entries1 -> {
                    Map<Object, Object> kvs = entries1.stream()
                            .filter(Entry::isNeedCache)
                            .collect(Collectors.toMap(Entry::getCachedKeyObj, Entry::getValue));
                    return cache.setKvs(kvs, cacheConfig.getDefaultExpireTime());
                })
                .thenMany(Flux.fromIterable(entries))
                .map(Entry::getValue)
                .collectList();
    }

    private List<Object> replaceNull(List<?> originalList){
        List<Object> list = new ArrayList<>();
        originalList.forEach(o -> {
            if (o == null){
                list.add(NullValue.NULL);
            } else {
                list.add(o);
            }
        });
        return list;
    }

    private Mono<Map<Object, Object>> getCacheMap(List<Object> keys){
        return cache.get(keys)
                .zipWith(Flux.fromIterable(keys))
                .collectMap(Tuple2::getT1, Tuple2::getT2);
    }

    private Mono<Map<Object, Object>> getSecondaryCacheMap(List<Object> keys){
        return secondaryCache.get(keys)
                .zipWith(Flux.fromIterable(keys))
                .collectMap(Tuple2::getT1, Tuple2::getT2);
    }

}
