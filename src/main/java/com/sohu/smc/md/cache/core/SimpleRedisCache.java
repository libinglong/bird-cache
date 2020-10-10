package com.sohu.smc.md.cache.core;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/10
 */
public class SimpleRedisCache implements Cache, InitializingBean {

    RedisCommands<byte[], byte[]> syncCommand;
    RedisAsyncCommands<byte[], byte[]> asyncCommands;
    RedisURI redisURI;

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


        StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection = redisClient.connectPubSub(ByteArrayCodec.INSTANCE);
        RedisPubSubCommands<byte[], byte[]> sync = pubSubConnection.sync();
        sync.subscribe();
    }
}
