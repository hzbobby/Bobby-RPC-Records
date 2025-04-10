package com.bobby.blog;

import com.bobby.rpc.starter.annotation.EnableBRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author: Bobby
 * @email: vividbobby@163.com
 * @date: 2025/4/6
 */
@EnableBRpc
@SpringBootApplication
public class BlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
    }
}
