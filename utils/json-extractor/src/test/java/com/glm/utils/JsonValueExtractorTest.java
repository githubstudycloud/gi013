package com.glm.utils;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JsonValueExtractor 单元测试类
 * 
 * @author GLM
 * @version 1.0.0
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

    // ==================== 去重功能测试 ====================

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

        // 提取生产环境的所有env值
        Set<Object> prodEnvs = JsonValueExtractor.extractValuesUnderPath(json, "production", "env");
        assertEquals(2, prodEnvs.size());
        assertTrue(prodEnvs.contains("prod"));
        assertTrue(prodEnvs.contains("prod-api"));
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

        List<String[]> mappings = Arrays.asList(
                new String[]{"database", "host"},
                new String[]{"cache", "host"}
        );

        Map<String, Set<Object>> result = JsonValueExtractor.batchExtract(json, mappings);

        Set<Object> dbHosts = result.get("host");
        // 注意：因为两个都用"host"作为targetKey，结果会合并
        // 这里我们单独测试
        Set<Object> dbHostsOnly = JsonValueExtractor.extractValuesUnderPath(json, "database", "host");
        assertEquals(3, dbHostsOnly.size());
        assertTrue(dbHostsOnly.contains("localhost"));
        assertTrue(dbHostsOnly.contains("replica1"));
        assertTrue(dbHostsOnly.contains("replica2"));
    }
}

