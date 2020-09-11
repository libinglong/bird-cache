package com.sohu.smc.core;

import java.util.List;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/7
 */
public interface Cache {

    /**
     * 更新前
     * @param key the key of this cache
     */
    void putBefore(Object key);

    /**
     * 更新后
     * @param key the key of this cache
     * @param val the value to be updated of this cache
     */
    void putAfter(Object key,Object val);

    /**
     * 缓存查询
     * @param key the key of this cache
     * @return 缓存的对象.如果缓存未命中,返回null;如果缓存的值为null,返回{@link NullValue#NULL},
     */
    Object get(Object key);

    /**
     * 批量缓存查询需要实现此接口,来达到更好的性能
     * 返回的list中的Object规则参见{@link Cache#get(java.lang.Object)}
     * @param keys the keys of this cache
     * @return the list mapping to keys
     */
    List<Object> get(List<Object> keys);

    /**
     * evict the key of this cache
     * @param key the key to be evicted of this cache
     */
    void evict(Object key);

    /**
     * remove all mappings from this cache.
     */
    void clear();

}
