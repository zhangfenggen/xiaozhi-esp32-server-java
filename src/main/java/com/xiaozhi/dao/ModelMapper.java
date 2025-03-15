package com.xiaozhi.dao;

import java.util.List;

import com.xiaozhi.entity.SysModel;

/**
 * 模型 数据层
 * 
 * @author Joey
 * 
 */
public interface ModelMapper {
    int add(SysModel model);

    int update(SysModel model);

    List<SysModel> query(SysModel model);

    SysModel selectModelByModelId(Integer modelId);
}
