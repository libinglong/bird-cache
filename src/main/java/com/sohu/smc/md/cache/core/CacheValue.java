package com.sohu.smc.md.cache.core;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/12/13
 */
@Data
@AllArgsConstructor
public class CacheValue {
    Long pttl;
    Object v;
}
