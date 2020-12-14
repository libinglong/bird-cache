package com.sohu.smc.md.cache.core;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/12/14
 */
public interface SyncCacheManager extends CacheManager {

    /**
     * 根据命名空间获取Cache
     * @param cacheSpaceName cacheSpaceName
     * @return cache
     */
    Cache getSecondaryCache(String cacheSpaceName);

    /**
     * 当needSync()返回true时,返回SyncHandler用于同步缓存
     * 否则返回null
     * @return SyncHandler同步器
     */
    SyncHandler getSyncHandler();
}
