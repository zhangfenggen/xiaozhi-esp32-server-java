package com.xiaozhi.service;

import java.util.List;

import com.xiaozhi.entity.SysDevice;
import com.xiaozhi.entity.SysMessage;

/**
 * 聊天记录查询/添加
 * 
 * @author Joey
 * 
 */
public interface SysMessageService {

  /**
   * 新增记录
   * 
   * @param device
   * @return
   */
  public int add(SysDevice device);

  /**
   * 查询聊天记录
   * 
   * @param message
   * @return
   */
  public List<SysMessage> query(SysMessage message);

}