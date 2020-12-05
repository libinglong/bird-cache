package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.cache.ObjectRedisCodec;
import com.sohu.smc.md.cache.cache.RedisCacheManager;
import com.sohu.smc.md.cache.cache.SyncOp;
import com.sohu.smc.md.cache.serializer.Serializer;
import com.sohu.smc.md.cache.util.RedisClientUtils;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.resource.ClientResources;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static com.sohu.smc.md.cache.cache.SyncOp.Op.Clear;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/27
 */
@Slf4j
public class RedisSyncHandler implements SyncHandler, InitializingBean {

    private final Duration timeInterval = Duration.of(200, ChronoUnit.MILLIS);
    //make timeout a little less than timeInterval
    private final Duration timeout = Duration.of(180, ChronoUnit.MILLIS);
    private static final int count = 1000;

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
        if (log.isDebugEnabled()){
            log.debug("cacheSpaceName={},clear sync", cacheSpaceName);
        }
        return secondaryRedisCacheManager.getCache(cacheSpaceName)
                .clear()
                .timeout(timeout)
                .onErrorResume(throwable -> {
                    log.error("error", throwable);
                    SyncOp op = SyncOp.builder()
                            .cacheSpaceName(cacheSpaceName)
                            .op(Clear)
                            .build();
                    return primaryReactive.sadd(ERROR_SYNC_EVENT, op)
                            .then();
                });
    }

    @Override
    public Mono<Void> evictSync(String cacheSpaceName, Object key) {
        if (log.isDebugEnabled()){
            log.debug("cacheSpaceName={},key={},evict sync", cacheSpaceName, key);
        }
        return secondaryRedisCacheManager.getCache(cacheSpaceName)
                .delete(key)
                .timeout(timeout)
                .onErrorResume(throwable -> {
                    log.error("error", throwable);
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
        if (log.isDebugEnabled()){
            log.debug("cacheSpaceName={},key={},value={},put sync", cacheSpaceName, key, value);
        }
        return secondaryRedisCacheManager.getCache(cacheSpaceName)
                .delete(key)
                .timeout(timeout)
                .onErrorResume(throwable -> {
                    log.error("error", throwable);
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
        Mono<Void> syncOpFlux = secondaryReactive.ping()
                .thenMany(primaryReactive.srandmember(ERROR_SYNC_EVENT, count))
                .flatMap(o -> {
                    SyncOp syncOp = (SyncOp) o;
                    Cache cache = secondaryRedisCacheManager.getCache(syncOp.getCacheSpaceName());
                    if (Clear.equals(syncOp.getOp())) {
                        return cache.clear();
                    }
                    return cache.delete(syncOp.getKey())
                            .then(primaryReactive.srem(ERROR_SYNC_EVENT, syncOp))
                            .timeout(timeout);
                })
                .onErrorResume(throwable -> Mono.empty())
                .then();
        Flux.interval(timeInterval)
                .onBackpressureDrop()
                .flatMap(aLong -> syncOpFlux,1,1)
                .subscribe(new BaseSubscriber<Void>() {
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        request(1);
                    }

                    @Override
                    protected void hookOnNext(Void value) {
                        request(1);
                    }

                });

    }
}
