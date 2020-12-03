package com.sohu.smc.md.cache.util;

import io.lettuce.core.*;
import io.lettuce.core.resource.ClientResources;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/17
 */
public class RedisClientUtils {

    public static RedisClient initRedisClient(RedisURI redisURI, ClientResources clientResources){
        RedisClient redisClient;
        if (clientResources == null){
            redisClient = RedisClient.create(redisURI);
        } else {
            redisClient = RedisClient.create(clientResources, redisURI);
        }
        TimeoutOptions timeoutOptions = TimeoutOptions.builder()
                .connectionTimeout()
                .fixedTimeout(Duration.of(3000, ChronoUnit.MILLIS))
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


}
