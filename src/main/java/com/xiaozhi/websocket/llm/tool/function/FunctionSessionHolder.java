package com.xiaozhi.websocket.llm.tool.function;

import com.xiaozhi.websocket.llm.tool.function.bean.FunctionCallTool;
import com.xiaozhi.websocket.llm.tool.function.bean.FunctionLlmDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 与session绑定的functionTools
 */
public class FunctionSessionHolder {
    private final Logger logger = LoggerFactory.getLogger(FunctionSessionHolder.class);

    private static final String TAG = "FUNCTION_SESSION";

    private final Map<String, FunctionCallTool> functionRegistry = new HashMap<>();

    private String sessionId;

    private FunctionGlobalRegistry globalFunctionRegistry;

    public FunctionSessionHolder(String sessionId, FunctionGlobalRegistry globalFunctionRegistry) {
        this.sessionId = sessionId;
        this.globalFunctionRegistry = globalFunctionRegistry;
    }

    /**
     * Register a global function by name
     *
     * @param name the name of the function to register
     * @return the registered function or null if not found
     */
    public FunctionCallTool registerFunction(String name) {
        // Look up the function in the globalFunctionRegistry
        FunctionCallTool func = globalFunctionRegistry.getFunction(name);
        if (func == null) {
            logger.error("[{}] - SessionId:{} Function:{} not found in globalFunctionRegistry", TAG, sessionId, name);
            return null;
        }
        functionRegistry.put(name, func);
        logger.debug("[{}] - SessionId:{} Function:{} registered from global successfully", TAG, sessionId, name);
        return func;
    }

    /**
     * Register a function by name
     *
     * @param name the name of the function to register
     * @return the registered function or null if not found
     */
    public void registerFunction(String name, FunctionCallTool functionCallTool) {
        functionRegistry.put(name, functionCallTool);
        logger.debug("[{}] - SessionId:{} Function:{} registered successfully", TAG, sessionId, name);
    }

    /**
     * Unregister a function by name
     *
     * @param name the name of the function to unregister
     * @return true if successful, false otherwise
     */
    public boolean unregisterFunction(String name) {
        // Check if the function exists before unregistering
        if (!functionRegistry.containsKey(name)) {
            logger.error("[{}] - SessionId:{} Function:{} not found", TAG, sessionId, name);
            return false;
        }
        functionRegistry.remove(name);
        logger.info("[{}] - SessionId:{} Function:{} unregistered successfully", TAG, sessionId, name);
        return true;
    }

    /**
     * Get a function by name
     *
     * @param name the name of the function to retrieve
     * @return the function or null if not found
     */
    public FunctionCallTool getFunction(String name) {
        return functionRegistry.get(name);
    }

    /**
     * Get all registered functions
     *
     * @return a map of all registered functions
     */
    public Map<String, FunctionCallTool> getAllFunction() {
        return functionRegistry;
    }

    /**
     * Get all registered functions name
     *
     * @return a list of all registered function name
     */
    public List<String> getAllFunctionName() {
        return new ArrayList<>(functionRegistry.keySet());
    }

    /**
     * Get llm descriptions of all registered functions
     *
     * @return a list of all function descriptions
     */
    public List<FunctionLlmDescription> getAllFunctionLlmDescription() {
        return functionRegistry.values().stream()
                .map(FunctionCallTool::getFunctionLlmDescription).collect(Collectors.toList());
    }

}
