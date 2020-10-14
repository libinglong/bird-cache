package com.sohu.smc.md.cache.cache.impl.multidc;

import com.sohu.smc.md.cache.cache.impl.simple.SingleRedisCacheManage;
import com.sohu.smc.md.cache.core.Cache;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.resource.ClientResources;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/13
 */
@Slf4j
public class MdRedisCacheManage extends SingleRedisCacheManage implements ApplicationContextAware {

    @Setter
    private AsyncTaskExecutor mdCacheExecutor;

    private RedisClient secondaryRedisClient;

    private RedisURI secondaryRedisURI;
    private ClientResources secondaryClientResources;
    private RedisCommands<String, String> secondaryCommand;

    public MdRedisCacheManage(RedisURI redisURI, RedisURI secondaryRedisURI) {
        super(redisURI);
        this.secondaryRedisURI = secondaryRedisURI;
    }

    public MdRedisCacheManage(RedisURI redisURI, ClientResources clientResources,
                              RedisURI secondaryRedisURI, ClientResources secondaryClientResources) {
        super(redisURI,clientResources);
        this.secondaryRedisURI = secondaryRedisURI;
        this.secondaryClientResources = secondaryClientResources;
    }

    @Override
    public void incrVersion(String cacheSpaceVersionKey) {
        super.incrVersion(cacheSpaceVersionKey);
        mdCacheExecutor.submit(() -> {
            try{
                secondaryCommand.incr(cacheSpaceVersionKey);
                secondaryCommand.publish(CACHE_SPACE_CHANGE_CHANNEL, cacheSpaceVersionKey);
            } catch (Exception e){
                log.info("cacheSpaceVersionKey={}",cacheSpaceVersionKey);
                log.debug("err",e);
            }
        });


    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        initExecutor();
        secondaryRedisClient = initRedisClient(secondaryRedisURI,secondaryClientResources);
        Assert.notNull(secondaryRedisClient,"secondaryRedisClient can not be null");
        secondaryCommand = secondaryRedisClient.connect(StringCodec.UTF8)
                .sync();
    }


    private void initExecutor(){
        if (mdCacheExecutor == null){
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(5);
            executor.setMaxPoolSize(20);
            executor.setQueueCapacity(1024);
            executor.afterPropertiesSet();
            mdCacheExecutor = executor;
        }
    }

    @Override
    public Cache getCache(String cacheSpaceName) {
        return cacheMap.computeIfAbsent(cacheSpaceName, cacheSpaceName1 ->
                applicationContext.getBean(MdRedisCache.class,cacheSpaceName1, redisClient, secondaryRedisClient,
                        this, serializer, mdCacheExecutor));
    }

    @Override
    @PreDestroy
    public void shutdown(){
        if (redisClient != null){
            redisClient.shutdown();
        }
        if (secondaryRedisClient != null){
            secondaryRedisClient.shutdown();
        }
    }
}
