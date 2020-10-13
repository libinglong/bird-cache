package com.sohu.smc.md.cache.spring;

import lombok.Builder;
import lombok.Data;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/29
 */
@Data
@Builder
public class CacheProperties {

    private Long expireTime;

}