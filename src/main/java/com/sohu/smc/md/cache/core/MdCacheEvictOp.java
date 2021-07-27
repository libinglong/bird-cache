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

    private final Cache cache;
    private final CacheProperty cacheProperty;
    private final Expression keyExpr;

    public MdCacheEvictOp(MdCacheEvict mdCacheEvict, String cacheSpaceName, CacheManager cacheManager, CacheProperty cacheProperty,
                          SpelParseService spelParseService) {
        this.cacheProperty = cacheProperty;
        this.keyExpr = spelParseService.getExpression(mdCacheEvict.key());
        this.cache = cacheManager.getCache(cacheSpaceName);
    }

    public Mono<Void> delayInvalid(InvocationContext invocationContext) throws RuntimeException {
        return cache.expire(OpHelper.getKey(invocationContext, this, keyExpr), cacheProperty.getDelayInvalidTime());
    }

    public Mono<Void> delete(InvocationContext invocationContext) throws RuntimeException {
        Object key = OpHelper.getKey(invocationContext, this, keyExpr);
        return cache.delete(key);
    }

}
