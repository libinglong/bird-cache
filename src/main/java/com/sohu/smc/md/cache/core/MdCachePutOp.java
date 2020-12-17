package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCachePut;
import com.sohu.smc.md.cache.spring.CacheProperty;
import com.sohu.smc.md.cache.spring.SpelParseService;
import org.springframework.expression.Expression;
import reactor.core.publisher.Mono;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
public class MdCachePutOp {

    private Cache cache;
    private final String cacheSpaceName;
    private final CacheManager cacheManager;
    private final CacheProperty cacheProperty;
    private final Expression keyExpr;

    public MdCachePutOp(MdCachePut mdCachePut, String cacheSpaceName, CacheManager cacheManager, CacheProperty cacheProperty,
                        SpelParseService spelParseService) {
        this.cacheSpaceName = cacheSpaceName;
        this.cacheManager = cacheManager;
        this.cacheProperty = cacheProperty;
        this.keyExpr = spelParseService.getExpression(mdCachePut.key());
        init();
    }

    private void init() {
        this.cache = cacheManager.getCache(cacheSpaceName);
    }

    public Mono<Void> delayInvalid(InvocationContext invocationContext) throws RuntimeException {
        return cache.expire(OpHelper.getKey(invocationContext, this, keyExpr), cacheProperty.getDelayInvalidTime());
    }

    public Mono<Void> set(InvocationContext invocationContext, Object value){
        Object key = OpHelper.getKey(invocationContext, this, keyExpr);
        Mono<Void> set;
        if (cacheProperty.getExpireTime() > 0){
            set = cache.set(key, value, cacheProperty.getExpireTime());
        } else {
            set = cache.set(key, value);
        }
        if (cacheManager instanceof SyncCacheManager){
            SyncHandler syncHandler = ((SyncCacheManager) cacheManager).getSyncHandler();
            return set.doOnTerminate(() -> syncHandler.putSync(cache.getCacheSpaceName(), key, value));
        }
        return set;
    }

}
