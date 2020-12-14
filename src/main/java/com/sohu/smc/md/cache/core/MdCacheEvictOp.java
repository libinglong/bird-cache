package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCacheEvict;
import com.sohu.smc.md.cache.spring.CacheProperty;
import com.sohu.smc.md.cache.spring.SpelParseService;
import org.springframework.expression.Expression;
import reactor.core.publisher.Mono;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
public class MdCacheEvictOp {

    private Cache cache;
    private final CacheProperty cacheProperty;
    private final Expression keyExpr;
    private final String cacheSpaceName;
    private final CacheManager cacheManager;

    public MdCacheEvictOp(MdCacheEvict mdCacheEvict, String cacheSpaceName, CacheManager cacheManager, CacheProperty cacheProperty,
                          SpelParseService spelParseService) {
        this.cacheSpaceName = cacheSpaceName;
        this.cacheManager = cacheManager;
        this.cacheProperty = cacheProperty;
        this.keyExpr = spelParseService.getExpression(mdCacheEvict.key());
        init();
    }

    private void init() {
        this.cache = cacheManager.getCache(cacheSpaceName);
    }

    public Mono<Void> delayInvalid(InvocationContext invocationContext) throws RuntimeException {
        return cache.expire(OpHelper.getKey(invocationContext, this, keyExpr), cacheProperty.getDelayInvalidTime());
    }

    public Mono<Void> delete(InvocationContext invocationContext) throws RuntimeException {
        Object key = OpHelper.getKey(invocationContext, this, keyExpr);
        Mono<Void> delete = cache.delete(key);
        if (cacheManager instanceof SyncCacheManager){
            SyncHandler syncHandler = ((SyncCacheManager) cacheManager).getSyncHandler();
            return delete.doOnTerminate(() -> syncHandler.evictSync(cache.getCacheSpaceName(), key));
        }
        return delete;
    }

}
