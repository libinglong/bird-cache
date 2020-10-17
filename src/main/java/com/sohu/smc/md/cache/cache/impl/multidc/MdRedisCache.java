package com.sohu.smc.md.cache.cache.impl.multidc;

import com.sohu.smc.md.cache.core.Cache;
import lombok.extern.slf4j.Slf4j;

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
    private ErrorHandler errorHandler;

    public MdRedisCache(Cache primaryCache, Cache secondaryCache, ExecutorService executorService, ErrorHandler errorHandler) {
        this.primaryCache = primaryCache;
        this.secondaryCache = secondaryCache;
        this.executorService = executorService;
        this.errorHandler = errorHandler;
    }

    @Override
    public String getCacheSpaceName() {
        return primaryCache.getCacheSpaceName();
    }

    @Override
    public void expire(Object key, long milliseconds) {
        primaryCache.expire(key, milliseconds);
        executorService.submit(() -> {
            try{
                secondaryCache.expire(key,milliseconds);
            }catch (Exception e){
                ErrorCache errorCache = new ErrorCache();
                errorCache.cacheSpaceName = getCacheSpaceName();
                errorCache.key = key;
                errorCache.e = e;
                errorCache.opName = "expire";
                errorHandler.handle(errorCache);
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
                ErrorCache errorCache = new ErrorCache();
                errorCache.cacheSpaceName = getCacheSpaceName();
                errorCache.key = key;
                errorCache.e = e;
                errorCache.opName = "delete";
                errorHandler.handle(errorCache);
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
                ErrorCache errorCache = new ErrorCache();
                errorCache.cacheSpaceName = getCacheSpaceName();
                errorCache.key = key;
                errorCache.value = val;
                errorCache.e = e;
                errorCache.opName = "set";
                errorHandler.handle(errorCache);
            }
        });
    }

    @Override
    public void setKvs(Map<Object, Object> kvs, long milliseconds) throws ExecutionException, InterruptedException {
        primaryCache.setKvs(kvs, milliseconds);
        executorService.submit(() -> {
            try {
                secondaryCache.setKvs(kvs, milliseconds);
            } catch (Exception e){
                ErrorCache errorCache = new ErrorCache();
                errorCache.cacheSpaceName = getCacheSpaceName();
                errorCache.key = kvs;
                errorCache.e = e;
                errorCache.opName = "setKvs";
                errorHandler.handle(errorCache);
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
                ErrorCache errorCache = new ErrorCache();
                errorCache.cacheSpaceName = getCacheSpaceName();
                errorCache.e = e;
                errorCache.opName = "clear";
                errorHandler.handle(errorCache);
            }
        });
    }


}
