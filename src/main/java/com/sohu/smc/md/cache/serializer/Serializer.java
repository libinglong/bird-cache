package com.sohu.smc.md.cache.serializer;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/9/30
 */
public interface Serializer {

    byte[] serialize(Object obj);

    Object deserialize(byte[] bytes);

}
