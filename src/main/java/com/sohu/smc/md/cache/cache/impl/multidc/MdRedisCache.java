package com.sohu.smc.md.cache.cache.impl.multidc;

import com.sohu.smc.md.cache.cache.impl.CacheSpace;
import com.sohu.smc.md.cache.cache.impl.simple.PbObjectRedisCodec;
import com.sohu.smc.md.cache.cache.impl.simple.SingleRedisCache;
import com.sohu.smc.md.cache.serializer.Serializer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/13
 */
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MdRedisCache extends SingleRedisCache {

    private RedisClient secondaryRedisClient;
    RedisCommands<Object, Object> secondaryCommand;
    private AsyncTaskExecutor asyncTaskExecutor;

    public MdRedisCache(String cacheSpaceName, RedisClient redisClient, RedisClient secondaryRedisClient,
                        CacheSpace cacheSpace, Serializer serializer, AsyncTaskExecutor asyncTaskExecutor) {
        super(cacheSpaceName, redisClient, cacheSpace, serializer);
        this.secondaryRedisClient = secondaryRedisClient;
        this.asyncTaskExecutor = asyncTaskExecutor;
    }

    @Override
    public void doExpire(Object keyWithSpace, long milliseconds) {
        super.doExpire(keyWithSpace, milliseconds);
        asyncTaskExecutor.submit(() -> {
            try{
                secondaryCommand.expire(keyWithSpace,milliseconds);
            }catch (Exception e){
                log.info("expire error in secondary redis,the base64 of the key bytes is {}",encodeKey2Base64Strinn(keyWithSpace));
            }
        });
    }

    @Override
    public void doDelete(Object keyWithSpace) {
        super.doDelete(keyWithSpace);
        asyncTaskExecutor.submit(() -> {
            try{
                secondaryCommand.del(keyWithSpace);
            }catch (Exception e){
                log.info("delete error in secondary redis,the base64 of the key bytes is {}",encodeKey2Base64Strinn(keyWithSpace));
            }
        });
    }

    @Override
    public void doSet(Object keyWithSpace, Object val, long time) {
        super.doSet(keyWithSpace, val, time);
        asyncTaskExecutor.submit(() -> {
            try{
                secondaryCommand.psetex(keyWithSpace, time, val);
            }catch (Exception e){
                log.info("set error in secondary redis,the base64 of the key bytes is {}",encodeKey2Base64Strinn(keyWithSpace));
            }
        });
    }

    @Override
    public void doSet(Map<Object, Object> kvsWithSpace, long time) throws ExecutionException, InterruptedException {
        super.doSet(kvsWithSpace, time);
        kvsWithSpace.forEach((o1, o2) ->
                asyncTaskExecutor.submit(() -> {
                    try {
                        secondaryCommand.psetex(o1, time, o2);
                    } catch (Exception e){
                        log.info("set error in secondary redis,the base64 of the key bytes is {}",encodeKey2Base64Strinn(o1));
                    }
                }));

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        secondaryCommand = secondaryRedisClient.connect(new PbObjectRedisCodec(serializer))
                .sync();
    }

    private String encodeKey2Base64Strinn(Object keyWithSpace){
        byte[] serialize = serializer.serialize(keyWithSpace);
        return Base64.getEncoder().encodeToString(serialize);
    }

}
