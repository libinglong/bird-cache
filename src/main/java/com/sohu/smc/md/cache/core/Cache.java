package com.sohu.smc.md.cache.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/7
 */
public interface Cache {

    /**
     * 设置key的过期时间
     * key在time指定的时间后过期
     * @param key the key of this cache
     * @param milliseconds time to expire in ms
     */
    void expire(Object key, long milliseconds);

    /**
     * 删除key
     * @param key the key of this cache
     */
    void delete(Object key);

    /**
     * 添加缓存
     * @param key the key of this cache
     * @param val the value to be updated of this cache
     * @param milliseconds time to expire in ms
     */
    void set(Object key, Object val, long milliseconds);

    /**
     * 添加缓存
     * @param kvs
     * @param milliseconds time time to expire in ms
     * @throws Exception e
     */
    void set(Map<Object,Object> kvs, long milliseconds) throws ExecutionException, InterruptedException;

    /**
     * 缓存查询
     * @param key the key of this cache
     * @return 缓存的对象.如果缓存未命中,返回null;如果缓存的值为null,返回{@link NullValue#NULL},
     */
    Object get(Object key);

    /**
     * 批量缓存查询需要实现此接口,来达到更好的性能
     * 参数和返回值顺序必须是对应的
     * @param keys the keys of this cache
     * @return the list mapping to keys in order
     */
    List<Object> get(List<Object> keys) throws ExecutionException, InterruptedException;

    /**
     * 清空缓存
     */
    void clear();

}
