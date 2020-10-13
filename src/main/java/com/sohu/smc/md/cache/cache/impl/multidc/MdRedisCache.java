package com.sohu.smc.md.cache.cache.impl.multidc;

import com.sohu.smc.md.cache.cache.impl.CacheSpace;
import com.sohu.smc.md.cache.cache.impl.simple.PbObjectRedisCodec;
import com.sohu.smc.md.cache.cache.impl.simple.SingleRedisCache;
import com.sohu.smc.md.cache.serializer.PbSerializer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.async.RedisAsyncCommands;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/13
 */
@Slf4j
public class MdRedisCache extends SingleRedisCache {

    PbSerializer pbSerializer = new PbSerializer();

    private RedisClient secondaryRedisClient;
    RedisAsyncCommands<Object, Object> secondaryAsyncCommand;

    public MdRedisCache(String cacheSpaceName, RedisClient redisClient, RedisClient secondaryRedisClient,CacheSpace cacheSpace) {
        super(cacheSpaceName, redisClient, cacheSpace);
        this.secondaryRedisClient = secondaryRedisClient;
    }

    @Override
    public void doExpire(Object keyWithSpace, long milliseconds) {
        super.doExpire(keyWithSpace, milliseconds);
        secondaryAsyncCommand.expire(keyWithSpace,milliseconds)
                .handle((aBoolean, throwable) -> {
                    if (!aBoolean || throwable != null){
                        log.info("expire error in secondary redis,the base64 of the key bytes is {}",encodeKey2Base64Strinn(keyWithSpace));
                    }
                    return null;
                });
    }

    @Override
    public void doDelete(Object keyWithSpace) {
        super.doDelete(keyWithSpace);
        secondaryAsyncCommand.del(keyWithSpace)
                .handle((aLong, throwable) -> {
                    if (aLong == 0 || throwable != null){
                        log.info("delete error in secondary redis,the base64 of the key bytes is {}",encodeKey2Base64Strinn(keyWithSpace));
                    }
                    return null;
                });
    }

    @Override
    public void doSet(Object keyWithSpace, Object val, long time) {
        super.doSet(keyWithSpace, val, time);
        secondaryAsyncCommand.psetex(keyWithSpace, time, val)
                .handle((s, throwable) -> {
                    if (!"OK".equals(s) || throwable != null){
                        log.info("set key error in secondary redis,the base64 of the key bytes is {}",encodeKey2Base64Strinn(keyWithSpace));
                    }
                    return null;
                });
    }

    @Override
    public void doSet(Map<Object, Object> kvsWithSpace, long time) throws ExecutionException, InterruptedException {
        super.doSet(kvsWithSpace, time);
        kvsWithSpace.forEach((o1, o2) -> {
            secondaryAsyncCommand.psetex(o1, time, o2)
                    .handle((s, throwable) -> {
                        if (!"OK".equals(s) || throwable != null){
                            log.info("set key error in secondary redis,the base64 of the key bytes is {}",encodeKey2Base64Strinn(o1));
                        }
                        return null;
                    });
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        secondaryAsyncCommand = secondaryRedisClient.connect(PbObjectRedisCodec.INSTANCE)
                .async();
    }

    private String encodeKey2Base64Strinn(Object keyWithSpace){
        byte[] serialize = pbSerializer.serialize(keyWithSpace);
        return Base64.getEncoder().encodeToString(serialize);
    }

}
