package com.xiaozhi.dao;

import java.util.List;

import com.xiaozhi.entity.SysUser;
import org.apache.ibatis.annotations.Param;

/**
 * 用户资料 数据层
 * 
 * @author Joey
 * 
 */
public interface UserMapper {
    SysUser selectUserByUserId(@Param("userId") Integer userId);

    SysUser selectUserByUsername(@Param("username") String username);

    SysUser query(@Param("username") String username, @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    int update(SysUser sysUser);

    List<SysUser> queryUsers(@Param("user") SysUser user, @Param("startTime") String startTime,
            @Param("endTime") String endTime);
}
