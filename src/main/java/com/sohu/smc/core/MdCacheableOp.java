package com.sohu.smc.core;

import com.sohu.smc.anno.MdCacheable;
import org.springframework.stereotype.Component;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
@Component
public class MdCacheableOp extends AbstractOp<MdCacheable> {

    public MdCacheableOp(MetaData<MdCacheable> metaData) {
        super(metaData);
    }

    /**
     *
     * @param key
     * @throws RuntimeException
     * @return 返回null表示缓存没有命中,如果命中,返回{@link NullValue#NULL}表示缓存的值为null
     */
    public ValueWrapper getCacheValue(byte[] key) throws RuntimeException {
        return byte2ValueWrapper(cache.get(key));
    }

    public void put(byte[] key, Object value) throws RuntimeException {
        cache.put(key,value);
    }

    @Override
    protected String getKeyExpr() {
        return metaData.getAnno()
                .key();
    }

    public Object processCacheableOp(InvocationContext invocationContext) throws Throwable {
        byte[] prefixedKeyBytes = getPrefixedKey(invocationContext);
        ValueWrapper valueWrapper = getCacheValue(prefixedKeyBytes);
        if (valueWrapper == null){
            Object result = invocationContext.getMethodInvocation()
                    .proceed();
            put(prefixedKeyBytes, result);
            return result;
        }
        return valueWrapper.get();
    }

}
