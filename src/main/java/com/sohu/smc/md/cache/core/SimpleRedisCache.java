package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.util.ByteArrayUtils;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/10
 */
public class SimpleRedisCache implements Cache, CacheSpace, InitializingBean {

    RedisCommands<byte[], byte[]> syncCommand;
    RedisAsyncCommands<byte[], byte[]> asyncCommands;
    RedisURI redisURI;
    private static final String CACHE_SPACE_CHANGE_CHANNEL = "CACHE_SPACE_CHANGE_CHANNEL";
    public SimpleRedisCache(RedisURI redisURI) {
        this.redisURI = redisURI;
    }

    @Override
    public void expire(byte[] key, long time) {
        syncCommand.expire(key, time);
    }

    @Override
    public void delete(byte[] key) {
        syncCommand.del(key);
    }

    @Override
    public void set(byte[] key, byte[] val, long time) {
        syncCommand.psetex(key, time, val);
    }

    @Override
    public void set(Map<byte[], byte[]> kvs, long time) throws ExecutionException, InterruptedException {
        List<RedisFuture<String>> futures = new ArrayList<>();
        kvs.forEach((bytes, bytes2) -> futures.add(asyncCommands.psetex(bytes, time, bytes2)));
        for (RedisFuture<String> stringRedisFuture : futures) {
            stringRedisFuture.get();
        }
    }

    @Override
    public byte[] get(byte[] key) {
        return syncCommand.get(key);
    }

    @Override
    public List<byte[]> get(List<byte[]> keys) throws ExecutionException, InterruptedException {
        List<RedisFuture<byte[]>> futures = new ArrayList<>();
        keys.forEach(bytes -> futures.add(asyncCommands.get(bytes)));
        List<byte[]> result = new ArrayList<>();
        for (RedisFuture<byte[]> future : futures) {
            result.add(future.get());
        }
        return result;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        RedisClient redisClient = RedisClient.create(redisURI);
        redisClient.setOptions(ClientOptions.builder()
                .autoReconnect(false)
                .build());
        StatefulRedisConnection<byte[], byte[]> connection = redisClient.connect(ByteArrayCodec.INSTANCE);
        syncCommand = connection.sync();
        asyncCommands = connection.async();


        StatefulRedisPubSubConnection<String, String> pubSubConnection = redisClient.connectPubSub(StringCodec.UTF8);
        pubSubConnection.addListener(new RedisPubSubListener<String, String>() {
            @Override
            public void message(String channel, String message) {
                System.out.println();
            }

            @Override
            public void message(String pattern, String channel, String message) {
                System.out.println();
            }

            @Override
            public void subscribed(String channel, long count) {
                System.out.println();
            }

            @Override
            public void psubscribed(String pattern, long count) {
                System.out.println();
            }

            @Override
            public void unsubscribed(String channel, long count) {
                System.out.println();
            }

            @Override
            public void punsubscribed(String pattern, long count) {
                System.out.println();
            }
        });
        RedisPubSubCommands<String, String> sync = pubSubConnection.sync();
        sync.subscribe(CACHE_SPACE_CHANGE_CHANNEL);
    }


    private ConcurrentReferenceHashMap<String, byte[]> versionMap = new ConcurrentReferenceHashMap<>(256);

    @Override
    public void incrVersion(String cacheSpaceVersionKey){
        byte[] bytes = cacheSpaceVersionKey.getBytes(StandardCharsets.UTF_8);
        syncCommand.incr(bytes);
        syncCommand.publish(CACHE_SPACE_CHANGE_CHANNEL.getBytes(StandardCharsets.UTF_8),bytes);
    }

    @Override
    public byte[] getVersion(String cacheSpaceVersionKey) {
        return versionMap.computeIfAbsent(cacheSpaceVersionKey, this::doGetVersion);
    }

    private byte[] doGetVersion(String cacheSpaceVersionKey){
        byte[] clsVersionKey = cacheSpaceVersionKey.getBytes(StandardCharsets.UTF_8);
        byte[] version = syncCommand.get(clsVersionKey);
        if(version == null){
            version = "0".getBytes(StandardCharsets.UTF_8);
            syncCommand.setnx(clsVersionKey,version);
        }
        return ByteArrayUtils.combine(clsVersionKey,version);
    }
}
