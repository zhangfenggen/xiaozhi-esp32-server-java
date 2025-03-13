package com.xiaozhi.service;

import com.xiaozhi.common.exception.UserPasswordNotMatchException;
import com.xiaozhi.common.exception.UsernameNotFoundException;
import com.xiaozhi.entity.SysModelConfig;
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

  public SysUser selectUserByUserId(Integer userId);

  public SysUser selectUserByUsername(String username);

  /**
   * 修改用户信息
   * 
   * @param user
   * @return
   */
  public int update(SysUser user);

  /**
   * 添加模型配置
   * 
   * @param SysModelConfig
   * @return
   */
  public int addModel(SysModelConfig config);

  /**
   * 修改模型配置
   * 
   * @param SysModelConfig
   * @return
   */
  public int updateModel(SysModelConfig config);
}