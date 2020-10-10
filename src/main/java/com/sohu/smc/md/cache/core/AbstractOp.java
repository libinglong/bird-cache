package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.serializer.Serializer;
import com.sohu.smc.md.cache.spring.CacheProperties;
import com.sohu.smc.md.cache.spring.SpelParseService;
import com.sohu.smc.md.cache.util.PrefixedKeyUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/30
 */
public abstract class AbstractOp<A extends Annotation> implements InitializingBean {

    protected byte[] prefix;

    protected Expression keyExpression;

    protected MetaData<A> metaData;

    @Autowired
    protected SpelParseService spelParseService;

    @Autowired
    protected Cache cache;

    @Autowired
    protected Serializer serializer;

    @Autowired
    protected CacheProperties cacheProperties;

    public AbstractOp(MetaData<A> metaData) {
        this.metaData = metaData;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String clsName = this.metaData.getMethod()
                .getDeclaringClass()
                .getName();
        this.prefix = clsName.getBytes(StandardCharsets.UTF_8);
        getKeyExpr();
        String keyExpr = getKeyExpr();
        this.keyExpression = spelParseService.getExpression(keyExpr);
    }

    protected ValueWrapper byte2ValueWrapper(byte[] bytes){
        if (bytes == null || bytes.length == 0){
            return null;
        }
        return new ValueWrapper(serializer.deserialize(bytes));
    }

    public byte[] getPrefixedKey(InvocationContext invocationContext){
        OpContext opContext = invocationContext.getOpContext(this);
        if (opContext.getPrefixedKey() != null){
            return opContext.getPrefixedKey();
        }
        EvaluationContext context = new StandardEvaluationContext(invocationContext.getMethodInvocation().getArguments());
        byte[] rawKey = serializer.serialize(keyExpression.getValue(context));
        byte[] prefixedKey = PrefixedKeyUtils.getPrefixedKey(prefix, rawKey);
        opContext.setPrefixedKey(prefixedKey);
        return prefixedKey;
    }

    abstract protected String getKeyExpr();
}
