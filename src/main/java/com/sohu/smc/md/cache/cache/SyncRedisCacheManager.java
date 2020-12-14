package com.sohu.smc.md.cache.cache;

import com.sohu.smc.md.cache.core.Cache;
import com.sohu.smc.md.cache.core.RedisSyncHandler;
import com.sohu.smc.md.cache.core.SyncCacheManager;
import com.sohu.smc.md.cache.core.SyncHandler;
import com.sohu.smc.md.cache.serializer.PbSerializer;
import com.sohu.smc.md.cache.serializer.ReactorSerializer;
import com.sohu.smc.md.cache.serializer.Serializer;
import io.lettuce.core.RedisURI;
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
    private final RedisURI primaryRedisURI;
    private final RedisURI secondaryRedisURI;
    private SyncHandler syncHandler;

    private RedisCacheManager primaryRedisCacheManager;
    private RedisCacheManager secondaryRedisCacheManager;

    public SyncRedisCacheManager(RedisURI redisURI, RedisURI secondaryRedisURI) {
        this(redisURI, DefaultClientResources.create(), secondaryRedisURI, DefaultClientResources.create());
    }

    public SyncRedisCacheManager(RedisURI primaryRedisURI, ClientResources primaryClientResources, RedisURI secondaryRedisURI, ClientResources secondaryClientResources) {
        Assert.notNull(primaryRedisURI,"primaryRedisURI can not be null");
        Assert.notNull(primaryClientResources,"primaryClientResources can not be null");
        Assert.notNull(secondaryRedisURI,"secondaryRedisURI can not be null");
        Assert.notNull(secondaryClientResources,"secondaryClientResources can not be null");
        this.primaryRedisURI = primaryRedisURI;
        this.primaryClientResources = primaryClientResources;
        this.secondaryRedisURI = secondaryRedisURI;
        this.secondaryClientResources = secondaryClientResources;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        primaryRedisCacheManager = new RedisCacheManager(primaryRedisURI, primaryClientResources);
        secondaryRedisCacheManager = new RedisCacheManager(secondaryRedisURI, secondaryClientResources);
        primaryRedisCacheManager.afterPropertiesSet();
        secondaryRedisCacheManager.afterPropertiesSet();
        if (syncHandler == null){
            RedisSyncHandler redisSyncHandler = new RedisSyncHandler(primaryRedisURI, primaryClientResources, secondaryRedisURI,
                    secondaryClientResources, serializer);
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
