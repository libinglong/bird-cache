package com.sohu.smc.md.cache.core;

import reactor.core.publisher.Mono;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/24
 */
public interface SyncHandler {

    Mono<Void> clearSync(String cacheSpaceName);
    Mono<Void> evictSync(String cacheSpaceName, Object key);
    Mono<Void> putSync(String cacheSpaceName, Object key, Object value);

}
