package com.xiaozhi.service.impl;

import com.xiaozhi.entity.SysAgent;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.service.SysAgentService;
import com.xiaozhi.service.SysConfigService;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

/**
 * 智能体服务实现
 * 
 * @author Joey
 */
@Service
public class SysAgentServiceImpl implements SysAgentService {

    private static final Logger logger = LoggerFactory.getLogger(SysAgentServiceImpl.class);

    @Autowired
    private SysConfigService configService;

    private final WebClient webClient = WebClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 添加智能体
     * 
     * @param agent 智能体信息
     * @return 结果
     */
    @Override
    @Transactional
    public int add(SysAgent agent) {
        return 0;
    }

    /**
     * 修改智能体
     * 
     * @param agent 智能体信息
     * @return 结果
     */
    @Override
    @Transactional
    public int update(SysAgent agent) {
        return 0;
    }

    /**
     * 删除智能体
     * 
     * @param agentId 智能体ID
     * @return 结果
     */
    @Override
    @Transactional
    public int delete(Integer agentId) {
        return 0;
    }

    /**
     * 查询智能体列表
     * 
     * @param agent 智能体信息
     * @return 智能体集合的Mono对象
     */
    @Override
    public Mono<List<SysAgent>> query(SysAgent agent) {
        // 如果设置了平台为Coze，则从Coze API获取智能体列表
        if ("coze".equalsIgnoreCase(agent.getProvider())) {
            return getCozeAgents(agent);
        } else {
            // 如果不是Coze平台，返回空列表
            return Mono.just(new ArrayList<>());
        }
    }

    /**
     * 从Coze API获取智能体列表
     * 
     * @param agent 智能体信息
     * @return 智能体集合的Mono对象
     */
    private Mono<List<SysAgent>> getCozeAgents(SysAgent agent) {

        List<SysConfig> configs = configService.query(agent);
        SysConfig config = new SysConfig();
        if (!ObjectUtils.isEmpty(configs)) {
            config = configs.get(0);
        } else {
            return Mono.just(new ArrayList<>());
        }
        // 获取API密钥和空间ID，优先使用传入的值，否则使用默认值
        String apiSecret = config.getApiSecret();

        String spaceId = config.getAppId();

        // 调用Coze API获取智能体列表
        return webClient.get()
                .uri("https://api.coze.cn/v1/space/published_bots_list?space_id=" + spaceId)
                .header("Authorization", "Bearer " + apiSecret)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    List<SysAgent> agentList = new ArrayList<>();
                    try {
                        JsonNode rootNode = objectMapper.readTree(response);
                        if (rootNode.has("code") && rootNode.get("code").asInt() == 0) {
                            JsonNode spaceBots = rootNode.path("data").path("space_bots");

                            // 遍历智能体列表
                            for (JsonNode botNode : spaceBots) {
                                SysAgent botAgent = new SysAgent();
                                botAgent.setBotId(botNode.path("bot_id").asText());
                                botAgent.setAgentName(botNode.path("bot_name").asText());
                                botAgent.setAgentDesc(botNode.path("description").asText());
                                botAgent.setIconUrl(botNode.path("icon_url").asText());
                                botAgent.setPublishTime(
                                        new Date(Long.parseLong(botNode.path("publish_time").asText()) * 1000));
                                botAgent.setProvider("coze");

                                // 如果前端传入了智能体名称过滤条件，则进行过滤
                                if (StringUtils.hasText(agent.getAgentName())) {
                                    if (botAgent.getAgentName().toLowerCase()
                                            .contains(agent.getAgentName().toLowerCase())) {
                                        agentList.add(botAgent);
                                    }
                                } else {
                                    agentList.add(botAgent);
                                }
                            }

                        } else {
                            String errorMsg = rootNode.has("msg") ? rootNode.get("msg").asText() : "未知错误";
                            logger.error("查询Coze智能体列表失败：{}", errorMsg);
                        }
                    } catch (Exception e) {
                        logger.error("解析Coze API响应异常", e);
                    }
                    return agentList;
                })
                .onErrorResume(e -> {
                    logger.error("查询Coze智能体列表异常", e);
                    return Mono.just(new ArrayList<>());
                });
    }

}