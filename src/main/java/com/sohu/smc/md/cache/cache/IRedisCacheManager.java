package com.sohu.smc.md.cache.cache;

import com.sohu.smc.md.cache.core.CacheManager;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/16
 */
public interface IRedisCacheManager extends CacheManager {

    /**
     * 释放必要的资源
     */
    void shutdown();

}
