package com.sohu.smc.md.cache.spring;

import com.sohu.smc.md.cache.anno.EnableMdCaching;
import org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/7
 */
@ComponentScan(basePackages = "com.sohu.smc.md.cache")
@Configuration
public class ProxyCacheConfiguration implements ImportAware {

    protected AnnotationAttributes enableMdCaching;

    @Bean
    public DefaultBeanFactoryPointcutAdvisor cacheAdvisor() {
        DefaultBeanFactoryPointcutAdvisor advisor =
                new DefaultBeanFactoryPointcutAdvisor();
        advisor.setPointcut(cacheOpInvocation());
        advisor.setAdvice(cacheOpInvocation());
        advisor.setOrder(this.enableMdCaching.<Integer>getNumber("order"));
        return advisor;
    }

    @Bean
    public CacheOpInvocation cacheOpInvocation() {
        return new CacheOpInvocation();
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
}
