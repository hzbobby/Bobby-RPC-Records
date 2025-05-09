package com.bobby.rpc.core.common.loadbalance;

import com.bobby.rpc.core.common.ServiceMetadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 平滑的加权轮询
 * 模仿 dubbo 的实现
 * 思想：
 * 1. 维护服务里面，某个实例当前的权重。我们定义了 nodeMap 来维护服务->实例->WeightNode
 * 2. 整体思想是遍历选出给定实例里面，当前权重最大的实例。
 * 3. 在遍历过程中，动态增大当前权重：currentWeight += weight
 * 4. 遍历结束后，将选出的实例的当前权重-总权重: currentWeight -= totalWeight (平滑处理)，以便下次选出其他实例
 * <p>
 * p.s: 为了防止内存泄露，dubbo 里面多维护了 lastUpdateTime, 当超过一定时间 60s 没有被轮循到，会删除该实例
 */
public class WeightRoundRobinLoadBalance extends AbstractMetadataLoadBalance {

    private static final long RECYCLE_TIME = 60000L; // 60s

    // 内部类表示加权节点
    private static class WeightedNode {
        String address;
        int weight;          // 配置的权重
        final AtomicLong currentWeight;   // 当前权重
        long lastUpdateTime;

        WeightedNode(String address, int weight) {
            this.address = address;
            this.weight = weight;
            this.currentWeight = new AtomicLong(0L);
        }

        public long addAndGetWeight() {
            return currentWeight.addAndGet(weight);
        }

        public void minusWeight(long weight) {
            currentWeight.getAndAdd(-1 * weight);
        }

        public String getAddress() {
            return address;
        }
    }

    private final ConcurrentMap<String, ConcurrentMap<String, WeightedNode>> nodeMap = new ConcurrentHashMap<>();

    @Override
    public String balanceWithMetadata(Map<String, ServiceMetadata> instances, String servicePath) {
        // 一些初始化
        ConcurrentMap<String, WeightedNode> map = nodeMap.computeIfAbsent(servicePath, k -> new ConcurrentHashMap<>());
        long now = System.currentTimeMillis();
        long maxCurrent = Long.MIN_VALUE;
        WeightedNode selected = null;
        long totalWeight = 0L;
        String defaultAddress = null;
        // 遍历实例
        for (Map.Entry<String, ServiceMetadata> entry : instances.entrySet()) {
            String address = entry.getKey();
            defaultAddress = address;
            ServiceMetadata metadata = entry.getValue();

            int weight = metadata.getWeight();
            // 如果不存在，则添加
            WeightedNode weightedNode = map.computeIfAbsent(address, k -> {
                WeightedNode t = new WeightedNode(address, weight);
                t.lastUpdateTime = now;
                return t;
            });

            if (weightedNode.weight != weight) {
                // 说明发生了权重更新，这里要更新下缓存
                weightedNode.weight = weight;
            }

            // 当前权重
            // 一开始 current = 0 所以先 add 再 get
            long current = weightedNode.addAndGetWeight();
            weightedNode.lastUpdateTime = now;
            // 选出最大的那个
            if (current > maxCurrent) {
                maxCurrent = current;
                selected = weightedNode;
            }
            // 累加，获取总权重
            totalWeight += weight;
        }

        // 防止内存泄露，将故障的实例从缓存中删除
        if (map.size() != instances.size()) {
            map.entrySet().removeIf(entry -> now - entry.getValue().lastUpdateTime > RECYCLE_TIME);
        }

        // 对选出的实例的当前权重 - 总权重
        if (selected != null) {
            selected.minusWeight(totalWeight);
            return selected.getAddress();
        }
        // 理论上不会遍历到这里
        return defaultAddress;
    }

    public static void main(String[] args) {
        ILoadBalance loadBalance = new WeightRoundRobinLoadBalance();
        String servicePath = "demo";
        Map<String, ServiceMetadata> instances = new HashMap<>();
        ServiceMetadata sm = new ServiceMetadata(3,false);
        instances.put("demo/A", sm);

        sm = new ServiceMetadata(2,false);
        instances.put("demo/B", sm);

        sm = new ServiceMetadata(1,false);
        instances.put("demo/C", sm);
        // 轮询 60 次
        for (int i = 0; i < 60; i++) {
            String s = loadBalance.balanceWithMetadata(instances, servicePath);
            System.out.print(s + " ");
        }
    }

}
