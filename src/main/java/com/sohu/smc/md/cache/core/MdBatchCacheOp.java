package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdBatchCache;
import com.sohu.smc.md.cache.spel.ParamEvaluationContext;
import com.sohu.smc.md.cache.spring.CacheProperties;
import com.sohu.smc.md.cache.spring.SpelParseService;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
public class MdBatchCacheOp extends AbstractKeyOp<MdBatchCache> {

    public MdBatchCacheOp(MetaData<MdBatchCache> metaData, Cache cache, CacheProperties cacheProperties,
                          SpelParseService spelParseService) {
        super(metaData, cache, cacheProperties, spelParseService);
    }

    private Expression listExpr;

    public List<BatchEntry> getBatchEntries(MethodInvocation methodInvocation){
        EvaluationContext context = new ParamEvaluationContext(methodInvocation.getArguments());
        List<?> list = listExpr.getValue(context, List.class);
        List<BatchEntry> batchEntries = new ArrayList<>();
        for (Object o : list){
            EvaluationContext ctx = new ParamEvaluationContext(methodInvocation.getArguments());
            ctx.setVariable("obj", o);
            Object keyObj = keyExpression.getValue(ctx);
            BatchEntry batchEntry = new BatchEntry();
            batchEntry.setOriginObj(o);
            batchEntry.setKeyObj(keyObj);
            batchEntries.add(batchEntry);
        }
        return batchEntries;
    }

    @Override
    protected String getKeyExpr() {
        return metaData.getAnno()
                .key();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        this.listExpr = spelParseService.getExpression("#p" + metaData.getAnno().index());
    }

    public List<ValueWrapper> getBatchCache(List<Object> keys) throws ExecutionException, InterruptedException {
        return cache.get(keys)
                .stream()
                .map(this::wrapper)
                .collect(Collectors.toList());
    }

    public int getListIndex() {
        return metaData.getAnno().index();
    }


    public List<?> processBatchCacheOp(InvocationContext invocationContext) throws Throwable {
        MethodInvocation methodInvocation = invocationContext.getMethodInvocation();
        List<BatchEntry> batchEntries = getBatchEntries(methodInvocation);
        List<Object> keyList = batchEntries.stream()
                .map(BatchEntry::getKeyObj)
                .collect(Collectors.toList());
        List<ValueWrapper> cacheList = getBatchCache(keyList);
        for (int i = 0; i < batchEntries.size(); i++) {
            batchEntries.get(i)
                    .setValueWrapper(cacheList.get(i));
        }
        fetchIfMissing(batchEntries, invocationContext);
        return batchEntries.stream()
                .map(batchEntry -> batchEntry.getValueWrapper().get())
                .collect(Collectors.toList());
    }

    private void fetchIfMissing(List<BatchEntry> batchEntries,InvocationContext invocationContext)
            throws Throwable {
        List<BatchEntry> missingBatchEntries = batchEntries.stream()
                .filter(batchEntry -> batchEntry.getValueWrapper() == null)
                .collect(Collectors.toList());
        if (missingBatchEntries.size() == 0){
            return;
        }
        List<Object> missingList = missingBatchEntries.stream()
                .map(BatchEntry::getOriginObj)
                .collect(Collectors.toList());
        invocationContext.getMethodInvocation()
                .getArguments()[getListIndex()] = missingList;
        Object result = invocationContext.doInvoke();
        List<?> missingValueList = (List<?>) result;
        for (int i = 0; i < missingBatchEntries.size(); i++) {
            missingBatchEntries.get(i)
                    .setValueWrapper(new ValueWrapper(missingValueList.get(i)));
        }
        Map<Object, Object> kvs = missingBatchEntries.stream()
                .collect(Collectors.toMap(BatchEntry::getKeyObj, batchEntry -> batchEntry.getValueWrapper().get()));
        cache.set(kvs, getExpiredTime());
    }

}
