package com.sohu.smc.md.cache.cache.impl.multidc;

import com.sohu.smc.md.cache.cache.impl.simple.SingleRedisCacheManage;
import com.sohu.smc.md.cache.core.Cache;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.StringCodec;
import org.springframework.util.Assert;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/13
 */
public class MdRedisCacheManage extends SingleRedisCacheManage {

    private RedisClient secondaryRedisClient;
    private RedisAsyncCommands<String, String> secondaryAsyncCommand;

    public MdRedisCacheManage(RedisURI redisURI, RedisURI secondaryRedisURI) {
        super(redisURI);
        this.secondaryRedisClient = RedisClient.create(secondaryRedisURI);
    }

    public MdRedisCacheManage(RedisClient redisClient, RedisClient secondaryRedisClient) {
        super(redisClient);
        this.secondaryRedisClient = secondaryRedisClient;
    }

    @Override
    public void incrVersion(String cacheSpaceVersionKey) {
        super.incrVersion(cacheSpaceVersionKey);
        secondaryAsyncCommand.incr(cacheSpaceVersionKey);
        secondaryAsyncCommand.publish(CACHE_SPACE_CHANGE_CHANNEL, cacheSpaceVersionKey);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Assert.notNull(secondaryRedisClient,"secondaryRedisClient can not be null");
        secondaryAsyncCommand = secondaryRedisClient.connect(StringCodec.UTF8)
                .async();
    }

    @Override
    public Cache getCache(String cacheSpaceName) {
        return cacheMap.computeIfAbsent(cacheSpaceName, cacheSpaceName1 ->
                applicationContext.getBean(MdRedisCache.class,cacheSpaceName1, redisClient, secondaryRedisClient, this, serializer));
    }
}
