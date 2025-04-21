package com.bobby.rpc.core.common.codec.serializer;

import com.bobby.rpc.core.common.RpcRequest;
import com.bobby.rpc.core.common.RpcResponse;
import com.bobby.rpc.core.common.enums.MessageType;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ProtostuffSerializer implements ISerializer {

    // 重用缓冲区（线程安全）
    private static final LinkedBuffer BUFFER = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
    // Schema缓存
    private static final Map<Class<?>, Schema<?>> SCHEMA_CACHE = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private static <T> Schema<T> getSchema(Class<T> clazz) {
        return (Schema<T>) SCHEMA_CACHE.computeIfAbsent(clazz, RuntimeSchema::getSchema);
    }

    @Override
    public byte[] serialize(Object obj) {
        // 检查 null 对象
        if (obj == null) {
            throw new IllegalArgumentException("Cannot serialize null object");
        }
        try {
            Schema schema = getSchema(obj.getClass());
            return ProtostuffIOUtil.toByteArray(obj, schema, BUFFER);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        } finally {
            BUFFER.clear();
        }
    }

    @Override
    public Object deserialize(byte[] bytes, int messageType) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Cannot deserialize null or empty byte array");
        }
        if (messageType == MessageType.REQUEST.getCode()) {
            return deserializeObject(bytes, RpcRequest.class);
        } else if (messageType == MessageType.RESPONSE.getCode()) {
            return deserializeObject(bytes, RpcResponse.class);
        } else {
            throw new IllegalArgumentException("Unknown message type: " + messageType);
        }

    }

    private <T> T deserializeObject(byte[] bytes, Class<T> clazz) {
        Schema<T> schema = getSchema(clazz);
        T obj;
        try {
            obj = clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        }
        ProtostuffIOUtil.mergeFrom(bytes, obj, schema);
        return obj;
    }


    @Override
    public int getType() {
        return SerializerType.PROTOSTUFF.getCode();
    }
}
