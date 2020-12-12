package com.sohu.smc.md.cache.cache;

import com.sohu.smc.md.cache.core.Cache;
import com.sohu.smc.md.cache.core.NullValue;
import com.sohu.smc.md.cache.serializer.Serializer;
import io.lettuce.core.KeyValue;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/10
 */
public class RedisCache implements Cache {

    private final CacheSpace cacheSpace;
    private final String cacheSpaceName;
    private final String cacheSpaceVersionKey;

    private final RedisReactiveCommands<Object, Object> reactive;
    protected Serializer serializer;

    public RedisCache(String cacheSpaceName, RedisReactiveCommands<Object, Object> reactive, CacheSpace cacheSpace, Serializer serializer) {
        this.cacheSpaceName = cacheSpaceName;
        this.cacheSpace = cacheSpace;
        this.serializer = serializer;
        this.reactive = reactive;
        cacheSpaceVersionKey = "v:" + cacheSpaceName;
    }

    @Override
    public String getCacheSpaceName() {
        return cacheSpaceName;
    }

    @Override
    public Mono<Void> expire(Object key, long milliseconds) {
        return processSpace(key)
                .flatMap(o -> reactive.pexpire(o, milliseconds))
                .then();
    }

    @Override
    public Mono<Void> delete(Object key) {
        return processSpace(key)
                .flatMap(reactive::del)
                .then();
    }

    @Override
    public Mono<Void> set(Object key, Object val, long time) {
        return processSpace(key)
                .flatMap(o -> reactive.psetex(o, time, val))
                .then();
    }

    @Override
    public Mono<Void> setKvs(Map<Object, Object> kvs, long time) {
        return Flux.fromIterable(kvs.entrySet())
                .flatMap(entry -> set(entry.getKey(), entry.getValue(),time))
                .then();
    }

    @Override
    public Mono<Object> get(Object key) {
        return processSpace(key)
                .flatMap(reactive::get);
    }

    @Override
    public Flux<Object> get(List<Object> key) {
        return Flux.fromIterable(key)
                .flatMap(this::processSpace)
                .collectList()
                .map(List::toArray)
                .flatMapMany(reactive::mget)
                .map(this::processNullValue);
    }

    @Override
    public Mono<Void> clear() {
        return cacheSpace.incrVersion(cacheSpaceVersionKey);
    }

    @Override
    public Mono<Long> pttl(Object key){
        return processSpace(key)
                .flatMap(reactive::pttl);
    }

    private Mono<Object> processSpace(Object key) {
        return cacheSpace.getVersion(cacheSpaceVersionKey)
                .map(version -> new SpaceWrapper(cacheSpaceName + ":" + version, key));
    }


    private Object processNullValue(KeyValue<?,?> keyValue){
        if (keyValue.hasValue()){
            return keyValue.getValue();
        }
        return NullValue.MISS_NULL;
    }

}
