package com.sohu.smc.md.cache.cache;

import com.sohu.smc.md.cache.core.CacheManager;
import com.sohu.smc.md.cache.serializer.PbSerializer;
import com.sohu.smc.md.cache.serializer.ReactorSerializer;
import com.sohu.smc.md.cache.serializer.Serializer;
import com.sohu.smc.md.cache.util.RedisClientUtils;
import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/12
 */
public class RedisCacheManager implements Closeable, CacheManager, InitializingBean {

    @Getter
    private RedisClient redisClient;
    @Getter
    private RedisClusterClient redisClusterClient;
    private final String redisURI;
    private final ClientResources clientResources;
    private final Map<String,RedisCache> cacheMap = new ConcurrentHashMap<>();
    @Setter
    @Getter
    private Serializer serializer;
    @Getter
    private final boolean isCluster;
    private final Duration commandTimeout = Duration.of(3000, ChronoUnit.MILLIS);

    public RedisCacheManager(String redisURI, boolean isCluster) {
        this(redisURI, DefaultClientResources.create(), isCluster);
    }

    public RedisCacheManager(String redisURI, ClientResources clientResources, boolean isCluster) {
        Assert.hasText(redisURI,"redisURI can not be empty");
        Assert.notNull(clientResources,"clientResources can not be null");
        this.redisURI = redisURI;
        this.clientResources = clientResources;
        this.isCluster = isCluster;
    }

    @Override
    public void afterPropertiesSet() {
        if (isCluster){
            redisClusterClient = RedisClientUtils.initRedisClusterClient(redisURI, clientResources, commandTimeout);
        } else {
            redisClient = RedisClientUtils.initRedisClient(redisURI, clientResources, commandTimeout);
        }
        if (serializer == null){
            serializer = new ReactorSerializer(new PbSerializer());
        }
    }

    @Override
    @PreDestroy
    public void close(){
        if (redisClient != null){
            redisClient.shutdown();
        }
    }

    @Override
    public RedisCache getCache(String cacheSpaceName) {
        return cacheMap.computeIfAbsent(cacheSpaceName, cacheSpaceName1 -> {
            RedisCache redisCache = new RedisCache(cacheSpaceName1, this);
            redisCache.afterPropertiesSet();
            return redisCache;
        });
    }

}
