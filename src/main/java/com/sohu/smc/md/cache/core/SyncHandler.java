package com.sohu.smc.md.cache.core;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/24
 */
public interface SyncHandler {

    void clearSync(String cacheSpaceName);
    void evictSync(String cacheSpaceName, Object key);
    void putSync(String cacheSpaceName, Object key, Object value);

}
