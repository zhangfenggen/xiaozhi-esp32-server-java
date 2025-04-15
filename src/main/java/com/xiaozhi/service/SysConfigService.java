package com.xiaozhi.service;

import java.util.List;

import com.xiaozhi.entity.SysConfig;

/**
 * 配置
 * 
 * @author Joey
 * 
 */
public interface SysConfigService {

  /**
   * 添加配置
   * 
   * @param SysConfig
   * @return
   */
  public int add(SysConfig config);

  /**
   * 修改配置
   * 
   * @param SysConfig
   * @return
   */
  public int update(SysConfig config);


  /**
   * 查询
   * 
   * @param config;
   * @return
   */
  public List<SysConfig> query(SysConfig config);

  /**
   * 查询配置
   * 
   * @param configId;
   * @return
   */
  public SysConfig selectConfigById(Integer configId);
}