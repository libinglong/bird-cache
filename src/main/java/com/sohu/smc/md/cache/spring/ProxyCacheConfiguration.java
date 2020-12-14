package com.sohu.smc.md.cache.spring;

import com.sohu.smc.md.cache.anno.EnableMdCaching;
import com.sohu.smc.md.cache.core.CacheManager;
import org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/7
 */
@Configuration
public class ProxyCacheConfiguration implements ImportAware, InitializingBean {

    private AnnotationAttributes enableMdCaching;

    @Autowired(required = false)
    private CacheProperty cacheProperty;

    @Autowired
    private CacheManager cacheManager;

    @Bean
    public DefaultBeanFactoryPointcutAdvisor cacheAdvisor(CacheOpInvocation cacheOpInvocation) {
        DefaultBeanFactoryPointcutAdvisor advisor =
                new DefaultBeanFactoryPointcutAdvisor();
        advisor.setPointcut(cacheOpInvocation);
        advisor.setAdvice(cacheOpInvocation);
        advisor.setOrder(this.enableMdCaching.<Integer>getNumber("order"));
        return advisor;
    }

    @Bean
    public CacheOpInvocation cacheOpInvocation() {
        return new CacheOpInvocation(cacheOpParseService(), cacheProperty);
    }

    @Bean
    public SpelParseService spelParseService(){
        return new SpelParseService();
    }

    @Bean
    public CacheOpParseService cacheOpParseService(){
        return new CacheOpParseService(cacheManager, cacheProperty, spelParseService());
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableMdCaching = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnableMdCaching.class.getName(), false));
        if (this.enableMdCaching == null) {
            throw new IllegalArgumentException(
                   "@EnableMdCaching is not present on importing class " + importMetadata.getClassName());
        }
    }


    @Override
    public void afterPropertiesSet() {
        if (cacheProperty == null){
            cacheProperty = new CacheProperty();
        }
    }
}
