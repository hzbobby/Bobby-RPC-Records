package com.bobby.rpc.core.common.resolver;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public class RpcWeightResolver {
    private final Environment environment;

    public RpcWeightResolver(Environment environment) {
        this.environment = environment;
    }

    public int resolveWeight(String weightExpression) {
        if (StringUtils.isEmpty(weightExpression)) {
            return 0; // 默认权重
        }
        
        try {
            // 先尝试直接解析为数字
            return Integer.parseInt(weightExpression);
        } catch (NumberFormatException e) {
            // 如果是属性占位符，使用环境解析
            String resolved = environment.resolvePlaceholders(weightExpression);
            try {
                return Integer.parseInt(resolved);
            } catch (NumberFormatException ex) {
                return 0; // 解析失败使用默认值
            }
        }
    }
}