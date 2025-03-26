package com.xiaozhi;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.agentsflex.core.llm.Llm;
import com.agentsflex.core.message.HumanMessage;
import com.agentsflex.core.message.SystemMessage;
import com.agentsflex.core.prompt.HistoriesPrompt;
import com.agentsflex.llm.openai.OpenAILlm;
import com.agentsflex.llm.openai.OpenAILlmConfig;
import com.xiaozhi.websocket.llm.DatabaseChatMemory;

@SpringBootTest
@ActiveProfiles("test")
public class ApplicationTest {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationTest.class);

    public static void main(String[] args) throws Exception {
        OpenAILlmConfig config = new OpenAILlmConfig();
        config.setEndpoint("https://api.hunyuan.cloud.tencent.com");
        config.setModel("hunyuan-turbo");
        config.setApiKey("");
        config.setDebug(true);
        Llm llm = new OpenAILlm(config);
        HistoriesPrompt prompt = new HistoriesPrompt();
        prompt.setSystemMessage(new SystemMessage("你是我的助手"));
        // DatabaseChatMemory.printFieldsRecursive(prompt.getMemory().getMessages(), "",
        // new HashSet<>(), 5);

        prompt.addMessage(new HumanMessage("你好"));

        llm.chatStream(prompt, (context, response) -> {
            if (response.getMessage().getStatus().toString().equals("END")) {
                System.out.println("default>>>> " + response);
            }
        });
        ;

        Thread.sleep(5000);

        prompt.addMessage(new HumanMessage("你是谁"));

        llm.chatStream(prompt, (context, response) -> {
            if (response.getMessage().getStatus().toString().equals("END")) {
                System.out.println("default>>>> " + response);
            }
        });
        ;

        DatabaseChatMemory.printFieldsRecursive(prompt.getMemory().getMessages(), "", new HashSet<>(), 5);
    }
}