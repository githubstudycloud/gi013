package com.glm.utils;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JsonValueExtractor 单元测试类
 * 
 * @author GLM
 * @version 1.1.0
 */
public class JsonValueExtractorTest {

    // ==================== 基础功能测试 ====================

    @Test
    public void testExtractSimpleValue() {
        String json = "{\"a\":{\"aenv\":\"env1\"}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("env1"));
    }

    @Test
    public void testExtractNestedValues() {
        String json = "{\"a\":{\"aenv\":\"env1\",\"nested\":{\"aenv\":\"env2\"}}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("env1"));
        assertTrue(result.contains("env2"));
    }

    @Test
    public void testExtractFromArray() {
        String json = "{\"a\":{\"list\":[{\"aenv\":\"env1\"},{\"aenv\":\"env2\"}]}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("env1"));
        assertTrue(result.contains("env2"));
    }

    @Test
    public void testExtractDeeplyNested() {
        String json = "{\"a\":{\"level1\":{\"level2\":{\"level3\":{\"aenv\":\"deepValue\"}}}}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("deepValue"));
    }

    // ==================== 去重并保留顺序测试 ====================

    @Test
    public void testDeduplication() {
        String json = "{\"a\":{\"aenv\":\"env1\",\"nested\":{\"aenv\":\"env1\"},\"list\":[{\"aenv\":\"env1\"}]}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        // 虽然有3个env1，但去重后只有1个
        assertEquals(1, result.size());
        assertTrue(result.contains("env1"));
    }

    @Test
    public void testMixedDeduplication() {
        String json = "{\"a\":{\"aenv\":\"env1\",\"nested\":{\"aenv\":\"env2\"},\"list\":[{\"aenv\":\"env1\"},{\"aenv\":\"env3\"}]}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        assertEquals(3, result.size());
        assertTrue(result.contains("env1"));
        assertTrue(result.contains("env2"));
        assertTrue(result.contains("env3"));
    }

    @Test
    public void testOrderPreservation() {
        // 测试保留插入顺序
        String json = "{\"a\":{\"aenv\":\"first\",\"b\":{\"aenv\":\"second\"},\"c\":{\"aenv\":\"third\"}}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        List<Object> resultList = new ArrayList<>(result);
        assertEquals(3, resultList.size());
        assertEquals("first", resultList.get(0));
        assertEquals("second", resultList.get(1));
        assertEquals("third", resultList.get(2));
    }

    // ==================== 数组索引测试 ====================

    @Test
    public void testArrayIndexFirst() {
        // 测试只取数组第一个元素
        String json = "{\"a\":{\"list\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"},{\"aenv\":\"third\"}]}}";
        Set<Object> result = JsonValueExtractor.extractValuesWithArrayIndex(json, "a", "aenv", 0);
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
        assertFalse(result.contains("second"));
        assertFalse(result.contains("third"));
    }

    @Test
    public void testArrayIndexSecond() {
        // 测试只取数组第二个元素
        String json = "{\"a\":{\"list\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"},{\"aenv\":\"third\"}]}}";
        Set<Object> result = JsonValueExtractor.extractValuesWithArrayIndex(json, "a", "aenv", 1);
        
        assertEquals(1, result.size());
        assertTrue(result.contains("second"));
    }

    @Test
    public void testArrayIndexOutOfBounds() {
        // 测试索引超出范围
        String json = "{\"a\":{\"list\":[{\"aenv\":\"first\"}]}}";
        Set<Object> result = JsonValueExtractor.extractValuesWithArrayIndex(json, "a", "aenv", 5);
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void testArrayIndexAcrossMultipleArrays() {
        // 测试跨数组的情况 - 每个数组都取第一个
        String json = "{\"a\":{\"list1\":[{\"aenv\":\"arr1-first\"},{\"aenv\":\"arr1-second\"}],\"list2\":[{\"aenv\":\"arr2-first\"},{\"aenv\":\"arr2-second\"}]}}";
        Set<Object> result = JsonValueExtractor.extractValuesWithArrayIndex(json, "a", "aenv", 0);
        
        assertEquals(2, result.size());
        assertTrue(result.contains("arr1-first"));
        assertTrue(result.contains("arr2-first"));
        assertFalse(result.contains("arr1-second"));
        assertFalse(result.contains("arr2-second"));
    }

    @Test
    public void testExtractFirstValuesFromArrays() {
        // 测试便捷方法
        String json = "{\"a\":{\"list\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"}]}}";
        Set<Object> result = JsonValueExtractor.extractFirstValuesFromArrays(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
    }

    @Test
    public void testArrayIndexWithMixedStructure() {
        // 测试混合结构：对象中的值 + 数组中的值
        String json = "{\"a\":{\"aenv\":\"direct\",\"list\":[{\"aenv\":\"arr-first\"},{\"aenv\":\"arr-second\"}]}}";
        Set<Object> result = JsonValueExtractor.extractValuesWithArrayIndex(json, "a", "aenv", 0);
        
        assertEquals(2, result.size());
        assertTrue(result.contains("direct")); // 对象中的值不受数组索引影响
        assertTrue(result.contains("arr-first")); // 数组中只取第一个
        assertFalse(result.contains("arr-second"));
    }

    // ==================== 嵌套同名路径测试 (a套a) ====================

    @Test
    public void testNestedSamePathKey() {
        // 测试 a 套 a 的情况，只算最内层子集a
        String json = "{\"a\":{\"aenv\":\"parent\",\"a\":{\"aenv\":\"child\"}}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        // 应该只有child，因为内层a覆盖了外层a
        assertEquals(1, result.size());
        assertTrue(result.contains("child"));
        assertFalse(result.contains("parent"));
    }

    @Test
    public void testNestedSamePathKeyMultipleLevels() {
        // 测试多层嵌套 a -> a -> a
        String json = "{\"a\":{\"aenv\":\"level1\",\"a\":{\"aenv\":\"level2\",\"a\":{\"aenv\":\"level3\"}}}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        // 应该只有level3，最内层
        assertEquals(1, result.size());
        assertTrue(result.contains("level3"));
    }

    @Test
    public void testNestedSamePathKeyWithSiblings() {
        // 测试 a 套 a，同时有其他兄弟节点
        String json = "{\"a\":{\"aenv\":\"parent\",\"other\":{\"aenv\":\"sibling\"},\"a\":{\"aenv\":\"child\"}}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        // 应该只有child（内层a的值），parent和sibling都不算
        assertEquals(1, result.size());
        assertTrue(result.contains("child"));
    }

    @Test
    public void testNestedSamePathKeyInArray() {
        // 测试数组中的嵌套同名路径
        String json = "{\"a\":{\"list\":[{\"a\":{\"aenv\":\"nested-in-array\"}}]}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("nested-in-array"));
    }

    @Test
    public void testNoNestedSamePathKey() {
        // 测试没有嵌套同名路径的正常情况
        String json = "{\"a\":{\"aenv\":\"value1\",\"b\":{\"aenv\":\"value2\"}}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("value1"));
        assertTrue(result.contains("value2"));
    }

    // ==================== 复杂嵌套测试 ====================

    @Test
    public void testComplexNestedStructure() {
        String json = "{"
                + "\"a\":{"
                + "  \"aenv\":\"env1\","
                + "  \"nested\":{"
                + "    \"aenv\":\"env2\","
                + "    \"deep\":[{\"aenv\":\"env3\"},{\"items\":[{\"aenv\":\"env4\"},{\"aenv\":\"env1\"}]}]"
                + "  },"
                + "  \"list_data\":[{\"aenv\":\"env5\"},{\"other\":{\"aenv\":\"env6\"}}]"
                + "},"
                + "\"b\":{"
                + "  \"benv\":\"benv1\","
                + "  \"config\":[{\"benv\":\"benv2\"},{\"sub\":{\"benv\":\"benv3\"}}]"
                + "}"
                + "}";

        // 提取 a 下所有 aenv
        Set<Object> aenvValues = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        assertEquals(6, aenvValues.size());
        assertTrue(aenvValues.contains("env1"));
        assertTrue(aenvValues.contains("env2"));
        assertTrue(aenvValues.contains("env3"));
        assertTrue(aenvValues.contains("env4"));
        assertTrue(aenvValues.contains("env5"));
        assertTrue(aenvValues.contains("env6"));

        // 提取 b 下所有 benv
        Set<Object> benvValues = JsonValueExtractor.extractValuesUnderPath(json, "b", "benv");
        assertEquals(3, benvValues.size());
        assertTrue(benvValues.contains("benv1"));
        assertTrue(benvValues.contains("benv2"));
        assertTrue(benvValues.contains("benv3"));
    }

    @Test
    public void testComplexWithArrayIndex() {
        String json = "{"
                + "\"a\":{"
                + "  \"aenv\":\"direct\","
                + "  \"list\":[{\"aenv\":\"arr1\"},{\"aenv\":\"arr2\"}],"
                + "  \"nested\":{\"items\":[{\"aenv\":\"nested1\"},{\"aenv\":\"nested2\"}]}"
                + "}"
                + "}";

        // 只取每个数组的第一个
        Set<Object> result = JsonValueExtractor.extractValuesWithArrayIndex(json, "a", "aenv", 0);
        
        assertEquals(3, result.size());
        assertTrue(result.contains("direct"));   // 非数组的直接值
        assertTrue(result.contains("arr1"));     // 第一个数组的第一个
        assertTrue(result.contains("nested1"));  // 嵌套数组的第一个
        assertFalse(result.contains("arr2"));
        assertFalse(result.contains("nested2"));
    }

    // ==================== extractAllValues 测试 ====================

    @Test
    public void testExtractAllValuesWithNestedPath() {
        // pathKey本身也嵌套在深层
        String json = "{\"root\":{\"config\":{\"a\":{\"aenv\":\"env1\"}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("env1"));
    }

    @Test
    public void testExtractAllValuesMultiplePaths() {
        // 多个位置都有pathKey
        String json = "{\"first\":{\"a\":{\"aenv\":\"env1\"}},\"second\":{\"a\":{\"aenv\":\"env2\"}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("env1"));
        assertTrue(result.contains("env2"));
    }

    @Test
    public void testExtractAllValuesWithArrayIndex() {
        String json = "{\"root\":{\"a\":{\"list\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"}]}}}";
        Set<Object> result = JsonValueExtractor.extractAllValuesWithArrayIndex(json, "a", "aenv", 0);
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
    }

    @Test
    public void testExtractAllFirstValues() {
        String json = "{\"root\":{\"a\":{\"list\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"}]}}}";
        Set<Object> result = JsonValueExtractor.extractAllFirstValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
    }

    // ==================== 批量提取测试 ====================

    @Test
    public void testBatchExtract() {
        String json = "{\"a\":{\"aenv\":\"env1\"},\"b\":{\"benv\":\"benv1\"}}";
        List<String[]> mappings = Arrays.asList(
                new String[]{"a", "aenv"},
                new String[]{"b", "benv"}
        );

        Map<String, Set<Object>> result = JsonValueExtractor.batchExtract(json, mappings);

        assertEquals(2, result.size());
        assertTrue(result.get("aenv").contains("env1"));
        assertTrue(result.get("benv").contains("benv1"));
    }

    @Test
    public void testBatchExtractWithArrayIndex() {
        String json = "{\"a\":{\"list\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"}]},\"b\":{\"list\":[{\"benv\":\"b-first\"},{\"benv\":\"b-second\"}]}}";
        List<String[]> mappings = Arrays.asList(
                new String[]{"a", "aenv"},
                new String[]{"b", "benv"}
        );

        Map<String, Set<Object>> result = JsonValueExtractor.batchExtractWithArrayIndex(json, mappings, 0);

        assertEquals(2, result.size());
        assertEquals(1, result.get("aenv").size());
        assertEquals(1, result.get("benv").size());
        assertTrue(result.get("aenv").contains("first"));
        assertTrue(result.get("benv").contains("b-first"));
    }

    @Test
    public void testBatchExtractAsList() {
        String json = "{\"a\":{\"aenv\":\"env1\",\"nested\":{\"aenv\":\"env2\"}}}";
        List<String[]> mappings = Collections.singletonList(new String[]{"a", "aenv"});

        Map<String, List<Object>> result = JsonValueExtractor.batchExtractAsList(json, mappings);

        assertEquals(1, result.size());
        assertEquals(2, result.get("aenv").size());
    }

    // ==================== 类型处理测试 ====================

    @Test
    public void testExtractNumericValues() {
        String json = "{\"a\":{\"aenv\":123,\"nested\":{\"aenv\":456}}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains(123L));
        assertTrue(result.contains(456L));
    }

    @Test
    public void testExtractBooleanValues() {
        String json = "{\"a\":{\"aenv\":true,\"nested\":{\"aenv\":false}}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains(true));
        assertTrue(result.contains(false));
    }

    @Test
    public void testExtractMixedTypeValues() {
        String json = "{\"a\":{\"aenv\":\"str\",\"nested\":{\"aenv\":123},\"other\":{\"aenv\":true}}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        assertEquals(3, result.size());
        assertTrue(result.contains("str"));
        assertTrue(result.contains(123L));
        assertTrue(result.contains(true));
    }

    @Test
    public void testExtractArrayValue() {
        // 如果目标值本身是数组，应该展开
        String json = "{\"a\":{\"aenv\":[\"v1\",\"v2\",\"v3\"]}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        assertEquals(3, result.size());
        assertTrue(result.contains("v1"));
        assertTrue(result.contains("v2"));
        assertTrue(result.contains("v3"));
    }

    // ==================== 字符串专用方法测试 ====================

    @Test
    public void testExtractStringValues() {
        String json = "{\"a\":{\"aenv\":\"str1\",\"nested\":{\"aenv\":123},\"other\":{\"aenv\":\"str2\"}}}";
        Set<String> result = JsonValueExtractor.extractStringValues(json, "a", "aenv");
        
        // 只包含字符串类型
        assertEquals(2, result.size());
        assertTrue(result.contains("str1"));
        assertTrue(result.contains("str2"));
        assertFalse(result.contains("123"));
    }

    @Test
    public void testExtractStringValuesAsList() {
        String json = "{\"a\":{\"aenv\":\"str1\",\"nested\":{\"aenv\":\"str2\"}}}";
        List<String> result = JsonValueExtractor.extractStringValuesAsList(json, "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("str1"));
        assertTrue(result.contains("str2"));
    }

    @Test
    public void testExtractFirstStringValues() {
        String json = "{\"a\":{\"list\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"}]}}";
        Set<String> result = JsonValueExtractor.extractFirstStringValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
        assertFalse(result.contains("second"));
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testPathNotFound() {
        String json = "{\"a\":{\"aenv\":\"env1\"}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "nonexistent", "aenv");
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTargetKeyNotFound() {
        String json = "{\"a\":{\"other\":\"value\"}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void testEmptyObject() {
        String json = "{\"a\":{}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void testEmptyArray() {
        String json = "{\"a\":[]}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        assertTrue(result.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullJsonString() {
        JsonValueExtractor.extractValuesUnderPath((String) null, "a", "aenv");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyJsonString() {
        JsonValueExtractor.extractValuesUnderPath("", "a", "aenv");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPathKey() {
        JsonValueExtractor.extractValuesUnderPath("{\"a\":{}}", null, "aenv");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullTargetKey() {
        JsonValueExtractor.extractValuesUnderPath("{\"a\":{}}", "a", null);
    }

    // ==================== 实际场景模拟测试 ====================

    @Test
    public void testRealWorldScenario_EnvironmentConfig() {
        String json = "{"
                + "\"development\":{"
                + "  \"env\":\"dev\","
                + "  \"services\":[{"
                + "    \"name\":\"api\","
                + "    \"env\":\"dev-api\""
                + "  },{"
                + "    \"name\":\"web\","
                + "    \"env\":\"dev-web\""
                + "  }]"
                + "},"
                + "\"production\":{"
                + "  \"env\":\"prod\","
                + "  \"services\":[{"
                + "    \"name\":\"api\","
                + "    \"env\":\"prod-api\""
                + "  }]"
                + "}"
                + "}";

        // 提取开发环境的所有env值
        Set<Object> devEnvs = JsonValueExtractor.extractValuesUnderPath(json, "development", "env");
        assertEquals(3, devEnvs.size());
        assertTrue(devEnvs.contains("dev"));
        assertTrue(devEnvs.contains("dev-api"));
        assertTrue(devEnvs.contains("dev-web"));

        // 只取每个数组的第一个
        Set<Object> devEnvsFirst = JsonValueExtractor.extractValuesWithArrayIndex(json, "development", "env", 0);
        assertEquals(2, devEnvsFirst.size());
        assertTrue(devEnvsFirst.contains("dev"));
        assertTrue(devEnvsFirst.contains("dev-api"));
    }

    @Test
    public void testRealWorldScenario_BatchExtractConfig() {
        String json = "{"
                + "\"database\":{"
                + "  \"host\":\"localhost\","
                + "  \"replicas\":[{\"host\":\"replica1\"},{\"host\":\"replica2\"}]"
                + "},"
                + "\"cache\":{"
                + "  \"host\":\"redis-server\","
                + "  \"clusters\":[{\"host\":\"cluster1\"}]"
                + "}"
                + "}";

        // 只取第一个replica
        Set<Object> dbHostsFirst = JsonValueExtractor.extractValuesWithArrayIndex(json, "database", "host", 0);
        assertEquals(2, dbHostsFirst.size());
        assertTrue(dbHostsFirst.contains("localhost"));
        assertTrue(dbHostsFirst.contains("replica1"));
        assertFalse(dbHostsFirst.contains("replica2"));
    }

    @Test
    public void testRealWorldScenario_NestedConfig() {
        // 模拟配置文件中 config 套 config 的情况
        String json = "{"
                + "\"config\":{"
                + "  \"env\":\"global\","
                + "  \"config\":{"
                + "    \"env\":\"override\""
                + "  }"
                + "}"
                + "}";

        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "config", "env");
        
        // 应该只取最内层的 config 下的 env
        assertEquals(1, result.size());
        assertTrue(result.contains("override"));
        assertFalse(result.contains("global"));
    }
}
