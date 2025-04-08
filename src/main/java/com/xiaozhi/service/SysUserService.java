package com.xiaozhi.service;

import java.util.List;

import com.xiaozhi.common.exception.UserPasswordNotMatchException;
import com.xiaozhi.common.exception.UsernameNotFoundException;
import com.xiaozhi.entity.SysUser;

/**
 * 用户操作
 * 
 * @author Joey
 * 
 */
public interface SysUserService {

    /**
     * 用户名sessionkey
     */
    public static final String USER_SESSIONKEY = "user_sessionkey";

    /**
     * 登录校验
     * 
     * @param username
     * @param password
     * @return
     */
    public SysUser login(String username, String password)
            throws UsernameNotFoundException, UserPasswordNotMatchException;

    /**
     * 查询用户信息
     * 
     * @param username
     * @return 用户信息
     */
    public SysUser query(String username);

    /**
     * 用户查询列表
     * 
     * @param user
     * @return 用户列表
     */
    public List<SysUser> queryUsers(SysUser user);

    public SysUser selectUserByUserId(Integer userId);

    public SysUser selectUserByUsername(String username);

    public SysUser selectUserByEmail(String email);

    /**
     * 新增用户
     * 
     * @param user
     * @return
     */
    public int add(SysUser user);

    /**
     * 修改用户信息
     * 
     * @param user
     * @return
     */
    public int update(SysUser user);

    /**
     * 生成验证码
     * 
     */
    public SysUser generateCode(SysUser user);

    /**
     * 查询验证码是否有效
     * 
     * @param code
     * @param email
     * @return
     */
    public int queryCaptcha(String code, String email);

}