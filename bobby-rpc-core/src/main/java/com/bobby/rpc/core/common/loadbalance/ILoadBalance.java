package com.bobby.rpc.core.common.loadbalance;

import com.bobby.rpc.core.common.ServiceMetadata;

import java.util.List;
import java.util.Map;

/**
 * 给服务器地址列表，根据不同的负载均衡策略选择一个
 */
public interface ILoadBalance {
    String balance(List<String> addressList);

    String balanceWithMetadata(Map<String, ServiceMetadata> instances, String servicePath);
}