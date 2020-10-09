package com.sohu.smc.core;

import org.springframework.beans.factory.InitializingBean;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/30
 */
public interface InitializingOpBean extends InitializingBean {

    void initPrefix();
    void initKeyExpression();

    @Override
    default void afterPropertiesSet() throws Exception{
        initPrefix();
        initKeyExpression();
    }
}
