package com.xiaozhi.websocket.llm.providers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xiaozhi.utils.JsonUtil;
import com.xiaozhi.websocket.llm.api.AbstractLlmService;
import com.xiaozhi.websocket.llm.api.StreamResponseListener;
import com.xiaozhi.websocket.llm.api.ToolCallInfo;
import com.xiaozhi.websocket.llm.memory.ModelContext;
import com.xiaozhi.websocket.llm.tool.ActionType;
import com.xiaozhi.websocket.llm.tool.ToolResponse;
import com.xiaozhi.websocket.llm.tool.function.FunctionSessionHolder;
import com.xiaozhi.websocket.llm.tool.function.bean.FunctionCallTool;
import com.xiaozhi.websocket.llm.tool.function.bean.FunctionLlmDescription;
import okhttp3.*;
import okio.BufferedSource;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI LLM服务实现
 */
public class OpenAiService extends AbstractLlmService {

    /**
     * 构造函数
     * 
     * @param endpoint API端点
     * @param apiKey   API密钥
     * @param model    模型名称
     */
    public OpenAiService(String endpoint, String appId, String apiKey, String apiSecret, String model) {
        super(endpoint, appId, apiKey, apiSecret, model);
    }

    @Override
    protected String chat(List<Map<String, Object>> messages) throws IOException {
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);

        // 转换为JSON
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // 构建请求
        Request request = new Request.Builder()
                .url(endpoint + "/chat/completions")
                .post(RequestBody.create(jsonBody, JSON))
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        // 发送请求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody,
                    new TypeReference<Map<String, Object>>() {
                    });

            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }

            throw new IOException("无法解析OpenAI响应");
        }
    }

    @Override
    protected void chatStream(List<Map<String, Object>> messages, StreamResponseListener streamListener, ModelContext modelContext)
            throws IOException {
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("stream", true);
        requestBody.put("messages", messages);

        FunctionSessionHolder functionSessionHolder = modelContext.getFunctionSessionHolder();
        if(functionSessionHolder != null){
            List<FunctionLlmDescription> tools = functionSessionHolder.getAllFunctionLlmDescription();
            if(!tools.isEmpty()){
                requestBody.put("tools", tools);
            }
        }

        // 转换为JSON
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // 构建请求
        Request request = new Request.Builder()
                .url(endpoint + "/chat/completions")
                .post(RequestBody.create(jsonBody, JSON))
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        // 通知开始
        streamListener.onStart();

        // 发送请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("流式请求失败: {}", e.getMessage(), e);
                streamListener.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorMsg = "流式请求响应失败: " + response;
                    logger.error(errorMsg);
                    streamListener.onError(new IOException(errorMsg));
                    return;
                }
                boolean isFunctionCall = false;
                String tool_call_id = null;
                String functionName = null;
                StringBuilder functionArguments = new StringBuilder();

                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        String errorMsg = "响应体为空";
                        logger.error(errorMsg);
                        streamListener.onError(new IOException(errorMsg));
                        return;
                    }

                    BufferedSource source = responseBody.source();
                    StringBuilder fullResponse = new StringBuilder();

                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();
                        if (line == null) {
                            break;
                        }
                        if (line.isEmpty() || line.equals("data: [DONE]")) {
                            continue;
                        }
                        if (line.startsWith("data: ")) {
                            String jsonData = line.substring(6);
                            try {
                                Map<String, Object> data = objectMapper.readValue(jsonData,
                                        new TypeReference<Map<String, Object>>() {
                                        });
                                List<Map<String, Object>> choices = (List<Map<String, Object>>) data.get("choices");
                                if (choices != null && !choices.isEmpty()) {
                                    Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                                    if (delta != null) {
                                        //处理function_call
                                        if(delta.containsKey("tool_calls") && delta.get("tool_calls") != null){
                                            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) delta.get("tool_calls");
                                            if (toolCalls != null && !toolCalls.isEmpty()) {
                                                Map<String, Object> toolInfo =  toolCalls.get(0);
                                                tool_call_id = isFunctionCall? tool_call_id : (String) toolInfo.get("id");
                                                String new_tool_id = (String) toolInfo.get("id");
                                                if(new_tool_id != null && !new_tool_id.isEmpty()){//new_tool_id不为空，则说明是一个function调用信息
                                                    tool_call_id = new_tool_id;
                                                    Map<String, String> toolCall = (Map<String, String>)toolInfo.get("function");
                                                    functionName = toolCall.get("name");//获取function的名称
                                                    if(isFunctionCall){//如果已经标识为true，则当前流回复里，有多function调用，暂时只处理最后一个调用
                                                        functionArguments = new StringBuilder();//重置下参数字符串，避免把多函数的参数都拼一起去了
                                                    }else{
                                                        isFunctionCall = true;
                                                    }
                                                }
                                                if (isFunctionCall) {
                                                    Map<String, String> toolCall = (Map<String, String>)toolInfo.get("function");
                                                    String arguments = toolCall.get("arguments");
                                                    if (arguments != null && !arguments.isEmpty()) {
                                                        functionArguments.append(arguments);
                                                    }
                                                }
                                            }
                                        }
                                        //处理普通消息内容
                                        if(delta.containsKey("content")){
                                            String content = (String) delta.get("content");
                                            if (content != null && !content.isEmpty()) {
                                                streamListener.onToken(content);
                                                fullResponse.append(content);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("解析流式响应失败: {}", e.getMessage(), e);
                                streamListener.onError(e);
                            }
                        }
                    }

                    ToolCallInfo toolCallInfo = null;
                    // 处理函数调用
                    if(isFunctionCall){
                        try{
                            toolCallInfo = new ToolCallInfo(tool_call_id, functionName, objectMapper.readValue(functionArguments.toString(),
                                    new TypeReference<Map<String, Object>>() {}));
                            doFunctionCall(modelContext, toolCallInfo, streamListener, messages, fullResponse);
                        }catch (Exception e){
                            logger.error("函数调用失败: {}", e.getMessage(), e);
                            streamListener.onError(e);
                        }
                    }else{
                        Map<String, Object> responseMessage = new HashMap<>();
                        responseMessage.put("role", "assistant");
                        responseMessage.put("content", fullResponse);
                        responseMessage.put("messageType", "NORMAL");
                        messages.add(responseMessage);
                    }
                    // 通知完成
                    streamListener.onComplete(fullResponse.toString());
                    streamListener.onFinal(messages, OpenAiService.this, toolCallInfo);
                }
            }
        });
    }

    private void doFunctionCall(ModelContext modelContext, ToolCallInfo toolCallInfo, StreamResponseListener streamListener, List<Map<String, Object>> messages, StringBuilder fullResponse) throws Exception {
        FunctionSessionHolder functionSessionHolder = modelContext.getFunctionSessionHolder();
        if(functionSessionHolder != null){
            FunctionCallTool functionCallTool = functionSessionHolder.getFunction(toolCallInfo.getName());
            if(functionCallTool!=null){
                FunctionCallTool.FunctionParams functionParams = new FunctionCallTool.FunctionParams(modelContext, toolCallInfo.getArguments());
                ToolResponse toolResponse = functionCallTool.getFunction().apply(functionParams);
                logger.debug("Function call: {} with arguments: {} result： {}", toolCallInfo.getName(), toolCallInfo.getArguments(), toolResponse);
                if(ActionType.REQLLM.equals(toolResponse.getActionType())){
                    //这里的工具消息没有存入历史，如有需要再处理
                    Map<String, Object> assistantMessage = createLlAssistantMessage("", toolCallInfo.getTool_call_id(), toolCallInfo.getName(),
                            toolCallInfo.getArguments());
                    Map<String, Object> toolMessage = createLlmToolMessage(toolResponse.getResult(), toolCallInfo.getTool_call_id());
                    messages.add(assistantMessage);
                    messages.add(toolMessage);
                    //继续把工具消息传给llm，让大模型总结输出
                    chatStream(messages, streamListener, modelContext);
                }else if(ActionType.RESPONSE.equals(toolResponse.getActionType())) {
                    streamListener.onToken(toolResponse.getResponse());
                    fullResponse.append(toolResponse.getResponse());
                }else if(ActionType.ERROR.equals(toolResponse.getActionType())) {
                    streamListener.onToken(toolResponse.getResult());
                    fullResponse.append(toolResponse.getResponse());
                }
                Map<String, Object> responeMessage = new HashMap<>();
                responeMessage.put("role", "assistant");
                responeMessage.put("content", toolResponse.getResponse());
                responeMessage.put("messageType", "FUNCTION_CALL");
                messages.add(responeMessage);
            }else{
                logger.error("llm回调未找到函数: 函数名: {} with arguments: {}toolId: {} ", toolCallInfo.getName(), toolCallInfo.getArguments(), toolCallInfo.getTool_call_id());
            }
        }
    }

    private Map<String, Object> createLlAssistantMessage(String content, String tool_call_id, String functionName, Map<String, Object> arguments) {
        Map<String, Object> message = new HashMap<>();

        Map<String, Object> function = new HashMap<>();
        function.put("arguments", JsonUtil.toJson(arguments));
        function.put("name", functionName);

        Map<String, Object> tool_call = new HashMap<>();
        tool_call.put("id", tool_call_id);
        tool_call.put("function", function);
        tool_call.put("type", "function");
        tool_call.put("index", 0);
        List<Map<String, Object>> tool_calls = Collections.singletonList(tool_call);

        message.put("role", "assistant");
        message.put("content", content);
        message.put("tool_calls", tool_calls);
        //平台自行标记，非接口字段
        message.put("messageType", "FUNCTION_CALL");

        return message;
    }

    private Map<String, Object> createLlmToolMessage(String content, String toolId) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "tool");
        message.put("content", content);
        message.put("tool_id", toolId);
        //平台自行标记，非接口字段
        message.put("messageType", "FUNCTION_CALL");
        return message;
    }

    @Override
    public String getProviderName() {
        return "openai";
    }
}