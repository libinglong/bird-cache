package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCacheEvict;
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
public class MdCacheEvictOp extends AbstractKeyOp<MdCacheEvict> {

    public MdCacheEvictOp(MetaData<MdCacheEvict> metaData, Cache cache) {
        super(metaData, cache);
    }

    public void delayInvalid(InvocationContext invocationContext) throws RuntimeException {
        cache.expire(getKey(invocationContext), getDelayInvalidTime());
    }

    public void delete(InvocationContext invocationContext) throws RuntimeException {
        cache.delete(getKey(invocationContext));
    }

    @Override
    protected String getKeyExpr() {
        return metaData.getAnno()
                .key();
    }
}
