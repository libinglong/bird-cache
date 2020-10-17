package com.sohu.smc.md.cache.cache.impl.multidc;

import com.sohu.smc.md.cache.cache.impl.simple.SingleRedisCacheManager;
import com.sohu.smc.md.cache.core.Cache;
import com.sohu.smc.md.cache.serializer.PbSerializer;
import com.sohu.smc.md.cache.serializer.Serializer;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/13
 */
public class MdRedisCacheManager implements RedisCacheManager, InitializingBean {

    private SingleRedisCacheManager secondaryCacheManager;
    private SingleRedisCacheManager primaryCacheManager;
    @Setter
    private ExecutorService executorService;
    @Setter
    private ErrorHandler errorHandler;
    @Getter
    @Setter
    private Serializer serializer;

    private Map<String,Cache> cacheMap = new ConcurrentHashMap<>();
    private RedisURI primaryRedisURI;
    private RedisURI secondaryRedisURI;
    private ClientResources primaryClientResources;
    private ClientResources secondaryClientResources;

    public MdRedisCacheManager(RedisURI primaryRedisURI, RedisURI secondaryRedisURI) {
        this(primaryRedisURI, DefaultClientResources.create(), secondaryRedisURI, DefaultClientResources.create());
    }

    public MdRedisCacheManager(RedisURI primaryRedisURI, ClientResources primaryClientResources,
                               RedisURI secondaryRedisURI, ClientResources secondaryClientResources) {
        Assert.notNull(primaryRedisURI,"primaryRedisURI can not be null");
        Assert.notNull(secondaryRedisURI,"secondaryRedisURI can not be null");
        Assert.notNull(primaryClientResources,"primaryClientResources can not be null");
        Assert.notNull(secondaryClientResources,"secondaryClientResources can not be null");
        this.primaryRedisURI = primaryRedisURI;
        this.secondaryRedisURI = secondaryRedisURI;
        this.primaryClientResources = primaryClientResources;
        this.secondaryClientResources = secondaryClientResources;
        this.primaryCacheManager = new SingleRedisCacheManager(primaryRedisURI, primaryClientResources);
        this.secondaryCacheManager = new SingleRedisCacheManager(secondaryRedisURI, secondaryClientResources);
    }

    @Override
    public Cache getCache(String cacheSpaceName) {
        Cache primaryCache = primaryCacheManager.getCache(cacheSpaceName);
        Cache secondaryCache = secondaryCacheManager.getCache(cacheSpaceName);
        return cacheMap.computeIfAbsent(cacheSpaceName, cacheSpaceName1 -> new MdRedisCache(primaryCache, secondaryCache,
                executorService, errorHandler));
    }


    @Override
    @PreDestroy
    public void shutdown(){
        primaryCacheManager.shutdown();
        secondaryCacheManager.shutdown();
    }

    @Override
    public void afterPropertiesSet() {
        if (serializer == null){
            serializer = new PbSerializer();
        }
        primaryCacheManager.setSerializer(serializer);
        secondaryCacheManager.setSerializer(serializer);
        primaryCacheManager.afterPropertiesSet();
        secondaryCacheManager.afterPropertiesSet();
        if (executorService == null){
            executorService = Executors.newCachedThreadPool();
        }
        if (errorHandler == null){
            errorHandler = new DefaultErrorHandler(primaryRedisURI, primaryClientResources,
                    secondaryRedisURI, secondaryClientResources, serializer);
        }
    }
}
