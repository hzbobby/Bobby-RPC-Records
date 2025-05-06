package com.bobby.rpc.core.common.loadbalance;

import com.bobby.rpc.core.client.cache.ServiceCache;
import com.bobby.rpc.core.common.ServiceMetadata;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 平滑的加权轮询
 * 思想：
 * 1. 维护一个初始配置权重和当前权重
 * 2. 对所有实例的当前权重 + 配置权重
 * 3. 每一轮负载选出当前权重最大的实例
 * 4. 将选中的实例，当前权重-总权重
 */
public class SmoothWeightRoundRobinLoadBalance implements ILoadBalance {

    // 内部类表示加权节点
    private static class WeightedNode {
        String address;
        int weight;          // 配置的权重
        int currentWeight;   // 当前权重

        WeightedNode(String address, int weight) {
            this.address = address;
            this.weight = weight;
            this.currentWeight = 0;
        }
    }

    private final ConcurrentMap<String, WeightedNode> nodeMap = new ConcurrentHashMap<>();
    private final AtomicInteger totalWeight = new AtomicInteger(0);

    public SmoothWeightRoundRobinLoadBalance(ServiceCache serviceCache) {

    }


    @Override
    public String balance(List<String> addressList) {
        throw new RuntimeException("Not Implement.");
    }

    @Override
    public String balanceWithMetadata(Map<String, ServiceMetadata> instances, String servicePath) {
        // 一些初始化
        if () {}
        return "";
    }

}
