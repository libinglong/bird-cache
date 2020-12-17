package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.cache.ObjectRedisCodec;
import com.sohu.smc.md.cache.cache.RedisCacheManager;
import com.sohu.smc.md.cache.cache.SyncOp;
import com.sohu.smc.md.cache.serializer.Serializer;
import com.sohu.smc.md.cache.util.RedisClientUtils;
import io.lettuce.core.api.reactive.BaseRedisReactiveCommands;
import io.lettuce.core.api.reactive.RedisSetReactiveCommands;
import io.lettuce.core.resource.ClientResources;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

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

    private static final Scheduler scheduler = Schedulers.newParallel("parallel-scheduler", 4);

    private RedisCacheManager secondaryRedisCacheManager;
    private RedisSetReactiveCommands<Object, Object> primaryReactive;
    private static final String ERROR_SYNC_EVENT = "ERROR_SYNC_EVENT";
    private final Serializer serializer;
    private final boolean isCluster;
    private final String primaryRedisURI;
    private final ClientResources primaryClientResources;
    private final String secondaryRedisURI;
    private final ClientResources secondaryClientResources;

    public RedisSyncHandler(String primaryRedisURI, ClientResources primaryClientResources, String secondaryRedisURI, ClientResources secondaryClientResources, Serializer serializer, boolean isCluster) {
        Assert.notNull(primaryRedisURI,"primaryRedisURI can not be null");
        Assert.notNull(primaryClientResources,"primaryClientResources can not be null");
        Assert.notNull(secondaryRedisURI,"secondaryRedisURI can not be null");
        Assert.notNull(secondaryClientResources,"secondaryClientResources can not be null");
        this.serializer = serializer;
        this.isCluster = isCluster;
        this.primaryRedisURI = primaryRedisURI;
        this.primaryClientResources = primaryClientResources;
        this.secondaryRedisURI = secondaryRedisURI;
        this.secondaryClientResources = secondaryClientResources;
    }

    @Override
    public void clearSync(String cacheSpaceName) {
        log.debug("cacheSpaceName={},clear sync", cacheSpaceName);
        secondaryRedisCacheManager.getCache(cacheSpaceName)
                .clear()
                .timeout(timeout)
                .onErrorResume(throwable -> {
                    log.debug("clear sync error,fallback to store in primary redis so that we can restore consistency in the future,error={}", throwable.getMessage());
                    SyncOp op = SyncOp.builder()
                            .cacheSpaceName(cacheSpaceName)
                            .op(Clear)
                            .build();
                    return primaryReactive.sadd(ERROR_SYNC_EVENT, op)
                            .then();
                })
                .subscribeOn(scheduler)
                .subscribe();
    }

    @Override
    public void evictSync(String cacheSpaceName, Object key) {
        log.debug("cacheSpaceName={},key={},evict sync", cacheSpaceName, key);
        secondaryRedisCacheManager.getCache(cacheSpaceName)
                .delete(key)
                .timeout(timeout)
                .onErrorResume(throwable -> {
                    log.debug("evict sync error,fallback to store in primary redis so that we can restore consistency in the future,error={}", throwable.getMessage());
                    SyncOp op = SyncOp.builder()
                            .cacheSpaceName(cacheSpaceName)
                            .op(SyncOp.Op.Evict)
                            .key(key)
                            .build();
                    return primaryReactive.sadd(ERROR_SYNC_EVENT, op)
                            .then();
                })
                .subscribeOn(scheduler)
                .subscribe();
    }

    @Override
    public void putSync(String cacheSpaceName, Object key, Object value) {
        log.debug("cacheSpaceName={},key={},value={},put sync", cacheSpaceName, key, value);
        secondaryRedisCacheManager.getCache(cacheSpaceName)
                .delete(key)
                .timeout(timeout)
                .onErrorResume(throwable -> {
                    log.debug("put sync error,fallback to store in primary redis so that we can restore consistency in the future,error={}", throwable.getMessage());
                    SyncOp op = SyncOp.builder()
                            .cacheSpaceName(cacheSpaceName)
                            .op(SyncOp.Op.Put)
                            .key(key)
                            .value(value)
                            .build();
                    return primaryReactive.sadd(ERROR_SYNC_EVENT, op)
                            .then();
                })
                .subscribeOn(scheduler)
                .subscribe();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (isCluster){
            primaryReactive = RedisClientUtils.initRedisClusterClient(primaryRedisURI, primaryClientResources, Duration.of(3000, ChronoUnit.MILLIS))
                    .connect(new ObjectRedisCodec(serializer))
                    .reactive();
        } else {
            primaryReactive = RedisClientUtils.initRedisClient(primaryRedisURI, primaryClientResources, Duration.of(3000, ChronoUnit.MILLIS))
                    .connect(new ObjectRedisCodec(serializer))
                    .reactive();
        }
        BaseRedisReactiveCommands<Object, Object> secondaryReactive;
        if (isCluster){
            secondaryReactive = RedisClientUtils.initRedisClusterClient(secondaryRedisURI, secondaryClientResources, Duration.of(4000, ChronoUnit.MILLIS))
                    .connect(new ObjectRedisCodec(serializer))
                    .reactive();
        } else {
            secondaryReactive = RedisClientUtils.initRedisClient(secondaryRedisURI, secondaryClientResources, Duration.of(4000, ChronoUnit.MILLIS))
                    .connect(new ObjectRedisCodec(serializer))
                    .reactive();
        }
        secondaryRedisCacheManager = new RedisCacheManager(secondaryRedisURI, secondaryClientResources, isCluster);
        secondaryRedisCacheManager.afterPropertiesSet();

        Flux<Long> syncOpFlux = secondaryReactive.ping()
                .thenMany(primaryReactive.srandmember(ERROR_SYNC_EVENT, count))
                .flatMap(o -> {
                    SyncOp syncOp = (SyncOp) o;
                    log.debug("sync op={}", syncOp);
                    Cache cache = secondaryRedisCacheManager.getCache(syncOp.getCacheSpaceName());
                    Mono<Void> op;
                    if (Clear.equals(syncOp.getOp())) {
                        op = cache.clear();
                    } else {
                        op = cache.delete(syncOp.getKey());
                    }
                    return op.then(primaryReactive.srem(ERROR_SYNC_EVENT, syncOp));
                })
                // return a value so that we can trigger hookOnNext method
                .onErrorResume(throwable -> {
                    log.debug("syncOpFlux error={}", throwable.getMessage());
                    return Mono.just(1L);
                });
        Flux.interval(timeInterval)
                .onBackpressureDrop()
                .flatMap(aLong -> syncOpFlux,1,1)
                .onBackpressureDrop()
                .subscribe(new BaseSubscriber<Object>() {
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        request(1);
                    }

                    @Override
                    protected void hookOnNext(Object value) {
                        request(1);
                    }

                });

    }
}
