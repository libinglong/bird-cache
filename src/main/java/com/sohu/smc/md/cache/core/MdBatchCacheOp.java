package com.sohu.smc.md.cache.core;

import com.sohu.smc.md.cache.anno.MdBatchCache;
import com.sohu.smc.md.cache.spel.ParamEvaluationContext;
import com.sohu.smc.md.cache.util.ByteArrayUtils;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.stereotype.Component;

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
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MdBatchCacheOp extends AbstractKeyOp<MdBatchCache> {

    public MdBatchCacheOp(MetaData<MdBatchCache> metaData) {
        super(metaData);
    }

    private Expression listExpr;

    public List<BatchEntry> getBatchEntries(MethodInvocation methodInvocation){
        EvaluationContext context = new ParamEvaluationContext(methodInvocation.getArguments());
        List<?> list = listExpr.getValue(context, List.class);
        List<BatchEntry> batchEntries = new ArrayList<>();
        for (Object o : list){
            EvaluationContext ctx = new ParamEvaluationContext(methodInvocation.getArguments());
            ctx.setVariable("obj", o);
            byte[] rawKey = serializer.serialize(keyExpression.getValue(ctx));
            byte[] prefixedKey = ByteArrayUtils.combine(cacheSpace.getVersion(cacheSpaceVersionKey), rawKey);
            BatchEntry batchEntry = new BatchEntry();
            batchEntry.setKeyObj(o);
            batchEntry.setPrefixedKey(prefixedKey);
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

    public List<ValueWrapper> getBatchCache(List<byte[]> keys) throws ExecutionException, InterruptedException {
        return cache.get(keys)
                .stream()
                .map(this::byte2ValueWrapper)
                .collect(Collectors.toList());
    }

    public int getListIndex() {
        return metaData.getAnno().index();
    }


    public List<?> processBatchCacheOp(InvocationContext invocationContext) throws Throwable {
        MethodInvocation methodInvocation = invocationContext.getMethodInvocation();
        if (!List.class.isAssignableFrom(methodInvocation.getMethod().getReturnType())){
            throw new UnsupportedOperationException("the method returnType must be List");
        }
        List<BatchEntry> batchEntries = getBatchEntries(methodInvocation);
        List<byte[]> byteKeyList = batchEntries.stream()
                .map(BatchEntry::getPrefixedKey)
                .collect(Collectors.toList());
        List<ValueWrapper> cacheList = getBatchCache(byteKeyList);
        for (int i = 0; i < batchEntries.size(); i++) {
            batchEntries.get(i)
                    .setValueWrapper(cacheList.get(i));
        }
        fetchIfMissing(batchEntries, methodInvocation);
        return batchEntries.stream()
                .map(batchEntry -> batchEntry.getValueWrapper().get())
                .collect(Collectors.toList());
    }

    private void fetchIfMissing(List<BatchEntry> batchEntries,MethodInvocation methodInvocation)
            throws Throwable {
        List<BatchEntry> missingBatchEntries = batchEntries.stream()
                .filter(batchEntry -> batchEntry.getValueWrapper() == null)
                .collect(Collectors.toList());
        if (missingBatchEntries.size() == 0){
            return;
        }
        List<Object> missingList = missingBatchEntries.stream()
                .map(BatchEntry::getKeyObj)
                .collect(Collectors.toList());
        methodInvocation.getArguments()[getListIndex()] = missingList;
        Object result = methodInvocation.proceed();
        List<?> missingValueList = (List<?>) result;
        for (int i = 0; i < missingBatchEntries.size(); i++) {
            missingBatchEntries.get(i)
                    .setValueWrapper(new ValueWrapper(missingValueList.get(i)));
        }
        Map<byte[], byte[]> kvs = missingBatchEntries.stream()
                .collect(Collectors.toMap(BatchEntry::getPrefixedKey, this::serialize));
        cache.set(kvs, cacheProperties.getExpireTime());
    }

    private byte[] serialize(BatchEntry batchEntry){
        Object o = batchEntry.getValueWrapper()
                .get();
        return serializer.serialize(o);
    }

}
