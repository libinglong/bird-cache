package com.sohu.smc.md.cache.spring;

import com.sohu.smc.md.cache.core.*;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/30
 */
public class CacheOpMethodInterceptor implements MethodInterceptor {

    @Autowired
    private CacheOpParseService cacheOpParseService;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        InvocationContext invocationContext = new InvocationContext(invocation);
        Method method = invocation.getMethod();
        List<MdCacheEvictOp> evictOps = cacheOpParseService.getOps(method, MdCacheEvictOp.class);
        List<MdCachePutOp> putOps = cacheOpParseService.getOps(method, MdCachePutOp.class);
        MdCacheableOp cacheableOp = cacheOpParseService.getOp(method, MdCacheableOp.class);
        MdBatchCacheOp batchCacheOp = cacheOpParseService.getOp(method, MdBatchCacheOp.class);
        boolean twoCacheOpsExist = cacheableOp != null && batchCacheOp != null;
        Assert.isTrue(!twoCacheOpsExist, "MdCacheable and MdBatchCacheOp can not exist at the same time");
        evictOps.forEach(mdCacheEvictOp -> mdCacheEvictOp.expire(invocationContext));
        putOps.forEach(mdCachePutOp -> mdCachePutOp.expire(invocationContext));
        Object result;
        if (cacheableOp != null){
            result = processCacheableOp(cacheableOp, invocationContext);
        } else if (batchCacheOp != null){
            result = processBatchCacheOp(batchCacheOp, invocationContext);
        } else {
            result = invocation.proceed();
        }
        putOps.forEach(mdCachePutOp -> mdCachePutOp.put(invocationContext, result));
        evictOps.forEach(mdCacheEvictOp -> mdCacheEvictOp.delete(invocationContext));
        return result;
    }

    private Object processCacheableOp(MdCacheableOp cacheableOp, InvocationContext invocationContext) throws Throwable {
        return cacheableOp.processCacheableOp(invocationContext);
    }

    private Object processBatchCacheOp(MdBatchCacheOp batchCacheOp, InvocationContext invocationContext) throws Throwable {
        return batchCacheOp.processBatchCacheOp(invocationContext);
    }

}
