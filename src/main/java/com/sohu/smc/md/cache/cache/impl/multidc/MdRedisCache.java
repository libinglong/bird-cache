package com.sohu.smc.md.cache.cache.impl.multidc;

import com.sohu.smc.md.cache.cache.impl.CacheSpace;
import com.sohu.smc.md.cache.cache.impl.simple.PbObjectRedisCodec;
import com.sohu.smc.md.cache.cache.impl.simple.SingleRedisCache;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.async.RedisAsyncCommands;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/13
 */
public class MdRedisCache extends SingleRedisCache {

    private RedisClient secondaryRedisClient;
    RedisAsyncCommands<Object, Object> secondaryAsyncCommand;

    public MdRedisCache(String cacheSpaceName, RedisClient redisClient, RedisClient secondaryRedisClient,CacheSpace cacheSpace) {
        super(cacheSpaceName, redisClient, cacheSpace);
        this.secondaryRedisClient = secondaryRedisClient;
    }

    @Override
    public void doExpire(Object keyWithSpace, long milliseconds) {
        super.doExpire(keyWithSpace, milliseconds);
        secondaryAsyncCommand.expire(keyWithSpace,milliseconds);
    }

    @Override
    public void doDelete(Object keyWithSpace) {
        super.doDelete(keyWithSpace);
        secondaryAsyncCommand.del(keyWithSpace);
    }

    @Override
    public void doSet(Object keyWithSpace, Object val, long time) {
        super.doSet(keyWithSpace, val, time);
        secondaryAsyncCommand.psetex(keyWithSpace, time, val);
    }

    @Override
    public void doSet(Map<Object, Object> kvsWithSpace, long time) throws ExecutionException, InterruptedException {
        super.doSet(kvsWithSpace, time);
        kvsWithSpace.forEach((o1, o2) -> secondaryAsyncCommand.psetex(o1, time, o2));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        secondaryAsyncCommand = secondaryRedisClient.connect(PbObjectRedisCodec.INSTANCE)
                .async();
    }

}
