package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCacheEvict;
import com.sohu.smc.md.cache.spring.CacheConfig;
import com.sohu.smc.md.cache.spring.SpelParseService;
import org.springframework.expression.Expression;
import reactor.core.publisher.Mono;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
public class MdCacheEvictOp {

    private final Cache cache;
    private final CacheConfig cacheConfig;
    private final Expression keyExpr;
    private final boolean needSync;
    private final SyncHandler syncHandler;

    public MdCacheEvictOp(MdCacheEvict mdCacheEvict, Cache cache, CacheConfig cacheConfig,
                          SpelParseService spelParseService, boolean needSync, SyncHandler syncHandler) {
        this.cache = cache;
        this.cacheConfig = cacheConfig;
        this.needSync = needSync;
        this.syncHandler = syncHandler;
        this.keyExpr = spelParseService.getExpression(mdCacheEvict.key());
    }

    public Mono<Void> delayInvalid(InvocationContext invocationContext) throws RuntimeException {
        return cache.expire(OpHelper.getKey(invocationContext, this, keyExpr), cacheConfig.getDefaultDelayInvalidTime());
    }

    public Mono<Void> delete(InvocationContext invocationContext) throws RuntimeException {
        Object key = OpHelper.getKey(invocationContext, this, keyExpr);
        Mono<Void> delete = cache.delete(key);
        if (!needSync){
            return delete;
        }
        return delete.doOnTerminate(() -> syncHandler.evictSync(cache.getCacheSpaceName(), key));
    }

}
