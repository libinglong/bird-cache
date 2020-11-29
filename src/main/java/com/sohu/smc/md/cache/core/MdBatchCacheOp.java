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
    private final CacheConfig cacheConfig;
    private final int listIndex;

    public MdBatchCacheOp(MdBatchCache mdBatchCache, Cache cache, CacheConfig cacheConfig,
                          SpelParseService spelParseService) {
        this.cache = cache;
        this.cacheConfig = cacheConfig;
        this.keyExpr = spelParseService.getExpression(mdBatchCache.key());
        this.listExpr = spelParseService.getExpression("p" + mdBatchCache.index());
        this.retKeyExpr = spelParseService.getExpression(mdBatchCache.retKey());
        this.listIndex = mdBatchCache.index();
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
            entry.setOriginObj(o);
            entry.setKeyObj(keyObj);
            entries.add(entry);
        }
        return entries;
    }


    public Mono<List<Object>> processBatchCacheOp(InvocationContext invocationContext) {
        MethodInvocation methodInvocation = invocationContext.getMethodInvocation();
        List<Entry> entries = getEntries(methodInvocation);
        return Mono.just(entries)
                .flatMapMany(Flux::fromIterable)
                .map(Entry::getKeyObj)
                .collectList()
                .flatMap(keys -> cache.get(keys)
                        .flatMapMany(Flux::fromIterable)
                        .zipWith(Flux.fromIterable(keys))
                        .collectMap(Tuple2::getT1, Tuple2::getT2))
                .zipWith(Mono.just(entries))
                .doOnNext(tuple -> {
                    Map<?, ?> map = tuple.getT1();
                    List<Entry> entries1 = tuple.getT2();
                    entries1.forEach(entry -> entry.setValueWrapper(ValueWrapper.wrap(map.get(entry.getKeyObj()))));
                })
                .thenMany(Flux.fromIterable(entries))
                .filter(entry -> entry.getValueWrapper() == null)
                .map(Entry::getOriginObj)
                .collectList()
                .flatMap(objects -> {
                    invocationContext.getMethodInvocation()
                            .getArguments()[listIndex] = objects;
                    return invocationContext.doInvoke();
                })
                .map(o -> (List<?>)o)
                .flatMapMany(Flux::fromIterable)
                .collectMap(this.retKeyExpr::getValue)
                .zipWith(Mono.just(entries))
                .doOnNext(tuple -> {
                    Map<Object, ?> map = tuple.getT1();
                    List<Entry> entries1 = tuple.getT2();
                    entries1.forEach(entry -> {
                        Object o = map.get(entry.getKeyObj());
                        if (o != null){
                            entry.setValueWrapper(ValueWrapper.wrap(o));
                        }
                    });
                    Map<Object, Object> kvs = entries1.stream()
                            .collect(Collectors.toMap(Entry::getKeyObj, entry -> entry.getValueWrapper().get()));
                    cache.setKvs(kvs, cacheConfig.getDefaultExpireTime());
                })
                .thenMany(Flux.fromIterable(entries))
                .map(entry -> entry.getValueWrapper().get())
                .collectList();
    }

}
