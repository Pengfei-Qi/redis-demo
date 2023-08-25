package com.union.redisdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.union.redisdemo.dto.LoginFormDTO;
import com.union.redisdemo.dto.Result;
import com.union.redisdemo.entity.User;

import javax.servlet.http.HttpSession;

public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);
}
