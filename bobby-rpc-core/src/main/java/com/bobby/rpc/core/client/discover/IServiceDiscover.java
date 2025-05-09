package com.bobby.rpc.core.client.discover;

import com.bobby.rpc.core.common.RpcRequest;

import java.net.InetSocketAddress;

// 服务发现
public interface IServiceDiscover {
    InetSocketAddress serviceDiscovery(String serviceName);

    InetSocketAddress discoveryWithMetadata(String serviceName);

    InetSocketAddress discovery(RpcRequest rpcRequest);
    // 服务级别的重试机制
    boolean retryable(String serviceName);
//
////    // 方法级别的重试机制
////    boolean retryable(String serviceName);
}
