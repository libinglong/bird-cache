package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.util.ByteArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.nio.charset.StandardCharsets;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/10
 */
@Service
public class CacheSpace {

    @Autowired
    private Cache cache;

    private ConcurrentReferenceHashMap<String, byte[]> prefixMap = new ConcurrentReferenceHashMap<>(256);

    public byte[] getPrefix(String cacheSpaceName){
        return prefixMap.computeIfAbsent(cacheSpaceName, this::doGetPrefix);
    }

    public void remove(String cacheSpaceName){
        prefixMap.remove(getcacheSpaceNameKey(cacheSpaceName));
    }

    private byte[] doGetPrefix(String cacheSpaceName){
        byte[] clsVersionKey = getcacheSpaceNameKey(cacheSpaceName)
                .getBytes(StandardCharsets.UTF_8);
        byte[] version = cache.get(clsVersionKey);
        return ByteArrayUtils.combine(clsVersionKey,version);
    }

    private String getcacheSpaceNameKey(String cacheSpaceName){
        return cacheSpaceName + "v";
    }



}
