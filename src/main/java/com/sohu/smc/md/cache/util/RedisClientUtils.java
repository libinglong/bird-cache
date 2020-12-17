package com.sohu.smc.md.cache.util;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.resource.ClientResources;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/17
 */
public class RedisClientUtils {

    public static RedisClient initRedisClient(String redisURI, ClientResources clientResources, Duration commandTimeout){
        RedisClient redisClient;
        if (clientResources == null){
            redisClient = RedisClient.create(redisURI);
        } else {
            redisClient = RedisClient.create(clientResources, redisURI);
        }
        TimeoutOptions timeoutOptions = TimeoutOptions.builder()
                .fixedTimeout(commandTimeout)
                .build();
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.of(3000, ChronoUnit.MILLIS))
                .build();
        ClientOptions options = ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .cancelCommandsOnReconnectFailure(true)
                .timeoutOptions(timeoutOptions)
                .socketOptions(socketOptions)
                .build();
        redisClient.setOptions(options);
        return redisClient;
    }

    public static RedisClusterClient initRedisClusterClient(String redisURI, ClientResources clientResources, Duration commandTimeout){
        RedisClusterClient redisClusterClient;
        if (clientResources == null){
            redisClusterClient = RedisClusterClient.create(redisURI);
        } else {
            redisClusterClient = RedisClusterClient.create(clientResources, redisURI);
        }
        TimeoutOptions timeoutOptions = TimeoutOptions.builder()
                .fixedTimeout(commandTimeout)
                .build();
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.of(3000, ChronoUnit.MILLIS))
                .build();
        ClusterClientOptions options = ClusterClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .cancelCommandsOnReconnectFailure(true)
                .timeoutOptions(timeoutOptions)
                .socketOptions(socketOptions)
                .build();
        redisClusterClient.setOptions(options);
        return redisClusterClient;
    }


}
