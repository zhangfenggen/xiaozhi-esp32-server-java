package com.xiaozhi.service.impl;

import com.github.pagehelper.PageHelper;
import com.xiaozhi.dao.ModelMapper;
import com.xiaozhi.entity.SysModel;
import com.xiaozhi.service.SysModelService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

import javax.annotation.Resource;

/**
 * 模型配置
 * 
 * @author Joey
 * 
 */

@Service
public class SysModelServiceImpl implements SysModelService {

    @Resource
    private ModelMapper modelMapper;

    /**
     * 添加模型配置
     * 
     * @param SysModel
     * @return
     */
    @Override
    @Transactional
    public int add(SysModel model) {
        return modelMapper.add(model);
    }

    /**
     * 修改模型配置
     * 
     * @param SysModel
     * @return
     */
    @Override
    @Transactional
    public int update(SysModel model) {
        return modelMapper.update(model);
    }

    /**
     * 查询模型
     * 
     * @param model
     * @return
     */
    @Override
    @Transactional
    public List<SysModel> query(SysModel model) {
        if (!StringUtils.isEmpty(model.getLimit())) {
            PageHelper.startPage(model.getStart(), model.getLimit());
        }
        return modelMapper.query(model);
    }

    /**
     * 查询模型配置
     * 
     * @param modelId
     * @return
     */
    @Override
    public SysModel selectModelByModelId(Integer modelId) {
        return modelMapper.selectModelByModelId(modelId);
    }

}