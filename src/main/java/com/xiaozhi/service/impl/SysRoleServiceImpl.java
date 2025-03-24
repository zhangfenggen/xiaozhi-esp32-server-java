package com.xiaozhi.service.impl;

import java.util.List;

import com.github.pagehelper.PageHelper;
import com.xiaozhi.dao.RoleMapper;
import com.xiaozhi.entity.SysRole;
import com.xiaozhi.service.SysRoleService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

/**
 * 角色操作
 *
 * @author Joey
 *
 */

@Service
public class SysRoleServiceImpl implements SysRoleService {

    @Resource
    private RoleMapper roleMapper;

    /**
     * 添加角色
     *
     * @param role
     * @return
     */
    @Override
    @Transactional
    public void add(SysRole role) {
        // 添加设备
        int addRole = roleMapper.add(role);
        if (0 == addRole) {
            throw new RuntimeException();
        }
    }

    /**
     * 查询角色信息
     *
     * @param role
     * @return
     */
    @Override
    public List<SysRole> query(SysRole role) {
        if (!StringUtils.isEmpty(role.getLimit())) {
            PageHelper.startPage(role.getStart(), role.getLimit());
        }
        return roleMapper.query(role);
    }

    /**
     * 更新角色信息
     *
     * @param role
     * @return
     */
    @Override
    @Transactional
    public int update(SysRole role) {
        return roleMapper.update(role);
    }

}