package com.glm.utils;

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
 * <p>从嵌套的JSON结构中递归提取指定路径下特定键的所有值并去重（保留顺序）。</p>
 * 
 * <h3>核心特性：</h3>
 * <ul>
 *   <li><b>任意深度搜索pathKey</b>：pathKey可以在JSON的任意深度位置，不限于根节点</li>
 *   <li><b>任意深度搜索targetKey</b>：targetKey可以在pathKey下的任意深度（子孙、曾孙等）</li>
 *   <li><b>嵌套同名路径</b>：pathKey内部如果还有pathKey，只取最内层的子集</li>
 *   <li><b>去重保序</b>：使用LinkedHashSet，自动去重并保留插入顺序</li>
 *   <li><b>数组索引</b>：支持指定只取数组中的第n个元素</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 示例JSON：pathKey "a" 在深层，targetKey "aenv" 在 a 的孙子节点
 * String json = "{\"root\":{\"config\":{\"a\":{\"level1\":{\"level2\":{\"aenv\":\"value\"}}}}}}";
 * Set<Object> values = JsonValueExtractor.extractAllValues(json, "a", "aenv");
 * // 结果: [value]
 * 
 * // 嵌套同名路径：a 套 a 时只取最内层
 * String json2 = "{\"a\":{\"aenv\":\"parent\",\"a\":{\"aenv\":\"child\"}}}";
 * Set<Object> inner = JsonValueExtractor.extractAllValues(json2, "a", "aenv");
 * // 结果: [child]（parent不计入，因为外层a包含内层a）
 * 
 * // 数组索引：只取每个数组的第一个元素
 * String json3 = "{\"a\":{\"list\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"}]}}";
 * Set<Object> first = JsonValueExtractor.extractAllFirstValues(json3, "a", "aenv");
 * // 结果: [first]
 * }</pre>
 * 
 * @author GLM
 * @version 1.3.0
 * @since JDK 1.8
 */
public class JsonValueExtractor {

    /**
     * 常量：表示不限制数组索引，遍历数组中的所有元素
     */
    public static final int ARRAY_INDEX_ALL = -1;

    /**
     * 私有构造函数，防止工具类被实例化
     */
    private JsonValueExtractor() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================================================================================
    // 第一部分：主要公开方法
    // 推荐使用 extractAllValues 系列方法，支持 pathKey 在任意深度
    // ==================================================================================

    /**
     * 【推荐】从JSON中提取指定路径下目标键的所有值
     * 
     * <p><b>工作原理：</b></p>
     * <ol>
     *   <li>在整个JSON中递归搜索所有名为 pathKey 的节点</li>
     *   <li>对于每个找到的 pathKey 节点：
     *     <ul>
     *       <li>如果其内部还有同名的 pathKey（嵌套），则只处理最内层</li>
     *       <li>在最内层 pathKey 的整个子树中搜索所有 targetKey</li>
     *     </ul>
     *   </li>
     *   <li>收集所有找到的 targetKey 的值，去重并保留顺序</li>
     * </ol>
     * 
     * <h4>示例：</h4>
     * <pre>{@code
     * // pathKey "a" 可以在任意深度
     * String json = "{\"root\":{\"config\":{\"a\":{\"deep\":{\"aenv\":\"value\"}}}}}";
     * Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
     * // 结果: [value]
     * }</pre>
     *
     * @param jsonString JSON字符串
     * @param pathKey    路径键名（如 "a"），可以在JSON的任意深度
     * @param targetKey  目标键名（如 "aenv"），可以在pathKey下的任意深度
     * @return 去重后的值集合，保留插入顺序
     */
    public static Set<Object> extractAllValues(String jsonString, String pathKey, String targetKey) {
        return extractAllValuesWithArrayIndex(jsonString, pathKey, targetKey, ARRAY_INDEX_ALL);
    }

    /**
     * 从JSON中提取值，支持指定数组索引
     * 
     * <p>数组索引说明：</p>
     * <ul>
     *   <li>-1 (ARRAY_INDEX_ALL)：遍历所有数组元素</li>
     *   <li>0：只取每个数组的第一个元素</li>
     *   <li>n：只取每个数组的第n个元素（0-based）</li>
     * </ul>
     * 
     * <p><b>注意：</b>数组索引只影响同一数组内的选择，跨数组各自独立处理。</p>
     *
     * @param jsonString JSON字符串
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @param arrayIndex 数组索引，-1表示遍历所有
     * @return 去重后的值集合
     */
    public static Set<Object> extractAllValuesWithArrayIndex(String jsonString, String pathKey, 
                                                              String targetKey, int arrayIndex) {
        validateInputs(jsonString, pathKey, targetKey);
        JsonElement root = JsonParser.parseString(jsonString);
        return findPathKeysAndExtract(root, pathKey, targetKey, arrayIndex);
    }

    /**
     * 便捷方法：只取每个数组的第一个元素
     *
     * @param jsonString JSON字符串
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @return 去重后的值集合
     */
    public static Set<Object> extractAllFirstValues(String jsonString, String pathKey, String targetKey) {
        return extractAllValuesWithArrayIndex(jsonString, pathKey, targetKey, 0);
    }

    // ==================================================================================
    // 第二部分：兼容方法（保留向后兼容）
    // 这些方法内部调用 extractAllValues 系列
    // ==================================================================================

    /**
     * 从JSON中提取指定路径下目标键的所有值
     * 
     * <p><b>注意：</b>此方法与 extractAllValues 功能相同，保留是为了向后兼容。
     * 推荐使用 extractAllValues 方法。</p>
     *
     * @param jsonString JSON字符串
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @return 去重后的值集合
     */
    public static Set<Object> extractValuesUnderPath(String jsonString, String pathKey, String targetKey) {
        return extractAllValues(jsonString, pathKey, targetKey);
    }

    /**
     * 从JsonObject中提取值
     *
     * @param jsonObject JSON对象
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @return 去重后的值集合
     */
    public static Set<Object> extractValuesUnderPath(JsonObject jsonObject, String pathKey, String targetKey) {
        if (jsonObject == null) {
            return Collections.emptySet();
        }
        validatePathAndTargetKey(pathKey, targetKey);
        return findPathKeysAndExtract(jsonObject, pathKey, targetKey, ARRAY_INDEX_ALL);
    }

    /**
     * 从JSON中提取值，支持数组索引
     *
     * @param jsonString JSON字符串
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @param arrayIndex 数组索引
     * @return 去重后的值集合
     */
    public static Set<Object> extractValuesWithArrayIndex(String jsonString, String pathKey, 
                                                           String targetKey, int arrayIndex) {
        return extractAllValuesWithArrayIndex(jsonString, pathKey, targetKey, arrayIndex);
    }

    /**
     * 从JsonObject中提取值，支持数组索引
     *
     * @param jsonObject JSON对象
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @param arrayIndex 数组索引
     * @return 去重后的值集合
     */
    public static Set<Object> extractValuesWithArrayIndex(JsonObject jsonObject, String pathKey, 
                                                           String targetKey, int arrayIndex) {
        if (jsonObject == null) {
            return Collections.emptySet();
        }
        validatePathAndTargetKey(pathKey, targetKey);
        return findPathKeysAndExtract(jsonObject, pathKey, targetKey, arrayIndex);
    }

    /**
     * 便捷方法：只取每个数组的第一个元素
     *
     * @param jsonString JSON字符串
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @return 去重后的值集合
     */
    public static Set<Object> extractFirstValuesFromArrays(String jsonString, String pathKey, String targetKey) {
        return extractAllFirstValues(jsonString, pathKey, targetKey);
    }

    // ==================================================================================
    // 第三部分：批量提取方法
    // ==================================================================================

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
     * 批量提取，支持数组索引
     *
     * @param jsonString JSON字符串
     * @param mappings   映射列表
     * @param arrayIndex 数组索引
     * @return Map
     */
    public static Map<String, Set<Object>> batchExtractWithArrayIndex(String jsonString, 
                                                                       List<String[]> mappings, 
                                                                       int arrayIndex) {
        validateJsonString(jsonString);
        if (mappings == null) {
            throw new IllegalArgumentException("Mappings cannot be null");
        }

        Map<String, Set<Object>> result = new LinkedHashMap<>();
        for (String[] mapping : mappings) {
            if (mapping != null && mapping.length >= 2) {
                Set<Object> values = extractAllValuesWithArrayIndex(jsonString, mapping[0], mapping[1], arrayIndex);
                result.put(mapping[1], values);
            }
        }
        return result;
    }

    /**
     * 批量提取并返回List形式
     *
     * @param jsonString JSON字符串
     * @param mappings   映射列表
     * @return Map
     */
    public static Map<String, List<Object>> batchExtractAsList(String jsonString, List<String[]> mappings) {
        Map<String, Set<Object>> setResult = batchExtract(jsonString, mappings);
        Map<String, List<Object>> listResult = new LinkedHashMap<>();
        for (Map.Entry<String, Set<Object>> entry : setResult.entrySet()) {
            listResult.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return listResult;
    }

    // ==================================================================================
    // 第四部分：字符串专用方法
    // ==================================================================================

    /**
     * 提取所有字符串类型的值
     *
     * @param jsonString JSON字符串
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @return 字符串值集合
     */
    public static Set<String> extractStringValues(String jsonString, String pathKey, String targetKey) {
        Set<Object> values = extractAllValues(jsonString, pathKey, targetKey);
        return filterStrings(values);
    }

    /**
     * 提取字符串值并返回List
     *
     * @param jsonString JSON字符串
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @return 字符串值列表
     */
    public static List<String> extractStringValuesAsList(String jsonString, String pathKey, String targetKey) {
        return new ArrayList<>(extractStringValues(jsonString, pathKey, targetKey));
    }

    /**
     * 提取字符串值，只取每个数组的第一个
     *
     * @param jsonString JSON字符串
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @return 字符串值集合
     */
    public static Set<String> extractFirstStringValues(String jsonString, String pathKey, String targetKey) {
        Set<Object> values = extractAllFirstValues(jsonString, pathKey, targetKey);
        return filterStrings(values);
    }

    // ==================================================================================
    // 第五部分：核心算法 - 搜索 pathKey
    // 在整个JSON中递归搜索所有 pathKey，然后提取其下的 targetKey
    // ==================================================================================

    /**
     * 在JSON元素中搜索所有 pathKey，并提取其下的 targetKey 值
     * 
     * <p>算法步骤：</p>
     * <ol>
     *   <li>遍历JSON结构的每个节点</li>
     *   <li>如果节点的键等于 pathKey：
     *     <ul>
     *       <li>检查该节点内部是否还有 pathKey（嵌套情况）</li>
     *       <li>如果有嵌套，递归找到最内层的 pathKey</li>
     *       <li>在最内层 pathKey 的整个子树中搜索 targetKey</li>
     *     </ul>
     *   </li>
     *   <li>如果节点的键不等于 pathKey，继续递归搜索子节点</li>
     * </ol>
     *
     * @param element    JSON元素
     * @param pathKey    路径键
     * @param targetKey  目标键
     * @param arrayIndex 数组索引
     * @return 提取到的值集合
     */
    private static Set<Object> findPathKeysAndExtract(JsonElement element, String pathKey, 
                                                       String targetKey, int arrayIndex) {
        Set<Object> results = new LinkedHashSet<>();

        if (element == null || element.isJsonNull()) {
            return results;
        }

        if (element.isJsonObject()) {
            searchPathKeyInObject(results, element.getAsJsonObject(), pathKey, targetKey, arrayIndex);
        } else if (element.isJsonArray()) {
            searchPathKeyInArray(results, element.getAsJsonArray(), pathKey, targetKey, arrayIndex);
        }

        return results;
    }

    /**
     * 在JsonObject中搜索 pathKey
     *
     * @param results    结果集合
     * @param obj        JSON对象
     * @param pathKey    路径键
     * @param targetKey  目标键
     * @param arrayIndex 数组索引
     */
    private static void searchPathKeyInObject(Set<Object> results, JsonObject obj, String pathKey, 
                                               String targetKey, int arrayIndex) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            if (key.equals(pathKey)) {
                // 找到 pathKey，处理嵌套并提取 targetKey
                Set<Object> extracted = processPathKeyValue(value, pathKey, targetKey, arrayIndex);
                results.addAll(extracted);
            } else {
                // 不是 pathKey，继续在子节点中搜索 pathKey
                results.addAll(findPathKeysAndExtract(value, pathKey, targetKey, arrayIndex));
            }
        }
    }

    /**
     * 在JsonArray中搜索 pathKey
     *
     * @param results    结果集合
     * @param array      JSON数组
     * @param pathKey    路径键
     * @param targetKey  目标键
     * @param arrayIndex 数组索引
     */
    private static void searchPathKeyInArray(Set<Object> results, JsonArray array, String pathKey, 
                                              String targetKey, int arrayIndex) {
        if (arrayIndex == ARRAY_INDEX_ALL) {
            for (JsonElement item : array) {
                results.addAll(findPathKeysAndExtract(item, pathKey, targetKey, arrayIndex));
            }
        } else if (arrayIndex >= 0 && arrayIndex < array.size()) {
            results.addAll(findPathKeysAndExtract(array.get(arrayIndex), pathKey, targetKey, arrayIndex));
        }
    }

    // ==================================================================================
    // 第六部分：核心算法 - 处理 pathKey 值并提取 targetKey
    // ==================================================================================

    /**
     * 处理找到的 pathKey 的值
     * 
     * <p>处理嵌套同名路径（a套a）的逻辑：</p>
     * <ul>
     *   <li>如果 pathKey 的值内部还有同名的 pathKey，递归到最内层</li>
     *   <li>在最内层的 pathKey 子树中搜索所有 targetKey</li>
     * </ul>
     *
     * @param pathValue  pathKey 对应的值
     * @param pathKey    路径键（用于检测嵌套）
     * @param targetKey  目标键
     * @param arrayIndex 数组索引
     * @return 提取到的值集合
     */
    private static Set<Object> processPathKeyValue(JsonElement pathValue, String pathKey, 
                                                    String targetKey, int arrayIndex) {
        // 找到最内层的 pathKey（处理嵌套同名路径）
        JsonElement innermostValue = findInnermostPathKey(pathValue, pathKey, arrayIndex);
        
        // 在最内层 pathKey 的子树中搜索所有 targetKey
        return searchTargetKeyInSubtree(innermostValue, targetKey, arrayIndex);
    }

    /**
     * 找到最内层的同名 pathKey
     * 
     * <p>例如：对于 a 套 a 的情况，递归到最内层的 a</p>
     *
     * @param element    当前元素
     * @param pathKey    路径键
     * @param arrayIndex 数组索引
     * @return 最内层 pathKey 的值
     */
    private static JsonElement findInnermostPathKey(JsonElement element, String pathKey, int arrayIndex) {
        if (element == null || element.isJsonNull()) {
            return element;
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            // 检查是否有内层同名 pathKey
            if (obj.has(pathKey)) {
                // 有嵌套，递归到内层
                return findInnermostPathKey(obj.get(pathKey), pathKey, arrayIndex);
            }
        } else if (element.isJsonArray()) {
            // 数组中可能有 pathKey，需要搜索
            // 但这里我们返回原始数组，在 searchTargetKeyInSubtree 中处理
        }

        // 没有嵌套或已到最内层，返回当前元素
        return element;
    }

    // ==================================================================================
    // 第七部分：核心算法 - 在子树中搜索 targetKey
    // ==================================================================================

    /**
     * 在子树中递归搜索所有 targetKey
     * 
     * <p>会遍历整个子树的所有层级，找到所有名为 targetKey 的键并提取其值。</p>
     *
     * @param element    要搜索的元素
     * @param targetKey  目标键
     * @param arrayIndex 数组索引
     * @return 提取到的值集合
     */
    private static Set<Object> searchTargetKeyInSubtree(JsonElement element, String targetKey, int arrayIndex) {
        Set<Object> results = new LinkedHashSet<>();

        if (element == null || element.isJsonNull()) {
            return results;
        }

        if (element.isJsonObject()) {
            searchTargetInObject(results, element.getAsJsonObject(), targetKey, arrayIndex);
        } else if (element.isJsonArray()) {
            searchTargetInArray(results, element.getAsJsonArray(), targetKey, arrayIndex);
        }

        return results;
    }

    /**
     * 在JsonObject中搜索 targetKey
     *
     * @param results    结果集合
     * @param obj        JSON对象
     * @param targetKey  目标键
     * @param arrayIndex 数组索引
     */
    private static void searchTargetInObject(Set<Object> results, JsonObject obj, 
                                              String targetKey, int arrayIndex) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            // 如果键匹配 targetKey，提取值
            if (key.equals(targetKey)) {
                addValue(results, value);
            }

            // 继续在子节点中搜索 targetKey（任意深度）
            results.addAll(searchTargetKeyInSubtree(value, targetKey, arrayIndex));
        }
    }

    /**
     * 在JsonArray中搜索 targetKey
     *
     * @param results    结果集合
     * @param array      JSON数组
     * @param targetKey  目标键
     * @param arrayIndex 数组索引
     */
    private static void searchTargetInArray(Set<Object> results, JsonArray array, 
                                             String targetKey, int arrayIndex) {
        if (arrayIndex == ARRAY_INDEX_ALL) {
            for (JsonElement item : array) {
                results.addAll(searchTargetKeyInSubtree(item, targetKey, arrayIndex));
            }
        } else if (arrayIndex >= 0 && arrayIndex < array.size()) {
            results.addAll(searchTargetKeyInSubtree(array.get(arrayIndex), targetKey, arrayIndex));
        }
    }

    // ==================================================================================
    // 第八部分：值处理方法
    // ==================================================================================

    /**
     * 将值添加到结果集合
     *
     * @param results 结果集合
     * @param element JSON元素
     */
    private static void addValue(Set<Object> results, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonPrimitive()) {
            addPrimitive(results, element.getAsJsonPrimitive());
        } else if (element.isJsonArray()) {
            // 如果值是数组，展开添加
            for (JsonElement item : element.getAsJsonArray()) {
                addValue(results, item);
            }
        }
        // JsonObject 不作为值添加
    }

    /**
     * 添加原始类型值
     *
     * @param results   结果集合
     * @param primitive JSON原始值
     */
    private static void addPrimitive(Set<Object> results, JsonPrimitive primitive) {
        if (primitive.isString()) {
            results.add(primitive.getAsString());
        } else if (primitive.isNumber()) {
            results.add(convertNumber(primitive.getAsNumber()));
        } else if (primitive.isBoolean()) {
            results.add(primitive.getAsBoolean());
        }
    }

    /**
     * 转换数字类型
     * 
     * <p>注意：不能使用三元运算符，会导致类型统一为 double</p>
     *
     * @param number 数字
     * @return Long 或 Double
     */
    private static Number convertNumber(Number number) {
        double doubleValue = number.doubleValue();
        long longValue = number.longValue();
        if (doubleValue == longValue) {
            return longValue;
        }
        return doubleValue;
    }

    // ==================================================================================
    // 第九部分：参数校验方法
    // ==================================================================================

    /**
     * 校验所有输入参数
     */
    private static void validateInputs(String jsonString, String pathKey, String targetKey) {
        validateJsonString(jsonString);
        validatePathAndTargetKey(pathKey, targetKey);
    }

    /**
     * 校验JSON字符串
     */
    private static void validateJsonString(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
    }

    /**
     * 校验路径键和目标键
     */
    private static void validatePathAndTargetKey(String pathKey, String targetKey) {
        if (pathKey == null || targetKey == null) {
            throw new IllegalArgumentException("pathKey and targetKey cannot be null");
        }
    }

    // ==================================================================================
    // 第十部分：辅助工具方法
    // ==================================================================================

    /**
     * 过滤出字符串类型的值
     *
     * @param values 值集合
     * @return 字符串集合
     */
    private static Set<String> filterStrings(Set<Object> values) {
        Set<String> result = new LinkedHashSet<>();
        for (Object value : values) {
            if (value instanceof String) {
                result.add((String) value);
            }
        }
        return result;
    }
}
