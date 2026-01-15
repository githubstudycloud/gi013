package com.glm.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JSON嵌套值提取工具类
 * 
 * <p>从嵌套的JSON结构中递归提取指定路径下特定键的所有值并去重。
 * 支持任意深度的嵌套结构（包括嵌套的对象、数组、数组中的对象等）。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * String json = "{\"a\":{\"aenv\":\"env1\",\"nested\":{\"aenv\":\"env2\"}}}";
 * Set<Object> values = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
 * // 结果: ["env1", "env2"]
 * }</pre>
 * 
 * @author GLM
 * @version 1.0.0
 * @since JDK 1.8
 */
public class JsonValueExtractor {

    private static final Gson GSON = new Gson();

    /**
     * 私有构造函数，防止实例化
     */
    private JsonValueExtractor() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 从JSON字符串的指定路径下提取目标键的所有值（去重）
     *
     * @param jsonString JSON字符串
     * @param pathKey    限定搜索的路径键名（如 "a" 或 "b"）
     * @param targetKey  要提取的目标键名（如 "aenv" 或 "benv"）
     * @return 去重后的值集合
     * @throws IllegalArgumentException 如果jsonString为null或空
     */
    public static Set<Object> extractValuesUnderPath(String jsonString, String pathKey, String targetKey) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
        if (pathKey == null || targetKey == null) {
            throw new IllegalArgumentException("pathKey and targetKey cannot be null");
        }

        JsonElement root = JsonParser.parseString(jsonString);
        if (!root.isJsonObject()) {
            return Collections.emptySet();
        }

        JsonObject jsonObject = root.getAsJsonObject();
        if (jsonObject.has(pathKey)) {
            return extractValuesFromNested(jsonObject.get(pathKey), targetKey);
        }
        return Collections.emptySet();
    }

    /**
     * 从JsonObject的指定路径下提取目标键的所有值（去重）
     *
     * @param jsonObject JSON对象
     * @param pathKey    限定搜索的路径键名
     * @param targetKey  要提取的目标键名
     * @return 去重后的值集合
     */
    public static Set<Object> extractValuesUnderPath(JsonObject jsonObject, String pathKey, String targetKey) {
        if (jsonObject == null) {
            return Collections.emptySet();
        }
        if (pathKey == null || targetKey == null) {
            throw new IllegalArgumentException("pathKey and targetKey cannot be null");
        }

        if (jsonObject.has(pathKey)) {
            return extractValuesFromNested(jsonObject.get(pathKey), targetKey);
        }
        return Collections.emptySet();
    }

    /**
     * 从任意嵌套结构中递归搜索路径键，然后在其下提取目标键的所有值
     * 
     * <p>适用于pathKey本身也可能嵌套在任意深度的情况</p>
     *
     * @param jsonString JSON字符串
     * @param pathKey    限定搜索的路径键名
     * @param targetKey  要提取的目标键名
     * @return 去重后的值集合
     */
    public static Set<Object> extractAllValues(String jsonString, String pathKey, String targetKey) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
        if (pathKey == null || targetKey == null) {
            throw new IllegalArgumentException("pathKey and targetKey cannot be null");
        }

        JsonElement root = JsonParser.parseString(jsonString);
        return extractAllValuesRecursive(root, pathKey, targetKey);
    }

    /**
     * 批量提取多组 pathKey -> targetKey 的值
     *
     * @param jsonString JSON字符串
     * @param mappings   映射列表，每个元素为 [pathKey, targetKey]
     * @return Map，key为targetKey，value为提取到的值集合
     */
    public static Map<String, Set<Object>> batchExtract(String jsonString, List<String[]> mappings) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
        if (mappings == null) {
            throw new IllegalArgumentException("Mappings cannot be null");
        }

        Map<String, Set<Object>> result = new LinkedHashMap<>();
        for (String[] mapping : mappings) {
            if (mapping != null && mapping.length >= 2) {
                String pathKey = mapping[0];
                String targetKey = mapping[1];
                Set<Object> values = extractAllValues(jsonString, pathKey, targetKey);
                result.put(targetKey, values);
            }
        }
        return result;
    }

    /**
     * 批量提取并返回List形式的结果
     *
     * @param jsonString JSON字符串
     * @param mappings   映射列表
     * @return Map，key为targetKey，value为提取到的值列表
     */
    public static Map<String, List<Object>> batchExtractAsList(String jsonString, List<String[]> mappings) {
        Map<String, Set<Object>> setResult = batchExtract(jsonString, mappings);
        return setResult.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new ArrayList<>(e.getValue()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * 提取指定目标键的所有字符串值（去重）
     *
     * @param jsonString JSON字符串
     * @param pathKey    限定搜索的路径键名
     * @param targetKey  要提取的目标键名
     * @return 去重后的字符串值集合
     */
    public static Set<String> extractStringValues(String jsonString, String pathKey, String targetKey) {
        Set<Object> values = extractAllValues(jsonString, pathKey, targetKey);
        return values.stream()
                .filter(v -> v instanceof String)
                .map(v -> (String) v)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 提取指定目标键的所有字符串值并返回List
     *
     * @param jsonString JSON字符串
     * @param pathKey    限定搜索的路径键名
     * @param targetKey  要提取的目标键名
     * @return 去重后的字符串值列表
     */
    public static List<String> extractStringValuesAsList(String jsonString, String pathKey, String targetKey) {
        return new ArrayList<>(extractStringValues(jsonString, pathKey, targetKey));
    }

    // ==================== 内部方法 ====================

    /**
     * 从嵌套的JsonElement中递归提取所有指定key的值
     */
    private static Set<Object> extractValuesFromNested(JsonElement element, String targetKey) {
        Set<Object> results = new LinkedHashSet<>();

        if (element == null || element.isJsonNull()) {
            return results;
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (entry.getKey().equals(targetKey)) {
                    // 找到目标key，添加其值
                    addValueToSet(results, entry.getValue());
                }
                // 继续递归搜索
                results.addAll(extractValuesFromNested(entry.getValue(), targetKey));
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                results.addAll(extractValuesFromNested(item, targetKey));
            }
        }

        return results;
    }

    /**
     * 递归搜索路径键，然后在其下提取目标键的值
     */
    private static Set<Object> extractAllValuesRecursive(JsonElement element, String pathKey, String targetKey) {
        Set<Object> results = new LinkedHashSet<>();

        if (element == null || element.isJsonNull()) {
            return results;
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (entry.getKey().equals(pathKey)) {
                    // 找到路径节点，在其下提取目标值
                    results.addAll(extractValuesFromNested(entry.getValue(), targetKey));
                } else {
                    // 继续向下搜索路径节点
                    results.addAll(extractAllValuesRecursive(entry.getValue(), pathKey, targetKey));
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                results.addAll(extractAllValuesRecursive(item, pathKey, targetKey));
            }
        }

        return results;
    }

    /**
     * 将JsonElement的值添加到结果集合中
     */
    private static void addValueToSet(Set<Object> results, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                results.add(primitive.getAsString());
            } else if (primitive.isNumber()) {
                Number number = primitive.getAsNumber();
                // 尝试保持原始类型
                if (number.doubleValue() == number.longValue()) {
                    results.add(number.longValue());
                } else {
                    results.add(number.doubleValue());
                }
            } else if (primitive.isBoolean()) {
                results.add(primitive.getAsBoolean());
            }
        } else if (element.isJsonArray()) {
            // 如果值本身是数组，展开添加
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                addValueToSet(results, item);
            }
        }
        // 如果是JsonObject，不直接添加（通常不会把整个对象作为值）
    }
}

