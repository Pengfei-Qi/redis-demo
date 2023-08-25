package com.union.redisdemo.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.union.redisdemo.dto.LoginFormDTO;
import com.union.redisdemo.dto.Result;
import com.union.redisdemo.entity.User;
import com.union.redisdemo.mapper.UserMapper;
import com.union.redisdemo.service.IUserService;
import com.union.redisdemo.utils.RegexUtils;
import com.union.redisdemo.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpSession;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {

        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号码格式有误");
        }
        String code = RandomUtil.randomNumbers(6);
        session.setAttribute(SystemConstants.SESSION_CODE,code);
        log.info("调用短信机，发送验证码:{}",code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号码格式有误");
        }
        String code = loginForm.getCode();
        Object sessionCode = session.getAttribute(SystemConstants.SESSION_CODE);
        if (sessionCode == null || !sessionCode.toString().equals(code)){
            return Result.fail("验证码不正确");
        }
        User user = query().eq("phone", phone).one();
        if (user == null){
            user = createUserWithPhone(phone);
        }
        session.setAttribute(SystemConstants.SESSION_USER,user);
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
