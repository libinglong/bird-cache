package com.sohu.smc.md.cache.spring;

import com.sohu.smc.md.cache.anno.EnableMdCaching;
import org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
public class ProxyCacheConfiguration implements ImportAware, InitializingBean, ApplicationContextAware {

    private AnnotationAttributes enableMdCaching;

    @Autowired(required = false)
    private CacheProperty globalCacheProperty;

    private ApplicationContext ctx;

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
        return new CacheOpInvocation(cacheOpParseService(), globalCacheProperty);
    }

    @Bean
    public SpelParseService spelParseService(){
        return new SpelParseService();
    }

    @Bean
    public CacheOpParseService cacheOpParseService(){
        return new CacheOpParseService(ctx, globalCacheProperty, spelParseService());
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
        if (globalCacheProperty == null){
            globalCacheProperty = new CacheProperty();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }
}
