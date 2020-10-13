package com.sohu.smc.md.cache.cache.impl.simple;

import com.sohu.smc.md.cache.serializer.PbSerializer;
import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/13
 */
public class PbObjectRedisCodec implements RedisCodec<Object,Object> {

    public static final PbObjectRedisCodec INSTANCE = new PbObjectRedisCodec();
    private static final byte[] EMPTY = new byte[0];

    private PbSerializer pbSerializer = new PbSerializer();

    @Override
    public Object decodeKey(ByteBuffer byteBuffer) {
        return pbSerializer.deserialize(getBytes(byteBuffer));
    }

    @Override
    public Object decodeValue(ByteBuffer byteBuffer) {
        return pbSerializer.deserialize(getBytes(byteBuffer));
    }

    @Override
    public ByteBuffer encodeKey(Object key) {
        if (key == null) {
            return ByteBuffer.wrap(EMPTY);
        }

        return ByteBuffer.wrap(pbSerializer.serialize(key));
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
