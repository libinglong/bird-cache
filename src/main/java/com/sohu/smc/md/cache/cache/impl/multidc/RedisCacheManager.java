package com.sohu.smc.md.cache.cache.impl.multidc;

import com.sohu.smc.md.cache.core.CacheManager;
import com.sohu.smc.md.cache.serializer.Serializer;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/16
 */
public interface RedisCacheManager extends CacheManager {

    /**
     * 序列化方式
     * @return serializer
     */
    Serializer getSerializer();

    /**
     * 释放必要的资源
     */
    void shutdown();

}
