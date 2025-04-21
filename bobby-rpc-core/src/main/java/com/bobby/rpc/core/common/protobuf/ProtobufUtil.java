package com.bobby.rpc.core.common.protobuf;

import com.bobby.rpc.core.common.RpcRequest;
import com.bobby.rpc.core.common.RpcResponse;
import com.google.protobuf.ByteString;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 类型转换的工具类
 */
public class ProtobufUtil {

    // Object <--> ByteString
    public static ByteString objectToByteString(Object obj) throws IOException {
        if (obj == null) return ByteString.EMPTY;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.flush();
        return ByteString.copyFrom(bos.toByteArray());
    }

    public static Object byteStringToObject(ByteString byteString) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(byteString.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }

    public static List<ByteString> objectArrayToByteStringList(Object[] objs) throws IOException {
        List<ByteString> list = new ArrayList<>();
        for (Object o : objs) {
            list.add(objectToByteString(o));
        }
        return list;
    }

    public static Object[] byteStringListToObjectArray(List<ByteString> byteStringList) throws IOException, ClassNotFoundException {
        Object[] objs = new Object[byteStringList.size()];
        for (int i = 0; i < byteStringList.size(); i++) {
            objs[i] = byteStringToObject(byteStringList.get(i));
        }
        return objs;
    }


    // String <--> Clazz
    public static String clazzToString(Class<?> clazz) {
        return clazz.getName();
    }

    public static Class<?> stringToClass(String clazz) throws ClassNotFoundException {
        return Class.forName(clazz);
    }

    public static List<String> clazzArrayToStringList(Class<?>[] clazzArray) {
        List<String> list = new ArrayList<>();
        for (Class<?> clazz : clazzArray) {
            list.add(clazzToString(clazz));
        }
        return list;
    }

    public static Class<?>[] stringListToClassArray(List<String> stringList) throws ClassNotFoundException {
        Class<?>[] clazzArray = new Class<?>[stringList.size()];
        for (int i = 0; i < stringList.size(); i++) {
            clazzArray[i] = stringToClass(stringList.get(i));
        }
        return clazzArray;
    }


    public static RpcRequestProto.RpcRequest toRpcRequestProto(RpcRequest rpcRequest) throws IOException {
        return RpcRequestProto.RpcRequest.newBuilder()
                .setInterfaceName(rpcRequest.getInterfaceName())
                .setMethodName(rpcRequest.getMethodName())
                .addAllParams(objectArrayToByteStringList(rpcRequest.getParams()))
                .addAllParamsTypes(clazzArrayToStringList(rpcRequest.getParamsTypes()))
                .setType(RpcRequestProto.RpcRequest.RequestType.forNumber(rpcRequest.getType().getCode()))
                .build();
    }

    public static RpcResponseProto.RpcResponse toRpcResponseProto(RpcResponse rpcResponse) throws IOException {
        return RpcResponseProto.RpcResponse.newBuilder()
                .setCode(rpcResponse.getCode())
                .setMessage(rpcResponse.getMessage())
                .setData(objectToByteString(rpcResponse.getData()))
                .setDataType(clazzToString(rpcResponse.getDataType()))
                .build();
    }


    public static RpcRequest toRpcRequest(byte[] bytes) throws IOException, ClassNotFoundException {
        RpcRequestProto.RpcRequest protoRequest = RpcRequestProto.RpcRequest.parseFrom(bytes);
        Object[] params = byteStringListToObjectArray(protoRequest.getParamsList());
        Class<?>[] paramTypes = stringListToClassArray(protoRequest.getParamsTypesList());

        return RpcRequest.builder()
                .interfaceName(protoRequest.getInterfaceName())
                .methodName(protoRequest.getMethodName())
                .params(params)
                .paramsTypes(paramTypes)
                .type(RpcRequest.RequestType.fromCode(protoRequest.getTypeValue()))
                .build();
    }

    public static RpcResponse toRpcResponse(byte[] bytes) throws IOException, ClassNotFoundException {
        RpcResponseProto.RpcResponse protoRequest = RpcResponseProto.RpcResponse.parseFrom(bytes);
        Object data = byteStringToObject(protoRequest.getData());
        Class<?> clazz = stringToClass(protoRequest.getDataType());
        return RpcResponse.builder()
                .code(protoRequest.getCode())
                .message(protoRequest.getMessage())
                .data(data)
                .dataType(clazz)
                .build();
    }

}
