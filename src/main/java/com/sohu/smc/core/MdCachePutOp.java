package com.sohu.smc.core;

import com.sohu.smc.anno.MdCachePut;
import com.sohu.smc.spring.CacheProperties;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
@Component
public class MdCachePutOp extends AbstractOp<MdCachePut> {

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

    public void put(InvocationContext invocationContext, Object value){
        cache.put(getPrefixedKey(invocationContext), value);
    }

    @Override
    protected String getKeyExpr() {
        return null;
    }
}
