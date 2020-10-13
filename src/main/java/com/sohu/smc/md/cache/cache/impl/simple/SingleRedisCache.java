package com.sohu.smc.md.cache.cache.impl.simple;

import com.sohu.smc.md.cache.cache.impl.CacheSpace;
import com.sohu.smc.md.cache.core.Cache;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/10
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SingleRedisCache implements Cache, InitializingBean {

    private CacheSpace cacheSpace;
    private String cacheSpaceName;
    private String cacheSpaceVersionKey;
    private RedisClient redisClient;
    RedisCommands<Object, Object> syncCommand;
    RedisAsyncCommands<Object, Object> asyncCommand;

    public SingleRedisCache(String cacheSpaceName, RedisClient redisClient, CacheSpace cacheSpace) {
        this.cacheSpaceName = cacheSpaceName;
        this.redisClient = redisClient;
        this.cacheSpace = cacheSpace;
    }

    @Override
    public void expire(Object key, long milliseconds) {
        Object keyWithSpace = processSpace(key);
        doExpire(keyWithSpace, milliseconds);
    }

    @Override
    public void delete(Object key) {
        Object keyWithSpace = processSpace(key);
        doDelete(keyWithSpace);
    }

    @Override
    public void set(Object key, Object val, long time) {
        Object keyWithSpace = processSpace(key);
        doSet(keyWithSpace,val,time);
    }

    @Override
    public void set(Map<Object, Object> kvs, long time) throws ExecutionException, InterruptedException {
        Map<Object, Object> kvsWithSpace = kvs.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> processSpace(entry.getKey()), Map.Entry::getValue));
        doSet(kvsWithSpace, time);
    }

    @Override
    public Object get(Object key) {
        Object keyWithSpace = processSpace(key);
        return doGet(keyWithSpace);
    }

    @Override
    public List<Object> get(List<Object> keys) throws ExecutionException, InterruptedException {
        List<Object> keysWithSpace = keys.stream()
                .map(this::processSpace)
                .collect(Collectors.toList());
        return doGet(keysWithSpace);
    }

    @Override
    public void clear() {
        cacheSpace.incrVersion(cacheSpaceVersionKey);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        StatefulRedisConnection<Object, Object> connect = redisClient.connect(PbObjectRedisCodec.INSTANCE);
        syncCommand = connect.sync();
        asyncCommand = connect.async();
        cacheSpaceVersionKey = "v:" + cacheSpaceName;
    }

    protected Object processSpace(Object key) {
        return new SpaceWrapper(cacheSpace.getVersion(cacheSpaceVersionKey), key);
    }

    protected void doExpire(Object keyWithSpace, long milliseconds) {
        syncCommand.pexpire(keyWithSpace, milliseconds);
    }

    protected void doDelete(Object keyWithSpace) {
        syncCommand.del(keyWithSpace);
    }

    protected void doSet(Object keyWithSpace, Object val, long time) {
        syncCommand.psetex(keyWithSpace, time, val);
    }

    protected void doSet(Map<Object, Object> kvsWithSpace, long time) throws ExecutionException, InterruptedException {
        List<RedisFuture<String>> futures = new ArrayList<>();
        kvsWithSpace.forEach((o1, o2) -> futures.add(asyncCommand.psetex(o1, time, o2)));
        for (RedisFuture<String> stringRedisFuture : futures) {
            stringRedisFuture.get();
        }
    }

    protected Object doGet(Object kvsWithSpace) {
        return syncCommand.get(kvsWithSpace);
    }


    protected List<Object> doGet(List<Object> keysWithSpace) throws ExecutionException, InterruptedException {
        List<RedisFuture<Object>> futures = new ArrayList<>();
        keysWithSpace.forEach(o1 -> futures.add(asyncCommand.get(o1)));
        List<Object> result = new ArrayList<>();
        for (RedisFuture<Object> future : futures) {
            result.add(future.get());
        }
        return result;
    }

}
