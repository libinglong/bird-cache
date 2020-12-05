package com.sohu.smc.md.cache.cache;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/26
 */
@Builder
@Getter
@ToString
public class SyncOp {

    private Op op;
    private String cacheSpaceName;
    private Object key;
    private Object value;

    public enum Op {
        Clear,
        Evict,
        Put,
        ;
    }

}
