package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdCacheClear;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/10
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MdCacheClearOp extends AbstractOp<MdCacheClear> {

    public MdCacheClearOp(MetaData<MdCacheClear> metaData) {
        super(metaData);
    }

    public void clear(){
        cacheSpace.incrVersion(cacheSpaceVersionKey);
    }

}
