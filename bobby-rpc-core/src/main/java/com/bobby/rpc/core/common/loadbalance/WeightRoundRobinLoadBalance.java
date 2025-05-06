package com.bobby.rpc.core.common.loadbalance;

import com.bobby.rpc.core.common.ServiceMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基本加权轮询
 * 例如 A(3), B(2), C(1)
 * 总权重为 6，轮询方式为 A A A B B C
 * <p>
 * 轮询思想
 * 1. 计算总权重值 totalWight
 * 2. index 对 totalWeight 取模
 * 3. 计算累加权重值
 * 3.1 index >= accWeight 已经轮询过该权重分布的实例
 * 3.2 index < accWeight 还未轮询过当前实例
 */
public class WeightRoundRobinLoadBalance implements ILoadBalance {
    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public String balance(List<String> addressList) {
        throw new RuntimeException("Not Implement.");

    }

    @Override
    public String balanceWithMetadata(Map<String, ServiceMetadata> instances, String servicePath) {
        // 权重列表长度与 addressList 长度相同
        if (addressList == null || addressList.isEmpty()) {
            throw new IllegalArgumentException("Address list cannot be empty");
        }
        if (addressList.size() != weights.size()) {
            throw new IllegalArgumentException("Address list and weights size mismatch");
        }

        // 计算总权重
        int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) {
            throw new IllegalStateException("Total weight must be positive");
        }

        int currentIndex = index.getAndIncrement() % totalWeight;
        int accWeight = 0;
        for (int i = 0; i < addressList.size(); i++) {
            accWeight += weights.get(i);
            if (currentIndex < accWeight) {
                return addressList.get(i);
            }
        }

        return addressList.get(0);
    }

    public static void main(String[] args) {
        List<Integer> weights = new ArrayList<>() {{
            add(3);
            add(2);
            add(1);
        }};
        List<String> addressList = new ArrayList<>() {{
            add("A");
            add("B");
            add("C");
        }};

        ILoadBalance loadBalance = new WeightRoundRobinLoadBalance(weights);

        Map<String, Integer> cnts = new HashMap<>();
        for (int i = 0; i < 60; i++) {
            String instance = loadBalance.balance(addressList);
            cnts.put(instance, cnts.getOrDefault(instance, 0) + 1);
            System.out.print(instance + " ");
        }
        System.out.println();

        for (Map.Entry<String, Integer> entry : cnts.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}
