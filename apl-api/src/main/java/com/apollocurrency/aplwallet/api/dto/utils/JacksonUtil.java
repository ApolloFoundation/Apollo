package com.apollocurrency.aplwallet.api.dto.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

public class JacksonUtil {

    public static LinkedHashMap<?, ?> parseJsonNodeGraph(JsonNode node) {
        if (node.isObject()) {
            return (LinkedHashMap<?, ?>) parseValue(node);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static Object parseValue(JsonNode node) {
        if (node.isObject()) {
            LinkedHashMap<Object, Object> map = new LinkedHashMap<>();
            Iterator<String> fieldNames = node.fieldNames();

            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = node.get(fieldName);
                map.put(fieldName, parseValue(fieldValue));
            }
            return map;
        } else if (node.isArray()) {
            List<Object> array = new ArrayList<>();
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                array.add(parseValue(arrayNode.get(i)));
            }
            return array;
        } else {
            if (node.isNumber()) {
                return node.asLong();
            }
            if (node.isDouble()) {
                return node.asDouble();
            }
            if (node.isTextual() || node.isBinary()) {
                return node.asText();
            }
            if (node.isBoolean()) {
                return node.asBoolean();
            }
            if (node.isEmpty() || node.isNull()) {
                return null;
            } else {
                throw new IllegalArgumentException("Unknown node type. Node:" + node.asText());
            }
        }

    }
}
