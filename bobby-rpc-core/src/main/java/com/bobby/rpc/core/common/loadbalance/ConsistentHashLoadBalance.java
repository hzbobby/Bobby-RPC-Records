package com.bobby.rpc.core.common.loadbalance;

import com.bobby.rpc.core.common.RpcRequest;
import com.bobby.rpc.core.common.ServiceMetadata;
import io.protostuff.Rpc;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 1. 制定一个 hash 环
 * 2. 为了隔离服务，创建了一个 map 用于区分不同服务的负载均衡实例
 * 3. 需要在map中缓存实例节点，这个由 ConsistentHashSelector 来维护
 * 4.
 */
public class ConsistentHashLoadBalance extends AbstractHashLoadBalance {
    // 默认每个真实节点对应的虚拟节点数量
    private static final int DEFAULT_REPLICA_NUMBER = 160;

    private final ConcurrentMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    private static class ConsistentHashSelector {
        /**
         * 由这个类来维护一个服务的一致性hash选择
         */

        // 虚拟节点环，存储哈希值到真实节点的映射
        private final TreeMap<Long, String> virtualNodes;

        // 记录 instatnce 列表的 hashcode, 防止 instances 发生修改
        private final int identityHashCode;

        // 每个真实节点对应的虚拟节点数量
        private final int replicaNumber;

        // 用于计算哈希值的方法参数索引
        private final int[] argumentIndex;

        public ConsistentHashSelector(Map<String, ServiceMetadata> instances, int identityHashCode) {
            // 初始化过程：
            this.virtualNodes = new TreeMap<>();
            this.identityHashCode = identityHashCode;
            this.replicaNumber = DEFAULT_REPLICA_NUMBER;
            this.argumentIndex = new int[]{0}; // 默认使用第0个参数作为hash参数
            // 构建虚拟节点环
            for (Map.Entry<String, ServiceMetadata> entry : instances.entrySet()) {
                String address = entry.getKey();
                // 为每个真实节点创建多个虚拟节点
                // replicaNumber / 4 为了减少 MD5 的计算
                // reason: 一个 MD5 是 16 字节，一个 hash 码，这里只用到了 4 字节， 因此生成一个 md5 可以 复用
                for (int i = 0; i < replicaNumber / 4; i++) {
                    // 使用MD5算法生成哈希值
                    // key: adress编码，虚拟节点的 key 就是 adress+编号
                    byte[] digest = getMD5(address + i);
                    // 每个MD5哈希值可以生成4个long型哈希值
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        virtualNodes.put(m, address);
                    }
                }
            }
            // hash 码长度是 4 字节，相当于hash环的大小为 0-无符号 int 最大值


        }

        // 对 参数进行拼接，计算 hash key
        private String buildKey(RpcRequest rpcRequest) {
            // 默认使用第一个参数作为哈希键
            Object[] args = rpcRequest.getParams();
            if (args == null || args.length == 0) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            for (int index : argumentIndex) {
                if (index >= 0 && index < args.length) {
                    sb.append(args[index]);
                }
            }
            return sb.toString();
        }

        public String select(RpcRequest rpcRequest) {
            // 获取请求参数的哈希值
            String key = buildKey(rpcRequest);
            byte[] digest = getMD5(key);
            long hash = hash(digest, 0);

            // 在虚拟节点环上查找最近的节点
            // 找打比它大的第一个节点
            Map.Entry<Long, String> entry = virtualNodes.ceilingEntry(hash);
            if (entry == null) {
                entry = virtualNodes.firstEntry();
            }
            return entry.getValue();
        }

        /**
         * 将 md5 16字节，划分成 4 份，并将一个 4 字节转成 long 型 hash 码
         * @param digest
         * @param number
         * @return
         */
        private long hash(byte[] digest, int number) {
            return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                    | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                    | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                    | (digest[number * 4] & 0xFF))
                    & 0xFFFFFFFFL;
        }
        // 获取输入 key 的 md5
        private byte[] getMD5(String key) {
            // 这里应该实现MD5算法，可以使用MessageDigest
            // 简化实现，实际项目中应该使用标准库
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
                return md.digest(key.getBytes("UTF-8"));
            } catch (Exception e) {
                throw new RuntimeException("MD5 not supported", e);
            }
        }

    }


    @Override
    public String balanceWithRequest(Map<String, ServiceMetadata> instances, RpcRequest rpcRequest) {
        String servicePath = "/" + rpcRequest.getInterfaceName();
        ConsistentHashSelector selector = selectors.get(servicePath);
        int hashCode = instances.hashCode();
        if (selector == null || selector.identityHashCode != hashCode) {
            // 初始化 selector
            selectors.put(servicePath, new ConsistentHashSelector(instances, hashCode));
            selector = selectors.get(servicePath);
        }
        return selector.select(rpcRequest);
    }


    public static void main(String[] args) {
// 准备3个服务实例
        Map<String, ServiceMetadata> instances = new HashMap<>();
        instances.put("192.168.1.1:8080", new ServiceMetadata());
        instances.put("192.168.1.2:8080", new ServiceMetadata());
        instances.put("192.168.1.3:8080", new ServiceMetadata());

        // 创建负载均衡器
        ConsistentHashLoadBalance loadBalance = new ConsistentHashLoadBalance();

        // 创建测试请求
        RpcRequest request = RpcRequest.builder()
                .interfaceName("demoService")
                .params(new Object[]{5L})
                .paramsTypes(new Class[]{Long.class})
                .build();

        // 多次请求，验证是否总是路由到同一个节点(相同参数)
        String firstSelection = loadBalance.balanceWithRequest(instances, request);
        System.out.println("第一次选择节点: " + firstSelection);

        for (int i = 0; i < 5; i++) {
            String selected = loadBalance.balanceWithRequest(instances, request);
            System.out.println("第" + (i+1) + "次选择: " + selected);
            if (!selected.equals(firstSelection)) {
                System.err.println("错误: 相同参数请求路由到了不同节点!");
            }
        }

        // 创建测试请求
        RpcRequest request2 = RpcRequest.builder()
                .interfaceName("demoService")
                .params(new Object[]{4L})
                .paramsTypes(new Class[]{Long.class})
                .build();

        System.out.println(loadBalance.balanceWithRequest(instances, request2));
    }
}
