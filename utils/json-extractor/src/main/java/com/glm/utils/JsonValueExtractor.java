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
 * <p>从嵌套的JSON结构中递归提取指定路径下特定键的所有值并去重（保留顺序）。
 * 支持任意深度的嵌套结构（包括嵌套的对象、数组、数组中的对象等）。</p>
 * 
 * <h3>特性：</h3>
 * <ul>
 *   <li>去重时保留插入顺序（使用LinkedHashSet）</li>
 *   <li>支持指定数组索引，只取数组中的第n个元素</li>
 *   <li>支持处理嵌套同名路径（a套a），只取最内层子集</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * String json = "{\"a\":{\"aenv\":\"env1\",\"nested\":{\"aenv\":\"env2\"}}}";
 * Set<Object> values = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
 * // 结果: ["env1", "env2"]（保留顺序）
 * 
 * // 只取数组中的第一个元素
 * Set<Object> first = JsonValueExtractor.extractValuesWithArrayIndex(json, "a", "aenv", 0);
 * }</pre>
 * 
 * @author GLM
 * @version 1.1.0
 * @since JDK 1.8
 */
public class JsonValueExtractor {

    private static final Gson GSON = new Gson();
    
    /** 表示不限制数组索引，遍历所有元素 */
    public static final int ARRAY_INDEX_ALL = -1;

    /**
     * 私有构造函数，防止实例化
     */
    private JsonValueExtractor() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== 基础提取方法 ====================

    /**
     * 从JSON字符串的指定路径下提取目标键的所有值（去重，保留顺序）
     *
     * @param jsonString JSON字符串
     * @param pathKey    限定搜索的路径键名（如 "a" 或 "b"）
     * @param targetKey  要提取的目标键名（如 "aenv" 或 "benv"）
     * @return 去重后的值集合（保留插入顺序）
     * @throws IllegalArgumentException 如果jsonString为null或空
     */
    public static Set<Object> extractValuesUnderPath(String jsonString, String pathKey, String targetKey) {
        return extractValuesWithArrayIndex(jsonString, pathKey, targetKey, ARRAY_INDEX_ALL);
    }

    /**
     * 从JsonObject的指定路径下提取目标键的所有值（去重，保留顺序）
     *
     * @param jsonObject JSON对象
     * @param pathKey    限定搜索的路径键名
     * @param targetKey  要提取的目标键名
     * @return 去重后的值集合（保留插入顺序）
     */
    public static Set<Object> extractValuesUnderPath(JsonObject jsonObject, String pathKey, String targetKey) {
        return extractValuesWithArrayIndex(jsonObject, pathKey, targetKey, ARRAY_INDEX_ALL);
    }

    // ==================== 带数组索引的提取方法 ====================

    /**
     * 从JSON字符串的指定路径下提取目标键的值，支持指定数组索引
     * 
     * <p>当遍历到数组时，只取数组中的第arrayIndex个元素（0-based）。
     * 这只针对同一数组中的并列项，跨数组的元素不受影响。</p>
     *
     * @param jsonString JSON字符串
     * @param pathKey    限定搜索的路径键名
     * @param targetKey  要提取的目标键名
     * @param arrayIndex 数组索引（0-based），-1表示遍历所有元素
     * @return 去重后的值集合（保留插入顺序）
     */
    public static Set<Object> extractValuesWithArrayIndex(String jsonString, String pathKey, String targetKey, int arrayIndex) {
        validateInputs(jsonString, pathKey, targetKey);

        JsonElement root = JsonParser.parseString(jsonString);
        if (!root.isJsonObject()) {
            return Collections.emptySet();
        }

        JsonObject jsonObject = root.getAsJsonObject();
        if (jsonObject.has(pathKey)) {
            JsonElement pathElement = jsonObject.get(pathKey);
            // 检查是否有嵌套的同名pathKey，如果有则只处理最内层
            return extractValuesFromNestedWithOptions(pathElement, pathKey, targetKey, arrayIndex);
        }
        return Collections.emptySet();
    }

    /**
     * 从JsonObject的指定路径下提取目标键的值，支持指定数组索引
     *
     * @param jsonObject JSON对象
     * @param pathKey    限定搜索的路径键名
     * @param targetKey  要提取的目标键名
     * @param arrayIndex 数组索引（0-based），-1表示遍历所有元素
     * @return 去重后的值集合（保留插入顺序）
     */
    public static Set<Object> extractValuesWithArrayIndex(JsonObject jsonObject, String pathKey, String targetKey, int arrayIndex) {
        if (jsonObject == null) {
            return Collections.emptySet();
        }
        if (pathKey == null || targetKey == null) {
            throw new IllegalArgumentException("pathKey and targetKey cannot be null");
        }

        if (jsonObject.has(pathKey)) {
            JsonElement pathElement = jsonObject.get(pathKey);
            return extractValuesFromNestedWithOptions(pathElement, pathKey, targetKey, arrayIndex);
        }
        return Collections.emptySet();
    }

    /**
     * 从JSON字符串的指定路径下提取目标键的第一个值（只取每个数组的第一个元素）
     * 
     * <p>这是 extractValuesWithArrayIndex(json, pathKey, targetKey, 0) 的便捷方法</p>
     *
     * @param jsonString JSON字符串
     * @param pathKey    限定搜索的路径键名
     * @param targetKey  要提取的目标键名
     * @return 去重后的值集合（保留插入顺序）
     */
    public static Set<Object> extractFirstValuesFromArrays(String jsonString, String pathKey, String targetKey) {
        return extractValuesWithArrayIndex(jsonString, pathKey, targetKey, 0);
    }

    // ==================== 递归搜索方法 ====================

    /**
     * 从任意嵌套结构中递归搜索路径键，然后在其下提取目标键的所有值
     * 
     * <p>适用于pathKey本身也可能嵌套在任意深度的情况。
     * 如果存在嵌套的同名pathKey（a套a），只处理最内层的子集。</p>
     *
     * @param jsonString JSON字符串
     * @param pathKey    限定搜索的路径键名
     * @param targetKey  要提取的目标键名
     * @return 去重后的值集合（保留插入顺序）
     */
    public static Set<Object> extractAllValues(String jsonString, String pathKey, String targetKey) {
        return extractAllValuesWithArrayIndex(jsonString, pathKey, targetKey, ARRAY_INDEX_ALL);
    }

    /**
     * 从任意嵌套结构中递归搜索路径键，支持指定数组索引
     *
     * @param jsonString JSON字符串
     * @param pathKey    限定搜索的路径键名
     * @param targetKey  要提取的目标键名
     * @param arrayIndex 数组索引（0-based），-1表示遍历所有元素
     * @return 去重后的值集合（保留插入顺序）
     */
    public static Set<Object> extractAllValuesWithArrayIndex(String jsonString, String pathKey, String targetKey, int arrayIndex) {
        validateInputs(jsonString, pathKey, targetKey);

        JsonElement root = JsonParser.parseString(jsonString);
        return extractAllValuesRecursive(root, pathKey, targetKey, arrayIndex);
    }

    /**
     * 递归搜索路径键，只取每个数组的第一个元素
     *
     * @param jsonString JSON字符串
     * @param pathKey    限定搜索的路径键名
     * @param targetKey  要提取的目标键名
     * @return 去重后的值集合（保留插入顺序）
     */
    public static Set<Object> extractAllFirstValues(String jsonString, String pathKey, String targetKey) {
        return extractAllValuesWithArrayIndex(jsonString, pathKey, targetKey, 0);
    }

    // ==================== 批量提取方法 ====================

    /**
     * 批量提取多组 pathKey -> targetKey 的值
     *
     * @param jsonString JSON字符串
     * @param mappings   映射列表，每个元素为 [pathKey, targetKey]
     * @return Map，key为targetKey，value为提取到的值集合
     */
    public static Map<String, Set<Object>> batchExtract(String jsonString, List<String[]> mappings) {
        return batchExtractWithArrayIndex(jsonString, mappings, ARRAY_INDEX_ALL);
    }

    /**
     * 批量提取多组键值对，支持指定数组索引
     *
     * @param jsonString JSON字符串
     * @param mappings   映射列表，每个元素为 [pathKey, targetKey]
     * @param arrayIndex 数组索引（0-based），-1表示遍历所有元素
     * @return Map，key为targetKey，value为提取到的值集合
     */
    public static Map<String, Set<Object>> batchExtractWithArrayIndex(String jsonString, List<String[]> mappings, int arrayIndex) {
        validateInputs(jsonString);
        if (mappings == null) {
            throw new IllegalArgumentException("Mappings cannot be null");
        }

        Map<String, Set<Object>> result = new LinkedHashMap<>();
        for (String[] mapping : mappings) {
            if (mapping != null && mapping.length >= 2) {
                String pathKey = mapping[0];
                String targetKey = mapping[1];
                Set<Object> values = extractAllValuesWithArrayIndex(jsonString, pathKey, targetKey, arrayIndex);
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

    // ==================== 字符串专用方法 ====================

    /**
     * 提取指定目标键的所有字符串值（去重，保留顺序）
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

    /**
     * 提取字符串值，只取每个数组的第一个元素
     *
     * @param jsonString JSON字符串
     * @param pathKey    限定搜索的路径键名
     * @param targetKey  要提取的目标键名
     * @return 去重后的字符串值集合
     */
    public static Set<String> extractFirstStringValues(String jsonString, String pathKey, String targetKey) {
        Set<Object> values = extractAllFirstValues(jsonString, pathKey, targetKey);
        return values.stream()
                .filter(v -> v instanceof String)
                .map(v -> (String) v)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // ==================== 内部方法 ====================

    /**
     * 验证输入参数
     */
    private static void validateInputs(String jsonString, String pathKey, String targetKey) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
        if (pathKey == null || targetKey == null) {
            throw new IllegalArgumentException("pathKey and targetKey cannot be null");
        }
    }

    /**
     * 验证JSON字符串
     */
    private static void validateInputs(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
    }

    /**
     * 从嵌套的JsonElement中递归提取所有指定key的值
     * 支持数组索引和嵌套同名路径处理
     */
    private static Set<Object> extractValuesFromNestedWithOptions(JsonElement element, String pathKey, String targetKey, int arrayIndex) {
        Set<Object> results = new LinkedHashSet<>();

        if (element == null || element.isJsonNull()) {
            return results;
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            
            // 检查是否有嵌套的同名pathKey
            if (obj.has(pathKey)) {
                // 如果有嵌套的同名pathKey，只处理最内层的子集
                // 不在当前层级提取targetKey的值
                results.addAll(extractValuesFromNestedWithOptions(obj.get(pathKey), pathKey, targetKey, arrayIndex));
            } else {
                // 没有嵌套的同名pathKey，正常提取
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    if (entry.getKey().equals(targetKey)) {
                        // 找到目标key，添加其值
                        addValueToSet(results, entry.getValue());
                    }
                    // 继续递归搜索（排除同名pathKey的情况，上面已处理）
                    if (!entry.getKey().equals(pathKey)) {
                        results.addAll(extractValuesFromNestedWithOptions(entry.getValue(), pathKey, targetKey, arrayIndex));
                    }
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (arrayIndex == ARRAY_INDEX_ALL) {
                // 遍历所有元素
                for (JsonElement item : array) {
                    results.addAll(extractValuesFromNestedWithOptions(item, pathKey, targetKey, arrayIndex));
                }
            } else if (arrayIndex >= 0 && arrayIndex < array.size()) {
                // 只取指定索引的元素
                results.addAll(extractValuesFromNestedWithOptions(array.get(arrayIndex), pathKey, targetKey, arrayIndex));
            }
            // 如果索引超出范围，不返回任何值
        }

        return results;
    }

    /**
     * 递归搜索路径键，然后在其下提取目标键的值
     * 支持数组索引
     */
    private static Set<Object> extractAllValuesRecursive(JsonElement element, String pathKey, String targetKey, int arrayIndex) {
        Set<Object> results = new LinkedHashSet<>();

        if (element == null || element.isJsonNull()) {
            return results;
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (entry.getKey().equals(pathKey)) {
                    // 找到路径节点，在其下提取目标值（会处理嵌套同名pathKey）
                    results.addAll(extractValuesFromNestedWithOptions(entry.getValue(), pathKey, targetKey, arrayIndex));
                } else {
                    // 继续向下搜索路径节点
                    results.addAll(extractAllValuesRecursive(entry.getValue(), pathKey, targetKey, arrayIndex));
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (arrayIndex == ARRAY_INDEX_ALL) {
                for (JsonElement item : array) {
                    results.addAll(extractAllValuesRecursive(item, pathKey, targetKey, arrayIndex));
                }
            } else if (arrayIndex >= 0 && arrayIndex < array.size()) {
                results.addAll(extractAllValuesRecursive(array.get(arrayIndex), pathKey, targetKey, arrayIndex));
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
