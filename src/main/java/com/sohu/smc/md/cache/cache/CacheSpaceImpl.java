package com.sohu.smc.md.cache.cache;

import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
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

    private RedisClusterReactiveCommands<Object, Object> reactive;
    private final ConcurrentReferenceHashMap<Object, Object> versionMap = new ConcurrentReferenceHashMap<>(256);
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
    public Mono<Object> getVersion() {
        return Mono.justOrEmpty(versionMap.get(getCacheSpaceVersionKey()))
                .switchIfEmpty(doGetVersion())
                .doOnNext(version -> versionMap.put(getCacheSpaceVersionKey(),version));
    }

    private String getCacheSpaceVersionKey() {
        return "v:" + redisCache.getCacheSpaceName();
    }

    private Mono<Object> doGetVersion() {
        AtomicBoolean needInitCacheSpaceVersionKey = new AtomicBoolean(true);
        Mono<Object> versionCache = reactive.get(getCacheSpaceVersionKey())
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
        StatefulRedisPubSubConnection<Object, Object> pubSubConnection;
        if (redisCacheManager.isCluster()){
            RedisClusterClient redisClusterClient = redisCacheManager.getRedisClusterClient();
            reactive = redisClusterClient
                    .connect(new ObjectRedisCodec(redisCacheManager.getSerializer()))
                    .reactive();
            pubSubConnection = redisClusterClient.connectPubSub(new ObjectRedisCodec(redisCacheManager.getSerializer()));
        } else {
            RedisClient redisClient = redisCacheManager.getRedisClient();
            reactive = redisClient
                    .connect(new ObjectRedisCodec(redisCacheManager.getSerializer()))
                    .reactive();
            pubSubConnection = redisClient.connectPubSub(new ObjectRedisCodec(redisCacheManager.getSerializer()));
        }
        pubSubConnection.addListener(new RedisPubSubAdapter<Object, Object>() {
            @Override
            public void message(Object channel, Object message) {
                versionMap.remove(message);
            }
        });
        RedisPubSubCommands<Object, Object> sync = pubSubConnection.sync();
        sync.subscribe(CACHE_SPACE_CHANGE_CHANNEL);
    }
}
