package com.sohu.smc.md.cache.cache.impl.multidc;

import com.sohu.smc.md.cache.cache.impl.simple.ObjectRedisCodec;
import com.sohu.smc.md.cache.cache.impl.simple.SingleRedisCacheManager;
import com.sohu.smc.md.cache.serializer.Serializer;
import com.sohu.smc.md.cache.util.RedisClientUtils;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.resource.ClientResources;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/16
 */
@Slf4j
class DefaultErrorHandler implements ErrorHandler {

    private RedisCommands<Object, Object> primaryCommand;
    private RedisCommands<Object, Object> secondCommand;
    private SingleRedisCacheManager secondaryRedisManager;
    private static String ERROR_CACHE_EVENT_SET = "ERROR_CACHE_EVENT_SET";

    public DefaultErrorHandler(RedisURI primaryRedisURI, ClientResources primaryClientResources,
                               RedisURI secondaryRedisURI, ClientResources secondaryClientResources, Serializer serializer) {
        Assert.notNull(primaryRedisURI, "the primaryRedisURI can not be null");
        Assert.notNull(primaryClientResources, "the primaryClientResources can not be null");
        Assert.notNull(secondaryRedisURI, "the secondaryRedisURI can not be null");
        Assert.notNull(secondaryClientResources, "the secondaryClientResources can not be null");
        Assert.notNull(serializer, "the serializer can not be null");
        primaryCommand = RedisClientUtils.initRedisClient(primaryRedisURI, primaryClientResources)
                .connect(new ObjectRedisCodec(serializer))
                .sync();
        secondCommand = RedisClientUtils.initRedisClient(secondaryRedisURI, secondaryClientResources)
                .connect(new ObjectRedisCodec(serializer))
                .sync();
        secondaryRedisManager = new SingleRedisCacheManager(secondaryRedisURI, secondaryClientResources);
        secondaryRedisManager.setSerializer(serializer);
        secondaryRedisManager.afterPropertiesSet();
        Executors.newScheduledThreadPool(1)
                .scheduleWithFixedDelay(new ErrorRunnable(),0,3, TimeUnit.SECONDS);
    }

    @Override
    public void handle(ErrorCache errorCache) {
        primaryCommand.sadd(ERROR_CACHE_EVENT_SET, errorCache);
    }

    private class ErrorRunnable implements Runnable {

        @Override
        public void run() {
            try {
                if ("PONG".equals(secondCommand.ping())){
                    while (true){
                        ErrorCache errorCache = (ErrorCache) primaryCommand.srandmember(ERROR_CACHE_EVENT_SET);
                        if (errorCache == null){
                            break;
                        }
                        secondaryRedisManager.getCache(errorCache.cacheSpaceName)
                                .delete(errorCache.key);
                        primaryCommand.srem(ERROR_CACHE_EVENT_SET, errorCache);
                    }
                }
            } catch (Exception e){
                //ignore
            }

        }
    }


}
