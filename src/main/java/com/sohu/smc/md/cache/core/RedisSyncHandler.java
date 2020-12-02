package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.cache.ObjectRedisCodec;
import com.sohu.smc.md.cache.cache.RedisCacheManager;
import com.sohu.smc.md.cache.cache.SyncOp;
import com.sohu.smc.md.cache.serializer.Serializer;
import com.sohu.smc.md.cache.util.RedisClientUtils;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.resource.ClientResources;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/27
 */
public class RedisSyncHandler implements SyncHandler, InitializingBean {

    private final RedisCacheManager secondaryRedisCacheManager;
    private final RedisReactiveCommands<Object, Object> primaryReactive;
    private final RedisReactiveCommands<Object, Object> secondaryReactive;
    private static final String ERROR_SYNC_EVENT = "ERROR_SYNC_EVENT";

    public RedisSyncHandler(RedisURI primaryRedisURI, ClientResources primaryClientResources, RedisURI secondaryRedisURI, ClientResources secondaryClientResources, Serializer serializer) {
        Assert.notNull(primaryRedisURI,"primaryRedisURI can not be null");
        Assert.notNull(primaryClientResources,"primaryClientResources can not be null");
        Assert.notNull(secondaryRedisURI,"secondaryRedisURI can not be null");
        Assert.notNull(secondaryClientResources,"secondaryClientResources can not be null");
        primaryReactive = RedisClientUtils.initRedisClient(primaryRedisURI, primaryClientResources)
                .connect(new ObjectRedisCodec(serializer))
                .reactive();
        secondaryReactive = RedisClientUtils.initRedisClient(secondaryRedisURI, secondaryClientResources)
                .connect(new ObjectRedisCodec(serializer))
                .reactive();
        this.secondaryRedisCacheManager = new RedisCacheManager(secondaryRedisURI, secondaryClientResources);
    }

    @Override
    public Mono<Void> clearSync(String cacheSpaceName) {
        return secondaryRedisCacheManager.getCache(cacheSpaceName)
                .clear()
                .onErrorResume(throwable -> {
                    SyncOp op = SyncOp.builder()
                            .cacheSpaceName(cacheSpaceName)
                            .op(SyncOp.Op.Clear)
                            .build();
                    return primaryReactive.sadd(ERROR_SYNC_EVENT, op)
                            .then();
                });
    }

    @Override
    public Mono<Void> evictSync(String cacheSpaceName, Object key) {
        return secondaryRedisCacheManager.getCache(cacheSpaceName)
                .delete(key)
                .onErrorResume(throwable -> {
                    SyncOp op = SyncOp.builder()
                            .cacheSpaceName(cacheSpaceName)
                            .op(SyncOp.Op.Evict)
                            .key(key)
                            .build();
                    return primaryReactive.sadd(ERROR_SYNC_EVENT, op)
                            .then();
                });
    }

    @Override
    public Mono<Void> putSync(String cacheSpaceName, Object key, Object value) {
        return secondaryRedisCacheManager.getCache(cacheSpaceName)
                .delete(key)
                .onErrorResume(throwable -> {
                    SyncOp op = SyncOp.builder()
                            .cacheSpaceName(cacheSpaceName)
                            .op(SyncOp.Op.Put)
                            .key(key)
                            .value(value)
                            .build();
                    return primaryReactive.sadd(ERROR_SYNC_EVENT, op)
                            .then();
                });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        secondaryRedisCacheManager.afterPropertiesSet();
        Flux.interval(Duration.of(200, ChronoUnit.MILLIS))
                .flatMap(aLong -> secondaryReactive.ping())
                .then(primaryReactive.srandmember(ERROR_SYNC_EVENT))
                .flatMap(o -> {
                    SyncOp syncOp = (SyncOp) o;
                    return secondaryRedisCacheManager.getCache(syncOp.getCacheSpaceName()).delete(syncOp.getKey())
                            .then(primaryReactive.srem(ERROR_SYNC_EVENT, syncOp));
                })
                .subscribe();
    }
}
