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
 * <p>从嵌套的JSON结构中递归提取指定路径下特定键的所有值并去重（保留顺序）。
 * 支持任意深度的嵌套结构（包括嵌套的对象、数组、数组中的对象等）。</p>
 * 
 * <h3>核心特性：</h3>
 * <ul>
 *   <li><b>去重保序</b>：使用LinkedHashSet，自动去重并保留插入顺序</li>
 *   <li><b>数组索引</b>：支持指定只取数组中的第n个元素（同一数组内生效，跨数组独立）</li>
 *   <li><b>嵌套同名路径</b>：支持处理 a 套 a 的情况，只取最内层子集的值</li>
 *   <li><b>类型保持</b>：保持原始数据类型（String/Long/Double/Boolean）</li>
 *   <li><b>批量提取</b>：支持一次提取多组路径-键值对</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 基础用法：提取 a 路径下所有 aenv 的值
 * String json = "{\"a\":{\"aenv\":\"env1\",\"nested\":{\"aenv\":\"env2\"}}}";
 * Set<Object> values = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
 * // 结果: [env1, env2]（保留顺序）
 * 
 * // 数组索引：只取每个数组的第一个元素
 * String json2 = "{\"a\":{\"list\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"}]}}";
 * Set<Object> first = JsonValueExtractor.extractValuesWithArrayIndex(json2, "a", "aenv", 0);
 * // 结果: [first]
 * 
 * // 嵌套同名路径：a 套 a 时只取最内层
 * String json3 = "{\"a\":{\"aenv\":\"parent\",\"a\":{\"aenv\":\"child\"}}}";
 * Set<Object> inner = JsonValueExtractor.extractValuesUnderPath(json3, "a", "aenv");
 * // 结果: [child]（parent不计入）
 * }</pre>
 * 
 * <h3>设计说明：</h3>
 * <ul>
 *   <li>所有公开方法均为静态方法，工具类不可实例化</li>
 *   <li>代码嵌套层数控制在4层以内，通过拆分辅助方法实现</li>
 *   <li>使用早返回（Early Return）模式减少嵌套</li>
 * </ul>
 * 
 * @author GLM
 * @version 1.2.0
 * @since JDK 1.8
 */
public class JsonValueExtractor {

    /**
     * 常量：表示不限制数组索引，遍历数组中的所有元素
     * <p>当 arrayIndex 参数为此值时，会遍历数组的每一个元素</p>
     */
    public static final int ARRAY_INDEX_ALL = -1;

    /**
     * 私有构造函数，防止工具类被实例化
     * 
     * @throws UnsupportedOperationException 总是抛出，禁止实例化
     */
    private JsonValueExtractor() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================================================================================
    // 第一部分：基础提取方法
    // 这些方法是最常用的入口，从指定路径下提取目标键的值
    // ==================================================================================

    /**
     * 从JSON字符串的指定路径下提取目标键的所有值
     * 
     * <p>这是最常用的方法，会遍历路径下的所有嵌套结构（包括数组中的所有元素），
     * 提取所有匹配目标键的值，自动去重并保留插入顺序。</p>
     * 
     * <h4>示例：</h4>
     * <pre>{@code
     * String json = "{\"config\":{\"env\":\"dev\",\"db\":{\"env\":\"db-dev\"}}}";
     * Set<Object> envs = JsonValueExtractor.extractValuesUnderPath(json, "config", "env");
     * // 结果: [dev, db-dev]
     * }</pre>
     *
     * @param jsonString JSON字符串，不能为null或空
     * @param pathKey    限定搜索的路径键名（如 "a" 或 "config"），不能为null
     * @param targetKey  要提取的目标键名（如 "aenv" 或 "env"），不能为null
     * @return 去重后的值集合，使用LinkedHashSet保留插入顺序；如果未找到则返回空集合
     * @throws IllegalArgumentException 如果任何参数为null或jsonString为空
     */
    public static Set<Object> extractValuesUnderPath(String jsonString, String pathKey, String targetKey) {
        return extractValuesWithArrayIndex(jsonString, pathKey, targetKey, ARRAY_INDEX_ALL);
    }

    /**
     * 从JsonObject的指定路径下提取目标键的所有值
     * 
     * <p>适用于已经解析好的JsonObject对象，避免重复解析JSON字符串。</p>
     *
     * @param jsonObject JSON对象，可以为null（返回空集合）
     * @param pathKey    限定搜索的路径键名，不能为null
     * @param targetKey  要提取的目标键名，不能为null
     * @return 去重后的值集合，使用LinkedHashSet保留插入顺序
     * @throws IllegalArgumentException 如果pathKey或targetKey为null
     */
    public static Set<Object> extractValuesUnderPath(JsonObject jsonObject, String pathKey, String targetKey) {
        return extractValuesWithArrayIndex(jsonObject, pathKey, targetKey, ARRAY_INDEX_ALL);
    }

    // ==================================================================================
    // 第二部分：带数组索引的提取方法
    // 这些方法支持指定只取数组中的第n个元素，用于减少重复项
    // ==================================================================================

    /**
     * 从JSON字符串的指定路径下提取目标键的值，支持指定数组索引
     * 
     * <p><b>数组索引规则：</b></p>
     * <ul>
     *   <li>arrayIndex = -1 (ARRAY_INDEX_ALL)：遍历数组中的所有元素（默认行为）</li>
     *   <li>arrayIndex = 0：只取每个数组的第一个元素</li>
     *   <li>arrayIndex = n：只取每个数组的第n个元素（0-based）</li>
     *   <li>如果索引超出数组范围，该数组不返回任何值</li>
     * </ul>
     * 
     * <p><b>重要说明：</b>数组索引只影响同一数组内的元素选择，跨数组的元素各自独立处理。</p>
     * 
     * <h4>示例：</h4>
     * <pre>{@code
     * // 两个数组各取第一个
     * String json = "{\"a\":{\"list1\":[{\"env\":\"a1\"},{\"env\":\"a2\"}]," +
     *                      "\"list2\":[{\"env\":\"b1\"},{\"env\":\"b2\"}]}}";
     * Set<Object> result = JsonValueExtractor.extractValuesWithArrayIndex(json, "a", "env", 0);
     * // 结果: [a1, b1]（两个数组各取第一个）
     * }</pre>
     *
     * @param jsonString JSON字符串，不能为null或空
     * @param pathKey    限定搜索的路径键名，不能为null
     * @param targetKey  要提取的目标键名，不能为null
     * @param arrayIndex 数组索引（0-based），-1表示遍历所有元素
     * @return 去重后的值集合，使用LinkedHashSet保留插入顺序
     * @throws IllegalArgumentException 如果任何必需参数为null或jsonString为空
     */
    public static Set<Object> extractValuesWithArrayIndex(String jsonString, String pathKey, 
                                                           String targetKey, int arrayIndex) {
        // 参数校验
        validateRequiredInputs(jsonString, pathKey, targetKey);

        // 解析JSON
        JsonElement root = JsonParser.parseString(jsonString);
        if (!root.isJsonObject()) {
            return Collections.emptySet();
        }

        // 检查路径是否存在
        JsonObject jsonObject = root.getAsJsonObject();
        if (!jsonObject.has(pathKey)) {
            return Collections.emptySet();
        }

        // 提取值（处理嵌套同名路径）
        JsonElement pathElement = jsonObject.get(pathKey);
        return extractFromElement(pathElement, pathKey, targetKey, arrayIndex);
    }

    /**
     * 从JsonObject的指定路径下提取目标键的值，支持指定数组索引
     *
     * @param jsonObject JSON对象，可以为null（返回空集合）
     * @param pathKey    限定搜索的路径键名，不能为null
     * @param targetKey  要提取的目标键名，不能为null
     * @param arrayIndex 数组索引（0-based），-1表示遍历所有元素
     * @return 去重后的值集合，使用LinkedHashSet保留插入顺序
     * @throws IllegalArgumentException 如果pathKey或targetKey为null
     */
    public static Set<Object> extractValuesWithArrayIndex(JsonObject jsonObject, String pathKey, 
                                                           String targetKey, int arrayIndex) {
        if (jsonObject == null) {
            return Collections.emptySet();
        }
        validatePathAndTargetKey(pathKey, targetKey);

        if (!jsonObject.has(pathKey)) {
            return Collections.emptySet();
        }

        JsonElement pathElement = jsonObject.get(pathKey);
        return extractFromElement(pathElement, pathKey, targetKey, arrayIndex);
    }

    /**
     * 便捷方法：只取每个数组的第一个元素
     * 
     * <p>等价于 {@code extractValuesWithArrayIndex(json, pathKey, targetKey, 0)}</p>
     * 
     * <h4>使用场景：</h4>
     * <p>当数组中有多个重复配置，通常只需要第一个（主配置）时使用此方法。</p>
     *
     * @param jsonString JSON字符串
     * @param pathKey    限定搜索的路径键名
     * @param targetKey  要提取的目标键名
     * @return 去重后的值集合
     */
    public static Set<Object> extractFirstValuesFromArrays(String jsonString, String pathKey, String targetKey) {
        return extractValuesWithArrayIndex(jsonString, pathKey, targetKey, 0);
    }

    // ==================================================================================
    // 第三部分：递归搜索方法
    // 这些方法会递归搜索整个JSON结构找到pathKey，适用于pathKey位置不确定的情况
    // ==================================================================================

    /**
     * 从任意嵌套结构中递归搜索路径键，然后在其下提取目标键的所有值
     * 
     * <p><b>与 extractValuesUnderPath 的区别：</b></p>
     * <ul>
     *   <li>extractValuesUnderPath：只在JSON根对象的直接子节点中查找pathKey</li>
     *   <li>extractAllValues：递归搜索整个JSON结构，在任意深度找到pathKey</li>
     * </ul>
     * 
     * <h4>示例：</h4>
     * <pre>{@code
     * // pathKey "a" 嵌套在深层
     * String json = "{\"root\":{\"config\":{\"a\":{\"env\":\"value\"}}}}";
     * Set<Object> values = JsonValueExtractor.extractAllValues(json, "a", "env");
     * // 结果: [value]
     * }</pre>
     * 
     * <p><b>嵌套同名路径处理：</b>如果存在 a 套 a 的情况，只处理最内层的子集。</p>
     *
     * @param jsonString JSON字符串，不能为null或空
     * @param pathKey    限定搜索的路径键名，不能为null
     * @param targetKey  要提取的目标键名，不能为null
     * @return 去重后的值集合，使用LinkedHashSet保留插入顺序
     * @throws IllegalArgumentException 如果任何必需参数为null或jsonString为空
     */
    public static Set<Object> extractAllValues(String jsonString, String pathKey, String targetKey) {
        return extractAllValuesWithArrayIndex(jsonString, pathKey, targetKey, ARRAY_INDEX_ALL);
    }

    /**
     * 从任意嵌套结构中递归搜索路径键，支持指定数组索引
     *
     * @param jsonString JSON字符串，不能为null或空
     * @param pathKey    限定搜索的路径键名，不能为null
     * @param targetKey  要提取的目标键名，不能为null
     * @param arrayIndex 数组索引（0-based），-1表示遍历所有元素
     * @return 去重后的值集合，使用LinkedHashSet保留插入顺序
     * @throws IllegalArgumentException 如果任何必需参数为null或jsonString为空
     */
    public static Set<Object> extractAllValuesWithArrayIndex(String jsonString, String pathKey, 
                                                              String targetKey, int arrayIndex) {
        validateRequiredInputs(jsonString, pathKey, targetKey);
        JsonElement root = JsonParser.parseString(jsonString);
        return searchAndExtract(root, pathKey, targetKey, arrayIndex);
    }

    /**
     * 便捷方法：递归搜索路径键，只取每个数组的第一个元素
     *
     * @param jsonString JSON字符串
     * @param pathKey    限定搜索的路径键名
     * @param targetKey  要提取的目标键名
     * @return 去重后的值集合
     */
    public static Set<Object> extractAllFirstValues(String jsonString, String pathKey, String targetKey) {
        return extractAllValuesWithArrayIndex(jsonString, pathKey, targetKey, 0);
    }

    // ==================================================================================
    // 第四部分：批量提取方法
    // 这些方法支持一次调用提取多组路径-键值对，提高效率
    // ==================================================================================

    /**
     * 批量提取多组 pathKey -> targetKey 的值
     * 
     * <h4>示例：</h4>
     * <pre>{@code
     * List<String[]> mappings = Arrays.asList(
     *     new String[]{"database", "host"},
     *     new String[]{"cache", "host"}
     * );
     * Map<String, Set<Object>> result = JsonValueExtractor.batchExtract(json, mappings);
     * // result.get("host") 包含所有提取的host值
     * }</pre>
     * 
     * <p><b>注意：</b>返回的Map以targetKey为键，如果多个映射使用相同的targetKey，
     * 后面的结果会覆盖前面的结果。</p>
     *
     * @param jsonString JSON字符串，不能为null或空
     * @param mappings   映射列表，每个元素为 [pathKey, targetKey] 数组
     * @return Map，key为targetKey，value为提取到的值集合
     * @throws IllegalArgumentException 如果jsonString为null或空，或mappings为null
     */
    public static Map<String, Set<Object>> batchExtract(String jsonString, List<String[]> mappings) {
        return batchExtractWithArrayIndex(jsonString, mappings, ARRAY_INDEX_ALL);
    }

    /**
     * 批量提取多组键值对，支持指定数组索引
     *
     * @param jsonString JSON字符串，不能为null或空
     * @param mappings   映射列表，每个元素为 [pathKey, targetKey] 数组
     * @param arrayIndex 数组索引（0-based），-1表示遍历所有元素
     * @return Map，key为targetKey，value为提取到的值集合
     * @throws IllegalArgumentException 如果jsonString为null或空，或mappings为null
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
            processMapping(result, jsonString, mapping, arrayIndex);
        }
        return result;
    }

    /**
     * 批量提取并返回List形式的结果
     * 
     * <p>与 batchExtract 的区别在于返回值类型是 List 而不是 Set，方便某些场景使用。</p>
     *
     * @param jsonString JSON字符串
     * @param mappings   映射列表
     * @return Map，key为targetKey，value为提取到的值列表
     */
    public static Map<String, List<Object>> batchExtractAsList(String jsonString, List<String[]> mappings) {
        Map<String, Set<Object>> setResult = batchExtract(jsonString, mappings);
        return convertToListMap(setResult);
    }

    // ==================================================================================
    // 第五部分：字符串专用方法
    // 这些方法只提取字符串类型的值，过滤掉数字、布尔等其他类型
    // ==================================================================================

    /**
     * 提取指定目标键的所有字符串值
     * 
     * <p>只返回字符串类型的值，自动过滤数字、布尔值等其他类型。</p>
     *
     * @param jsonString JSON字符串
     * @param pathKey    限定搜索的路径键名
     * @param targetKey  要提取的目标键名
     * @return 去重后的字符串值集合
     */
    public static Set<String> extractStringValues(String jsonString, String pathKey, String targetKey) {
        Set<Object> values = extractAllValues(jsonString, pathKey, targetKey);
        return filterStrings(values);
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
        return filterStrings(values);
    }

    // ==================================================================================
    // 第六部分：参数校验方法
    // 统一的参数校验逻辑，抛出明确的异常信息
    // ==================================================================================

    /**
     * 校验所有必需的输入参数
     * 
     * @param jsonString JSON字符串
     * @param pathKey    路径键
     * @param targetKey  目标键
     * @throws IllegalArgumentException 如果任何参数无效
     */
    private static void validateRequiredInputs(String jsonString, String pathKey, String targetKey) {
        validateJsonString(jsonString);
        validatePathAndTargetKey(pathKey, targetKey);
    }

    /**
     * 校验JSON字符串
     * 
     * @param jsonString JSON字符串
     * @throws IllegalArgumentException 如果jsonString为null或空
     */
    private static void validateJsonString(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
    }

    /**
     * 校验pathKey和targetKey
     * 
     * @param pathKey   路径键
     * @param targetKey 目标键
     * @throws IllegalArgumentException 如果任一参数为null
     */
    private static void validatePathAndTargetKey(String pathKey, String targetKey) {
        if (pathKey == null || targetKey == null) {
            throw new IllegalArgumentException("pathKey and targetKey cannot be null");
        }
    }

    // ==================================================================================
    // 第七部分：核心提取逻辑
    // 这些私有方法实现实际的值提取逻辑，通过拆分降低嵌套层数
    // ==================================================================================

    /**
     * 从JsonElement中提取值（处理嵌套同名路径）
     * 
     * <p>这是核心提取方法，负责：</p>
     * <ul>
     *   <li>处理嵌套同名路径（a套a）：如果发现同名pathKey，递归到最内层</li>
     *   <li>遍历对象属性，找到targetKey时提取值</li>
     *   <li>处理数组时根据arrayIndex决定遍历策略</li>
     * </ul>
     * 
     * @param element    要处理的JSON元素
     * @param pathKey    路径键（用于检测嵌套同名路径）
     * @param targetKey  目标键
     * @param arrayIndex 数组索引
     * @return 提取到的值集合
     */
    private static Set<Object> extractFromElement(JsonElement element, String pathKey, 
                                                   String targetKey, int arrayIndex) {
        Set<Object> results = new LinkedHashSet<>();

        // 空值检查（Early Return）
        if (element == null || element.isJsonNull()) {
            return results;
        }

        // 根据元素类型分发处理
        if (element.isJsonObject()) {
            extractFromObject(results, element.getAsJsonObject(), pathKey, targetKey, arrayIndex);
        } else if (element.isJsonArray()) {
            extractFromArray(results, element.getAsJsonArray(), pathKey, targetKey, arrayIndex);
        }

        return results;
    }

    /**
     * 从JsonObject中提取值
     * 
     * @param results    结果集合（会被修改）
     * @param obj        JSON对象
     * @param pathKey    路径键
     * @param targetKey  目标键
     * @param arrayIndex 数组索引
     */
    private static void extractFromObject(Set<Object> results, JsonObject obj, String pathKey, 
                                          String targetKey, int arrayIndex) {
        // 检查嵌套同名路径：如果有内层pathKey，只处理内层
        if (obj.has(pathKey)) {
            results.addAll(extractFromElement(obj.get(pathKey), pathKey, targetKey, arrayIndex));
            return;
        }

        // 没有嵌套同名路径，遍历所有属性
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            processObjectEntry(results, entry, pathKey, targetKey, arrayIndex);
        }
    }

    /**
     * 处理对象的单个属性
     * 
     * @param results    结果集合
     * @param entry      属性条目
     * @param pathKey    路径键
     * @param targetKey  目标键
     * @param arrayIndex 数组索引
     */
    private static void processObjectEntry(Set<Object> results, Map.Entry<String, JsonElement> entry,
                                           String pathKey, String targetKey, int arrayIndex) {
        String key = entry.getKey();
        JsonElement value = entry.getValue();

        // 如果是目标键，提取值
        if (key.equals(targetKey)) {
            addPrimitiveValue(results, value);
        }

        // 如果不是pathKey（避免重复处理），继续递归
        if (!key.equals(pathKey)) {
            results.addAll(extractFromElement(value, pathKey, targetKey, arrayIndex));
        }
    }

    /**
     * 从JsonArray中提取值
     * 
     * @param results    结果集合
     * @param array      JSON数组
     * @param pathKey    路径键
     * @param targetKey  目标键
     * @param arrayIndex 数组索引
     */
    private static void extractFromArray(Set<Object> results, JsonArray array, String pathKey, 
                                         String targetKey, int arrayIndex) {
        if (arrayIndex == ARRAY_INDEX_ALL) {
            // 遍历所有元素
            for (JsonElement item : array) {
                results.addAll(extractFromElement(item, pathKey, targetKey, arrayIndex));
            }
        } else if (isValidArrayIndex(array, arrayIndex)) {
            // 只取指定索引的元素
            results.addAll(extractFromElement(array.get(arrayIndex), pathKey, targetKey, arrayIndex));
        }
        // 索引超出范围时不返回任何值
    }

    /**
     * 检查数组索引是否有效
     * 
     * @param array      JSON数组
     * @param arrayIndex 数组索引
     * @return 如果索引有效返回true
     */
    private static boolean isValidArrayIndex(JsonArray array, int arrayIndex) {
        return arrayIndex >= 0 && arrayIndex < array.size();
    }

    // ==================================================================================
    // 第八部分：递归搜索逻辑
    // 在整个JSON结构中搜索pathKey，然后提取其下的值
    // ==================================================================================

    /**
     * 递归搜索pathKey并提取其下的值
     * 
     * @param element    要搜索的JSON元素
     * @param pathKey    要搜索的路径键
     * @param targetKey  目标键
     * @param arrayIndex 数组索引
     * @return 提取到的值集合
     */
    private static Set<Object> searchAndExtract(JsonElement element, String pathKey, 
                                                 String targetKey, int arrayIndex) {
        Set<Object> results = new LinkedHashSet<>();

        if (element == null || element.isJsonNull()) {
            return results;
        }

        if (element.isJsonObject()) {
            searchInObject(results, element.getAsJsonObject(), pathKey, targetKey, arrayIndex);
        } else if (element.isJsonArray()) {
            searchInArray(results, element.getAsJsonArray(), pathKey, targetKey, arrayIndex);
        }

        return results;
    }

    /**
     * 在JsonObject中搜索pathKey
     * 
     * @param results    结果集合
     * @param obj        JSON对象
     * @param pathKey    路径键
     * @param targetKey  目标键
     * @param arrayIndex 数组索引
     */
    private static void searchInObject(Set<Object> results, JsonObject obj, String pathKey, 
                                       String targetKey, int arrayIndex) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (entry.getKey().equals(pathKey)) {
                // 找到pathKey，提取其下的值
                results.addAll(extractFromElement(entry.getValue(), pathKey, targetKey, arrayIndex));
            } else {
                // 继续递归搜索
                results.addAll(searchAndExtract(entry.getValue(), pathKey, targetKey, arrayIndex));
            }
        }
    }

    /**
     * 在JsonArray中搜索pathKey
     * 
     * @param results    结果集合
     * @param array      JSON数组
     * @param pathKey    路径键
     * @param targetKey  目标键
     * @param arrayIndex 数组索引
     */
    private static void searchInArray(Set<Object> results, JsonArray array, String pathKey, 
                                      String targetKey, int arrayIndex) {
        if (arrayIndex == ARRAY_INDEX_ALL) {
            for (JsonElement item : array) {
                results.addAll(searchAndExtract(item, pathKey, targetKey, arrayIndex));
            }
        } else if (isValidArrayIndex(array, arrayIndex)) {
            results.addAll(searchAndExtract(array.get(arrayIndex), pathKey, targetKey, arrayIndex));
        }
    }

    // ==================================================================================
    // 第九部分：值处理方法
    // 将JsonElement转换为Java对象并添加到结果集合
    // ==================================================================================

    /**
     * 将JSON元素的值添加到结果集合
     * 
     * <p>处理规则：</p>
     * <ul>
     *   <li>原始类型（字符串、数字、布尔）：直接添加</li>
     *   <li>数组类型：展开后逐个添加</li>
     *   <li>对象类型：忽略（不把整个对象作为值）</li>
     *   <li>null：忽略</li>
     * </ul>
     * 
     * @param results 结果集合
     * @param element JSON元素
     */
    private static void addPrimitiveValue(Set<Object> results, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonPrimitive()) {
            addPrimitive(results, element.getAsJsonPrimitive());
        } else if (element.isJsonArray()) {
            // 如果值本身是数组，展开添加每个元素
            expandArrayValues(results, element.getAsJsonArray());
        }
        // JsonObject类型的值不添加
    }

    /**
     * 将JsonPrimitive添加到结果集合
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
     * 转换数字类型，保持原始精度
     * 
     * <p>如果数字没有小数部分，转为Long；否则保持为Double。</p>
     * <p>注意：不能使用三元运算符，因为 long 和 double 会被统一为 double 类型。</p>
     * 
     * @param number 数字
     * @return 转换后的数字（Long或Double）
     */
    private static Number convertNumber(Number number) {
        double doubleValue = number.doubleValue();
        long longValue = number.longValue();
        // 注意：不能写成 return (doubleValue == longValue) ? longValue : doubleValue;
        // 因为三元运算符会将 long 和 double 统一转换为 double 类型
        if (doubleValue == longValue) {
            return longValue;  // 返回 Long
        }
        return doubleValue;  // 返回 Double
    }

    /**
     * 展开数组，将每个元素添加到结果集合
     * 
     * @param results 结果集合
     * @param array   JSON数组
     */
    private static void expandArrayValues(Set<Object> results, JsonArray array) {
        for (JsonElement item : array) {
            addPrimitiveValue(results, item);
        }
    }

    // ==================================================================================
    // 第十部分：辅助工具方法
    // ==================================================================================

    /**
     * 处理单个映射条目
     * 
     * @param result     结果Map
     * @param jsonString JSON字符串
     * @param mapping    映射数组 [pathKey, targetKey]
     * @param arrayIndex 数组索引
     */
    private static void processMapping(Map<String, Set<Object>> result, String jsonString, 
                                        String[] mapping, int arrayIndex) {
        if (mapping == null || mapping.length < 2) {
            return;
        }
        String pathKey = mapping[0];
        String targetKey = mapping[1];
        Set<Object> values = extractAllValuesWithArrayIndex(jsonString, pathKey, targetKey, arrayIndex);
        result.put(targetKey, values);
    }

    /**
     * 将Set<Object>的Map转换为List<Object>的Map
     * 
     * @param setMap 原始Map
     * @return 转换后的Map
     */
    private static Map<String, List<Object>> convertToListMap(Map<String, Set<Object>> setMap) {
        return setMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new ArrayList<>(e.getValue()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * 从值集合中过滤出字符串类型
     * 
     * @param values 值集合
     * @return 只包含字符串的集合
     */
    private static Set<String> filterStrings(Set<Object> values) {
        return values.stream()
                .filter(v -> v instanceof String)
                .map(v -> (String) v)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
