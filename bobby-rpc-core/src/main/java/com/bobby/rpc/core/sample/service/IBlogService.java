package com.bobby.rpc.core.sample.service;

import com.bobby.rpc.core.sample.domain.Blog;

public interface IBlogService {
    public Blog getBlogById(Long id);
    public void addBlog(Blog blog);
}
