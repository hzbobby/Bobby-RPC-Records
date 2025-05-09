package com.bobby.rpc.core.common.loadbalance;

import com.bobby.rpc.core.common.RpcRequest;
import com.bobby.rpc.core.common.ServiceMetadata;

import java.util.List;
import java.util.Map;

public abstract class AbstractHashLoadBalance implements ILoadBalance {

    @Override
    public String balance(List<String> addressList) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String balanceWithMetadata(Map<String, ServiceMetadata> instances, String servicePath) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public abstract String balanceWithRequest(Map<String, ServiceMetadata> instances, RpcRequest rpcRequest);
}
