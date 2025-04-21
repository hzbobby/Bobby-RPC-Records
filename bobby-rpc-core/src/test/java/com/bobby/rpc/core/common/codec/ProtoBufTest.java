package com.bobby.rpc.core.common.codec;

import com.bobby.rpc.core.common.RpcRequest;
import com.bobby.rpc.core.common.protobuf.ProtobufUtil;
import com.bobby.rpc.core.common.protobuf.RpcRequestProto;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ProtoBufTest {
    @Test
    public void test() throws InvalidProtocolBufferException {
        // 序列化
        RpcRequestProto.RpcRequest rpcRequest = RpcRequestProto.RpcRequest.newBuilder()
                .setInterfaceName("com.bobby.rpc.core.sample.service.IUserService")
                .setMethodName("getUser")
                .addParams(ByteString.copyFromUtf8("5"))
                .addParamsTypes(Long.class.getName())
                .setType(RpcRequestProto.RpcRequest.RequestType.NORMAL)
                .build();

        byte[] byteArray = rpcRequest.toByteArray();

        // 反序列化
        RpcRequestProto.RpcRequest dec = RpcRequestProto.RpcRequest.parseFrom(byteArray);

        System.out.println(dec);

        // 进行断言验证
        assertNotNull(dec, "反序列化后的 RpcRequest 不应为空");
        assertEquals("com.bobby.rpc.core.sample.service.IUserService", dec.getInterfaceName());
        assertEquals("getUser", dec.getMethodName());
        assertEquals(1, dec.getParamsCount());
        assertEquals(ByteString.copyFromUtf8("5"), dec.getParams(0));
        assertEquals(1, dec.getParamsTypesCount());
        assertEquals(Long.class.getName(), dec.getParamsTypes(0));
        assertEquals(RpcRequestProto.RpcRequest.RequestType.NORMAL, dec.getType());
    }

    @Test
    public void testTypeCast() throws IOException, ClassNotFoundException {
        RpcRequest rpcRequest = RpcRequest.builder()
                .interfaceName("com.bobby.rpc.core.sample.service.IUserService")
                .methodName("getUser")
                .params(new Object[]{5L})
                .paramsTypes(new Class[]{Long.class})
                .type(RpcRequest.RequestType.NORMAL)
                .build();

        RpcRequestProto.RpcRequest rpcRequestProto = ProtobufUtil.toRpcRequestProto(rpcRequest);
        RpcRequest rpcRequest1 = ProtobufUtil.toRpcRequest(rpcRequestProto.toByteArray());
        System.out.println(rpcRequestProto);
        System.out.println(rpcRequest1);
    }

}
