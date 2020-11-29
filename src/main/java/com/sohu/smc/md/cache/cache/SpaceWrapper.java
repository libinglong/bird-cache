package com.sohu.smc.md.cache.cache;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/12
 */
public class SpaceWrapper {

    String cacheSpaceName;
    Object key;

    public SpaceWrapper(String cacheSpaceName, Object key) {
        this.cacheSpaceName = cacheSpaceName;
        this.key = key;
    }
}
