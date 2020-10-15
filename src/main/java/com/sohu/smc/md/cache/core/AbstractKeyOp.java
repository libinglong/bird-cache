package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.spel.ParamEvaluationContext;
import com.sohu.smc.md.cache.spring.CacheProperties;
import com.sohu.smc.md.cache.spring.SpelParseService;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

import java.lang.annotation.Annotation;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/30
 */
public abstract class AbstractKeyOp<A extends Annotation> extends AbstractOp<A> {

    protected Expression keyExpression;

    public AbstractKeyOp(MetaData<A> metaData, Cache cache, CacheProperties cacheProperties, SpelParseService spelParseService) {
        super(metaData, cache, cacheProperties, spelParseService);
        this.keyExpression = spelParseService.getExpression(getKeyExpr());
    }

    protected ValueWrapper wrapper(Object key){
        if (key == null){
            return null;
        }
        return new ValueWrapper(key);
    }

    public Object getKey(InvocationContext invocationContext){
        OpContext opContext = invocationContext.getOpContext(this);
        Object key = opContext.getKey();
        if (key != null){
            return key;
        }
        EvaluationContext context = new ParamEvaluationContext(invocationContext.getMethodInvocation().getArguments());
        return keyExpression.getValue(context);
    }

    protected Long getDelayInvalidTime(){
        return cacheProperties.getExecTime() + cacheProperties.getUnexpectedTime();
    }

    public Long getExecTime(){
        return cacheProperties.getExecTime();
    }

    protected Long getExpiredTime(){
        return cacheProperties.getExpireTime();
    }

    abstract protected String getKeyExpr();
}
