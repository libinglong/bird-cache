package com.sohu.smc.md.cache.cache.impl.simple;

import com.sohu.smc.md.cache.cache.impl.CacheSpace;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/15
 */
public class SingleCacheSpace implements CacheSpace {

    private RedisCommands<String, String> stringSyncCommand;
    private ConcurrentReferenceHashMap<String, String> versionMap = new ConcurrentReferenceHashMap<>(256);

    public SingleCacheSpace(RedisClient redisClient){
        stringSyncCommand = redisClient.connect(StringCodec.UTF8).sync();
        StatefulRedisPubSubConnection<String, String> pubSubConnection = redisClient.connectPubSub(StringCodec.UTF8);
        pubSubConnection.addListener(new RedisPubSubAdapter<String, String>() {
            @Override
            public void message(String channel, String cacheSpaceName) {
                versionMap.remove(cacheSpaceName);
            }
        });
        RedisPubSubCommands<String, String> sync = pubSubConnection.sync();
        sync.subscribe(CACHE_SPACE_CHANGE_CHANNEL);
    }

    protected static final String CACHE_SPACE_CHANGE_CHANNEL = "CACHE_SPACE_CHANGE_CHANNEL";

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

}
