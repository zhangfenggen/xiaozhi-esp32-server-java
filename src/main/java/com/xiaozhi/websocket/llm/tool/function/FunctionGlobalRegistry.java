package com.xiaozhi.websocket.llm.tool.function;

import com.xiaozhi.websocket.llm.tool.function.bean.FunctionCallTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FunctionGlobalRegistry {
    private final Logger logger = LoggerFactory.getLogger(FunctionGlobalRegistry.class);
    private static final String TAG = "FUNCTION_GLOBAL";

    // 用于存储所有function列表
    protected static final ConcurrentHashMap<String, FunctionCallTool> allFunction
            = new ConcurrentHashMap<>();

    @Resource
    protected List<GlobalFunction> globalFunctions;

    @PostConstruct
    protected void init(){
        globalFunctions.forEach(
                globalFunction -> {
                    registerFunction(globalFunction.getFunctionCallTool().getName(), globalFunction.getFunctionCallTool());
                }
        );
    }
    /**
     * Register a function by name
     *
     * @param name the name of the function to register
     * @return the registered function or null if not found
     */
    public FunctionCallTool registerFunction(String name, FunctionCallTool functionCallTool) {
        FunctionCallTool result = allFunction.putIfAbsent(name, functionCallTool);
        logger.info("[{}] Function:{} registered into global successfully", TAG, name);
        return result;
    }

    /**
     * Unregister a function by name
     *
     * @param name the name of the function to unregister
     * @return true if successful, false otherwise
     */
    public boolean unregisterFunction(String name) {
        // Check if the function exists before unregistering
        if (!allFunction.containsKey(name)) {
            logger.error("[{}] Function:{} not found", TAG, name);
            return false;
        }
        allFunction.remove(name);
        logger.info("[{}] Function:{} unregistered successfully", TAG, name);
        return true;
    }

    /**
     * Get a function by name
     *
     * @param name the name of the function to retrieve
     * @return the function or null if not found
     */
    public FunctionCallTool getFunction(String name) {
        return allFunction.get(name);
    }

    /**
     * Get all registered functions
     *
     * @return a map of all registered functions
     */
    public Map<String, FunctionCallTool> getAllFunctions() {
        return allFunction;
    }

    public interface GlobalFunction{
        FunctionCallTool getFunctionCallTool();
    }
}
