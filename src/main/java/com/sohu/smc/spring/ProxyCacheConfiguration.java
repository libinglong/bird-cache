package com.sohu.smc.spring;

import com.sohu.smc.anno.EnableMdCaching;
import org.aopalliance.intercept.MethodInterceptor;
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
@ComponentScan
@Configuration
public class ProxyCacheConfiguration implements ImportAware {

    protected AnnotationAttributes enableMdCaching;

    @Bean
    public DefaultBeanFactoryPointcutAdvisor cacheAdvisor() {
        DefaultBeanFactoryPointcutAdvisor advisor =
                new DefaultBeanFactoryPointcutAdvisor();
        advisor.setPointcut(new CacheSourcePointcut());
        advisor.setAdvice(cacheInterceptor());
        advisor.setOrder(this.enableMdCaching.<Integer>getNumber("order"));
        return advisor;
    }

    @Bean
    public MethodInterceptor cacheInterceptor() {
        return new CacheOpMethodInterceptor();
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
