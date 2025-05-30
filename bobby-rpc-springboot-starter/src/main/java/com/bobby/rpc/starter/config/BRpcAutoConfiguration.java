package com.bobby.rpc.starter.config;

import com.bobby.rpc.core.client.circuitBreaker.CircuitBreakerProvider;
import com.bobby.rpc.core.client.discover.IServiceDiscover;
import com.bobby.rpc.core.client.discover.RpcReferenceProcessor;
import com.bobby.rpc.core.client.discover.impl.ZkServiceDiscover;
import com.bobby.rpc.core.client.proxy.ClientProxy;
import com.bobby.rpc.core.client.rpcClient.IRpcClient;
import com.bobby.rpc.core.client.rpcClient.impl.NettyRpcClient;
import com.bobby.rpc.core.common.loadbalance.ILoadBalance;
import com.bobby.rpc.core.common.loadbalance.RoundLoadBalance;
import com.bobby.rpc.core.common.loadbalance.WeightRoundRobinLoadBalance;
import com.bobby.rpc.core.common.resolver.RpcWeightResolver;
import com.bobby.rpc.core.server.provider.ServiceProvider;
import com.bobby.rpc.core.server.ratelimit.provider.RateLimitProvider;
import com.bobby.rpc.core.server.register.IServiceRegister;
import com.bobby.rpc.core.server.register.RpcServiceProcessor;
import com.bobby.rpc.core.server.register.impl.ZkServiceRegister;
import com.bobby.rpc.core.server.rpcServer.IRpcServer;
import com.bobby.rpc.core.server.rpcServer.impl.NettyRpcServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;

import java.net.InetSocketAddress;

import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;

/**
 * @author: Bobby
 * @email: vividbobby@163.com
 * @date: 2025/4/6
 */
@EnableConfigurationProperties(value = {BRpcProperties.class})
@Configuration
@Slf4j
public class BRpcAutoConfiguration {
    // 在这个配置项里面，创建相关的 bean 对象

    private final BRpcProperties brpcProperties;

    public BRpcAutoConfiguration(BRpcProperties brpcProperties) {
        this.brpcProperties = brpcProperties;
    }

    // zk client
    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public CuratorFramework zkClient() {
        log.info("Create bean of CuratorFramework zkClient");
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(
                brpcProperties.getZk().getRetry().getMaxRetries(),
                brpcProperties.getZk().getRetry().getMaxRetries()
        );

        try (CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(brpcProperties.getZk().getAddress())   // zk 服务地址 host:port
//                .connectString("192.168.160.128:2181")   // zk 服务地址 host:port
                .sessionTimeoutMs(brpcProperties.getZk().getSessionTimeoutMs())
                .retryPolicy(retryPolicy)
                .namespace(brpcProperties.getZk().getNamespace())
                .build()) {
//            client.start();
            return client;
        } catch (Exception e) {
            log.error("zk client create error", e);
            throw new RuntimeException("zk client create error", e);
        }
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public IServiceRegister serviceRegister(CuratorFramework client) {
        log.info("Create bean of IServiceRegister serviceRegister");
        return new ZkServiceRegister(client);
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public RateLimitProvider rateLimitProvider() {
        log.info("Create bean of RateLimitProvider rateLimitProvider");
        return new RateLimitProvider();
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public ServiceProvider serviceProvider(IServiceRegister serviceRegister, RateLimitProvider rateLimitProvider) {
        log.info("Create bean of ServiceProvider serviceProvider");

        // 本机 ip + 指定 netty 通信的端口
        return new ServiceProvider(serviceRegister, new InetSocketAddress("127.0.0.1", brpcProperties.getNetty().getPort()), rateLimitProvider);
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public IRpcServer rpcServer(ServiceProvider serviceProvider) {
        log.info("Create bean of IRpcServer rpcServer");
        log.debug("RpcServer Serializer: {}", brpcProperties.getNetty().getSerializer());

        NettyRpcServer nettyRpcServer = new NettyRpcServer(serviceProvider, brpcProperties.getNetty().getSerializer());
        nettyRpcServer.start(brpcProperties.getNetty().getPort());
        return nettyRpcServer;
    }

    // Client

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public ILoadBalance loadBalance() {
        log.info("Create bean of ILoadBalance loadBalance");
        WeightRoundRobinLoadBalance weightRoundRobinLoadBalance = new WeightRoundRobinLoadBalance();
        return weightRoundRobinLoadBalance;
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public IServiceDiscover serviceDiscover(CuratorFramework client, ILoadBalance loadBalance) {
        log.info("Create bean of IServiceDiscover serviceDiscover");

        return new ZkServiceDiscover(client, loadBalance);
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public CircuitBreakerProvider circuitBreakerProvider() {
        log.info("Create bean of CircuitBreakerProvider circuitBreakerProvider");

        return new CircuitBreakerProvider();
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public IRpcClient rpcClient(IServiceDiscover serviceDiscover) {
        log.info("Create bean of IRpcClient rpcClient");
        log.debug("RpcClient Serializer: {}", brpcProperties.getNetty().getSerializer());
        return new NettyRpcClient(serviceDiscover, brpcProperties.getNetty().getSerializer());
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public ClientProxy clientProxy(IRpcClient rpcClient, CircuitBreakerProvider circuitBreakerProvider, IServiceDiscover serviceDiscover) {
        log.info("Create bean of ClientProxy clientProxy");

        return new ClientProxy(rpcClient, circuitBreakerProvider, serviceDiscover);
    }


    @Bean
    public RpcWeightResolver rpcWeightResolver(Environment environment) {
        return new RpcWeightResolver(environment);
    }

    // 注解驱动
    @Bean
    public RpcServiceProcessor rpcServiceProcessor(ServiceProvider serviceProvider) {
        log.info("Create bean of RpcServiceProcessor rpcServiceProcessor");
        return new RpcServiceProcessor(serviceProvider);
    }

    @Bean
    public RpcReferenceProcessor rpcReferenceProcessor(ClientProxy clientProxy) {
        log.info("Create bean of RpcReferenceProcessor rpcReferenceProcessor");
        return new RpcReferenceProcessor(clientProxy);
    }
}
