package com.sohu.smc.md.cache.cache;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/15
 */
public class CacheSpaceImpl implements CacheSpace {

    private final RedisAsyncCommands<String, String> stringAsyncCommand;
    private final ConcurrentReferenceHashMap<String, String> versionMap = new ConcurrentReferenceHashMap<>(256);

    public CacheSpaceImpl(RedisClient redisClient){
        stringAsyncCommand = redisClient.connect(StringCodec.UTF8).async();
        StatefulRedisPubSubConnection<String, String> pubSubConnection = redisClient.connectPubSub(StringCodec.UTF8);
        pubSubConnection.addListener(new RedisPubSubAdapter<String, String>() {
            @Override
            public void message(String channel, String cacheSpaceVersionKey) {
                versionMap.remove(cacheSpaceVersionKey);
            }
        });
        RedisPubSubCommands<String, String> sync = pubSubConnection.sync();
        sync.subscribe(CACHE_SPACE_CHANGE_CHANNEL);
    }

    protected static final String CACHE_SPACE_CHANGE_CHANNEL = "CACHE_SPACE_CHANGE_CHANNEL";

    @Override
    public Mono<Void> incrVersion(String cacheSpaceVersionKey) {
        return Mono.fromCompletionStage(stringAsyncCommand.incr(cacheSpaceVersionKey))
                //保证当前jvm的实时性,立刻remove
                .doOnNext(aLong -> versionMap.remove(cacheSpaceVersionKey))
                .then(Mono.fromCompletionStage(stringAsyncCommand
                        .publish(CACHE_SPACE_CHANGE_CHANNEL, cacheSpaceVersionKey)))
                .then();
    }

    @Override
    public Mono<String> getVersion(String cacheSpaceVersionKey) {
        return Mono.justOrEmpty(versionMap.get(cacheSpaceVersionKey))
                .switchIfEmpty(doGetVersion(cacheSpaceVersionKey))
                .doOnNext(version -> versionMap.put(cacheSpaceVersionKey,version));
    }

    private Mono<String> doGetVersion(String cacheSpaceVersionKey) {
        Mono<String> versionCache = Mono.fromCompletionStage(stringAsyncCommand.get(cacheSpaceVersionKey))
                .switchIfEmpty(Mono.just("0"))
                .cache();
        return versionCache.flatMap(version -> Mono.fromCompletionStage(stringAsyncCommand.setnx(cacheSpaceVersionKey, version)))
                .then(versionCache);
    }

}
