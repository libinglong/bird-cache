package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.spel.ParamEvaluationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/25
 */
public class OpHelper {

    public static Object getKey(InvocationContext invocationContext, Object op, Expression keyExpr){
        OpContext opContext = invocationContext.getOpContext(op);
        Object key = opContext.getKey();
        if (key != null){
            return key;
        }
        EvaluationContext context = new ParamEvaluationContext(invocationContext.getMethodInvocation().getArguments());
        return keyExpr.getValue(context);
    }



}
