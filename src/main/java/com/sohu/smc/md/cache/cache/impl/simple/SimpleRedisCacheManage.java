package com.sohu.smc.md.cache.cache.impl.simple;

import com.sohu.smc.md.cache.core.Cache;
import com.sohu.smc.md.cache.core.CacheManage;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/12
 */
public class SimpleRedisCacheManage implements CacheManage, CacheSpace, InitializingBean, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private static final String CACHE_SPACE_CHANGE_CHANNEL = "CACHE_SPACE_CHANGE_CHANNEL";

    private RedisCommands<String, String> stringSyncCommand;
    private RedisClient redisClient;

    public SimpleRedisCacheManage(RedisURI redisURI) {
        redisClient = RedisClient.create(redisURI);
    }

    public SimpleRedisCacheManage(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    private ConcurrentReferenceHashMap<String, String> versionMap = new ConcurrentReferenceHashMap<>(256);

    @Override
    public void incrVersion(String cacheSpaceVersionKey) {
        stringSyncCommand.incr(cacheSpaceVersionKey);
        stringSyncCommand.publish(CACHE_SPACE_CHANGE_CHANNEL, cacheSpaceVersionKey);
    }

    @Override
    public String getVersion(String cacheSpaceVersionKey) {
        return versionMap.computeIfAbsent(cacheSpaceVersionKey, this::doGetVersion);
    }

    private String doGetVersion(String cacheSpaceVersionKey) {
        String version = stringSyncCommand.get(cacheSpaceVersionKey);
        if (!StringUtils.isEmpty(version)) {
            return version;
        }
        version = "0";
        Boolean suc = stringSyncCommand.setnx(cacheSpaceVersionKey, version);
        if (suc){
            return version;
        }
        return stringSyncCommand.get(cacheSpaceVersionKey);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(redisClient,"redisClient can not be null");
        stringSyncCommand = redisClient.connect(StringCodec.UTF8)
                .sync();
        StatefulRedisPubSubConnection<String, String> pubSubConnection = redisClient.connectPubSub(StringCodec.UTF8);
        pubSubConnection.addListener(new RedisPubSubAdapter<String, String>() {
            @Override
            public void message(String channel, String cacheSpaceName) {
                Assert.isTrue(CACHE_SPACE_CHANGE_CHANNEL.equals(channel),"unexpected error,channel=" + channel);
                versionMap.remove(cacheSpaceName);
            }

            @Override
            public void subscribed(String channel, long count) {
                Assert.isTrue(CACHE_SPACE_CHANGE_CHANNEL.equals(channel) && count == 1,
                        "unexpected error,channel=" + channel + ",count=" + count);
            }
        });
        RedisPubSubCommands<String, String> sync = pubSubConnection.sync();
        sync.subscribe(CACHE_SPACE_CHANGE_CHANNEL);
    }

    private Map<String,Cache> cacheMap = new ConcurrentHashMap<>();

    @Override
    public Cache getCache(String cacheSpaceName) {
        return cacheMap.computeIfAbsent(cacheSpaceName, cacheSpaceName1 ->
                applicationContext.getBean(SimpleRedisCache.class,cacheSpaceName1, redisClient, this));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
