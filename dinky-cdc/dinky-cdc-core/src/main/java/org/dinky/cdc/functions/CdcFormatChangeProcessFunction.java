package org.dinky.cdc.functions;


import org.apache.commons.lang.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.dinky.cdc.utils.FlinkStatementUtil;
import org.dinky.data.model.FlinkCDCConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * CDC格式转换MapFunction
 * 用于处理CDC数据格式，特别是delete操作时将before数据复制到after
 *
 * @author qilianchao@gyyx.cn
 * @date 2025-08-16
 * @Description CDC数据格式转换函数
 */
public class CdcFormatChangeProcessFunction extends ProcessFunction<String, String> {
    private static final Logger logger = LoggerFactory.getLogger(CdcFormatChangeProcessFunction.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FlinkCDCConfig config;


    public CdcFormatChangeProcessFunction(FlinkCDCConfig config) {
        this.config = config;
    }

    @Override
    public void processElement(String value, ProcessFunction<String, String>.Context ctx, Collector<String> out) throws Exception {
        try {
            String addColumns = config.getAddColumns();
            if (StringUtils.isNotBlank(addColumns)) {
                JsonNode jsonNode = objectMapper.readTree(value);
                ObjectNode mutableNode = (ObjectNode) jsonNode;
                // 解析CDC JSON数据
                String op = jsonNode.get("op").asText();
                // 转换为可修改的ObjectNode
                Map<String, String> parsedAddColumnsMap = FlinkStatementUtil.parseAddColumns(addColumns);
                if ("d".equals(op)) {
                    JsonNode afterNode = jsonNode.get("before");
                    if (afterNode != null && !afterNode.isNull()) {
                        ObjectNode afterObjectNode = (ObjectNode) afterNode;
                        addCustomFields(afterObjectNode, parsedAddColumnsMap);
                    }
                } else {
                    // 非删除操作：直接在after数据中添加字段
                    JsonNode afterNode = jsonNode.get("after");
                    if (afterNode != null && !afterNode.isNull()) {
                        ObjectNode afterObjectNode = (ObjectNode) afterNode;
                        addCustomFields(afterObjectNode, parsedAddColumnsMap);
                    }
                }
                out.collect(objectMapper.writeValueAsString(mutableNode));
            } else {
                out.collect(value);
            }
        } catch (Exception e) {
            // 出错时返回原始数据，避免数据丢失
            if (StringUtils.isNotBlank(value) && value.contains("before") && value.contains("after")) {
                logger.error("Error processing CDC data: {}", e.getMessage(), e);
            }
            out.collect(value);
        }

    }

    /**
     * 添加自定义字段到数据中
     *
     * @param dataNode 要添加字段的数据节点
     */
    private void addCustomFields(ObjectNode dataNode, Map<String, String> parsedAddColumnsMap) {
        try {
            // 1. 从配置中解析要添加的字段
            if (!parsedAddColumnsMap.isEmpty()) {
                for (Map.Entry<String, String> entry : parsedAddColumnsMap.entrySet()) {
                    String fieldName = entry.getKey();
                    String fieldValue = entry.getValue();
                    dataNode.put(fieldName, fieldValue);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to add custom fields: {}", e.getMessage());
        }
    }
}