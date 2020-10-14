package com.sohu.smc.md.cache.cache.impl.simple;

import com.sohu.smc.md.cache.cache.impl.CacheSpace;
import com.sohu.smc.md.cache.core.Cache;
import com.sohu.smc.md.cache.core.CacheManage;
import com.sohu.smc.md.cache.serializer.PbSerializer;
import com.sohu.smc.md.cache.serializer.Serializer;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.lettuce.core.resource.ClientResources;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/12
 */
public class SingleRedisCacheManage implements CacheManage, CacheSpace, InitializingBean, ApplicationContextAware {

    protected ApplicationContext applicationContext;

    protected static final String CACHE_SPACE_CHANGE_CHANNEL = "CACHE_SPACE_CHANGE_CHANNEL";

    private RedisCommands<String, String> stringSyncCommand;
    protected RedisClient redisClient;
    protected Serializer serializer;
    protected RedisURI redisURI;
    protected ClientResources clientResources;

    public SingleRedisCacheManage(RedisURI redisURI) {
        this.redisURI = redisURI;
    }

    public SingleRedisCacheManage(RedisURI redisURI, ClientResources clientResources) {
        this.redisURI = redisURI;
        this.clientResources = clientResources;
    }

    private ConcurrentReferenceHashMap<String, String> versionMap = new ConcurrentReferenceHashMap<>(256);

    @Override
    public void incrVersion(String cacheSpaceVersionKey) {
        stringSyncCommand.incr(cacheSpaceVersionKey);
        stringSyncCommand.publish(CACHE_SPACE_CHANGE_CHANNEL, cacheSpaceVersionKey);
    }

    @Override
    public String getVersion(String cacheSpaceVersionKey) {
        return versionMap.computeIfAbsent(cacheSpaceVersionKey, this::doGetVersion);
    }

    private String doGetVersion(String cacheSpaceVersionKey) {
        String version = stringSyncCommand.get(cacheSpaceVersionKey);
        if (!StringUtils.isEmpty(version)) {
            return version;
        }
        version = "0";
        Boolean suc = stringSyncCommand.setnx(cacheSpaceVersionKey, version);
        if (suc){
            return version;
        }
        return stringSyncCommand.get(cacheSpaceVersionKey);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(redisURI,"redisURI can not be null");
        redisClient = initRedisClient(redisURI, clientResources);
        stringSyncCommand = redisClient.connect(StringCodec.UTF8)
                .sync();
        StatefulRedisPubSubConnection<String, String> pubSubConnection = redisClient.connectPubSub(StringCodec.UTF8);
        pubSubConnection.addListener(new RedisPubSubAdapter<String, String>() {
            @Override
            public void message(String channel, String cacheSpaceName) {
                versionMap.remove(cacheSpaceName);
            }
        });
        RedisPubSubCommands<String, String> sync = pubSubConnection.sync();
        sync.subscribe(CACHE_SPACE_CHANGE_CHANNEL);
        if (serializer == null){
            serializer = new PbSerializer();
        }
    }

    protected RedisClient initRedisClient(RedisURI redisURI,ClientResources clientResources){
        RedisClient redisClient;
        if (clientResources == null){
            redisClient = RedisClient.create(redisURI);
        } else {
            redisClient = RedisClient.create(clientResources, redisURI);
        }
        ClientOptions options = ClientOptions.builder()
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build();
        redisClient.setOptions(options);
        return redisClient;
    }

    @PreDestroy
    public void shutdown(){
        if (redisClient != null){
            redisClient.shutdown();
        }
    }

    protected Map<String,Cache> cacheMap = new ConcurrentHashMap<>();

    @Override
    public Cache getCache(String cacheSpaceName) {
        return cacheMap.computeIfAbsent(cacheSpaceName, cacheSpaceName1 ->
                applicationContext.getBean(SingleRedisCache.class,cacheSpaceName1, redisClient, this, serializer));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }
}
