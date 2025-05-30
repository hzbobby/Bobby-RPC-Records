package com.bobby.rpc.v9.client.circuitBreaker;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 熔断器
 * 当我们的系统依赖于外部服务，外部服务失败多次或不可用时，就可以先不再去尝试了，可以考虑对该服务进行熔断（即，快速失败，避免一直去调用）
 * 我们可以对一个接口进行监控，当失败次数超过一定次数之后，开启熔断机制。反之，当成功一定次数，可以将熔断器关闭
 *
 */
@Slf4j
public class CircuitBreaker {
    enum CircuitBreakerState {
        CLOSED,OPEN,HALF_OPEN
    }

    // 熔断器状态
    private CircuitBreakerState state = CircuitBreakerState.CLOSED;

    // 统计次数
    private AtomicInteger failureCount = new AtomicInteger(0);
    private AtomicInteger successCount = new AtomicInteger(0);
    private AtomicInteger requestCount = new AtomicInteger(0);

    // 失败次数阈值，超过该次数，熔断器就开启
    private final int failureThreshold;
    //半开启 -> 关闭状态的成功次数比例
    private final double halfOpenSuccessRate;
    //恢复时间
    private final long retryTimePeriod;

    //上一次失败时间
    private long lastFailureTime;

    public CircuitBreaker(int failureThreshold, double halfOpenSuccessRate, long retryTimePeriod) {
        this.failureThreshold = failureThreshold;
        this.halfOpenSuccessRate = halfOpenSuccessRate;
        this.retryTimePeriod = retryTimePeriod;
        this.lastFailureTime = 0;
    }


    /**
     * 查看当前熔断器是否允许请求通过
     * @return
     */
    public synchronized boolean allowRequest() {
        long currentTime = System.currentTimeMillis();
//        log.info("熔断器, 当前失败次数: {}", failureCount);
        switch (state) {
            case OPEN:
                if (currentTime - lastFailureTime > retryTimePeriod) {
                    state = CircuitBreakerState.HALF_OPEN;
                    resetCounts();
                    return true;
                }
                log.info("熔断生效");
                return false;
            case HALF_OPEN:
                requestCount.incrementAndGet();
                return true;
            case CLOSED:
            default:
                return true;
        }
    }

    /// ///////////////////////////////////////////////////////////////////////////////
    // 以下都是进行状态转换

    //记录成功
    public synchronized void recordSuccess() {
        if (state == CircuitBreakerState.HALF_OPEN) {
            successCount.incrementAndGet();
            if (successCount.get() >= halfOpenSuccessRate * requestCount.get()) {
                state = CircuitBreakerState.CLOSED;
                resetCounts();
            }
        } else {
            resetCounts();
        }
    }
    //记录失败

    /**
     * 出现一次失败时，就进入 half-open 状态
     * 当超过一定次数时，则进入 closed 状态
     */
    public synchronized void recordFailure() {
        failureCount.incrementAndGet();
        System.out.println("记录失败!!!!!!!失败次数"+failureCount);
        lastFailureTime = System.currentTimeMillis();
        if (state == CircuitBreakerState.HALF_OPEN) {
            state = CircuitBreakerState.OPEN;
            lastFailureTime = System.currentTimeMillis();
        } else if (failureCount.get() >= failureThreshold) {
            state = CircuitBreakerState.OPEN;
        }
    }
    //重置次数
    private void resetCounts() {
        failureCount.set(0);
        successCount.set(0);
        requestCount.set(0);
    }

}
