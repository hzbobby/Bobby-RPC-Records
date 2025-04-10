package com.bobby.rpc.core.sample.service;

import com.bobby.rpc.core.sample.domain.Blog;
import com.bobby.rpc.core.sample.domain.User;

public interface IUserService {

    public User getUser(Long id);

    public Blog getBlog(Long id);
}
