package com.xiaozhi.service.impl;

import java.util.List;

import com.github.pagehelper.PageHelper;
import com.xiaozhi.dao.DeviceMapper;
import com.xiaozhi.entity.SysDevice;
import com.xiaozhi.service.SysDeviceService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

/**
 * 设备操作
 *
 * @author Joey
 *
 */

@Service
public class SysDeviceServiceImpl implements SysDeviceService {

    @Resource
    private DeviceMapper deviceMapper;

    /**
     * 添加设备
     *
     * @param device
     * @return
     */
    @Override
    @Transactional
    public void add(SysDevice device) {
        // 添加设备
        int adddevice = deviceMapper.add(device);
        if (0 == adddevice) {
            throw new RuntimeException();
        }
    }

    /**
     * 查询设备信息
     *
     * @param device
     * @return
     */
    @Override
    public List<SysDevice> query(SysDevice device) {
        if (!StringUtils.isEmpty(device.getLimit())) {
            PageHelper.startPage(device.getStart(), device.getLimit());
        }
        return deviceMapper.query(device);
    }

    /**
     * 更新设备信息
     *
     * @param device
     * @return
     */
    @Override
    @Transactional
    public int update(SysDevice device) {
        return deviceMapper.update(device);
    }

}