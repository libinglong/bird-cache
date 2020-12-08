package com.sohu.smc.md.cache.core;

import reactor.core.publisher.Mono;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/10
 */
public class MdCacheClearOp {

    private final Cache cache;
    private final SyncHandler syncHandler;
    private final boolean needSync;

    public MdCacheClearOp(Cache cache, boolean needSync, SyncHandler syncHandler) {
        this.cache = cache;
        this.syncHandler = syncHandler;
        this.needSync = needSync;
    }

    public Mono<Void> clear(){
        Mono<Void> clear = cache.clear();
        if (!needSync){
            return clear;
        }
        return clear.doOnTerminate(() -> syncHandler.clearSync(cache.getCacheSpaceName()));
    }

}
