package org.dinky.cdc.functions;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import org.dinky.cdc.utils.FlinkStatementUtil;
import org.dinky.data.model.FlinkCDCConfig;
import org.dinky.utils.TextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;
import java.util.Map;

/**
 * CDC格式转换MapFunction
 * 用于处理CDC数据格式，特别是delete操作时将before数据复制到after
 *
 * @author qilianchao@gyyx.cn
 * @date 2025-08-16
 * @Description CDC数据格式转换函数
 */
public class CdcFormatChangeMapFunction implements MapFunction<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(CdcFormatChangeMapFunction.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FlinkCDCConfig config;


    public CdcFormatChangeMapFunction(FlinkCDCConfig config) {
        this.config = config;
    }

    @Override
    public String map(String cdcJson) throws Exception {
        try {
            // 解析CDC JSON数据
            JsonNode jsonNode = objectMapper.readTree(cdcJson);
            String op = jsonNode.get("op").asText();
            // 转换为可修改的ObjectNode
            ObjectNode mutableNode = (ObjectNode) jsonNode;
            // 处理删除操作：将before数据复制到after
            Map<String, String> parsedAddColumnsMap = FlinkStatementUtil.parseAddColumns(config.getAddColumns());
            // 处理删除操作：将before数据复制到after
            if ("d".equals(op)) {
                JsonNode beforeNode = jsonNode.get("before");
                if (beforeNode != null && !beforeNode.isNull()) {
                    // 将before数据复制到after
                    ObjectNode newAfterNode = (ObjectNode) beforeNode.deepCopy();

                    //添加自定义字段到after数据中
                    if (!parsedAddColumnsMap.isEmpty()) {
                        addCustomFields(newAfterNode, parsedAddColumnsMap);
                    }
                    // 将修改后的数据设置到after字段
                    mutableNode.set("after", newAfterNode);

                    logger.debug("Delete operation processed: copied before to after");
                }
            } else {
                // 非删除操作：直接在after数据中添加字段
                JsonNode afterNode = jsonNode.get("after");
                if (afterNode != null && !afterNode.isNull()) {
                    ObjectNode afterObjectNode = (ObjectNode) afterNode;
                    addCustomFields(afterObjectNode, parsedAddColumnsMap);
                }
            }
            return objectMapper.writeValueAsString(mutableNode);
        } catch (Exception e) {
            logger.error("Error processing CDC data: {}", e.getMessage(), e);
            // 出错时返回原始数据，避免数据丢失
            return cdcJson;
        }
    }


    /**
     * cdc json数据修改
     *
     * @param dataNode 要修改的数据节点
     */
    private ObjectNode cdcJsonChange(ObjectNode dataNode) {
        return null;
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
