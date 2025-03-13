package com.xiaozhi.dao;

import java.util.List;

import com.xiaozhi.entity.SysModelConfig;

/**
 * 模型 数据层
 * 
 * @author Joey
 * 
 */
public interface ModelConfigMapper {
    int add(SysModelConfig config);

    int update(SysModelConfig config);

    List<SysModelConfig> query(SysModelConfig config);

    SysModelConfig selectModelConfigByModelId(Integer modelId);
}
