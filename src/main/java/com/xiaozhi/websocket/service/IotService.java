package com.xiaozhi.websocket.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.xiaozhi.entity.SysDevice;
import com.xiaozhi.utils.JsonUtil;
import com.xiaozhi.websocket.iot.IotDescriptor;
import com.xiaozhi.websocket.iot.IotMethod;
import com.xiaozhi.websocket.iot.IotMethodParameter;
import com.xiaozhi.websocket.iot.IotProperty;
import com.xiaozhi.websocket.llm.tool.ActionType;
import com.xiaozhi.websocket.llm.tool.ToolResponse;
import com.xiaozhi.websocket.llm.tool.ToolType;
import com.xiaozhi.websocket.llm.tool.function.FunctionGlobalRegistry;
import com.xiaozhi.websocket.llm.tool.function.FunctionSessionHolder;
import com.xiaozhi.websocket.llm.tool.function.bean.FunctionCallTool;
import com.xiaozhi.websocket.llm.tool.function.bean.description.FunctionDesc;
import com.xiaozhi.websocket.llm.tool.function.bean.FunctionLlmDescription;
import com.xiaozhi.websocket.llm.tool.function.bean.description.ParameterDesc;
import com.xiaozhi.websocket.llm.tool.function.bean.description.PropertyDesc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Iot服务 - 负责iot处理和WebSocket发送
 */
@Service
public class IotService {
    private static final Logger logger = LoggerFactory.getLogger(IotService.class);
    private static final String TAG = "IotService";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private FunctionGlobalRegistry functionGlobalRegistry;

    @Autowired
    private MessageService messageService;

    /**
     * 处理iot设备描述信息，形成function_call，注册进sessionManager，用于后续的llm调用及设备调用
     *
     * @param sessionId 会话ID
     * @param descriptors  iot设备描述消息内容
     */
    public void handleDeviceDescriptors(String sessionId, JsonNode descriptors) {
        Iterator<JsonNode> iotDescriptorIterator = descriptors.elements();
        while (iotDescriptorIterator.hasNext()) {
            JsonNode iotDescriptorJson = iotDescriptorIterator.next();

            String iotName = iotDescriptorJson.path("name").asText();
            String description = iotDescriptorJson.path("description").asText();
            JsonNode properties = iotDescriptorJson.path("properties");
            JsonNode methods = iotDescriptorJson.path("methods");
            if(properties.isMissingNode() && methods.isMissingNode()) {
                return;
            }
            // 记录iot设备描述信息
            IotDescriptor iotDescriptor = new IotDescriptor(
                    iotName,
                    description,
                    properties,
                    methods
            );
            sessionManager.registerIot(sessionId, iotDescriptor);
            registerFunctionTools(sessionId, iotDescriptor);
        }
    }

    /**
     * 处理iot设备状态变更信息，可用于更新设备状态或进行其他操作
     *
     * @param sessionId 会话ID
     * @param states  iot状体消息内容
     */
    public void handleDeviceStates(String sessionId, JsonNode states) {
        Iterator<JsonNode> iotStateIterator = states.elements();

        while (iotStateIterator.hasNext()) {
            JsonNode iotState = iotStateIterator.next();

            String iotName = iotState.path("name").asText();
            IotDescriptor iotDescriptor = sessionManager.getIotDescriptor(sessionId, iotName);
            if (iotDescriptor == null) {
                logger.error("[{}] - SessionId: {} 未找到设备: {} 的描述信息", TAG, sessionId, iotName);
                continue;
            }
            JsonNode iotStateValues = iotState.path("state");
            Iterator<String> stateIterator = iotStateValues.fieldNames();
            while (stateIterator.hasNext()) {
                String propertyName = stateIterator.next();
                //获取新的属性值
                JsonNode propertyValue = iotStateValues.path(propertyName);

                IotProperty property = iotDescriptor.getProperties().get(propertyName);
                if (property != null) {
                    // 类型检查
                    String newValueType = propertyValue.getNodeType().toString();
                    if (!property.getType().equalsIgnoreCase(newValueType)) {
                        logger.error("[{}] - SessionId: {} handleDeviceStates 属性: {} 的值类型不匹配, 注册类型: {}, 入参类型: {}", TAG, sessionId, propertyName, property.getType(), newValueType);
                        continue;
                    }
                    property.setValue(propertyValue);
                    logger.info("[{}] - SessionId: {} handleDeviceStates 物联网状态更新: {} , {} = {}", TAG, sessionId, iotName, propertyName, propertyValue);
                }else{
                    logger.error("[{}] - SessionId: {} handleDeviceStates 未找到设备 {} 的属性 {}", TAG, sessionId, iotName, propertyName);
                }
            }
        }
    }

    /**
     * 获取物联网状态
     *
     * @param sessionId 会话ID
     * @param iotName iot设备名称
     * @param propertyName 属性名称
     * @return 属性值，如未找到则返回null
     */
    public Object getIotStatus(String sessionId, String iotName, String propertyName) {
        IotDescriptor iotDescriptor = sessionManager.getIotDescriptor(sessionId, iotName);

        if (iotDescriptor != null) {
            IotProperty property = iotDescriptor.getProperties().get(propertyName);
            if (property != null) {
                return property.getValue();
            }else{
                logger.error("[{}] - SessionId: {} getIotStatus 未找到设备 {} 的属性 {}", TAG, sessionId, iotName, propertyName);
            }
        }else{
            logger.error("[{}] - SessionId: {} getIotStatus 未找到设备 {}", TAG, sessionId, iotName);
        }
        return null;
    }

    /**
     * 设置物联网状态
     *
     * @param sessionId 会话ID
     * @param iotName iot设备名称
     * @param propertyName 属性名称
     * @param value 属性值
     * @return 是否设置成功
     */
    public boolean setIotStatus(String sessionId, String iotName, String propertyName, Object value) {
        IotDescriptor iotDescriptor = sessionManager.getIotDescriptor(sessionId, iotName);

        if (iotDescriptor != null) {
            IotProperty property = iotDescriptor.getProperties().get(propertyName);
            if (property != null) {
                // 类型检查
                boolean typeCheck = false;
                if(property.getType().equalsIgnoreCase(JsonNodeType.OBJECT.name())){
                    typeCheck = true;
                }else if(value instanceof Number && property.getType().equalsIgnoreCase(JsonNodeType.NUMBER.name())){
                    typeCheck = true;
                }else if(value instanceof String && property.getType().equalsIgnoreCase(JsonNodeType.STRING.name())){
                    typeCheck = true;
                }else if(value instanceof Boolean && property.getType().equalsIgnoreCase(JsonNodeType.BOOLEAN.name())){
                    typeCheck = true;
                }
                if (!typeCheck) {
                    logger.error("[{}] - SessionId: {} setIotStatus 属性: {} 的值类型不匹配, 注册类型: {}, 入参类型: {}", TAG, sessionId, propertyName,
                            property.getType(), value.getClass().getSimpleName());
                    return false;
                }
                property.setValue(value);
                logger.info("[{}] - SessionId: {} setIotStatus 物联网状态更新: {} , {} = {}", TAG, sessionId, iotName, propertyName, value);
                sendIotMessage(sessionId, iotName, propertyName, Collections.singletonMap(propertyName, value));
                return true;
            }
        }
        logger.error("[{}] - SessionId: {} setIotStatus 未找到设备 {} 的属性 {}", TAG, sessionId, iotName, propertyName);
        return false;
    }

    /**
     * 发送iot消息到设备
     *
     * @param sessionId 会话ID
     * @param iotName   iot设备名称
     * @param methodName 方法名称
     * @param parameters 方法参数
     */
    public boolean sendIotMessage(String sessionId, String iotName, String methodName, Map<String, Object> parameters) {
        try {
            logger.info("[{}] - SessionId: {}, message send iotName: {}, methodName: {}, parameters: {}", TAG, sessionId,
                    iotName, methodName, JsonUtil.toJson(parameters));
            WebSocketSession session = sessionManager.getSession(sessionId);
            if (session == null || !session.isOpen()) {
                logger.error("[{}] - SessionId: {} not found or closed", TAG, sessionId);
                return false;
            }
            IotDescriptor iotDescriptor = sessionManager.getIotDescriptor(sessionId, iotName);
            if(iotDescriptor != null && iotDescriptor.getMethods().containsKey(methodName)){
                Map<String, Object> command = new HashMap<>();
                command.put("name", iotName);
                command.put("method", methodName);
                command.put("parameters", parameters);
                messageService.sendIotCommandMessage(session, Collections.singletonList(command)).subscribe();
                return true;
            }else{
                logger.error("[{}] - SessionId: {}, {} method not found: {}", TAG, sessionId, iotName, methodName);
            }
        } catch (Exception e) {
            logger.error("[{}] - SessionId: {}, error sending Iot message", TAG, sessionId, e);
        }
        return false;
    }
    /**
     * 注册iot设备的函数到FunctionHolder
     *
     * @param sessionId 会话ID
     * @param iotDescriptor  session绑定的FunctionHolder
     */
    private void registerFunctionTools(String sessionId, IotDescriptor iotDescriptor) {
        FunctionSessionHolder functionSessionHolder = sessionManager.getFunctionSessionHolder(sessionId);
        if(functionSessionHolder == null){
            functionSessionHolder = new FunctionSessionHolder(sessionId, functionGlobalRegistry);
            sessionManager.registerFunctionSessionHolder(sessionId, functionSessionHolder);
        }
        registerPropertiesFunctionTools(sessionId, functionSessionHolder, iotDescriptor);
        registerMethodFunctionTools(sessionId, functionSessionHolder, iotDescriptor);
        registerGlobalFunctionTools(sessionId, functionSessionHolder);
    }

    /**
     * 注册全局函数到FunctionHolder
     * @param sessionId 会话ID
     * @param functionSessionHolder
     */
    private void registerGlobalFunctionTools(String sessionId, FunctionSessionHolder functionSessionHolder) {
        SysDevice sysDevice = sessionManager.getDeviceConfig(sessionId);
        String functionNames = sysDevice == null? null : sysDevice.getFunctionNames();
        if(functionNames != null && !functionNames.isEmpty()){//如果指定了function配置，则只加载指定的
            String[] functionNameArr = functionNames.split(",");
            for(String functionName : functionNameArr){
                functionSessionHolder.registerFunction(functionName);
            }
        }else{//否则加载所有的全局function
            functionGlobalRegistry.getAllFunctions().forEach(functionSessionHolder::registerFunction);
        }
    }

    /**
     * 注册iot设备的属性的查询方法到FunctionHolder
     *
     * @param sessionId 会话ID
     * @param functionSessionHolder session绑定的FunctionHolder
     * @param iotDescriptor  iot信息
     */
    private void registerPropertiesFunctionTools(String sessionId, FunctionSessionHolder functionSessionHolder, IotDescriptor iotDescriptor) {
        //遍历properties，生成FunctionCallTool
        String iotName = iotDescriptor.getName();
        for (IotProperty propInfo : iotDescriptor.getProperties().values()) {
            String propName = propInfo.getName();
            // 创建函数名称，格式：get_{属性名称}
            String funcName = "get_" + iotName.toLowerCase() + "_" + propName.toLowerCase();
            // 创建函数对象
            FunctionDesc function = new FunctionDesc(funcName, "查询" + iotName + "的" + propInfo.getDescription());
            // 创建参数对象
            ParameterDesc parameters = new ParameterDesc();
            // 添方法参数
            parameters.
                    addProperty("response_success", new PropertyDesc("查询成功时的友好回复，必须使用{value}作为占位符表示查询到的值"), true).
                    addProperty("response_failure", new PropertyDesc("查询失败时的友好回复，例如：'无法获取" + iotName + "的" + propInfo.getDescription() + "'"), true);
            // 向函数中添加属性值参数
            function.setParameters(parameters);

            FunctionLlmDescription functionLlmDescription = new FunctionLlmDescription(function);
            FunctionCallTool functionCallTool = new FunctionCallTool(funcName, ToolType.IOT_CTL, functionLlmDescription, (functionParams) -> {
                try {
                    // 获取参数
                    String success = (String) functionParams.params.get("response_success");
                    String failure = (String) functionParams.params.get("response_failure");
                    // 获取属性值
                    Object value = getIotStatus(sessionId, iotName, propName);
                    if (value != null) {
                        //如果有{value}占位符，用相关参数替换
                        if (success.contains("{value}")) {
                            success = success.replace("{value}", String.valueOf(value));
                        }
                        return new ToolResponse(ToolType.IOT_CTL, ActionType.RESPONSE, iotName + "的" + funcName + "操作执行成功", success);
                    } else {
                        return new ToolResponse(ToolType.IOT_CTL, ActionType.ERROR, "执行" + iotName + "的" + funcName + "操作失败", failure);
                    }
                }catch (Exception e){
                    logger.error("[{}] - SessionId: {} 执行 {} 方法 {} 失败", TAG, sessionId, iotName, funcName, e);
                    return new ToolResponse(ToolType.IOT_CTL, ActionType.ERROR, "执行"+ iotName + "的"+funcName+"操作失败", e.getMessage());
                }
            });
            // 注册到当前会话的函数持有者
            functionSessionHolder.registerFunction(funcName, functionCallTool);
        }

    }

    /**
     * 注册iot设备的可调用方法到FunctionHolder
     *
     * @param sessionId 会话ID
     * @param functionSessionHolder FunctionHolder实例
     * @param iotDescriptor  iot信息
     */
    private void registerMethodFunctionTools(String sessionId, FunctionSessionHolder functionSessionHolder, IotDescriptor iotDescriptor) {
        // 遍历methods，生成FunctionCallTool
        String iotName = iotDescriptor.getName();

        for (IotMethod iotMethod : iotDescriptor.getMethods().values()) {
            // 创建函数名称，格式：{iotName}_{methodName}
            String funcName = iotMethod.getName();
            // 创建函数对象
            FunctionDesc function = new FunctionDesc(funcName, iotDescriptor.getDescription() + " - " + iotMethod.getDescription());
            // 创建参数对象
            ParameterDesc parameters = new ParameterDesc();
            //便利iotMethod方法参数，添加到函数参数中
            for (IotMethodParameter iotMethodParameter : iotMethod.getParameters().values()) {
                String paramName = iotMethodParameter.getName();
                String paramType = iotMethodParameter.getType();
                String paramDescription = iotMethodParameter.getDescription();
                // 添加参数
                parameters.addProperty(paramName, new PropertyDesc(paramType, paramDescription), true);
            }
            // 添加响应参数
            parameters.
                    addProperty("response_success", new PropertyDesc("操作成功时的友好回复,关于该设备的操作结果，设备名称使用description中的名称，不要出现占位符"), true).
                    addProperty("response_failure", new PropertyDesc("操作失败时的友好回复,关于该设备的操作结果，设备名称使用description中的名称"), true
            );
            // 设置函数参数
            function.setParameters(parameters);
            // 创建函数工具
            FunctionLlmDescription functionLlmDescription = new FunctionLlmDescription(function);
            // 注册到当前会话的函数持有者
            FunctionCallTool functionCallTool = new FunctionCallTool(funcName, ToolType.IOT_CTL, functionLlmDescription, (functionParams) -> {
                try {
                    // 获取参数
                    String success = (String) functionParams.params.get("response_success");
                    String failure = (String) functionParams.params.get("response_failure");
                    //发送给设备前，把成功和失败消息参数去掉
                    functionParams.params.remove("response_success");
                    functionParams.params.remove("response_failure");
                    boolean result = sendIotMessage(sessionId, iotName, funcName, functionParams.params);
                    if (result) {
                        return new ToolResponse(ToolType.IOT_CTL, ActionType.RESPONSE, iotName + "的"+funcName+"操作执行成功", success);
                    } else {
                        return new ToolResponse(ToolType.IOT_CTL, ActionType.ERROR, "执行"+ iotName + "的"+funcName+"操作失败", failure);
                    }
                }catch (Exception e){
                    logger.error("[{}] - SessionId: {} 执行 {} 方法 {} 失败", TAG, sessionId, iotName, funcName, e);
                    return new ToolResponse(ToolType.IOT_CTL, ActionType.ERROR, "执行"+ iotName + "的"+funcName+"操作失败", e.getMessage());
                }
            });
            // 注册到当前会话的函数持有者
            functionSessionHolder.registerFunction(funcName, functionCallTool);
        }
    }

    /**
     * 通过设备能力描述生成类型ID
     *
     * @param descriptor 设备描述符
     * @return 设备类型ID
     */
    private String generateDeviceTypeId(IotDescriptor descriptor) {
        // 获取并排序属性名
        List<String> properties = new ArrayList<>(descriptor.getProperties().keySet());
        Collections.sort(properties);

        // 获取并排序方法名
        List<String> methods = new ArrayList<>(descriptor.getMethods().keySet());
        Collections.sort(methods);

        // 使用属性和方法的组合作为设备类型的唯一标识
        return descriptor.getName() + ":" +
                String.join(",", properties) + ":" +
                String.join(",", methods);
    }

}
