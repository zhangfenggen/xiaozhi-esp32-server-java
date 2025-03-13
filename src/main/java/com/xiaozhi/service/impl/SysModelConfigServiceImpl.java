package com.xiaozhi.service.impl;

import com.xiaozhi.dao.ModelConfigMapper;
import com.xiaozhi.entity.SysModelConfig;
import com.xiaozhi.service.SysModelConfigService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 模型配置
 * 
 * @author Joey
 * 
 */

@Service
public class SysModelConfigServiceImpl implements SysModelConfigService {

  @Resource
  private ModelConfigMapper modelConfigMapper;

  /**
   * 添加模型配置
   * 
   * @param SysModelConfig
   * @return
   */
  @Override
  @Transactional
  public int add(SysModelConfig config) {
    return modelConfigMapper.add(config);
  }

  /**
   * 修改模型配置
   * @param SysModelConfig
   * @return
   */
  @Override
  @Transactional
  public int update(SysModelConfig config) {
    return modelConfigMapper.update(config);
  }

  /**
   * 查询模型配置
   * @param modelId
   * @return
   */
  @Override
  public SysModelConfig selectModelConfigByModelId(Integer modelId) {
    return modelConfigMapper.selectModelConfigByModelId(modelId);
  }

}