package com.bobby.rpc.core.server.register;

import com.bobby.rpc.core.common.ServiceMetadata;

import java.net.InetSocketAddress;

// 服务注册接口，两大基本功能，注册：保存服务与地址。 查询：根据服务名查找地址
public interface IServiceRegister {
    void register(String serviceName, InetSocketAddress serverAddress, boolean retryable);

    void registerWithMetadata(String serviceName, InetSocketAddress serverAddress, ServiceMetadata metadata);


}