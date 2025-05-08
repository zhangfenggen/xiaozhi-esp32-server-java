package com.xiaozhi.websocket.llm.providers;

import com.coze.openapi.client.chat.CreateChatReq;
import com.coze.openapi.client.chat.SubmitToolOutputsReq;
import com.coze.openapi.client.chat.model.ChatEvent;
import com.coze.openapi.client.chat.model.ChatEventType;
import com.coze.openapi.client.chat.model.ChatToolCall;
import com.coze.openapi.client.chat.model.ToolOutput;
import com.coze.openapi.client.connversations.message.model.Message;
import com.coze.openapi.service.auth.TokenAuth;
import com.coze.openapi.service.service.CozeAPI;
import com.xiaozhi.entity.SysMessage;
import com.xiaozhi.websocket.llm.api.AbstractLlmService;
import com.xiaozhi.websocket.llm.api.StreamResponseListener;
import com.xiaozhi.websocket.llm.api.ToolCallInfo;
import com.xiaozhi.websocket.llm.memory.ModelContext;
import com.xiaozhi.websocket.llm.tool.ActionType;
import com.xiaozhi.websocket.llm.tool.ToolResponse;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Coze LLM服务实现
 */
public class CozeService extends AbstractLlmService {

    private final TokenAuth authCli;
    private final CozeAPI coze;
    private final String botId;

    /**
     * 构造函数
     * 
     * @param endpoint  API端点
     * @param appId     应用ID (在Coze中对应botId)
     * @param apiKey    API密钥 (在Coze中不使用)
     * @param apiSecret API密钥 (在Coze中对应access_token)
     * @param model     模型名称 (在Coze中不使用)
     */
    public CozeService(String endpoint, String appId, String apiKey, String apiSecret, String model) {
        super(endpoint, appId, apiKey, apiSecret, model);

        // 使用apiSecret作为access_token
        this.authCli = new TokenAuth(apiSecret);

        // 使用endpoint或默认的Coze API地址
        String baseUrl = "https://api.coze.cn";

        // 初始化Coze API客户端
        this.coze = new CozeAPI.Builder()
                .baseURL(baseUrl)
                .auth(authCli)
                .readTimeout(60000) // 60秒超时
                .build();

        // 使用appId作为botId
        this.botId = model;

        logger.info("初始化Coze服务，botId: {}, baseUrl: {}", botId, baseUrl);
    }

    @Override
    protected String chat(List<Map<String, Object>> messages) throws IOException {
        if (messages == null || messages.isEmpty()) {
            throw new IOException("消息列表不能为空");
        }

        // 将消息格式转换为Coze API所需格式
        List<Message> cozeMessages = convertToCozeMessages(messages);

        // 创建唯一的用户ID
        String userId = "user_" + UUID.randomUUID().toString().replace("-", "");

        // 创建聊天请求
        CreateChatReq req = CreateChatReq.builder()
                .botID(botId)
                .userID(userId)
                .messages(cozeMessages)
                .build();

        try {
            // 创建CompletableFuture来处理异步响应
            CompletableFuture<String> future = new CompletableFuture<>();

            // 发送请求并获取流式响应
            StringBuilder fullResponse = new StringBuilder();
            Flowable<ChatEvent> resp = coze.chat().stream(req);

            resp.subscribeOn(Schedulers.io())
                    .subscribe(
                            event -> {
                                if (ChatEventType.CONVERSATION_MESSAGE_DELTA.equals(event.getEvent())) {
                                    String content = event.getMessage().getContent();
                                    if (content != null && !content.isEmpty()) {
                                        fullResponse.append(content);
                                    }
                                }
                                if (ChatEventType.CONVERSATION_CHAT_COMPLETED.equals(event.getEvent())) {
                                    future.complete(fullResponse.toString());
                                }
                            },
                            throwable -> {
                                logger.error("Coze API请求失败: {}", throwable.getMessage(), throwable);
                                future.completeExceptionally(throwable);
                            },
                            () -> {
                                // 如果没有收到CONVERSATION_CHAT_COMPLETED事件，确保完成future
                                if (!future.isDone()) {
                                    future.complete(fullResponse.toString());
                                }
                            });

            // 等待响应，设置超时
            return future.get(60, TimeUnit.SECONDS);

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("获取Coze响应时出错: {}", e.getMessage(), e);
            throw new IOException("获取Coze响应时出错: " + e.getMessage(), e);
        }
    }

    @Override
    protected void chatStream(List<Map<String, Object>> messages, StreamResponseListener streamListener, ModelContext modelContext)
            throws IOException {
        if (messages == null || messages.isEmpty()) {
            throw new IOException("消息列表不能为空");
        }

        // 通知开始
        streamListener.onStart();

        // 将消息格式转换为Coze API所需格式
        List<Message> cozeMessages = convertToCozeMessages(messages);

        // 创建唯一的用户ID
        String userId = "user_xz_" + modelContext.getDeviceId().replace(":", "");

        // 创建聊天请求
        CreateChatReq req = CreateChatReq.builder()
                .botID(botId)
                .userID(userId)
                .messages(cozeMessages)
                .build();

        // 保存完整响应
        StringBuilder fullResponse = new StringBuilder();

        // 发送请求
        try {
            Flowable<ChatEvent> resp = coze.chat().stream(req);
            CozeToolCallInfo toolCallInfo = new CozeToolCallInfo();
            resp.subscribeOn(Schedulers.io())
                    .subscribe(
                            event -> {
                                if (ChatEventType.CONVERSATION_MESSAGE_DELTA.equals(event.getEvent())) {
                                    String content = event.getMessage().getContent();
                                    if (content != null && !content.isEmpty()) {
                                        streamListener.onToken(content);
                                        fullResponse.append(content);
                                    }
                                }
                                /*
                                 * handle the chat event, if event type is CONVERSATION_CHAT_REQUIRES_ACTION,
                                 * it means the bot requires the invocation of local plugins.
                                 * In this example, we will invoke the local plugin to get the weather information.
                                 * */
                                if (ChatEventType.CONVERSATION_CHAT_REQUIRES_ACTION.equals(event.getEvent())) {
                                    toolCallInfo.chatID = event.getChat().getID();
                                    toolCallInfo.conversationID = event.getChat().getConversationID();
                                    //如果出现多工具调用，目前只执行第一个，后续再看是否需要处理多工具调用
                                    ChatToolCall callInfo = event.getChat().getRequiredAction().getSubmitToolOutputs().
                                            getToolCalls().get(0);
                                        String callID = callInfo.getID();
                                        String functionName = callInfo.getFunction().getName();
                                        String argsJson = callInfo.getFunction().getArguments();
                                        toolCallInfo.setTool_call_id(callID);
                                        toolCallInfo.setName(functionName);
                                        toolCallInfo.appendArgumentsJson(argsJson);
                                }
                            },
                            throwable -> {
                                logger.error("流式请求失败: {}", throwable.getMessage(), throwable);
                                streamListener.onError(throwable);
                            },
                            () -> {
                                boolean isFunctionResultReqLlm = false;
                                // 处理函数调用
                                if(toolCallInfo.getTool_call_id() != null){
                                    try{
                                        ToolResponse toolResponse = doFunctionCall(modelContext, toolCallInfo, streamListener, messages);
                                        if(toolResponse != null){
                                            if(ActionType.REQLLM.equals(toolResponse.getActionType())){
                                                isFunctionResultReqLlm = true;
                                            }else{
                                                //非REQLLM函数，则将消息添加到消息列表，并设置完整内容为工具的response内容
                                                fullResponse.append(toolResponse.getResponse());
                                                Map<String, Object> responseMessage = new HashMap<>();
                                                responseMessage.put("role", "assistant");
                                                responseMessage.put("content", fullResponse);
                                                responseMessage.put("messageType", SysMessage.MESSAGE_TYPE_FUNCTION_CALL);
                                                messages.add(responseMessage);
                                            }
                                        }
                                    }catch (Exception e){
                                        logger.error("函数调用失败: {}", e.getMessage(), e);
                                        streamListener.onError(e);
                                    }
                                }else{
                                    if (fullResponse.length() > 0) {
                                        Map<String, Object> responseMessage = new HashMap<>();
                                        responseMessage.put("role", "assistant");
                                        responseMessage.put("content", fullResponse);
                                        responseMessage.put("messageType", SysMessage.MESSAGE_TYPE_NORMAL);
                                        messages.add(responseMessage);
                                    }
                                }

                                // 如果没有收到CONVERSATION_CHAT_COMPLETED事件，在这里确保通知完成
                                if(!isFunctionResultReqLlm ){
                                    if (fullResponse.length() > 0) {
                                        streamListener.onComplete(fullResponse.toString());
                                        streamListener.onFinal(messages, CozeService.this);
                                    } else {
                                        streamListener.onError(new IOException("未收到有效响应"));
                                    }
                                }
                            });
        } catch (Exception e) {
            logger.error("创建流式请求时出错: {}", e.getMessage(), e);
            streamListener.onError(e);
        }
    }

    @Override
    protected void submitFunctionResultToLlm(ModelContext modelContext, ToolCallInfo toolCallInfo,
                                             StreamResponseListener streamListener, List<Map<String, Object>> messages,
                                             ToolResponse toolResponse){
        CozeToolCallInfo cozeToolCallInfo = (CozeToolCallInfo) toolCallInfo;
        List<ToolOutput> toolOutputs = new ArrayList<>();
        toolOutputs.add(ToolOutput.of(toolCallInfo.getTool_call_id(), "{ \"output\": \""+toolResponse.getResponse()+"\" }"));
        SubmitToolOutputsReq toolReq =
                SubmitToolOutputsReq.builder()
                        .chatID(cozeToolCallInfo.chatID)
                        .conversationID(cozeToolCallInfo.conversationID)
                        .toolOutputs(toolOutputs)
                        .stream(true)
                        .build();
        // 保存完整响应
        StringBuilder fullResponse = new StringBuilder();
        Flowable<ChatEvent> events = coze.chat().streamSubmitToolOutputs(toolReq);
        events.subscribeOn(Schedulers.io())
                .subscribe(
                        event -> {
                            if (ChatEventType.CONVERSATION_MESSAGE_DELTA.equals(event.getEvent())) {
                                String content = event.getMessage().getContent();
                                if (content != null && !content.isEmpty()) {
                                    if (content != null && !content.isEmpty()) {
                                        streamListener.onToken(content);
                                        fullResponse.append(content);
                                    }
                                }
                            }
                        },
                        throwable -> {
                            logger.error("提交函数结果失败: {}", throwable.getMessage(), throwable);
                            streamListener.onError(throwable);
                        }, ()->{
                            if (fullResponse.length() > 0) {
                                Map<String, Object> responseMessage = new HashMap<>();
                                responseMessage.put("role", "assistant");
                                responseMessage.put("content", fullResponse);
                                responseMessage.put("messageType", "NORMAL");
                                messages.add(responseMessage);
                                streamListener.onComplete(fullResponse.toString());
                                streamListener.onFinal(messages, CozeService.this);
                            } else {
                                streamListener.onError(new IOException("未收到有效响应"));
                            }
                        });
    }

    /**
     * 将通用消息格式转换为Coze API所需的消息格式
     * 
     * @param messages 通用格式的消息列表
     * @return Coze格式的消息列表
     */
    private List<Message> convertToCozeMessages(List<Map<String, Object>> messages) {
        List<Message> cozeMessages = new ArrayList<>();

        for (Map<String, Object> msg : messages) {
            String role = String.valueOf(msg.get("role"));
            String content = String.valueOf(msg.get("content"));

            if ("user".equals(role)) {
                cozeMessages.add(Message.buildUserQuestionText(content));
            } else if ("assistant".equals(role)) {
                cozeMessages.add(Message.buildAssistantAnswer(content));
            } else if ("system".equals(role)) {
                // coze 系统提示默认不在这里设定，需要在 coze 中设定
            }
        }

        return cozeMessages;
    }

    @Override
    public String getProviderName() {
        return "coze";
    }

    private class CozeToolCallInfo extends ToolCallInfo {
        private String conversationID;
        private String chatID;
    }

}