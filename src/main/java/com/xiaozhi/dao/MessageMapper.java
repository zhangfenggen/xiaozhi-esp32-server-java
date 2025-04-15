package com.xiaozhi.dao;

import java.util.List;

import com.xiaozhi.entity.SysDevice;
import com.xiaozhi.entity.SysMessage;

/**
 * 聊天记录 数据层
 * 
 * @author Joey
 * 
 */
public interface MessageMapper {

  int add(SysDevice message);

  int update(SysMessage message);

  List<SysMessage> query(SysMessage message);
}