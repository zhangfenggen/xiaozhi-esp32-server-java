package com.xiaozhi.service;

import com.xiaozhi.entity.SysModelConfig;

/**
 * 模型配置
 * 
 * @author Joey
 * 
 */
public interface SysModelConfigService {

  /**
   * 添加模型配置
   * 
   * @param SysModelConfig
   * @return
   */
  public int add(SysModelConfig config);

  /**
   * 修改模型配置
   * 
   * @param SysModelConfig
   * @return
   */
  public int update(SysModelConfig config);

  /**
   * 查询模型配置
   * 
   * @param modelId;
   * @return
   */
  public SysModelConfig selectModelConfigByModelId(Integer modelId);
}