package com.sohu.smc.md.cache.cache;

import lombok.Getter;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/12
 */
@Getter
public class SpaceWrapper {

    String cacheSpaceName;
    Object version;
    Object key;

    public SpaceWrapper(String cacheSpaceName, Object version, Object key) {
        this.cacheSpaceName = cacheSpaceName;
        this.version = version;
        this.key = key;
    }
}
