package com.sohu.smc.md.cache.util;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/17
 */
public class RedisClientUtils {

    public static RedisClient initRedisClient(RedisURI redisURI, ClientResources clientResources, ClientOptions.DisconnectedBehavior disconnectedBehavior){
        RedisClient redisClient;
        if (clientResources == null){
            redisClient = RedisClient.create(redisURI);
        } else {
            redisClient = RedisClient.create(clientResources, redisURI);
        }
        ClientOptions options = ClientOptions.builder()
                .disconnectedBehavior(disconnectedBehavior)
                .build();
        redisClient.setOptions(options);
        return redisClient;
    }


}
