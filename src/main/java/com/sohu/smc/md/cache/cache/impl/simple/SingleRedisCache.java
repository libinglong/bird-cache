package com.sohu.smc.md.cache.cache.impl.simple;

import com.sohu.smc.md.cache.cache.impl.CacheSpace;
import com.sohu.smc.md.cache.core.Cache;
import com.sohu.smc.md.cache.serializer.Serializer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;

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
public class SingleRedisCache implements Cache {

    private CacheSpace cacheSpace;
    private String cacheSpaceName;
    private String cacheSpaceVersionKey;

    private RedisCommands<Object, Object> syncCommand;
    private RedisAsyncCommands<Object, Object> asyncCommand;
    protected Serializer serializer;

    public SingleRedisCache(String cacheSpaceName, RedisClient redisClient, CacheSpace cacheSpace, Serializer serializer) {
        this.cacheSpaceName = cacheSpaceName;
        this.cacheSpace = cacheSpace;
        this.serializer = serializer;

        StatefulRedisConnection<Object, Object> connect = redisClient.connect(new ObjectRedisCodec(serializer));
        syncCommand = connect.sync();
        asyncCommand = connect.async();
        cacheSpaceVersionKey = "v:" + cacheSpaceName;
    }

    @Override
    public String getCacheSpaceName() {
        return cacheSpaceName;
    }

    @Override
    public void expire(Object key, long milliseconds) {
        syncCommand.pexpire(processSpace(key), milliseconds);
    }

    @Override
    public void delete(Object key) {
        syncCommand.del(processSpace(key));
    }

    @Override
    public void set(Object key, Object val, long time) {
        syncCommand.psetex(processSpace(key), time, val);
    }

    @Override
    public void setKvs(Map<Object, Object> kvs, long time) throws ExecutionException, InterruptedException {
        Map<Object, Object> kvsWithSpace = kvs.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> processSpace(entry.getKey()), Map.Entry::getValue));
        List<RedisFuture<String>> futures = new ArrayList<>();
        kvsWithSpace.forEach((o1, o2) -> futures.add(asyncCommand.psetex(o1, time, o2)));
        for (RedisFuture<String> stringRedisFuture : futures) {
            stringRedisFuture.get();
        }
    }

    @Override
    public Object get(Object key) {
        return syncCommand.get(processSpace(key));
    }

    @Override
    public List<Object> get(List<Object> keys) throws ExecutionException, InterruptedException {
        List<Object> keysWithSpace = keys.stream()
                .map(this::processSpace)
                .collect(Collectors.toList());
        List<RedisFuture<Object>> futures = new ArrayList<>();
        keysWithSpace.forEach(o1 -> futures.add(asyncCommand.get(o1)));
        List<Object> result = new ArrayList<>();
        for (RedisFuture<Object> future : futures) {
            result.add(future.get());
        }
        return result;
    }

    @Override
    public void clear() {
        cacheSpace.incrVersion(cacheSpaceVersionKey);
    }

    protected Object processSpace(Object key) {
        String version = cacheSpace.getVersion(cacheSpaceVersionKey);
        return new SpaceWrapper(cacheSpaceName + ":" + version, key);
    }

}
