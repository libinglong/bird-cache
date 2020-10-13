package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCachePut;
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

    public MdCachePutOp(MetaData<MdCachePut> metaData, Cache cache) {
        super(metaData, cache);
    }

    public void delayInvalid(InvocationContext invocationContext) throws RuntimeException {
        cache.expire(getKey(invocationContext), getDelayInvalidTime());
    }

    public void set(InvocationContext invocationContext, Object value){
        cache.set(getKey(invocationContext), value, getExpiredTime());
    }

    @Override
    protected String getKeyExpr() {
        return metaData.getAnno()
                .key();
    }
}
