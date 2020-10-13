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
public class MdCacheableOp extends AbstractKeyOp<MdCacheable> {

    public MdCacheableOp(MetaData<MdCacheable> metaData, Cache cache) {
        super(metaData, cache);
    }

    /**
     *
     * @param key key
     * @throws RuntimeException e
     * @return 返回null表示缓存没有命中,如果命中,返回{@link NullValue#NULL}表示缓存的值为null
     */
    public ValueWrapper getCacheValue(Object key) throws RuntimeException {
        return wrapper(cache.get(key));
    }

    @Override
    protected String getKeyExpr() {
        return metaData.getAnno()
                .key();
    }

    public Object processCacheableOp(InvocationContext invocationContext) throws Throwable {
        Object key = getKey(invocationContext);
        ValueWrapper valueWrapper = getCacheValue(key);
        if (valueWrapper == null){
            Object result = invocationContext.doInvoke();
            cache.set(key, result, getExpiredTime());
            return result;
        }
        return valueWrapper.get();
    }

}
