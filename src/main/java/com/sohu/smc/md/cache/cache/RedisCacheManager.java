package com.sohu.smc.md.cache.cache;

import com.sohu.smc.md.cache.core.Cache;
import com.sohu.smc.md.cache.core.SyncHandler;
import com.sohu.smc.md.cache.serializer.PbSerializer;
import com.sohu.smc.md.cache.serializer.Serializer;
import com.sohu.smc.md.cache.util.RedisClientUtils;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
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

    private CacheSpace cacheSpace;
    private RedisClient redisClient;
    private RedisReactiveCommands<Object, Object> reactive;
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
            serializer = new PbSerializer();
        }
        reactive = redisClient.connect(new ObjectRedisCodec(serializer))
                .reactive();
        cacheSpace = new CacheSpaceImpl(redisClient);
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
        return cacheMap.computeIfAbsent(cacheSpaceName, cacheSpaceName1 ->
                new RedisCache(cacheSpaceName1, reactive, cacheSpace, serializer));
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
