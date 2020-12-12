package com.sohu.smc.md.cache.cache;

import com.sohu.smc.md.cache.core.Cache;
import com.sohu.smc.md.cache.core.SyncHandler;
import com.sohu.smc.md.cache.serializer.PbSerializer;
import com.sohu.smc.md.cache.serializer.ReactorSerializer;
import com.sohu.smc.md.cache.serializer.Serializer;
import com.sohu.smc.md.cache.util.RedisClientUtils;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/12
 */
public class RedisCacheManager implements IRedisCacheManager, InitializingBean {

    @Getter
    private RedisClient redisClient;
    private final RedisURI redisURI;
    private final ClientResources clientResources;
    private final Map<String,Cache> cacheMap = new ConcurrentHashMap<>();
    @Setter
    private Serializer serializer;

    public RedisCacheManager(RedisURI redisURI) {
        this(redisURI, DefaultClientResources.create());
    }

    public RedisCacheManager(RedisURI redisURI, ClientResources clientResources) {
        Assert.notNull(redisURI,"redisURI can not be null");
        Assert.notNull(clientResources,"clientResources can not be null");
        this.redisURI = redisURI;
        this.clientResources = clientResources;
    }

    @Override
    public void afterPropertiesSet() {
        redisClient = RedisClientUtils.initRedisClient(redisURI, clientResources, Duration.of(3000, ChronoUnit.MILLIS));
        if (serializer == null){
            serializer = new ReactorSerializer(new PbSerializer());
        }
    }

    @Override
    @PreDestroy
    public void shutdown(){
        if (redisClient != null){
            redisClient.shutdown();
        }
    }

    @Override
    public Cache getCache(String cacheSpaceName) {
        return cacheMap.computeIfAbsent(cacheSpaceName, cacheSpaceName1 -> {
            RedisCache redisCache = new RedisCache(cacheSpaceName1, this);
            redisCache.afterPropertiesSet();
            return redisCache;
        });
    }

    @Override
    public boolean needSync() {
        return false;
    }

    @Override
    public SyncHandler getSyncHandler() {
        return null;
    }

}
