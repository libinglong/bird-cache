package com.sohu.smc.md.cache.cache.impl.multidc;

import com.sohu.smc.md.cache.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.Base64;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/16
 */
@Slf4j
public class DefaultErrorHandler implements ErrorHandler {

    private Serializer serializer;

    public DefaultErrorHandler(Serializer serializer){
        Assert.notNull(serializer, "the serializer can not be null");
        this.serializer = serializer;
    }

    @Override
    public void handle(ErrorCache errorCache) {
        log.info("cacheSpaceName={},opName={},the base64 of the key bytes={},e={}",errorCache.cacheSpaceName,
                errorCache.opName, encodeKey2Base64String(errorCache.key), errorCache.e.getMessage());
        log.debug("err occur",errorCache.e);
    }

    private String encodeKey2Base64String(Object key){
        byte[] serialize = serializer.serialize(key);
        return Base64.getEncoder().encodeToString(serialize);
    }


}
