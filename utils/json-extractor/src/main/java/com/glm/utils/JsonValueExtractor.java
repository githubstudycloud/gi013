package com.glm.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import java.util.*;

/**
 * JSON嵌套值提取工具类
 * 
 * <p>从嵌套的JSON结构中递归提取指定路径下特定键的所有值并去重（保留顺序）。</p>
 * 
 * <h3>核心特性：</h3>
 * <ul>
 *   <li><b>任意深度搜索pathKey</b>：pathKey可以在JSON的任意深度位置</li>
 *   <li><b>任意深度搜索targetKey</b>：targetKey可以在pathKey下的任意深度</li>
 *   <li><b>路径链支持</b>：支持 a -> a1 -> aenv 这样的多级路径</li>
 *   <li><b>字符串JSON解析</b>：支持解析字符串类型的JSON字段值</li>
 *   <li><b>嵌套同名路径</b>：pathKey内部如有同名pathKey，只取最内层</li>
 *   <li><b>去重保序</b>：使用LinkedHashSet，自动去重并保留插入顺序</li>
 *   <li><b>数组索引</b>：支持指定只取数组中的第n个元素</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 路径链：指定 a 下的 a1 里的 aenv
 * String json = "{\"a\":{\"a1\":{\"aenv\":\"value\"}}}";
 * Set<Object> result = JsonValueExtractor.extractWithPathChain(json, Arrays.asList("a", "a1"), "aenv");
 * // 结果: [value]
 * 
 * // 字符串JSON字段：sa字段的值是JSON字符串
 * String json2 = "{\"sa\":\"{\\\"a\\\":{\\\"aenv\\\":\\\"innerValue\\\"}}\"}";
 * Set<Object> result2 = JsonValueExtractor.extractFromStringField(json2, "sa", "a", "aenv");
 * // 结果: [innerValue]
 * }</pre>
 * 
 * @author GLM
 * @version 1.4.1
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
    // ==================================================================================

    /**
     * 【推荐】从JSON中提取指定路径下目标键的所有值
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
    // 第二部分：路径链方法（新增）
    // 支持指定多级路径，如 a -> a1 -> aenv
    // ==================================================================================

    /**
     * 【新增】使用路径链提取值
     * 
     * <p>按顺序依次进入每个路径节点，最后在最终路径下搜索目标键。</p>
     * 
     * <h4>示例：</h4>
     * <pre>{@code
     * // 提取 a 下的 a1 里的 aenv
     * String json = "{\"a\":{\"a1\":{\"aenv\":\"value\"},\"aenv\":\"ignored\"}}";
     * Set<Object> result = JsonValueExtractor.extractWithPathChain(json, Arrays.asList("a", "a1"), "aenv");
     * // 结果: [value]  （a下直接的aenv被忽略，只取a1下的）
     * 
     * // 深层路径链
     * String json2 = "{\"root\":{\"a\":{\"b\":{\"c\":{\"target\":\"found\"}}}}}";
     * Set<Object> result2 = JsonValueExtractor.extractWithPathChain(json2, Arrays.asList("a", "b", "c"), "target");
     * // 结果: [found]
     * }</pre>
     * 
     * <p><b>路径链规则：</b></p>
     * <ul>
     *   <li>每个路径节点都会在当前范围内搜索（任意深度）</li>
     *   <li>找到后进入该节点，继续搜索下一个路径节点</li>
     *   <li>最后一个路径节点下搜索 targetKey</li>
     *   <li>如果路径链中任何一个节点找不到，返回空集合</li>
     * </ul>
     *
     * @param jsonString JSON字符串
     * @param pathChain  路径链列表，如 ["a", "a1"] 表示先找 a，再在 a 下找 a1
     * @param targetKey  目标键名
     * @return 去重后的值集合
     * @throws IllegalArgumentException 如果参数无效
     */
    public static Set<Object> extractWithPathChain(String jsonString, List<String> pathChain, String targetKey) {
        return extractWithPathChainAndArrayIndex(jsonString, pathChain, targetKey, ARRAY_INDEX_ALL);
    }

    /**
     * 使用路径链提取值，支持数组索引
     *
     * @param jsonString JSON字符串
     * @param pathChain  路径链列表
     * @param targetKey  目标键名
     * @param arrayIndex 数组索引
     * @return 去重后的值集合
     */
    public static Set<Object> extractWithPathChainAndArrayIndex(String jsonString, List<String> pathChain, 
                                                                 String targetKey, int arrayIndex) {
        validateJsonString(jsonString);
        if (pathChain == null || pathChain.isEmpty()) {
            throw new IllegalArgumentException("Path chain cannot be null or empty");
        }
        if (targetKey == null) {
            throw new IllegalArgumentException("targetKey cannot be null");
        }

        JsonElement root = JsonParser.parseString(jsonString);
        return extractWithPathChainRecursive(root, pathChain, 0, targetKey, arrayIndex);
    }

    /**
     * 使用路径链提取第一个元素
     *
     * @param jsonString JSON字符串
     * @param pathChain  路径链列表
     * @param targetKey  目标键名
     * @return 去重后的值集合
     */
    public static Set<Object> extractFirstWithPathChain(String jsonString, List<String> pathChain, String targetKey) {
        return extractWithPathChainAndArrayIndex(jsonString, pathChain, targetKey, 0);
    }

    /**
     * 使用路径链提取字符串值
     *
     * @param jsonString JSON字符串
     * @param pathChain  路径链列表
     * @param targetKey  目标键名
     * @return 字符串值集合
     */
    public static Set<String> extractStringWithPathChain(String jsonString, List<String> pathChain, String targetKey) {
        Set<Object> values = extractWithPathChain(jsonString, pathChain, targetKey);
        return filterStrings(values);
    }

    // ==================================================================================
    // 第三部分：字符串JSON字段解析方法（新增）
    // 支持解析字符串类型的JSON字段值
    // ==================================================================================

    /**
     * 【新增】从字符串类型的JSON字段中提取值
     * 
     * <p>某些JSON中，一个字段的值本身是JSON字符串（而不是JSON对象）。
     * 此方法会先找到该字段，解析其字符串值为JSON，然后在里面搜索目标。</p>
     * 
     * <h4>示例：</h4>
     * <pre>{@code
     * // sa 字段的值是一个JSON字符串
     * String json = "{\"sa\":\"{\\\"a\\\":{\\\"aenv\\\":\\\"innerValue\\\"}}\"}";
     * Set<Object> result = JsonValueExtractor.extractFromStringField(json, "sa", "a", "aenv");
     * // 结果: [innerValue]
     * 
     * // 多个 sa 字段（在不同位置）
     * String json2 = "{\"item1\":{\"sa\":\"{\\\"a\\\":{\\\"aenv\\\":\\\"v1\\\"}}\"}," +
     *                 "\"item2\":{\"sa\":\"{\\\"a\\\":{\\\"aenv\\\":\\\"v2\\\"}}\"}}";
     * Set<Object> result2 = JsonValueExtractor.extractFromStringField(json2, "sa", "a", "aenv");
     * // 结果: [v1, v2]
     * }</pre>
     * 
     * <p><b>工作原理：</b></p>
     * <ol>
     *   <li>在整个JSON中搜索所有名为 stringFieldKey 的字段</li>
     *   <li>对于每个找到的字段，检查其值是否为字符串</li>
     *   <li>尝试将字符串值解析为JSON</li>
     *   <li>在解析后的JSON中搜索 pathKey 下的 targetKey</li>
     * </ol>
     *
     * @param jsonString     JSON字符串
     * @param stringFieldKey 字符串类型JSON字段的键名（如 "sa"）
     * @param pathKey        在解析后的JSON中搜索的路径键
     * @param targetKey      目标键名
     * @return 去重后的值集合
     */
    public static Set<Object> extractFromStringField(String jsonString, String stringFieldKey, 
                                                      String pathKey, String targetKey) {
        return extractFromStringFieldWithArrayIndex(jsonString, stringFieldKey, pathKey, targetKey, ARRAY_INDEX_ALL);
    }

    /**
     * 从字符串类型的JSON字段中提取值，支持数组索引
     *
     * @param jsonString     JSON字符串
     * @param stringFieldKey 字符串类型JSON字段的键名
     * @param pathKey        路径键
     * @param targetKey      目标键
     * @param arrayIndex     数组索引
     * @return 去重后的值集合
     */
    public static Set<Object> extractFromStringFieldWithArrayIndex(String jsonString, String stringFieldKey, 
                                                                    String pathKey, String targetKey, 
                                                                    int arrayIndex) {
        validateJsonString(jsonString);
        if (stringFieldKey == null) {
            throw new IllegalArgumentException("stringFieldKey cannot be null");
        }
        validatePathAndTargetKey(pathKey, targetKey);

        JsonElement root = JsonParser.parseString(jsonString);
        Set<Object> results = new LinkedHashSet<>();
        
        // 找到所有字符串JSON字段并解析
        collectFromStringFields(results, root, stringFieldKey, pathKey, targetKey, arrayIndex);
        
        return results;
    }

    /**
     * 从字符串类型的JSON字段中提取第一个元素
     *
     * @param jsonString     JSON字符串
     * @param stringFieldKey 字符串类型JSON字段的键名
     * @param pathKey        路径键
     * @param targetKey      目标键
     * @return 去重后的值集合
     */
    public static Set<Object> extractFirstFromStringField(String jsonString, String stringFieldKey, 
                                                           String pathKey, String targetKey) {
        return extractFromStringFieldWithArrayIndex(jsonString, stringFieldKey, pathKey, targetKey, 0);
    }

    /**
     * 从字符串类型的JSON字段中提取字符串值
     *
     * @param jsonString     JSON字符串
     * @param stringFieldKey 字符串类型JSON字段的键名
     * @param pathKey        路径键
     * @param targetKey      目标键
     * @return 字符串值集合
     */
    public static Set<String> extractStringFromStringField(String jsonString, String stringFieldKey, 
                                                            String pathKey, String targetKey) {
        Set<Object> values = extractFromStringField(jsonString, stringFieldKey, pathKey, targetKey);
        return filterStrings(values);
    }

    /**
     * 【新增】从字符串类型的JSON字段中使用路径链提取值
     * 
     * <p>结合字符串JSON解析和路径链功能。</p>
     * 
     * <h4>示例：</h4>
     * <pre>{@code
     * // sa 字段的值是JSON字符串，里面有 a -> a1 -> aenv
     * String json = "{\"sa\":\"{\\\"a\\\":{\\\"a1\\\":{\\\"aenv\\\":\\\"deep\\\"}}}\"}";
     * Set<Object> result = JsonValueExtractor.extractFromStringFieldWithPathChain(
     *     json, "sa", Arrays.asList("a", "a1"), "aenv");
     * // 结果: [deep]
     * }</pre>
     *
     * @param jsonString     JSON字符串
     * @param stringFieldKey 字符串类型JSON字段的键名
     * @param pathChain      路径链
     * @param targetKey      目标键
     * @return 去重后的值集合
     */
    public static Set<Object> extractFromStringFieldWithPathChain(String jsonString, String stringFieldKey, 
                                                                   List<String> pathChain, String targetKey) {
        return extractFromStringFieldWithPathChainAndArrayIndex(jsonString, stringFieldKey, pathChain, targetKey, ARRAY_INDEX_ALL);
    }

    /**
     * 从字符串类型的JSON字段中使用路径链提取值，支持数组索引
     *
     * @param jsonString     JSON字符串
     * @param stringFieldKey 字符串类型JSON字段的键名
     * @param pathChain      路径链
     * @param targetKey      目标键
     * @param arrayIndex     数组索引
     * @return 去重后的值集合
     */
    public static Set<Object> extractFromStringFieldWithPathChainAndArrayIndex(String jsonString, 
                                                                                String stringFieldKey, 
                                                                                List<String> pathChain, 
                                                                                String targetKey, 
                                                                                int arrayIndex) {
        validateJsonString(jsonString);
        if (stringFieldKey == null) {
            throw new IllegalArgumentException("stringFieldKey cannot be null");
        }
        if (pathChain == null || pathChain.isEmpty()) {
            throw new IllegalArgumentException("pathChain cannot be null or empty");
        }
        if (targetKey == null) {
            throw new IllegalArgumentException("targetKey cannot be null");
        }

        JsonElement root = JsonParser.parseString(jsonString);
        Set<Object> results = new LinkedHashSet<>();
        
        // 找到所有字符串JSON字段并用路径链解析
        collectFromStringFieldsWithPathChain(results, root, stringFieldKey, pathChain, targetKey, arrayIndex);
        
        return results;
    }

    /**
     * 【便捷方法】从字符串类型的JSON字段中使用路径链提取值，只取每个数组的第一个元素
     * 
     * <p>等同于 extractFromStringFieldWithPathChainAndArrayIndex(json, stringFieldKey, pathChain, targetKey, 0)</p>
     *
     * @param jsonString     JSON字符串
     * @param stringFieldKey 字符串类型JSON字段的键名
     * @param pathChain      路径链
     * @param targetKey      目标键
     * @return 去重后的值集合，只包含数组第一个元素
     */
    public static Set<Object> extractFirstFromStringFieldWithPathChain(String jsonString, String stringFieldKey, 
                                                                        List<String> pathChain, String targetKey) {
        return extractFromStringFieldWithPathChainAndArrayIndex(jsonString, stringFieldKey, pathChain, targetKey, 0);
    }

    /**
     * 【便捷方法】从字符串类型的JSON字段中提取字符串值，只取每个数组的第一个元素
     * 
     * <p>结合了字符串JSON解析、字符串类型过滤和数组第一个元素提取。</p>
     *
     * @param jsonString     JSON字符串
     * @param stringFieldKey 字符串类型JSON字段的键名
     * @param pathKey        路径键
     * @param targetKey      目标键
     * @return 字符串值集合，只包含数组第一个元素
     */
    public static Set<String> extractFirstStringFromStringField(String jsonString, String stringFieldKey, 
                                                                 String pathKey, String targetKey) {
        Set<Object> values = extractFirstFromStringField(jsonString, stringFieldKey, pathKey, targetKey);
        return filterStrings(values);
    }

    /**
     * 【便捷方法】从字符串类型的JSON字段中使用路径链提取字符串值
     * 
     * <p>结合了字符串JSON解析、路径链和字符串类型过滤。</p>
     *
     * @param jsonString     JSON字符串
     * @param stringFieldKey 字符串类型JSON字段的键名
     * @param pathChain      路径链
     * @param targetKey      目标键
     * @return 字符串值集合
     */
    public static Set<String> extractStringFromStringFieldWithPathChain(String jsonString, String stringFieldKey, 
                                                                         List<String> pathChain, String targetKey) {
        Set<Object> values = extractFromStringFieldWithPathChain(jsonString, stringFieldKey, pathChain, targetKey);
        return filterStrings(values);
    }

    /**
     * 【便捷方法】从字符串类型的JSON字段中使用路径链提取字符串值，只取每个数组的第一个元素
     * 
     * <p>结合了字符串JSON解析、路径链、字符串类型过滤和数组第一个元素提取。</p>
     *
     * @param jsonString     JSON字符串
     * @param stringFieldKey 字符串类型JSON字段的键名
     * @param pathChain      路径链
     * @param targetKey      目标键
     * @return 字符串值集合，只包含数组第一个元素
     */
    public static Set<String> extractFirstStringFromStringFieldWithPathChain(String jsonString, String stringFieldKey, 
                                                                              List<String> pathChain, String targetKey) {
        Set<Object> values = extractFirstFromStringFieldWithPathChain(jsonString, stringFieldKey, pathChain, targetKey);
        return filterStrings(values);
    }

    // ==================================================================================
    // 第四部分：兼容方法（保留向后兼容）
    // 这些方法是为了保持与早期版本的API兼容性而保留的
    // ==================================================================================

    /**
     * 从JSON中提取指定路径下目标键的所有值
     * 
     * <p><b>兼容方法：</b>此方法与 {@link #extractAllValues} 功能相同，保留是为了向后兼容。</p>
     *
     * @param jsonString JSON字符串
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @return 去重后的值集合
     * @see #extractAllValues(String, String, String)
     */
    public static Set<Object> extractValuesUnderPath(String jsonString, String pathKey, String targetKey) {
        return extractAllValues(jsonString, pathKey, targetKey);
    }

    /**
     * 从JsonObject中提取指定路径下目标键的所有值
     * 
     * <p>适用于已经解析好的JsonObject对象，避免重复解析JSON字符串。</p>
     *
     * @param jsonObject JSON对象，可以为null（返回空集合）
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
     * 从JSON中提取值，支持指定数组索引
     * 
     * <p><b>兼容方法：</b>与 {@link #extractAllValuesWithArrayIndex} 功能相同。</p>
     *
     * @param jsonString JSON字符串
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @param arrayIndex 数组索引（0-based），-1表示遍历所有元素
     * @return 去重后的值集合
     * @see #extractAllValuesWithArrayIndex(String, String, String, int)
     */
    public static Set<Object> extractValuesWithArrayIndex(String jsonString, String pathKey, 
                                                           String targetKey, int arrayIndex) {
        return extractAllValuesWithArrayIndex(jsonString, pathKey, targetKey, arrayIndex);
    }

    /**
     * 从JsonObject中提取值，支持指定数组索引
     *
     * @param jsonObject JSON对象，可以为null（返回空集合）
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @param arrayIndex 数组索引（0-based），-1表示遍历所有元素
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
     * <p><b>兼容方法：</b>与 {@link #extractAllFirstValues} 功能相同。</p>
     *
     * @param jsonString JSON字符串
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @return 去重后的值集合，只包含每个数组的第一个元素
     * @see #extractAllFirstValues(String, String, String)
     */
    public static Set<Object> extractFirstValuesFromArrays(String jsonString, String pathKey, String targetKey) {
        return extractAllFirstValues(jsonString, pathKey, targetKey);
    }

    // ==================================================================================
    // 第五部分：批量提取方法
    // 支持一次调用提取多组路径-键值对，提高效率
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
     * @param jsonString JSON字符串
     * @param mappings   映射列表，每个元素为 [pathKey, targetKey] 数组
     * @return Map，key为targetKey，value为提取到的值集合
     * @throws IllegalArgumentException 如果参数无效
     */
    public static Map<String, Set<Object>> batchExtract(String jsonString, List<String[]> mappings) {
        return batchExtractWithArrayIndex(jsonString, mappings, ARRAY_INDEX_ALL);
    }

    /**
     * 批量提取多组键值对，支持指定数组索引
     *
     * @param jsonString JSON字符串
     * @param mappings   映射列表，每个元素为 [pathKey, targetKey] 数组
     * @param arrayIndex 数组索引（0-based），-1表示遍历所有元素
     * @return Map，key为targetKey，value为提取到的值集合
     * @throws IllegalArgumentException 如果参数无效
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
     * 批量提取并返回List形式的结果
     * 
     * <p>与 {@link #batchExtract} 的区别在于返回值类型是 List 而不是 Set，方便某些场景使用。</p>
     *
     * @param jsonString JSON字符串
     * @param mappings   映射列表
     * @return Map，key为targetKey，value为提取到的值列表
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
    // 第六部分：字符串专用方法
    // 这些方法只提取字符串类型的值，自动过滤掉数字、布尔等其他类型
    // ==================================================================================

    /**
     * 提取指定路径下目标键的所有字符串类型值
     * 
     * <p>只返回字符串类型的值，自动过滤数字、布尔值等其他类型。</p>
     *
     * @param jsonString JSON字符串
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @return 去重后的字符串值集合
     */
    public static Set<String> extractStringValues(String jsonString, String pathKey, String targetKey) {
        Set<Object> values = extractAllValues(jsonString, pathKey, targetKey);
        return filterStrings(values);
    }

    /**
     * 提取字符串值并返回List形式
     *
     * @param jsonString JSON字符串
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @return 去重后的字符串值列表
     */
    public static List<String> extractStringValuesAsList(String jsonString, String pathKey, String targetKey) {
        return new ArrayList<>(extractStringValues(jsonString, pathKey, targetKey));
    }

    /**
     * 提取字符串值，只取每个数组的第一个元素
     *
     * @param jsonString JSON字符串
     * @param pathKey    路径键名
     * @param targetKey  目标键名
     * @return 去重后的字符串值集合，只包含每个数组的第一个元素
     */
    public static Set<String> extractFirstStringValues(String jsonString, String pathKey, String targetKey) {
        Set<Object> values = extractAllFirstValues(jsonString, pathKey, targetKey);
        return filterStrings(values);
    }

    // ==================================================================================
    // 第七部分：路径链内部实现
    // ==================================================================================

    /**
     * 递归处理路径链
     * 
     * @param element    当前元素
     * @param pathChain  路径链
     * @param pathIndex  当前路径索引
     * @param targetKey  目标键
     * @param arrayIndex 数组索引
     * @return 提取到的值集合
     */
    private static Set<Object> extractWithPathChainRecursive(JsonElement element, List<String> pathChain, 
                                                              int pathIndex, String targetKey, int arrayIndex) {
        Set<Object> results = new LinkedHashSet<>();

        if (element == null || element.isJsonNull()) {
            return results;
        }

        // 当前要找的路径键
        String currentPathKey = pathChain.get(pathIndex);
        boolean isLastPath = (pathIndex == pathChain.size() - 1);

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (entry.getKey().equals(currentPathKey)) {
                    JsonElement pathValue = entry.getValue();
                    // 处理嵌套同名路径
                    JsonElement innermostValue = findInnermostPathKey(pathValue, currentPathKey, arrayIndex);
                    
                    if (isLastPath) {
                        // 最后一个路径，搜索targetKey
                        results.addAll(searchTargetKeyInSubtree(innermostValue, targetKey, arrayIndex));
                    } else {
                        // 不是最后一个路径，继续下一个路径
                        results.addAll(extractWithPathChainRecursive(innermostValue, pathChain, pathIndex + 1, targetKey, arrayIndex));
                    }
                } else {
                    // 继续在子节点中搜索当前路径键
                    results.addAll(extractWithPathChainRecursive(entry.getValue(), pathChain, pathIndex, targetKey, arrayIndex));
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (arrayIndex == ARRAY_INDEX_ALL) {
                for (JsonElement item : array) {
                    results.addAll(extractWithPathChainRecursive(item, pathChain, pathIndex, targetKey, arrayIndex));
                }
            } else if (arrayIndex >= 0 && arrayIndex < array.size()) {
                results.addAll(extractWithPathChainRecursive(array.get(arrayIndex), pathChain, pathIndex, targetKey, arrayIndex));
            }
        }

        return results;
    }

    // ==================================================================================
    // 第八部分：字符串JSON字段解析内部实现
    // ==================================================================================

    /**
     * 收集字符串JSON字段中的值
     */
    private static void collectFromStringFields(Set<Object> results, JsonElement element, 
                                                 String stringFieldKey, String pathKey, 
                                                 String targetKey, int arrayIndex) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (entry.getKey().equals(stringFieldKey)) {
                    // 找到字符串JSON字段
                    JsonElement value = entry.getValue();
                    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                        String jsonStr = value.getAsString();
                        // 尝试解析字符串为JSON
                        Set<Object> extracted = parseAndExtract(jsonStr, pathKey, targetKey, arrayIndex);
                        results.addAll(extracted);
                    }
                }
                // 继续递归搜索
                collectFromStringFields(results, entry.getValue(), stringFieldKey, pathKey, targetKey, arrayIndex);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                collectFromStringFields(results, item, stringFieldKey, pathKey, targetKey, arrayIndex);
            }
        }
    }

    /**
     * 收集字符串JSON字段中的值（使用路径链）
     */
    private static void collectFromStringFieldsWithPathChain(Set<Object> results, JsonElement element, 
                                                              String stringFieldKey, List<String> pathChain, 
                                                              String targetKey, int arrayIndex) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (entry.getKey().equals(stringFieldKey)) {
                    // 找到字符串JSON字段
                    JsonElement value = entry.getValue();
                    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                        String jsonStr = value.getAsString();
                        // 尝试解析字符串为JSON并使用路径链提取
                        Set<Object> extracted = parseAndExtractWithPathChain(jsonStr, pathChain, targetKey, arrayIndex);
                        results.addAll(extracted);
                    }
                }
                // 继续递归搜索
                collectFromStringFieldsWithPathChain(results, entry.getValue(), stringFieldKey, pathChain, targetKey, arrayIndex);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                collectFromStringFieldsWithPathChain(results, item, stringFieldKey, pathChain, targetKey, arrayIndex);
            }
        }
    }

    /**
     * 解析JSON字符串并提取值
     */
    private static Set<Object> parseAndExtract(String jsonStr, String pathKey, String targetKey, int arrayIndex) {
        try {
            JsonElement parsed = JsonParser.parseString(jsonStr);
            return findPathKeysAndExtract(parsed, pathKey, targetKey, arrayIndex);
        } catch (JsonSyntaxException e) {
            // 解析失败，返回空集合
            return Collections.emptySet();
        }
    }

    /**
     * 解析JSON字符串并使用路径链提取值
     */
    private static Set<Object> parseAndExtractWithPathChain(String jsonStr, List<String> pathChain, 
                                                             String targetKey, int arrayIndex) {
        try {
            JsonElement parsed = JsonParser.parseString(jsonStr);
            return extractWithPathChainRecursive(parsed, pathChain, 0, targetKey, arrayIndex);
        } catch (JsonSyntaxException e) {
            // 解析失败，返回空集合
            return Collections.emptySet();
        }
    }

    // ==================================================================================
    // 第九部分：核心算法 - 搜索 pathKey
    // ==================================================================================

    /**
     * 在JSON元素中搜索所有 pathKey，并提取其下的 targetKey 值
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
     */
    private static void searchPathKeyInObject(Set<Object> results, JsonObject obj, String pathKey, 
                                               String targetKey, int arrayIndex) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            if (key.equals(pathKey)) {
                Set<Object> extracted = processPathKeyValue(value, pathKey, targetKey, arrayIndex);
                results.addAll(extracted);
            } else {
                results.addAll(findPathKeysAndExtract(value, pathKey, targetKey, arrayIndex));
            }
        }
    }

    /**
     * 在JsonArray中搜索 pathKey
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
    // 第十部分：核心算法 - 处理 pathKey 值并提取 targetKey
    // ==================================================================================

    /**
     * 处理找到的 pathKey 的值
     */
    private static Set<Object> processPathKeyValue(JsonElement pathValue, String pathKey, 
                                                    String targetKey, int arrayIndex) {
        JsonElement innermostValue = findInnermostPathKey(pathValue, pathKey, arrayIndex);
        return searchTargetKeyInSubtree(innermostValue, targetKey, arrayIndex);
    }

    /**
     * 找到最内层的同名 pathKey
     */
    private static JsonElement findInnermostPathKey(JsonElement element, String pathKey, int arrayIndex) {
        if (element == null || element.isJsonNull()) {
            return element;
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has(pathKey)) {
                return findInnermostPathKey(obj.get(pathKey), pathKey, arrayIndex);
            }
        }

        return element;
    }

    // ==================================================================================
    // 第十一部分：核心算法 - 在子树中搜索 targetKey
    // ==================================================================================

    /**
     * 在子树中递归搜索所有 targetKey
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
     */
    private static void searchTargetInObject(Set<Object> results, JsonObject obj, 
                                              String targetKey, int arrayIndex) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            if (key.equals(targetKey)) {
                addValue(results, value);
            }

            results.addAll(searchTargetKeyInSubtree(value, targetKey, arrayIndex));
        }
    }

    /**
     * 在JsonArray中搜索 targetKey
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
    // 第十二部分：值处理方法
    // ==================================================================================

    /**
     * 将值添加到结果集合
     */
    private static void addValue(Set<Object> results, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonPrimitive()) {
            addPrimitive(results, element.getAsJsonPrimitive());
        } else if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                addValue(results, item);
            }
        }
    }

    /**
     * 添加原始类型值
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
    // 第十三部分：参数校验方法
    // ==================================================================================

    private static void validateInputs(String jsonString, String pathKey, String targetKey) {
        validateJsonString(jsonString);
        validatePathAndTargetKey(pathKey, targetKey);
    }

    private static void validateJsonString(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
    }

    private static void validatePathAndTargetKey(String pathKey, String targetKey) {
        if (pathKey == null || targetKey == null) {
            throw new IllegalArgumentException("pathKey and targetKey cannot be null");
        }
    }

    // ==================================================================================
    // 第十四部分：辅助工具方法
    // ==================================================================================

    /**
     * 过滤出字符串类型的值
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
