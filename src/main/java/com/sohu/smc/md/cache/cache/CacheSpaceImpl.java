package com.sohu.smc.md.cache.cache;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ConcurrentReferenceHashMap;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/15
 */
public class CacheSpaceImpl implements CacheSpace, InitializingBean {

    private RedisReactiveCommands<String, String> reactive;
    private final ConcurrentReferenceHashMap<String, String> versionMap = new ConcurrentReferenceHashMap<>(256);
    private final RedisCacheManager redisCacheManager;
    private final RedisCache redisCache;

    public CacheSpaceImpl(RedisCacheManager redisCacheManager, RedisCache redisCache){
        this.redisCacheManager = redisCacheManager;
        this.redisCache = redisCache;
    }

    protected static final String CACHE_SPACE_CHANGE_CHANNEL = "CACHE_SPACE_CHANGE_CHANNEL";

    @Override
    public Mono<Void> incrVersion() {
        return reactive.incr(getCacheSpaceVersionKey())
                //保证当前jvm的实时性,立刻remove
                .doOnNext(aLong -> versionMap.remove(getCacheSpaceVersionKey()))
                .then(reactive.publish(CACHE_SPACE_CHANGE_CHANNEL, getCacheSpaceVersionKey()))
                .then();
    }

    @Override
    public Mono<String> getVersion() {
        return Mono.justOrEmpty(versionMap.get(getCacheSpaceVersionKey()))
                .switchIfEmpty(doGetVersion())
                .doOnNext(version -> versionMap.put(getCacheSpaceVersionKey(),version));
    }

    private String getCacheSpaceVersionKey() {
        return "v:" + redisCache.getCacheSpaceName();
    }

    private Mono<String> doGetVersion() {
        AtomicBoolean needInitCacheSpaceVersionKey = new AtomicBoolean(true);
        Mono<String> versionCache = reactive.get(getCacheSpaceVersionKey())
                .doOnNext(s -> needInitCacheSpaceVersionKey.set(false))
                .switchIfEmpty(Mono.just("0"))
                .cache();
        return versionCache
                .filter(s -> needInitCacheSpaceVersionKey.get())
                .flatMap(version -> reactive.setnx(getCacheSpaceVersionKey(), version))
                .then(versionCache);
    }

    @Override
    public void afterPropertiesSet() {
        RedisClient redisClient = redisCacheManager.getRedisClient();
        reactive = redisClient.connect(StringCodec.UTF8)
                .reactive();
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
}
