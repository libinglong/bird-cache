package com.sohu.smc.md.cache.core;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/12
 */
public interface CacheManager {

    /**
     * 根据命名空间获取Cache
     * @param cacheSpaceName cacheSpaceName
     * @return cache
     */
    Cache getCache(String cacheSpaceName);
}
