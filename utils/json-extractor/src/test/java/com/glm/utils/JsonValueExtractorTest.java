package com.glm.utils;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JsonValueExtractor 单元测试类
 * 
 * 测试覆盖：
 * - pathKey 在任意深度位置
 * - targetKey 在 pathKey 下的任意深度（子、孙、曾孙等）
 * - 嵌套同名路径（a套a）只取最内层
 * - 数组索引控制
 * - 去重并保留顺序
 * 
 * @author GLM
 * @version 1.3.0
 */
public class JsonValueExtractorTest {

    // ==================================================================================
    // 核心功能测试：pathKey 在任意深度
    // ==================================================================================

    @Test
    public void testPathKeyAtRoot() {
        // pathKey 在根级别
        String json = "{\"a\":{\"aenv\":\"value1\"}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("value1"));
    }

    @Test
    public void testPathKeyAtDeepLevel() {
        // pathKey 在深层
        String json = "{\"root\":{\"config\":{\"a\":{\"aenv\":\"deepValue\"}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("deepValue"));
    }

    @Test
    public void testPathKeyAtVeryDeepLevel() {
        // pathKey 在非常深的层级
        String json = "{\"l1\":{\"l2\":{\"l3\":{\"l4\":{\"l5\":{\"a\":{\"aenv\":\"veryDeep\"}}}}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("veryDeep"));
    }

    @Test
    public void testMultiplePathKeysAtDifferentLevels() {
        // 多个 pathKey 在不同层级
        String json = "{\"a\":{\"aenv\":\"root-a\"},\"nested\":{\"a\":{\"aenv\":\"nested-a\"}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("root-a"));
        assertTrue(result.contains("nested-a"));
    }

    @Test
    public void testPathKeyInArray() {
        // pathKey 在数组中的对象里
        String json = "{\"items\":[{\"a\":{\"aenv\":\"arr-value\"}}]}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("arr-value"));
    }

    // ==================================================================================
    // 核心功能测试：targetKey 在任意深度（子、孙、曾孙等）
    // ==================================================================================

    @Test
    public void testTargetKeyAsDirectChild() {
        // targetKey 是 pathKey 的直接子节点
        String json = "{\"a\":{\"aenv\":\"directChild\"}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("directChild"));
    }

    @Test
    public void testTargetKeyAsGrandchild() {
        // targetKey 是 pathKey 的孙子节点
        String json = "{\"a\":{\"level1\":{\"aenv\":\"grandchild\"}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("grandchild"));
    }

    @Test
    public void testTargetKeyAsGreatGrandchild() {
        // targetKey 是 pathKey 的曾孙子节点
        String json = "{\"a\":{\"level1\":{\"level2\":{\"aenv\":\"greatGrandchild\"}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("greatGrandchild"));
    }

    @Test
    public void testTargetKeyAtVeryDeepLevel() {
        // targetKey 在 pathKey 下非常深的层级
        String json = "{\"a\":{\"l1\":{\"l2\":{\"l3\":{\"l4\":{\"l5\":{\"aenv\":\"veryDeepTarget\"}}}}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("veryDeepTarget"));
    }

    @Test
    public void testTargetKeyAtMultipleLevels() {
        // targetKey 在 pathKey 下的多个层级都存在
        String json = "{\"a\":{\"aenv\":\"level0\",\"child\":{\"aenv\":\"level1\",\"grandchild\":{\"aenv\":\"level2\"}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(3, result.size());
        assertTrue(result.contains("level0"));
        assertTrue(result.contains("level1"));
        assertTrue(result.contains("level2"));
    }

    @Test
    public void testTargetKeyInArrayUnderPathKey() {
        // targetKey 在 pathKey 下的数组中
        String json = "{\"a\":{\"items\":[{\"aenv\":\"arr1\"},{\"aenv\":\"arr2\"}]}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("arr1"));
        assertTrue(result.contains("arr2"));
    }

    @Test
    public void testTargetKeyInDeepArrayUnderPathKey() {
        // targetKey 在 pathKey 下深层数组中的对象的深层位置
        String json = "{\"a\":{\"level1\":{\"items\":[{\"deep\":{\"aenv\":\"deepInArray\"}}]}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("deepInArray"));
    }

    // ==================================================================================
    // 核心功能测试：嵌套同名路径（a套a）
    // ==================================================================================

    @Test
    public void testNestedSamePathKey_OnlyInnermost() {
        // a 套 a，只取最内层的 aenv
        String json = "{\"a\":{\"aenv\":\"outer\",\"a\":{\"aenv\":\"inner\"}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        // 只应该有 inner，因为外层 a 包含内层 a
        assertEquals(1, result.size());
        assertTrue(result.contains("inner"));
        assertFalse(result.contains("outer"));
    }

    @Test
    public void testTripleNestedSamePathKey() {
        // a 套 a 套 a，只取最内层
        String json = "{\"a\":{\"aenv\":\"level1\",\"a\":{\"aenv\":\"level2\",\"a\":{\"aenv\":\"level3\"}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("level3"));
    }

    @Test
    public void testNestedPathKeyWithDeepTargetKey() {
        // a 套 a，内层 a 的 aenv 在深层
        String json = "{\"a\":{\"aenv\":\"outer\",\"a\":{\"deep\":{\"deeper\":{\"aenv\":\"innerDeep\"}}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("innerDeep"));
    }

    @Test
    public void testNoNestedPathKey() {
        // 没有嵌套同名路径，正常提取所有
        String json = "{\"a\":{\"aenv\":\"val1\",\"other\":{\"aenv\":\"val2\"}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("val1"));
        assertTrue(result.contains("val2"));
    }

    // ==================================================================================
    // 综合复杂场景测试
    // ==================================================================================

    @Test
    public void testComplexScenario_DeepPathAndTarget() {
        // pathKey 在深层，targetKey 也在 pathKey 下的深层
        String json = "{\"root\":{\"config\":{\"settings\":{\"a\":{\"options\":{\"values\":{\"aenv\":\"deepBoth\"}}}}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("deepBoth"));
    }

    @Test
    public void testComplexScenario_MultiplePathKeysWithNestedTargets() {
        // 多个 pathKey，每个下面有多层 targetKey
        String json = "{"
                + "\"section1\":{\"a\":{\"aenv\":\"s1-direct\",\"child\":{\"aenv\":\"s1-child\"}}},"
                + "\"section2\":{\"deep\":{\"a\":{\"aenv\":\"s2-direct\",\"grandchild\":{\"more\":{\"aenv\":\"s2-deep\"}}}}}"
                + "}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(4, result.size());
        assertTrue(result.contains("s1-direct"));
        assertTrue(result.contains("s1-child"));
        assertTrue(result.contains("s2-direct"));
        assertTrue(result.contains("s2-deep"));
    }

    @Test
    public void testComplexScenario_ArraysEverywhere() {
        // 数组在各个层级
        String json = "{"
                + "\"items\":["
                + "  {\"a\":{\"list\":[{\"aenv\":\"a1\"},{\"aenv\":\"a2\"}]}},"
                + "  {\"nested\":{\"a\":{\"deep\":[{\"aenv\":\"b1\"}]}}}"
                + "]"
                + "}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(3, result.size());
        assertTrue(result.contains("a1"));
        assertTrue(result.contains("a2"));
        assertTrue(result.contains("b1"));
    }

    @Test
    public void testRealWorldScenario_ConfigFile() {
        // 模拟真实配置文件场景
        String json = "{"
                + "\"application\":{"
                + "  \"profiles\":{"
                + "    \"development\":{"
                + "      \"database\":{"
                + "        \"connection\":{"
                + "          \"host\":\"localhost\","
                + "          \"port\":3306"
                + "        }"
                + "      }"
                + "    },"
                + "    \"production\":{"
                + "      \"database\":{"
                + "        \"connection\":{"
                + "          \"host\":\"prod-db.example.com\","
                + "          \"replicas\":[{\"host\":\"replica1\"},{\"host\":\"replica2\"}]"
                + "        }"
                + "      }"
                + "    }"
                + "  }"
                + "}"
                + "}";
        
        // 提取 database 下所有 host
        Set<Object> hosts = JsonValueExtractor.extractAllValues(json, "database", "host");
        
        assertEquals(4, hosts.size());
        assertTrue(hosts.contains("localhost"));
        assertTrue(hosts.contains("prod-db.example.com"));
        assertTrue(hosts.contains("replica1"));
        assertTrue(hosts.contains("replica2"));
    }

    // ==================================================================================
    // 数组索引测试
    // ==================================================================================

    @Test
    public void testArrayIndex_FirstOnly() {
        String json = "{\"a\":{\"items\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"},{\"aenv\":\"third\"}]}}";
        Set<Object> result = JsonValueExtractor.extractAllValuesWithArrayIndex(json, "a", "aenv", 0);
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
    }

    @Test
    public void testArrayIndex_SecondOnly() {
        String json = "{\"a\":{\"items\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"},{\"aenv\":\"third\"}]}}";
        Set<Object> result = JsonValueExtractor.extractAllValuesWithArrayIndex(json, "a", "aenv", 1);
        
        assertEquals(1, result.size());
        assertTrue(result.contains("second"));
    }

    @Test
    public void testArrayIndex_AcrossMultipleArrays() {
        // 多个数组，每个取第一个
        String json = "{\"a\":{\"list1\":[{\"aenv\":\"l1-first\"},{\"aenv\":\"l1-second\"}],"
                    + "\"list2\":[{\"aenv\":\"l2-first\"},{\"aenv\":\"l2-second\"}]}}";
        Set<Object> result = JsonValueExtractor.extractAllValuesWithArrayIndex(json, "a", "aenv", 0);
        
        assertEquals(2, result.size());
        assertTrue(result.contains("l1-first"));
        assertTrue(result.contains("l2-first"));
    }

    @Test
    public void testArrayIndex_OutOfBounds() {
        String json = "{\"a\":{\"items\":[{\"aenv\":\"only\"}]}}";
        Set<Object> result = JsonValueExtractor.extractAllValuesWithArrayIndex(json, "a", "aenv", 5);
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void testExtractFirstValuesFromArrays() {
        String json = "{\"a\":{\"items\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"}]}}";
        Set<Object> result = JsonValueExtractor.extractFirstValuesFromArrays(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
    }

    // ==================================================================================
    // 去重和顺序保留测试
    // ==================================================================================

    @Test
    public void testDeduplication() {
        String json = "{\"a\":{\"aenv\":\"dup\",\"child\":{\"aenv\":\"dup\"},\"other\":{\"aenv\":\"dup\"}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("dup"));
    }

    @Test
    public void testOrderPreservation() {
        String json = "{\"a\":{\"aenv\":\"first\",\"b\":{\"aenv\":\"second\"},\"c\":{\"aenv\":\"third\"}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        List<Object> resultList = new ArrayList<>(result);
        assertEquals(3, resultList.size());
        assertEquals("first", resultList.get(0));
        assertEquals("second", resultList.get(1));
        assertEquals("third", resultList.get(2));
    }

    // ==================================================================================
    // 类型处理测试
    // ==================================================================================

    @Test
    public void testNumericValues() {
        String json = "{\"a\":{\"aenv\":123,\"child\":{\"aenv\":456}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains(123L));
        assertTrue(result.contains(456L));
    }

    @Test
    public void testBooleanValues() {
        String json = "{\"a\":{\"aenv\":true,\"child\":{\"aenv\":false}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains(true));
        assertTrue(result.contains(false));
    }

    @Test
    public void testMixedTypeValues() {
        String json = "{\"a\":{\"aenv\":\"str\",\"b\":{\"aenv\":123},\"c\":{\"aenv\":true}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(3, result.size());
        assertTrue(result.contains("str"));
        assertTrue(result.contains(123L));
        assertTrue(result.contains(true));
    }

    @Test
    public void testArrayValueExpansion() {
        // targetKey 的值本身是数组，应该展开
        String json = "{\"a\":{\"aenv\":[\"v1\",\"v2\",\"v3\"]}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(3, result.size());
        assertTrue(result.contains("v1"));
        assertTrue(result.contains("v2"));
        assertTrue(result.contains("v3"));
    }

    // ==================================================================================
    // 字符串专用方法测试
    // ==================================================================================

    @Test
    public void testExtractStringValues() {
        String json = "{\"a\":{\"aenv\":\"str1\",\"b\":{\"aenv\":123},\"c\":{\"aenv\":\"str2\"}}}";
        Set<String> result = JsonValueExtractor.extractStringValues(json, "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("str1"));
        assertTrue(result.contains("str2"));
    }

    @Test
    public void testExtractStringValuesAsList() {
        String json = "{\"a\":{\"aenv\":\"str1\",\"b\":{\"aenv\":\"str2\"}}}";
        List<String> result = JsonValueExtractor.extractStringValuesAsList(json, "a", "aenv");
        
        assertEquals(2, result.size());
    }

    @Test
    public void testExtractFirstStringValues() {
        String json = "{\"a\":{\"items\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"}]}}";
        Set<String> result = JsonValueExtractor.extractFirstStringValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
    }

    // ==================================================================================
    // 批量提取测试
    // ==================================================================================

    @Test
    public void testBatchExtract() {
        String json = "{\"a\":{\"aenv\":\"a-val\"},\"b\":{\"benv\":\"b-val\"}}";
        List<String[]> mappings = Arrays.asList(
                new String[]{"a", "aenv"},
                new String[]{"b", "benv"}
        );
        
        Map<String, Set<Object>> result = JsonValueExtractor.batchExtract(json, mappings);
        
        assertEquals(2, result.size());
        assertTrue(result.get("aenv").contains("a-val"));
        assertTrue(result.get("benv").contains("b-val"));
    }

    @Test
    public void testBatchExtractWithDeepPaths() {
        String json = "{\"root\":{\"a\":{\"deep\":{\"aenv\":\"a-deep\"}}},\"other\":{\"b\":{\"nested\":{\"benv\":\"b-deep\"}}}}";
        List<String[]> mappings = Arrays.asList(
                new String[]{"a", "aenv"},
                new String[]{"b", "benv"}
        );
        
        Map<String, Set<Object>> result = JsonValueExtractor.batchExtract(json, mappings);
        
        assertEquals(2, result.size());
        assertTrue(result.get("aenv").contains("a-deep"));
        assertTrue(result.get("benv").contains("b-deep"));
    }

    @Test
    public void testBatchExtractAsList() {
        String json = "{\"a\":{\"aenv\":\"v1\",\"child\":{\"aenv\":\"v2\"}}}";
        List<String[]> mappings = Collections.singletonList(new String[]{"a", "aenv"});
        
        Map<String, List<Object>> result = JsonValueExtractor.batchExtractAsList(json, mappings);
        
        assertEquals(1, result.size());
        assertEquals(2, result.get("aenv").size());
    }

    // ==================================================================================
    // 边界条件测试
    // ==================================================================================

    @Test
    public void testPathKeyNotFound() {
        String json = "{\"other\":{\"key\":\"value\"}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTargetKeyNotFound() {
        String json = "{\"a\":{\"other\":\"value\"}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void testEmptyObject() {
        String json = "{\"a\":{}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void testEmptyArray() {
        String json = "{\"a\":[]}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertTrue(result.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullJsonString() {
        JsonValueExtractor.extractAllValues(null, "a", "aenv");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyJsonString() {
        JsonValueExtractor.extractAllValues("", "a", "aenv");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPathKey() {
        JsonValueExtractor.extractAllValues("{}", null, "aenv");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullTargetKey() {
        JsonValueExtractor.extractAllValues("{}", "a", null);
    }

    // ==================================================================================
    // 兼容性方法测试
    // ==================================================================================

    @Test
    public void testExtractValuesUnderPath_Compatibility() {
        String json = "{\"root\":{\"a\":{\"deep\":{\"aenv\":\"value\"}}}}";
        Set<Object> result = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("value"));
    }

    @Test
    public void testExtractValuesWithArrayIndex_Compatibility() {
        String json = "{\"a\":{\"items\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"}]}}";
        Set<Object> result = JsonValueExtractor.extractValuesWithArrayIndex(json, "a", "aenv", 0);
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
    }

    @Test
    public void testExtractAllFirstValues() {
        String json = "{\"a\":{\"items\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"}]}}";
        Set<Object> result = JsonValueExtractor.extractAllFirstValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
    }
}
