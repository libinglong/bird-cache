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
     * 设置序列化方式
     * @param serializer serializer
     */
    void setSerializer(Serializer serializer);

    /**
     * 释放必要的资源
     */
    void shutdown();

}
