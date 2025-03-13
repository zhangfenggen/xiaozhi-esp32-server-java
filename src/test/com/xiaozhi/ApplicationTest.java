package com.xiaozhi;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.agentsflex.core.llm.Llm;
import com.agentsflex.llm.ollama.OllamaLlm;
import com.agentsflex.llm.ollama.OllamaLlmConfig;

@SpringBootTest
@ActiveProfiles("test")
public class ApplicationTest {

    public static void main(String[] args) throws Exception {
        OllamaLlmConfig config = new OllamaLlmConfig();
        config.setEndpoint("http://localhost:11434");
        config.setModel("qwen2.5:7b");
        config.setDebug(true);

        Llm llm = new OllamaLlm(config);
        llm.chatStream("你是谁？", (context, response) -> System.out.println(response.getMessage()));

    }

}