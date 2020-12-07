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

    /**
     * 根据命名空间获取Cache
     * @param cacheSpaceName cacheSpaceName
     * @return cache
     */
    default Cache getSecondaryCache(String cacheSpaceName){
        return null;
    }

    /**
     * 该缓存管理器是否需要与其他数据中心的缓存同步
     * @return true表示需要同步,否则不需要
     */
    boolean needSync();

    /**
     * 当needSync()返回true时,返回SyncHandler用于同步缓存
     * 否则返回null
     * @return SyncHandler同步器
     */
    SyncHandler getSyncHandler();
}
