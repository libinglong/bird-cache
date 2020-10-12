package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCachePut;
import com.sohu.smc.md.cache.spring.CacheProperties;
import org.springframework.beans.factory.annotation.Autowired;
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
public class MdCachePutOp extends AbstractKeyOp<MdCachePut> {

    @Autowired
    private Cache cache;

    @Autowired
    private CacheProperties cacheProperties;

    public MdCachePutOp(MetaData<MdCachePut> metaData) {
        super(metaData);
    }

    public void expire(InvocationContext invocationContext) throws RuntimeException {
        cache.expire(getPrefixedKey(invocationContext),cacheProperties.getExpireTime());
    }

    public void set(InvocationContext invocationContext, Object value){
        cache.set(getPrefixedKey(invocationContext), serializer.serialize(value), cacheProperties.getExpireTime());
    }

    @Override
    protected String getKeyExpr() {
        return metaData.getAnno()
                .key();
    }
}
