package com.bobby.rpc.core.common.loadbalance;


import com.bobby.rpc.core.common.ServiceMetadata;

import java.util.List;
import java.util.Map;

/**
 * 轮询负载均衡
 */
public class RoundLoadBalance implements ILoadBalance {
    private int choose = -1;

    @Override
    public String balance(List<String> addressList) {
        choose++;
        choose = choose % addressList.size();
        return addressList.get(choose);
    }

    @Override
    public String balanceWithMetadata(Map<String, ServiceMetadata> instances, String servicePath) {
        throw new RuntimeException("Not Implement.");
    }
}