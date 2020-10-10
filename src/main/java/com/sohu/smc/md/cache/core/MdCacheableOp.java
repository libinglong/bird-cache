package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCacheable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
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
            cache.set(prefixedKeyBytes, serializer.serialize(result), cacheProperties.getExpireTime());
            return result;
        }
        return valueWrapper.get();
    }

}
