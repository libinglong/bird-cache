package com.sohu.smc.core;

import java.util.List;
import java.util.Map;

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
     * @param time time to expire in ms
     */
    void expire(byte[] key, long time);

    /**
     * 删除key
     * @param key the key of this cache
     */
    void delete(byte[] key);

    /**
     * 添加缓存
     * @param key the key of this cache
     * @param val the value to be updated of this cache
     */
    void put(byte[] key, Object val);

    /**
     * 添加缓存
     * @param kvs
     */
    void put(Map<byte[],byte[]> kvs);

    /**
     * 缓存查询
     * @param key the key of this cache
     * @return 缓存的对象.如果缓存未命中,返回null;如果缓存的值为null,返回{@link NullValue#NULL},
     */
    byte[] get(Object key);

    /**
     * 批量缓存查询需要实现此接口,来达到更好的性能
     * 参数和返回值顺序必须是对应的
     * 返回的list中的Object规则参见{@link Cache#get(java.lang.Object)}
     * @param keys the keys of this cache
     * @return the list mapping to keys
     */
    List<byte[]> get(List<byte[]> keys);

    /**
     * evict the key of this cache
     * @param key the key to be evicted of this cache
     */
    void evict(byte[] key);

    /**
     * remove all mappings from this cache.
     */
    void clear();

}
