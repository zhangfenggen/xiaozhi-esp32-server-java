package com.xiaozhi.websocket.llm.tool.function;

import com.xiaozhi.websocket.llm.tool.ActionType;
import com.xiaozhi.websocket.llm.tool.ToolResponse;
import com.xiaozhi.websocket.llm.tool.ToolType;
import com.xiaozhi.websocket.llm.tool.function.bean.FunctionCallTool;
import com.xiaozhi.websocket.llm.tool.function.bean.FunctionLlmDescription;
import com.xiaozhi.websocket.llm.tool.function.bean.description.FunctionDesc;
import com.xiaozhi.websocket.llm.tool.function.bean.description.ParameterDesc;
import com.xiaozhi.websocket.llm.tool.function.bean.description.PropertyDesc;
import com.xiaozhi.websocket.service.SessionManager;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 退出会话函数
 * 该函数用于退出会话
 */
@Service
public class SessionExitFunction implements FunctionGlobalRegistry.GlobalFunction {
    @Resource
    private SessionManager sessionManager;

    @Override
    public FunctionCallTool getFunctionCallTool() {
        // 创建函数名称
        String funcName = "handle_exit_intent";
        // 创建函数对象
        FunctionDesc function = new FunctionDesc(funcName, "当用户想结束对话或需要退出时调用function：handle_exit_intent");
        // 创建参数对象
        ParameterDesc parameters = new ParameterDesc();
        // 添方法参数
        parameters.addProperty("say_goodbye", new PropertyDesc("和用户友好结束对话的告别语"), true);
        // 向函数中添加属性值参数
        function.setParameters(parameters);

        FunctionLlmDescription functionLlmDescription = new FunctionLlmDescription(function);
        return new FunctionCallTool(funcName, ToolType.SYSTEM_CTL, functionLlmDescription, (functionParams) -> {
            try{
                sessionManager.setCloseAfterChat(functionParams.context.getSessionId(), true);
                return new ToolResponse(ToolType.IOT_CTL, ActionType.RESPONSE, "退出意图已处理", functionParams.params.get("say_goodbye").toString());
            }catch (Exception e){
                return new ToolResponse(ToolType.IOT_CTL, ActionType.ERROR, "退出意图处理失败", e.getMessage());
            }
        });
    }
}
