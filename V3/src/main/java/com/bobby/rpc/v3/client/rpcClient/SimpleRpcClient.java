package com.bobby.rpc.v3.client.rpcClient;

import com.bobby.rpc.v3.common.RpcRequest;
import com.bobby.rpc.v3.common.RpcResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SimpleRpcClient {
    public static RpcResponse sendRequest(String host, int port, RpcRequest request) {
        try {
            Socket socket = new Socket(host, port);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

            // 发送请求
            objectOutputStream.writeObject(request);
            // 获取响应
            RpcResponse response = (RpcResponse) objectInputStream.readObject();
            return response;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("SimpleRpcClient, sendRequest Exception: "+e.getMessage());
            return null;
        }
    }
}