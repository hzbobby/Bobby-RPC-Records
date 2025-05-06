package com.bobby.rpc.core.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.IOException;
import java.io.Serializable;

/**
 * 用于存储服务的元数据
 */
@Data
public class ServiceMetadata implements Serializable {
    private static final long serialVersionUID = 1L;
    public int weight;
    public boolean retryable;

    public ServiceMetadata() {
    }

    public ServiceMetadata(int weight, boolean retryable) {
        this.weight = weight;
        this.retryable = retryable;
    }

    public static byte[] serialize(ServiceMetadata metadata) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] bytes = objectMapper.writeValueAsBytes(metadata);
        return bytes;
    }

    public static ServiceMetadata deserialize(byte[] bytes) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ServiceMetadata serviceMetadata = objectMapper.readValue(bytes, ServiceMetadata.class);
        return serviceMetadata;
    }


//
//    public static void main(String[] args) throws IOException {
////        ServiceMetadata metadata = new ServiceMetadata();
////        metadata.weight = 100;
//
//        // serialize
//        byte[] serialize = ServiceMetadata.serialize(metadata);
//        ServiceMetadata deserialize = ServiceMetadata.deserialize(serialize);
//        System.out.println(deserialize);
//    }

}
