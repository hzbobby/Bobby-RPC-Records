package com.bobby.rpc.core.common.codec.serializer;


import com.bobby.rpc.core.common.RpcRequest;
import com.bobby.rpc.core.common.RpcResponse;
import com.bobby.rpc.core.common.enums.MessageType;
import com.bobby.rpc.core.common.protobuf.ProtobufUtil;
import com.bobby.rpc.core.common.protobuf.RpcRequestProto;
import com.bobby.rpc.core.common.protobuf.RpcResponseProto;

import java.io.IOException;

/**
 * Protobuf 序列化
 */
public class ProtobufSerializer implements ISerializer {
    @Override
    public byte[] serialize(Object obj) {
        // obj: RpcRequest 或 RpcResponse
        // 将 我的转为 protobuf 提供的
        try {
            if (obj instanceof RpcRequest) {
                RpcRequestProto.RpcRequest rpcRequest = ProtobufUtil.toRpcRequestProto((RpcRequest) obj);
                return rpcRequest.toByteArray();
            } else if (obj instanceof RpcResponse) {
                RpcResponseProto.RpcResponse rpcResponse = ProtobufUtil.toRpcResponseProto((RpcResponse) obj);
                return rpcResponse.toByteArray();
            } else {
                throw new RuntimeException("未知类型");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes, int messageType) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Cannot deserialize null or empty byte array");
        }
        // 传输的消息分为request与response
        if (MessageType.REQUEST.getCode() == messageType) {
            return handleRequest(bytes);
        } else if (MessageType.RESPONSE.getCode() == messageType) {
            return handleResponse(bytes);
        } else {
            System.out.println("暂时不支持此种消息");
            throw new RuntimeException("暂不支持此种类型的消息");
        }
    }

    private Object handleResponse(byte[] bytes) {
        RpcResponse rpcResponse = null;
        try {
            rpcResponse = ProtobufUtil.toRpcResponse(bytes);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return rpcResponse;
    }

    private Object handleRequest(byte[] bytes) {
        RpcRequest rpcRequest = null;
        try {
            rpcRequest = ProtobufUtil.toRpcRequest(bytes);
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
        return rpcRequest;
    }

    @Override
    public int getType() {
        return SerializerType.PROTOBUF.getCode();
    }

}
