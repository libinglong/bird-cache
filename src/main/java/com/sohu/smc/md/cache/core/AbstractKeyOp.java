package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.serializer.Serializer;
import com.sohu.smc.md.cache.spel.ParamEvaluationContext;
import com.sohu.smc.md.cache.spring.CacheProperties;
import com.sohu.smc.md.cache.spring.SpelParseService;
import com.sohu.smc.md.cache.util.ByteArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    protected SpelParseService spelParseService;

    @Autowired
    protected Cache cache;

    @Autowired
    protected Serializer serializer;

    @Autowired
    protected CacheProperties cacheProperties;

    public AbstractKeyOp(MetaData<A> metaData) {
        super(metaData);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        this.keyExpression = spelParseService.getExpression(getKeyExpr());
    }

    protected ValueWrapper byte2ValueWrapper(byte[] bytes){
        if (bytes == null || bytes.length == 0){
            return null;
        }
        return new ValueWrapper(serializer.deserialize(bytes));
    }

    public byte[] getPrefixedKey(InvocationContext invocationContext){
        OpContext opContext = invocationContext.getOpContext(this);
        byte[] result = opContext.getPrefixedKey();
        if (result != null){
            return result;
        }
        EvaluationContext context = new ParamEvaluationContext(invocationContext.getMethodInvocation().getArguments());
        byte[] rawKey = serializer.serialize(keyExpression.getValue(context));
        byte[] prefixedKey = ByteArrayUtils.combine(cacheSpace.getVersion(cacheSpaceVersionKey), rawKey);
        opContext.setPrefixedKey(prefixedKey);
        return prefixedKey;
    }

    abstract protected String getKeyExpr();
}
