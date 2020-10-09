package com.sohu.smc.md.cache.serializer;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;



/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/4/13
 */
public class PbSerializer implements Serializer {

    @Override
    public byte[] serialize(Object obj) {
        Wrapper wrapper = new Wrapper();
        wrapper.setObj(obj);
        Schema<Wrapper> schema = RuntimeSchema.getSchema(Wrapper.class);
        LinkedBuffer buffer = LinkedBuffer.allocate(512);
        final byte[] protostuff;
        try {
            protostuff = ProtostuffIOUtil.toByteArray(wrapper, schema, buffer);
        } finally {
            buffer.clear();
        }
        return protostuff;
    }

    @Override
    public Object deserialize(byte[] bytes) {
        Schema<Wrapper> schema = RuntimeSchema.getSchema(Wrapper.class);
        Wrapper wrapper = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(bytes,wrapper,schema);
        return wrapper.getObj();
    }
}
