package com.xiaozhi.service;

import java.util.List;

import com.xiaozhi.entity.SysModel;

/**
 * 模型配置
 * 
 * @author Joey
 * 
 */
public interface SysModelService {

  /**
   * 添加模型配置
   * 
   * @param SysModel
   * @return
   */
  public int add(SysModel model);

  /**
   * 修改模型配置
   * 
   * @param SysModel
   * @return
   */
  public int update(SysModel model);


  /**
   * 查询模型
   * 
   * @param model;
   * @return
   */
  public List<SysModel> query(SysModel model);

  /**
   * 查询模型配置
   * 
   * @param modelId;
   * @return
   */
  public SysModel selectModelByModelId(Integer modelId);
}