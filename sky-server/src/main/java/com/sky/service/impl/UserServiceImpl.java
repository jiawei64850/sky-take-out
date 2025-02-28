package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    /**
     * 用户登陆
     * @return userLoginDTO
     */
    public User login(UserLoginDTO userLoginDTO) {
        // 1. construct the login request of user via HttpClient
        // 1.1 construct param of request
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("appid", weChatProperties.getAppid());
        paramMap.put("secret", weChatProperties.getSecret());
        paramMap.put("js_code", userLoginDTO.getCode());
        paramMap.put("grant_type", "authorization_code");
        // 1.2 call the tool class to send request
        String res = HttpClientUtil.doGet("https://api.weixin.qq.com/sns/jscode2session", paramMap);
        log.info("res: {}", res);
        // 2. explain the result of response, and get the OpenId
        JSONObject jsonObject = JSON.parseObject(res);
        String openid = (String) jsonObject.get("openid");
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.USER_NOT_LOGIN);
        }
        // 3. verify whether new user or not based on OpenId
        User user = userMapper.selectByOpenId(openid);
        // 4. initialize the data of new user into user table if it is new user
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setCreateTime(LocalDateTime.now());
            user.setName(openid.substring(0, 5));
            userMapper.insert(user);
        }
        // 5. return user data directly if not
        return user;
    }
}
