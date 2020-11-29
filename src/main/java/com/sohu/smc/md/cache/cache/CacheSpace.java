package com.sohu.smc.md.cache.cache;

import reactor.core.publisher.Mono;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/11
 */
public interface CacheSpace {

    /**
     * 增加缓存空间版本
     * @param cacheSpaceVersionKey
     */
    Mono<Void> incrVersion(String cacheSpaceVersionKey);

    /**
     * 获取命名空间前缀
     * @param cacheSpaceVersionKey
     * @return
     */
    Mono<String> getVersion(String cacheSpaceVersionKey);

}
