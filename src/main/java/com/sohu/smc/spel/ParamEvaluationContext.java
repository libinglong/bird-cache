package com.sohu.smc.spel;

import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/30
 */
public class ParamEvaluationContext extends StandardEvaluationContext {

    public ParamEvaluationContext(Object[] arguments) {
        for (int i = 0; i < arguments.length; i++) {
            setVariable("p" + i, arguments[i]);
        }
    }
}
