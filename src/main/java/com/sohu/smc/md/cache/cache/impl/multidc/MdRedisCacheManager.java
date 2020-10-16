package com.sohu.smc.md.cache.cache.impl.multidc;

import com.sohu.smc.md.cache.cache.impl.simple.SingleRedisCacheManager;
import com.sohu.smc.md.cache.core.Cache;
import com.sohu.smc.md.cache.serializer.PbSerializer;
import com.sohu.smc.md.cache.serializer.Serializer;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;

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

    public MdRedisCacheManager(RedisURI primaryRedisURI, RedisURI secondaryRedisURI) {
        primaryCacheManager = new SingleRedisCacheManager(primaryRedisURI);
        secondaryCacheManager = new SingleRedisCacheManager(secondaryRedisURI);
    }

    public MdRedisCacheManager(RedisURI primaryRedisURI, ClientResources primaryClientResources,
                               RedisURI secondaryRedisURI, ClientResources secondaryClientResources) {
        primaryCacheManager = new SingleRedisCacheManager(primaryRedisURI, primaryClientResources);
        secondaryCacheManager = new SingleRedisCacheManager(secondaryRedisURI, secondaryClientResources);
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
            errorHandler = new DefaultErrorHandler(serializer);
        }
    }
}
