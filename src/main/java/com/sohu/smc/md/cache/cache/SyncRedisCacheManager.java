package com.sohu.smc.md.cache.cache;

import com.sohu.smc.md.cache.core.Cache;
import com.sohu.smc.md.cache.core.RedisSyncHandler;
import com.sohu.smc.md.cache.core.SyncCacheManager;
import com.sohu.smc.md.cache.core.SyncHandler;
import com.sohu.smc.md.cache.serializer.PbSerializer;
import com.sohu.smc.md.cache.serializer.ReactorSerializer;
import com.sohu.smc.md.cache.serializer.Serializer;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.io.Closeable;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/26
 */
@SuppressWarnings("unused")
public class SyncRedisCacheManager implements Closeable, SyncCacheManager, InitializingBean {

    private final Serializer serializer = new ReactorSerializer(new PbSerializer());
    private final ClientResources primaryClientResources;
    private final ClientResources secondaryClientResources;
    private final String primaryRedisURI;
    private final String secondaryRedisURI;
    private final boolean isCluster;
    private SyncHandler syncHandler;

    private RedisCacheManager primaryRedisCacheManager;
    private RedisCacheManager secondaryRedisCacheManager;

    public SyncRedisCacheManager(String redisURI, String secondaryRedisURI, boolean isCluster) {
        this(redisURI, DefaultClientResources.create(), secondaryRedisURI, DefaultClientResources.create(), isCluster);
    }

    public SyncRedisCacheManager(String primaryRedisURI, ClientResources primaryClientResources, String secondaryRedisURI,
                                 ClientResources secondaryClientResources, boolean isCluster) {
        Assert.hasText(primaryRedisURI,"primaryRedisURI can not be empty");
        Assert.notNull(primaryClientResources,"primaryClientResources can not be null");
        Assert.hasText(secondaryRedisURI,"secondaryRedisURI can not be empty");
        Assert.notNull(secondaryClientResources,"secondaryClientResources can not be null");
        this.primaryRedisURI = primaryRedisURI;
        this.primaryClientResources = primaryClientResources;
        this.secondaryRedisURI = secondaryRedisURI;
        this.secondaryClientResources = secondaryClientResources;
        this.isCluster = isCluster;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        primaryRedisCacheManager = new RedisCacheManager(primaryRedisURI, primaryClientResources, isCluster);
        secondaryRedisCacheManager = new RedisCacheManager(secondaryRedisURI, secondaryClientResources, isCluster);
        primaryRedisCacheManager.afterPropertiesSet();
        secondaryRedisCacheManager.afterPropertiesSet();
        if (syncHandler == null){
            RedisSyncHandler redisSyncHandler = new RedisSyncHandler(primaryRedisURI, primaryClientResources, secondaryRedisURI,
                    secondaryClientResources, serializer, isCluster);
            redisSyncHandler.afterPropertiesSet();
            syncHandler = redisSyncHandler;
        }
    }

    @Override
    public Cache getCache(String cacheSpaceName) {
        return primaryRedisCacheManager.getCache(cacheSpaceName);
    }

    @Override
    public Cache getSecondaryCache(String cacheSpaceName) {
        return secondaryRedisCacheManager.getCache(cacheSpaceName);
    }

    @Override
    public SyncHandler getSyncHandler() {
        return syncHandler;
    }

    @Override
    public void close() {
        primaryRedisCacheManager.close();
        secondaryRedisCacheManager.close();
    }
}
