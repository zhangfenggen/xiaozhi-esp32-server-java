package com.xiaozhi.websocket.iot;

import com.fasterxml.jackson.databind.JsonNode;
import com.xiaozhi.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Iot设备描述信息
 */
public class IotDescriptor {
    private static final Logger logger = LoggerFactory.getLogger(IotDescriptor.class);

    private String name;
    private String description;
    private Map<String, IotProperty> properties;
    private Map<String, IotMethod> methods;

    public IotDescriptor(String name, String description, JsonNode properties, JsonNode methods) {
        this.name = name;
        this.description = description;
        this.properties = new HashMap<>();
        this.methods = new HashMap<>();

        try{
            // Process methods based on their description
            if (!methods.isMissingNode()) {
                Iterator<Map.Entry<String, JsonNode>> methodIterator = methods.fields();
                while (methodIterator.hasNext()) {
                    Map.Entry<String, JsonNode> entry = methodIterator.next();
                    String methodName = entry.getKey();
                    JsonNode value = entry.getValue();
                    String methodDescription = value.path("description").asText();
                    Map<String, IotMethodParameter> parameters = new HashMap<>();
                    IotMethod iotMethod = new IotMethod(
                            methodName,
                            methodDescription,
                            parameters
                    );
                    // Check if method has parameters
                    if (value.has("parameters")) {
                        JsonNode params = value.path("parameters");
                        Iterator<Map.Entry<String, JsonNode>> paramFields = params.fields();
                        while (paramFields.hasNext()) {
                            Map.Entry<String, JsonNode> paramEntry = paramFields.next();
                            String paramName = paramEntry.getKey();
                            JsonNode paramValue = paramEntry.getValue();
                            IotMethodParameter iotMethodParameter = new IotMethodParameter(paramName,
                                    paramValue.path("description").asText(),
                                    paramValue.path("type").asText());
                            parameters.put(paramName, iotMethodParameter);
                        }
                    }
                    this.methods.put(methodName, iotMethod);
                }
            }

            // Process properties based on their description
            if (!properties.isMissingNode()) {
                Iterator<Map.Entry<String, JsonNode>> propertyIterator = properties.fields();
                while (propertyIterator.hasNext()) {
                    Map.Entry<String, JsonNode> entry = propertyIterator.next();
                    String propertyName = entry.getKey();
                    JsonNode value = entry.getValue();
                    String propertyDescription = value.path("description").asText();
                    String propertyType = value.path("type").asText();
                    IotProperty iotProperty = new IotProperty(
                            propertyName,
                            propertyDescription,
                            propertyType
                    );
                    this.properties.put(propertyName, iotProperty);
                }
            }else{
                //处理缺失properties的情况，从methods中提取所有参数作为properties
                for (IotMethod method : this.methods.values()) {
                    Map<String, IotMethodParameter> parameters = method.getParameters();
                    if (!parameters.isEmpty()) {
                        for (IotMethodParameter iotMethodParameter : parameters.values()) {
                            String propertyType = iotMethodParameter.getType();
                            IotProperty iotProperty = new IotProperty(
                                    iotMethodParameter.getName(),
                                    iotMethodParameter.getDescription(),
                                    propertyType
                            );
                            this.properties.put(iotMethodParameter.getName(), iotProperty);
                        }
                    }
                }
            }
        }catch (Exception e){
            logger.error("IotDescriptor构造函数异常", e);
        }
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, IotProperty> getProperties() {
        return properties;
    }

    public Map<String, IotMethod> getMethods() {
        return methods;
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
