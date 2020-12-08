package com.sohu.smc.md.cache.spring;

import com.sohu.smc.md.cache.core.*;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/30
 */
public class CacheOpInvocation extends StaticMethodMatcherPointcut implements MethodInterceptor{

    private final CacheOpParseService cacheOpParseService;
    private final CacheConfig cacheConfig;

    public CacheOpInvocation(CacheOpParseService cacheOpParseService, CacheConfig cacheConfig) {
        this.cacheOpParseService = cacheOpParseService;
        this.cacheConfig = cacheConfig;
    }

    private final Map<Method,MethodOpContext> contextMap = new ConcurrentHashMap<>(256);

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        MethodOpContext methodOpContext = cacheOpParseService.getContext(method);
        contextMap.put(method,methodOpContext);
        methodOpContext.validate();
        return methodOpContext.hasAnyOp();
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        InvocationContext invocationContext = new InvocationContext(invocation);
        MethodOpContext methodOpContext = contextMap.get(invocation.getMethod());
        List<MdCacheEvictOp> evictOps = methodOpContext.getEvictOps();
        List<MdCachePutOp> putOps = methodOpContext.getPutOps();
        invocationContext.setExecTime(cacheConfig.getDefaultDelayInvalidTime());
        MdCacheableOp cacheableOp = methodOpContext.getCacheableOp();
        MdBatchCacheOp batchCacheOp = methodOpContext.getBatchCacheOp();
        MdCacheClearOp clearOp = methodOpContext.getClearOp();
        Flux<Void> clearCache = Mono.justOrEmpty(clearOp)
                .flatMap(MdCacheClearOp::clear)
                .thenMany(Flux.fromIterable(evictOps))
                .flatMap(mdCacheEvictOp -> mdCacheEvictOp.delayInvalid(invocationContext))
                .thenMany(Flux.fromIterable(putOps))
                .flatMap(mdCachePutOp -> mdCachePutOp.delayInvalid(invocationContext));
        Mono<?> resultCache = clearCache.then(processCache(cacheableOp, batchCacheOp, invocationContext))
                .cache();
        Mono<?> result = resultCache.zipWith(Mono.justOrEmpty(putOps))
                .flatMapMany(tuple -> Flux.fromIterable(tuple.getT2())
                        .flatMap(mdCachePutOp -> mdCachePutOp.set(invocationContext, tuple.getT1())))
                .thenMany(Flux.fromIterable(evictOps))
                .flatMap(mdCacheEvictOp -> mdCacheEvictOp.delete(invocationContext))
                .then(resultCache);
        return unwrapIfNecessary(result, invocation);
    }

    private Object unwrapIfNecessary(Mono<?> result, MethodInvocation methodInvocation) throws ExecutionException, InterruptedException {
        result = result.flatMap(o -> {
            if(o instanceof NullValue){
                return Mono.empty();
            } else if (o instanceof List){
                List<?> wrapList = (List<?>) o;
                List<Object> unWrapList = new ArrayList<>();
                wrapList.forEach(o1 -> {
                    if (o1 instanceof NullValue){
                        unWrapList.add(null);
                    } else {
                        unWrapList.add(o1);
                    }
                });
                return Mono.just(unWrapList);
            } else {
                return Mono.just(o);
            }
        });
        if (Mono.class.isAssignableFrom(
                methodInvocation.getMethod().getReturnType())) {
            return result;
        } else if (Flux.class.isAssignableFrom(
                methodInvocation.getMethod().getReturnType())) {
            throw new RuntimeException("do not support Flux return type");
        } else if (CompletionStage.class.isAssignableFrom(
                methodInvocation.getMethod().getReturnType())) {
            return result.toFuture();
        }
        return result.toFuture()
                .get();
    }

    private Mono<?> processCache(MdCacheableOp cacheableOp, MdBatchCacheOp batchCacheOp, InvocationContext invocationContext) {
        if (cacheableOp != null) {
            return processCacheableOp(cacheableOp, invocationContext);
        } else if (batchCacheOp != null) {
            return processBatchCacheOp(batchCacheOp, invocationContext);
        } else {
            return invocationContext.doInvoke();
        }
    }

    private Mono<Object> processCacheableOp(MdCacheableOp cacheableOp, InvocationContext invocationContext) {
        return cacheableOp.processCacheableOp(invocationContext);
    }

    private Mono<List<Object>> processBatchCacheOp(MdBatchCacheOp batchCacheOp, InvocationContext invocationContext) {
        return batchCacheOp.processBatchCacheOp(invocationContext);
    }

}
