package com.bobby.rpc.core.common.loadbalance;

import com.bobby.rpc.core.common.RpcRequest;
import com.bobby.rpc.core.common.ServiceMetadata;

import java.util.List;
import java.util.Map;

public abstract class AbstractLoadBalance implements ILoadBalance {

    public abstract String balance(List<String> addressList);

    @Override
    public String balanceWithMetadata(Map<String, ServiceMetadata> instances, String servicePath) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String balanceWithRequest(Map<String, ServiceMetadata> instances, RpcRequest rpcRequest) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
