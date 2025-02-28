package com.xiaozhi.service.impl;

import com.xiaozhi.common.exception.UserPasswordNotMatchException;
import com.xiaozhi.common.exception.UsernameNotFoundException;
import com.xiaozhi.dao.UserMapper;
import com.xiaozhi.entity.SysUser;
import com.xiaozhi.security.AuthenticationService;
import com.xiaozhi.service.SysUserService;
import com.xiaozhi.ultis.DateUtil;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

/**
 * 用户操作
 * 
 * @author Joey
 * 
 */

@Service
public class SysUserServiceImpl implements SysUserService {

  private static final String dayOfMonthStart = DateUtil.dayOfMonthStart();
  private static final String dayOfMonthEnd = DateUtil.dayOfMonthEnd();

  @Resource
  private UserMapper userMapper;

  @Resource
  private AuthenticationService authenticationService;

  /**
   * 
   * @param username
   * @param password
   * @return 用户登录信息
   * @throws UsernameNotFoundException
   * @throws UserPasswordNotMatchException
   */
  @Override
  public SysUser login(String username, String password)
      throws UsernameNotFoundException, UserPasswordNotMatchException {
    SysUser user = userMapper.selectUserByUsername(username);
    if (StringUtils.isEmpty(user)) {
      throw new UsernameNotFoundException();
    } else if (!authenticationService.isPasswordValid(password, user.getPassword())) {
      throw new UserPasswordNotMatchException();
    }
    return user;
  }

  /**
   * 用户信息查询
   * 
   * @param username
   * @return 用户信息
   */
  @Override
  public SysUser query(String username) {
    return userMapper.query(username, dayOfMonthStart, dayOfMonthEnd);
  }

  @Override
  public SysUser selectUserByUserId(Integer userId) {
    return userMapper.selectUserByUserId(userId);
  }

  @Override
  public SysUser selectUserByUsername(String username) {
    return userMapper.selectUserByUsername(username);
  }

  /**
   * 用户信息更改
   * 
   * @param user
   * @return
   */
  @Override
  @Transactional
  public int update(SysUser user) {
    return userMapper.update(user);
  }
}