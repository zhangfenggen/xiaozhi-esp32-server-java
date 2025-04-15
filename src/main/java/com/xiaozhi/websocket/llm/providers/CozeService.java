package com.xiaozhi.websocket.llm.providers;

import com.coze.openapi.client.chat.CancelChatReq;
import com.coze.openapi.client.chat.CreateChatReq;
import com.coze.openapi.client.chat.CreateChatResp;
import com.coze.openapi.client.chat.RetrieveChatReq;
import com.coze.openapi.client.chat.RetrieveChatResp;
import com.coze.openapi.client.chat.model.Chat;
import com.coze.openapi.client.chat.model.ChatEvent;
import com.coze.openapi.client.chat.model.ChatEventType;
import com.coze.openapi.client.chat.model.ChatPoll;
import com.coze.openapi.client.chat.model.ChatStatus;
import com.coze.openapi.client.connversations.message.model.Message;
import com.coze.openapi.service.auth.TokenAuth;
import com.coze.openapi.service.service.CozeAPI;
import com.fasterxml.jackson.core.type.TypeReference;
import com.xiaozhi.websocket.llm.api.AbstractLlmService;
import com.xiaozhi.websocket.llm.api.StreamResponseListener;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Coze LLM服务实现
 */
public class CozeService extends AbstractLlmService {

    TokenAuth authCli = new TokenAuth(apiSecret);

    // Init the Coze client through the access_token.
    CozeAPI coze = new CozeAPI.Builder()
            .baseURL("https://api.coze.cn")
            .auth(authCli)
            .readTimeout(10000)
            .build();

    /**
     * 构造函数
     * 
     * @param endpoint API端点
     * @param apiKey   API密钥
     * @param model    模型名称
     */
    public CozeService(String endpoint, String appId, String apiKey, String apiSecret, String model) {
        super(endpoint, appId, apiKey, apiSecret, model);
    }

    @Override
    public String chat(List<Map<String, String>> messages) throws IOException {
        return null;
    }

    @Override
    public void chatStream(List<Map<String, String>> messages, StreamResponseListener streamListener)
            throws IOException {

        // 创建请求
        CreateChatReq req = CreateChatReq.builder()
                .botID("7493051061784215587")
                .userID("user_" + System.currentTimeMillis()) // 生成唯一用户ID
                .messages(Collections.singletonList(Message.buildUserQuestionText("What can you do?")))
                .build();

        // 通知开始
        streamListener.onStart();

        // 保存完整响应
        StringBuilder fullResponse = new StringBuilder();

        // 发送请求
        Flowable<ChatEvent> resp = coze.chat().stream(req);
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
                            if (ChatEventType.CONVERSATION_CHAT_COMPLETED.equals(event.getEvent())) {
                                // 通知完成
                                streamListener.onComplete(fullResponse.toString());
                            }
                        },
                        throwable -> {
                            logger.error("流式请求失败: {}", throwable.getMessage(), throwable);
                            streamListener.onError(throwable);
                        },
                        () -> {
                            // 如果没有收到CONVERSATION_CHAT_COMPLETED事件，在这里确保通知完成
                            if (fullResponse.length() > 0) {
                                streamListener.onComplete(fullResponse.toString());
                            }
                        });
    }

    @Override
    public String getProviderName() {
        return "coze";
    }
}
