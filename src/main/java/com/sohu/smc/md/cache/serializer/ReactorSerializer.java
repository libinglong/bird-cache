package com.sohu.smc.md.cache.serializer;


import org.springframework.util.Assert;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/12/7
 */
public class ReactorSerializer implements Serializer {

    private Serializer delegate;

    public ReactorSerializer(Serializer delegate) {
        this.delegate = delegate;
    }

    @Override
    public byte[] serialize(Object obj) {
        Assert.notNull(obj, "null value is not supported");
        return delegate.serialize(obj);
    }

    @Override
    public Object deserialize(byte[] bytes) {
        return delegate.deserialize(bytes);
    }
}
