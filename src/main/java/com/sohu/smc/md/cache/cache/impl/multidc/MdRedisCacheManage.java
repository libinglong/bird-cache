package com.sohu.smc.md.cache.cache.impl.multidc;

import com.sohu.smc.md.cache.cache.impl.simple.SingleRedisCacheManage;
import com.sohu.smc.md.cache.core.Cache;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import lombok.Setter;

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
public class MdRedisCacheManage extends SingleRedisCacheManage {

    private SingleRedisCacheManage secondaryCacheManage;
    @Setter
    private ExecutorService executorService;

    private Map<String,Cache> cacheMap = new ConcurrentHashMap<>();

    public MdRedisCacheManage(RedisURI primaryRedisURI, RedisURI secondaryRedisURI) {
        super(primaryRedisURI);
        secondaryCacheManage = new SingleRedisCacheManage(secondaryRedisURI);
    }

    public MdRedisCacheManage(RedisURI primaryRedisURI, ClientResources primaryClientResources,
                              RedisURI secondaryRedisURI, ClientResources secondaryClientResources) {
        super(primaryRedisURI, primaryClientResources);
        secondaryCacheManage = new SingleRedisCacheManage(secondaryRedisURI, secondaryClientResources);
    }

    @Override
    public Cache getCache(String cacheSpaceName) {
        Cache primaryCache = super.getCache(cacheSpaceName);
        Cache secondaryCache = secondaryCacheManage.getCache(cacheSpaceName);
        return cacheMap.computeIfAbsent(cacheSpaceName, cacheSpaceName1 -> new MdRedisCache(primaryCache, secondaryCache,
                serializer, executorService));
    }

    @Override
    @PreDestroy
    public void shutdown(){
        super.shutdown();
        secondaryCacheManage.shutdown();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        secondaryCacheManage.afterPropertiesSet();
        if (executorService == null){
            executorService = Executors.newCachedThreadPool();
        }
    }
}
