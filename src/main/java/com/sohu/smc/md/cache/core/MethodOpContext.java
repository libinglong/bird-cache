package com.sohu.smc.md.cache.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/12
 */
@Getter
@Setter
public class MethodOpContext {

    List<MdCacheEvictOp> evictOps;
    List<MdCachePutOp> putOps;
    MdCacheableOp cacheableOp;
    MdBatchCacheOp batchCacheOp;
    MdCacheClearOp clearOp;

    public void validate(){
        boolean twoCacheOpsExist = cacheableOp != null && batchCacheOp != null;
        Assert.isTrue(!twoCacheOpsExist, "MdCacheable and MdBatchCacheOp can not exist at the same time");
        boolean evictAndClearOpExist = (!CollectionUtils.isEmpty(evictOps)) && clearOp != null;
        Assert.isTrue(!evictAndClearOpExist, "MdCacheEvict and MdCacheClear can not exist at the same time");
    }

    public boolean hasAnyOp(){
        return (!CollectionUtils.isEmpty(evictOps))
                || (!CollectionUtils.isEmpty(putOps))
                || cacheableOp != null
                || batchCacheOp != null
                || clearOp != null;
    }

}
