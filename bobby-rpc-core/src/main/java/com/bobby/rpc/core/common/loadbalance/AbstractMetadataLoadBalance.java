package com.bobby.rpc.core.common.loadbalance;

import com.bobby.rpc.core.common.RpcRequest;
import com.bobby.rpc.core.common.ServiceMetadata;

import java.util.List;
import java.util.Map;

public abstract class AbstractMetadataLoadBalance implements ILoadBalance {
    @Override
    public String balance(List<String> addressList) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public abstract String balanceWithMetadata(Map<String, ServiceMetadata> instances, String servicePath);

    @Override
    public String balanceWithRequest(Map<String, ServiceMetadata> instances, RpcRequest rpcRequest) {


        throw new UnsupportedOperationException("Not supported yet.");
    }
}
