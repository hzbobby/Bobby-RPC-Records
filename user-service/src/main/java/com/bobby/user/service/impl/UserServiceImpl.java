package com.bobby.user.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.bobby.rpc.core.common.annotation.RpcReference;
import com.bobby.rpc.core.sample.domain.Blog;
import com.bobby.rpc.core.sample.domain.User;
import com.bobby.rpc.core.sample.service.IBlogService;
import com.bobby.rpc.core.sample.service.IUserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements IUserService {
    // 在这个服务里面引用
    @RpcReference
    private IBlogService blogService;


    @Override
    public User getUser(Long id) {
        return User.builder()
                .id(RandomUtil.randomLong())
                .name(RandomUtil.randomString(10))
                .age(RandomUtil.randomInt())
                .build();
    }

    public Blog getBlog(Long id) {
        return blogService.getBlogById(id);
    }
}
