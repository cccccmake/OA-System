package com.imooc.oa.service;

import com.imooc.oa.entity.User;
import com.imooc.oa.mapper.UserMapper;
import com.imooc.oa.service.exception.LoginException;

public class UserService {
    private UserMapper userMapper = new UserMapper();

    public User checkLogin(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new LoginException("User does not exist!");
        }
        if(!password.equals(user.getPassword())){
            throw new LoginException("Password error!");
        }
        return user;
    }
}
