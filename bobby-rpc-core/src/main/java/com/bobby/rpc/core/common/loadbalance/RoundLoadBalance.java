package com.bobby.rpc.core.common.loadbalance;


import java.util.List;

/**
 * 轮询负载均衡
 */
public class RoundLoadBalance extends AbstractLoadBalance {
    private int choose = -1;

    @Override
    public String balance(List<String> addressList) {
        choose++;
        choose = choose % addressList.size();
        return addressList.get(choose);
    }
}