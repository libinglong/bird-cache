package com.sohu.smc.md.cache.core;

import reactor.core.publisher.Mono;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/10
 */
public class MdCacheClearOp {

    private final String cacheSpaceName;
    private final CacheManager cacheManager;
    private Cache cache;

    public MdCacheClearOp(String cacheSpaceName, CacheManager cacheManager) {
        this.cacheSpaceName = cacheSpaceName;
        this.cacheManager = cacheManager;
        init();
    }

    private void init() {
        this.cache = cacheManager.getCache(cacheSpaceName);
    }

    public Mono<Void> clear(){
        return cache.clear();
    }

}
