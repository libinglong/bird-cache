package com.sohu.smc.spring;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/30
 */
@Service
public class SpelParseService {

    private ConcurrentReferenceHashMap<String, Expression> expressionCache = new ConcurrentReferenceHashMap<>(256);

    private ExpressionParser parser = new SpelExpressionParser();

    public Expression getExpression(String expr) {
        Expression expression = expressionCache.get(expr);
        if (expression != null){
            return expression;
        }
        return expressionCache.computeIfAbsent(expr,exprTmp -> parser.parseExpression(exprTmp));
    }
}
