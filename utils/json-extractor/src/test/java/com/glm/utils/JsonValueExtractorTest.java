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
 * - 路径链支持（a -> a1 -> aenv）
 * - 字符串JSON字段解析
 * - 数组索引控制
 * - 去重并保留顺序
 * 
 * @author GLM
 * @version 1.4.1
 */
public class JsonValueExtractorTest {

    // ==================================================================================
    // 核心功能测试：pathKey 在任意深度
    // ==================================================================================

    @Test
    public void testPathKeyAtRoot() {
        String json = "{\"a\":{\"aenv\":\"value1\"}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("value1"));
    }

    @Test
    public void testPathKeyAtDeepLevel() {
        String json = "{\"root\":{\"config\":{\"a\":{\"aenv\":\"deepValue\"}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("deepValue"));
    }

    @Test
    public void testPathKeyAtVeryDeepLevel() {
        String json = "{\"l1\":{\"l2\":{\"l3\":{\"l4\":{\"l5\":{\"a\":{\"aenv\":\"veryDeep\"}}}}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("veryDeep"));
    }

    @Test
    public void testMultiplePathKeysAtDifferentLevels() {
        String json = "{\"a\":{\"aenv\":\"root-a\"},\"nested\":{\"a\":{\"aenv\":\"nested-a\"}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("root-a"));
        assertTrue(result.contains("nested-a"));
    }

    @Test
    public void testPathKeyInArray() {
        String json = "{\"items\":[{\"a\":{\"aenv\":\"arr-value\"}}]}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("arr-value"));
    }

    // ==================================================================================
    // 核心功能测试：targetKey 在任意深度
    // ==================================================================================

    @Test
    public void testTargetKeyAsDirectChild() {
        String json = "{\"a\":{\"aenv\":\"directChild\"}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("directChild"));
    }

    @Test
    public void testTargetKeyAsGrandchild() {
        String json = "{\"a\":{\"level1\":{\"aenv\":\"grandchild\"}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("grandchild"));
    }

    @Test
    public void testTargetKeyAsGreatGrandchild() {
        String json = "{\"a\":{\"level1\":{\"level2\":{\"aenv\":\"greatGrandchild\"}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("greatGrandchild"));
    }

    @Test
    public void testTargetKeyAtVeryDeepLevel() {
        String json = "{\"a\":{\"l1\":{\"l2\":{\"l3\":{\"l4\":{\"l5\":{\"aenv\":\"veryDeepTarget\"}}}}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("veryDeepTarget"));
    }

    @Test
    public void testTargetKeyAtMultipleLevels() {
        String json = "{\"a\":{\"aenv\":\"level0\",\"child\":{\"aenv\":\"level1\",\"grandchild\":{\"aenv\":\"level2\"}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(3, result.size());
        assertTrue(result.contains("level0"));
        assertTrue(result.contains("level1"));
        assertTrue(result.contains("level2"));
    }

    @Test
    public void testTargetKeyInArrayUnderPathKey() {
        String json = "{\"a\":{\"items\":[{\"aenv\":\"arr1\"},{\"aenv\":\"arr2\"}]}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("arr1"));
        assertTrue(result.contains("arr2"));
    }

    @Test
    public void testTargetKeyInDeepArrayUnderPathKey() {
        String json = "{\"a\":{\"level1\":{\"items\":[{\"deep\":{\"aenv\":\"deepInArray\"}}]}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("deepInArray"));
    }

    // ==================================================================================
    // 嵌套同名路径（a套a）测试
    // ==================================================================================

    @Test
    public void testNestedSamePathKey_OnlyInnermost() {
        String json = "{\"a\":{\"aenv\":\"outer\",\"a\":{\"aenv\":\"inner\"}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("inner"));
        assertFalse(result.contains("outer"));
    }

    @Test
    public void testTripleNestedSamePathKey() {
        String json = "{\"a\":{\"aenv\":\"level1\",\"a\":{\"aenv\":\"level2\",\"a\":{\"aenv\":\"level3\"}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("level3"));
    }

    @Test
    public void testNestedPathKeyWithDeepTargetKey() {
        String json = "{\"a\":{\"aenv\":\"outer\",\"a\":{\"deep\":{\"deeper\":{\"aenv\":\"innerDeep\"}}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("innerDeep"));
    }

    @Test
    public void testNoNestedPathKey() {
        String json = "{\"a\":{\"aenv\":\"val1\",\"other\":{\"aenv\":\"val2\"}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("val1"));
        assertTrue(result.contains("val2"));
    }

    // ==================================================================================
    // 【新增】路径链测试
    // ==================================================================================

    @Test
    public void testPathChain_Simple() {
        // 简单路径链：a -> a1 -> aenv
        String json = "{\"a\":{\"a1\":{\"aenv\":\"value\"}}}";
        Set<Object> result = JsonValueExtractor.extractWithPathChain(json, Arrays.asList("a", "a1"), "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("value"));
    }

    @Test
    public void testPathChain_IgnoreOtherBranches() {
        // 路径链只取 a -> a1 下的 aenv，忽略 a 下直接的 aenv
        String json = "{\"a\":{\"aenv\":\"ignored\",\"a1\":{\"aenv\":\"found\"}}}";
        Set<Object> result = JsonValueExtractor.extractWithPathChain(json, Arrays.asList("a", "a1"), "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("found"));
        assertFalse(result.contains("ignored"));
    }

    @Test
    public void testPathChain_DeepPath() {
        // 深层路径链：a -> b -> c -> target
        String json = "{\"root\":{\"a\":{\"b\":{\"c\":{\"target\":\"deepValue\"}}}}}";
        Set<Object> result = JsonValueExtractor.extractWithPathChain(json, Arrays.asList("a", "b", "c"), "target");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("deepValue"));
    }

    @Test
    public void testPathChain_TargetInDeepSubtree() {
        // 路径链指定后，在最终节点的深层子树中搜索 target
        String json = "{\"a\":{\"a1\":{\"deep\":{\"deeper\":{\"aenv\":\"veryDeep\"}}}}}";
        Set<Object> result = JsonValueExtractor.extractWithPathChain(json, Arrays.asList("a", "a1"), "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("veryDeep"));
    }

    @Test
    public void testPathChain_MultiplePathsFound() {
        // 多个位置都匹配路径链
        String json = "{\"section1\":{\"a\":{\"a1\":{\"aenv\":\"v1\"}}},\"section2\":{\"a\":{\"a1\":{\"aenv\":\"v2\"}}}}";
        Set<Object> result = JsonValueExtractor.extractWithPathChain(json, Arrays.asList("a", "a1"), "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("v1"));
        assertTrue(result.contains("v2"));
    }

    @Test
    public void testPathChain_WithArrayIndex() {
        // 路径链配合数组索引
        String json = "{\"a\":{\"a1\":{\"items\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"}]}}}";
        Set<Object> result = JsonValueExtractor.extractWithPathChainAndArrayIndex(json, Arrays.asList("a", "a1"), "aenv", 0);
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
    }

    @Test
    public void testPathChain_PathNotFound() {
        // 路径链中某个节点不存在
        String json = "{\"a\":{\"other\":{\"aenv\":\"value\"}}}";
        Set<Object> result = JsonValueExtractor.extractWithPathChain(json, Arrays.asList("a", "a1"), "aenv");
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void testPathChain_SinglePath() {
        // 单个路径（等同于普通提取）
        String json = "{\"a\":{\"aenv\":\"value\"}}";
        Set<Object> result = JsonValueExtractor.extractWithPathChain(json, Arrays.asList("a"), "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("value"));
    }

    @Test
    public void testPathChain_InArray() {
        // 路径链中的节点在数组中
        String json = "{\"items\":[{\"a\":{\"a1\":{\"aenv\":\"arrValue\"}}}]}";
        Set<Object> result = JsonValueExtractor.extractWithPathChain(json, Arrays.asList("a", "a1"), "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("arrValue"));
    }

    @Test
    public void testExtractFirstWithPathChain() {
        // 路径链取第一个元素
        String json = "{\"a\":{\"a1\":{\"items\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"}]}}}";
        Set<Object> result = JsonValueExtractor.extractFirstWithPathChain(json, Arrays.asList("a", "a1"), "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
    }

    @Test
    public void testExtractStringWithPathChain() {
        // 路径链提取字符串值
        String json = "{\"a\":{\"a1\":{\"aenv\":\"strValue\",\"child\":{\"aenv\":123}}}}";
        Set<String> result = JsonValueExtractor.extractStringWithPathChain(json, Arrays.asList("a", "a1"), "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("strValue"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPathChain_NullPathChain() {
        JsonValueExtractor.extractWithPathChain("{}", null, "aenv");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPathChain_EmptyPathChain() {
        JsonValueExtractor.extractWithPathChain("{}", Collections.emptyList(), "aenv");
    }

    // ==================================================================================
    // 【新增】字符串JSON字段解析测试
    // ==================================================================================

    @Test
    public void testStringField_Simple() {
        // 简单的字符串JSON字段
        String json = "{\"sa\":\"{\\\"a\\\":{\\\"aenv\\\":\\\"innerValue\\\"}}\"}";
        Set<Object> result = JsonValueExtractor.extractFromStringField(json, "sa", "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("innerValue"));
    }

    @Test
    public void testStringField_DeepInner() {
        // 字符串JSON中的目标在深层
        String json = "{\"sa\":\"{\\\"a\\\":{\\\"deep\\\":{\\\"aenv\\\":\\\"deepInner\\\"}}}\"}";
        Set<Object> result = JsonValueExtractor.extractFromStringField(json, "sa", "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("deepInner"));
    }

    @Test
    public void testStringField_MultipleFields() {
        // 多个字符串JSON字段
        String json = "{\"item1\":{\"sa\":\"{\\\"a\\\":{\\\"aenv\\\":\\\"v1\\\"}}\"}"
                    + ",\"item2\":{\"sa\":\"{\\\"a\\\":{\\\"aenv\\\":\\\"v2\\\"}}\"}}";
        Set<Object> result = JsonValueExtractor.extractFromStringField(json, "sa", "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("v1"));
        assertTrue(result.contains("v2"));
    }

    @Test
    public void testStringField_InArray() {
        // 字符串JSON字段在数组中
        String json = "{\"items\":[{\"sa\":\"{\\\"a\\\":{\\\"aenv\\\":\\\"arr1\\\"}}\"}"
                    + ",{\"sa\":\"{\\\"a\\\":{\\\"aenv\\\":\\\"arr2\\\"}}\"}]}";
        Set<Object> result = JsonValueExtractor.extractFromStringField(json, "sa", "a", "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("arr1"));
        assertTrue(result.contains("arr2"));
    }

    @Test
    public void testStringField_WithArrayIndex() {
        // 字符串JSON配合数组索引
        String json = "{\"sa\":\"{\\\"a\\\":{\\\"items\\\":[{\\\"aenv\\\":\\\"first\\\"},{\\\"aenv\\\":\\\"second\\\"}]}}\"}";
        Set<Object> result = JsonValueExtractor.extractFromStringFieldWithArrayIndex(json, "sa", "a", "aenv", 0);
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
    }

    @Test
    public void testStringField_InvalidJson() {
        // 字段值不是有效JSON，应该返回空
        String json = "{\"sa\":\"not a valid json\"}";
        Set<Object> result = JsonValueExtractor.extractFromStringField(json, "sa", "a", "aenv");
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void testStringField_NotString() {
        // 字段值不是字符串，应该忽略
        String json = "{\"sa\":{\"a\":{\"aenv\":\"objectValue\"}}}";
        Set<Object> result = JsonValueExtractor.extractFromStringField(json, "sa", "a", "aenv");
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void testStringField_FieldNotFound() {
        // 字段不存在
        String json = "{\"other\":\"{\\\"a\\\":{\\\"aenv\\\":\\\"value\\\"}}\"}";
        Set<Object> result = JsonValueExtractor.extractFromStringField(json, "sa", "a", "aenv");
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void testExtractFirstFromStringField() {
        // 字符串JSON取第一个元素
        String json = "{\"sa\":\"{\\\"a\\\":{\\\"items\\\":[{\\\"aenv\\\":\\\"first\\\"},{\\\"aenv\\\":\\\"second\\\"}]}}\"}";
        Set<Object> result = JsonValueExtractor.extractFirstFromStringField(json, "sa", "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
    }

    @Test
    public void testExtractStringFromStringField() {
        // 字符串JSON只提取字符串值
        String json = "{\"sa\":\"{\\\"a\\\":{\\\"aenv\\\":\\\"str\\\",\\\"child\\\":{\\\"aenv\\\":123}}}\"}";
        Set<String> result = JsonValueExtractor.extractStringFromStringField(json, "sa", "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("str"));
    }

    // ==================================================================================
    // 【新增】字符串JSON字段便捷方法测试
    // ==================================================================================

    @Test
    public void testExtractFirstStringFromStringField() {
        // 字符串JSON取第一个字符串值
        String json = "{\"sa\":\"{\\\"a\\\":{\\\"items\\\":[{\\\"aenv\\\":\\\"first\\\"},{\\\"aenv\\\":\\\"second\\\"}]}}\"}";
        Set<String> result = JsonValueExtractor.extractFirstStringFromStringField(json, "sa", "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
    }

    @Test
    public void testExtractFirstFromStringFieldWithPathChain() {
        // 字符串JSON + 路径链 + 取第一个
        String json = "{\"sa\":\"{\\\"a\\\":{\\\"a1\\\":{\\\"items\\\":[{\\\"aenv\\\":\\\"first\\\"},{\\\"aenv\\\":\\\"second\\\"}]}}}\"}";
        Set<Object> result = JsonValueExtractor.extractFirstFromStringFieldWithPathChain(
            json, "sa", Arrays.asList("a", "a1"), "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
    }

    @Test
    public void testExtractStringFromStringFieldWithPathChain() {
        // 字符串JSON + 路径链 + 只取字符串
        String json = "{\"sa\":\"{\\\"a\\\":{\\\"a1\\\":{\\\"aenv\\\":\\\"strVal\\\",\\\"child\\\":{\\\"aenv\\\":123}}}}\"}";
        Set<String> result = JsonValueExtractor.extractStringFromStringFieldWithPathChain(
            json, "sa", Arrays.asList("a", "a1"), "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("strVal"));
    }

    @Test
    public void testExtractFirstStringFromStringFieldWithPathChain() {
        // 字符串JSON + 路径链 + 只取字符串 + 取第一个
        String json = "{\"sa\":\"{\\\"a\\\":{\\\"a1\\\":{\\\"items\\\":[{\\\"aenv\\\":\\\"first\\\"},{\\\"aenv\\\":\\\"second\\\"}]}}}\"}";
        Set<String> result = JsonValueExtractor.extractFirstStringFromStringFieldWithPathChain(
            json, "sa", Arrays.asList("a", "a1"), "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
    }

    // ==================================================================================
    // 【新增】字符串JSON字段 + 路径链组合测试
    // ==================================================================================

    @Test
    public void testStringFieldWithPathChain_Simple() {
        // 字符串JSON配合路径链
        String json = "{\"sa\":\"{\\\"a\\\":{\\\"a1\\\":{\\\"aenv\\\":\\\"pathChainValue\\\"}}}\"}";
        Set<Object> result = JsonValueExtractor.extractFromStringFieldWithPathChain(
            json, "sa", Arrays.asList("a", "a1"), "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("pathChainValue"));
    }

    @Test
    public void testStringFieldWithPathChain_IgnoreOther() {
        // 字符串JSON配合路径链，忽略其他分支
        String json = "{\"sa\":\"{\\\"a\\\":{\\\"aenv\\\":\\\"ignored\\\",\\\"a1\\\":{\\\"aenv\\\":\\\"found\\\"}}}\"}";
        Set<Object> result = JsonValueExtractor.extractFromStringFieldWithPathChain(
            json, "sa", Arrays.asList("a", "a1"), "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("found"));
        assertFalse(result.contains("ignored"));
    }

    @Test
    public void testStringFieldWithPathChain_DeepTarget() {
        // 字符串JSON + 路径链 + 深层目标
        String json = "{\"sa\":\"{\\\"a\\\":{\\\"a1\\\":{\\\"deep\\\":{\\\"aenv\\\":\\\"veryDeep\\\"}}}}\"}";
        Set<Object> result = JsonValueExtractor.extractFromStringFieldWithPathChain(
            json, "sa", Arrays.asList("a", "a1"), "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("veryDeep"));
    }

    @Test
    public void testStringFieldWithPathChain_WithArrayIndex() {
        // 字符串JSON + 路径链 + 数组索引
        String json = "{\"sa\":\"{\\\"a\\\":{\\\"a1\\\":{\\\"items\\\":[{\\\"aenv\\\":\\\"first\\\"},{\\\"aenv\\\":\\\"second\\\"}]}}}\"}";
        Set<Object> result = JsonValueExtractor.extractFromStringFieldWithPathChainAndArrayIndex(
            json, "sa", Arrays.asList("a", "a1"), "aenv", 0);
        
        assertEquals(1, result.size());
        assertTrue(result.contains("first"));
    }

    @Test
    public void testStringFieldWithPathChain_MultipleFields() {
        // 多个字符串JSON字段 + 路径链
        String json = "{\"item1\":{\"sa\":\"{\\\"a\\\":{\\\"a1\\\":{\\\"aenv\\\":\\\"v1\\\"}}}\"},"
                    + "\"item2\":{\"sa\":\"{\\\"a\\\":{\\\"a1\\\":{\\\"aenv\\\":\\\"v2\\\"}}}\"}}";
        Set<Object> result = JsonValueExtractor.extractFromStringFieldWithPathChain(
            json, "sa", Arrays.asList("a", "a1"), "aenv");
        
        assertEquals(2, result.size());
        assertTrue(result.contains("v1"));
        assertTrue(result.contains("v2"));
    }

    // ==================================================================================
    // 综合复杂场景测试
    // ==================================================================================

    @Test
    public void testComplexScenario_DeepPathAndTarget() {
        String json = "{\"root\":{\"config\":{\"settings\":{\"a\":{\"options\":{\"values\":{\"aenv\":\"deepBoth\"}}}}}}}";
        Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
        
        assertEquals(1, result.size());
        assertTrue(result.contains("deepBoth"));
    }

    @Test
    public void testComplexScenario_MultiplePathKeysWithNestedTargets() {
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
