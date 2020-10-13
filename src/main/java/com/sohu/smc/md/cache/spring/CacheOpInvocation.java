package com.sohu.smc.md.cache.spring;

import com.sohu.smc.md.cache.core.*;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/30
 */
public class CacheOpInvocation extends StaticMethodMatcherPointcut implements MethodInterceptor {

    @Autowired
    private CacheOpParseService cacheOpParseService;

    private Map<Method,MethodOpContext> contextMap = new ConcurrentHashMap<>(256);

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
        invocationContext.setExecTime(getMaxExecTime(evictOps,putOps));
        MdCacheableOp cacheableOp = methodOpContext.getCacheableOp();
        MdBatchCacheOp batchCacheOp = methodOpContext.getBatchCacheOp();
        MdCacheClearOp clearOp = methodOpContext.getClearOp();
        if (clearOp != null){
            clearOp.clear();
        }
        evictOps.forEach(mdCacheEvictOp -> mdCacheEvictOp.delayInvalid(invocationContext));
        putOps.forEach(mdCachePutOp -> mdCachePutOp.delayInvalid(invocationContext));
        Object result;
        if (cacheableOp != null){
            result = processCacheableOp(cacheableOp, invocationContext);
        } else if (batchCacheOp != null){
            result = processBatchCacheOp(batchCacheOp, invocationContext);
        } else {
            result = invocationContext.doInvoke();
        }
        putOps.forEach(mdCachePutOp -> mdCachePutOp.set(invocationContext, result));
        evictOps.forEach(mdCacheEvictOp -> mdCacheEvictOp.delete(invocationContext));
        return result;
    }

    private Object processCacheableOp(MdCacheableOp cacheableOp, InvocationContext invocationContext) throws Throwable {
        return cacheableOp.processCacheableOp(invocationContext);
    }

    private Object processBatchCacheOp(MdBatchCacheOp batchCacheOp, InvocationContext invocationContext) throws Throwable {
        return batchCacheOp.processBatchCacheOp(invocationContext);
    }

    private long getMaxExecTime(List<MdCacheEvictOp> evictOps, List<MdCachePutOp> putOps){
        return Stream.of(evictOps, putOps)
                .flatMap(Collection::stream)
                .mapToLong(AbstractKeyOp::getExecTime)
                .max()
                .getAsLong();
    }

}
