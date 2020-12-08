package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCachePut;
import com.sohu.smc.md.cache.spring.CacheConfig;
import com.sohu.smc.md.cache.spring.SpelParseService;
import org.springframework.expression.Expression;
import reactor.core.publisher.Mono;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
public class MdCachePutOp {

    private final Cache cache;
    private final CacheConfig cacheConfig;
    private final Expression keyExpr;
    private final boolean needSync;
    private final SyncHandler syncHandler;

    public MdCachePutOp(MdCachePut mdCachePut, Cache cache, CacheConfig cacheConfig,
                        SpelParseService spelParseService, boolean needSync, SyncHandler syncHandler) {
        this.cache = cache;
        this.cacheConfig = cacheConfig;
        this.needSync = needSync;
        this.syncHandler = syncHandler;
        this.keyExpr = spelParseService.getExpression(mdCachePut.key());
    }

    public Mono<Void> delayInvalid(InvocationContext invocationContext) throws RuntimeException {
        return cache.expire(OpHelper.getKey(invocationContext, this, keyExpr), cacheConfig.getDefaultDelayInvalidTime());
    }

    public Mono<Void> set(InvocationContext invocationContext, Object value){
        Object key = OpHelper.getKey(invocationContext, this, keyExpr);
        Mono<Void> set = cache.set(key, value, cacheConfig.getDefaultExpireTime());
        if (!needSync){
            return set;
        }
        return set.doOnTerminate(() -> syncHandler.putSync(cache.getCacheSpaceName(), key, value));
    }

}
