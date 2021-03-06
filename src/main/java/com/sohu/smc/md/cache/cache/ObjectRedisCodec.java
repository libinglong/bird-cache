package com.sohu.smc.md.cache.cache;

import com.sohu.smc.md.cache.serializer.Serializer;
import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/13
 */
public class ObjectRedisCodec implements RedisCodec<Object,Object> {

    private static final byte[] EMPTY = new byte[0];

    private final Serializer serializer;

    public ObjectRedisCodec(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public Object decodeKey(ByteBuffer byteBuffer) {
        return serializer.deserialize(getBytes(byteBuffer));
    }

    @Override
    public Object decodeValue(ByteBuffer byteBuffer) {
        return serializer.deserialize(getBytes(byteBuffer));
    }

    @Override
    public ByteBuffer encodeKey(Object key) {
        if (key == null) {
            return ByteBuffer.wrap(EMPTY);
        }

        return ByteBuffer.wrap(serializer.serialize(key));
    }

    @Override
    public ByteBuffer encodeValue(Object value) {
        return encodeKey(value);
    }


    private static byte[] getBytes(ByteBuffer buffer) {

        int remaining = buffer.remaining();

        if (remaining == 0) {
            return EMPTY;
        }

        byte[] b = new byte[remaining];
        buffer.get(b);
        return b;
    }


}
