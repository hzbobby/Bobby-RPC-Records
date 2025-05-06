package com.bobby.blog.service.impl;


import com.bobby.rpc.core.common.annotation.RpcService;
import com.bobby.rpc.core.sample.domain.Blog;
import com.bobby.rpc.core.sample.service.IBlogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RpcService(weight = 2)
@Service
public class BlogServiceImpl implements IBlogService {
    @Override
    public Blog getBlogById(Long id) {
        Blog blog = Blog.builder().id(id).title("我的博客").useId(22L).build();
        System.out.println("客户端查询了" + id + "博客");
        return blog;
    }

    @Override
    public void addBlog(Blog blog) {
        System.out.println("插入的 Blog 为：" + blog.toString());
    }
}