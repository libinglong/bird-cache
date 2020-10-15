package com.sohu.smc.md.cache.cache.impl.multidc;

import com.sohu.smc.md.cache.core.Cache;
import com.sohu.smc.md.cache.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/13
 */
@Slf4j
public class MdRedisCache implements Cache {

    private Cache primaryCache;
    private Cache secondaryCache;
    private ExecutorService executorService;
    private Serializer serializer;

    public MdRedisCache(Cache primaryCache, Cache secondaryCache, Serializer serializer,
                        ExecutorService executorService) {
        this.primaryCache = primaryCache;
        this.secondaryCache = secondaryCache;
        this.executorService = executorService;
        this.serializer = serializer;
    }

    @Override
    public void expire(Object key, long milliseconds) {
        primaryCache.expire(key, milliseconds);
        executorService.submit(() -> {
            try{
                secondaryCache.expire(key,milliseconds);
            }catch (Exception e){
                log.info("expire error in secondary redis,the base64 of the key bytes is {}",encodeKey2Base64Strinn(key));
                log.debug("err",e);
            }
        });
    }

    @Override
    public void delete(Object key) {
        primaryCache.delete(key);
        executorService.submit(() -> {
            try{
                secondaryCache.delete(key);
            }catch (Exception e){
                log.info("delete error in secondary redis,the base64 of the key bytes is {}",encodeKey2Base64Strinn(key));
                log.debug("err",e);
            }
        });
    }

    @Override
    public void set(Object key, Object val, long milliseconds) {
        primaryCache.set(key, val, milliseconds);
        executorService.submit(() -> {
            try{
                secondaryCache.set(key, val, milliseconds);
            }catch (Exception e){
                log.info("set error in secondary redis,the base64 of the key bytes is {}",encodeKey2Base64Strinn(key));
                log.debug("err",e);
            }
        });
    }

    @Override
    public void set(Map<Object, Object> kvs, long milliseconds) throws ExecutionException, InterruptedException {
        primaryCache.set(kvs, milliseconds);
        executorService.submit(() -> {
            try {
                secondaryCache.set(kvs, milliseconds);
            } catch (Exception e){
                log.info("set kvs error in secondary redis,the base64 of the key bytes is {}",encodeKey2Base64Strinn(kvs));
                log.debug("err",e);
            }
        });
    }

    @Override
    public Object get(Object key) {
        return primaryCache.get(key);
    }

    @Override
    public List<Object> get(List<Object> keys) throws ExecutionException, InterruptedException {
        return primaryCache.get(keys);
    }

    @Override
    public void clear() {
        primaryCache.clear();
        executorService.submit(() -> {
            try {
                secondaryCache.clear();
            } catch (Exception e){
                log.info("clear error in secondary redis");
                log.debug("err",e);
            }
        });
    }

    private String encodeKey2Base64Strinn(Object key){
        byte[] serialize = serializer.serialize(key);
        return Base64.getEncoder().encodeToString(serialize);
    }
}
