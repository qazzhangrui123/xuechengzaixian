package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.feignClient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 帐号名密码方式
 */
@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {

    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    CheckCodeClient checkCodeClient;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //帐号
        String username = authParamsDto.getUsername();

        //用户输入的验证码
        String checkcode = authParamsDto.getCheckcode();
        //验证码对应的key
        String checkcodekey = authParamsDto.getCheckcodekey();
        if (StringUtils.isEmpty(checkcodekey)||StringUtils.isEmpty(checkcode)){
            throw new RuntimeException("请输入验证码");
        }
        //远程调用验证码服务接口去校验验证码
        Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);
        if (verify==null||verify==false){
            throw new RuntimeException("验证码输入错误");
        }

        //校验帐号是否存在
        //根据username帐号查询数据库
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        //查询到用户不存在，返回null，SpringSecurity框架抛出异常用户不存在
        if (xcUser==null){
            throw  new RuntimeException("帐号不存在");
        }
        //如果查到了用户，可以拿到正确的密码，最终封装成一个UserDetails对象给SpringSecurity框架返回，由框架进行密码比对
        String passwordDb = xcUser.getPassword();
        //用户输入的密码
        String pwdForm = authParamsDto.getPassword();
        //校验密码
        boolean matches = passwordEncoder.matches(pwdForm, passwordDb);
        if (!matches){
            throw new RuntimeException("帐号或密码错误");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser,xcUserExt);
        return xcUserExt;
    }
}
