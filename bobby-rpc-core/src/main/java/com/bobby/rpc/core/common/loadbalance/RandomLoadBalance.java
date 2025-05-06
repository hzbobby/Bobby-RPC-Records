package com.bobby.rpc.core.common.loadbalance;


import com.bobby.rpc.core.common.ServiceMetadata;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 随机负载均衡
 */
public class RandomLoadBalance implements ILoadBalance {
    @Override
    public String balance(List<String> addressList) {
        Random random = new
                Random();
        int choose = random.nextInt(addressList.size());
        System.out.println("负载均衡选择了" + choose + "服务器");
        return addressList.get(choose);
    }

    @Override
    public String balanceWithMetadata(Map<String, ServiceMetadata> instances, String servicePath) {
        throw new RuntimeException("Not Implement.");
    }
}