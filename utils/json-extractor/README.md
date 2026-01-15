# JSON Value Extractor 工具类

## 概述

`JsonValueExtractor` 是一个基于 JDK 8 的 Java 工具类，用于从嵌套的 JSON 结构中递归提取指定路径下特定键的所有值并去重。

### 核心特性

- ✅ **pathKey 任意深度**：pathKey 可以在 JSON 的任意深度位置，不限于根节点
- ✅ **targetKey 任意深度**：targetKey 可以在 pathKey 下的任意深度（子、孙、曾孙等）
- ✅ **嵌套同名路径**：a 套 a 时只取最内层子集的值
- ✅ **去重保序**：使用 LinkedHashSet，自动去重并保留插入顺序
- ✅ **数组索引**：支持指定只取数组中的第n个元素
- ✅ **类型保持**：保持原始数据类型（String/Long/Double/Boolean）

## 环境要求

- JDK 1.8+
- Maven 3.x
- Gson 2.10.1

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

### 2. 使用示例

```java
import com.glm.utils.JsonValueExtractor;

// 示例 JSON：pathKey "a" 在深层，targetKey "aenv" 在 a 的孙子节点
String json = "{\"root\":{\"config\":{\"a\":{\"level1\":{\"level2\":{\"aenv\":\"value\"}}}}}}";

// 提取所有值
Set<Object> values = JsonValueExtractor.extractAllValues(json, "a", "aenv");
// 结果: [value]
```

## API 文档

### 主要方法（推荐使用）

#### `extractAllValues(json, pathKey, targetKey)`

从 JSON 中提取指定路径下目标键的所有值。

**工作原理：**
1. 在整个 JSON 中递归搜索所有名为 `pathKey` 的节点（任意深度）
2. 对于每个 `pathKey`：如果内部还有同名 `pathKey`，递归到最内层
3. 在最内层 `pathKey` 的整个子树中搜索所有 `targetKey`（任意深度）
4. 收集所有值，去重并保留顺序

```java
// pathKey "a" 在深层
String json = "{\"root\":{\"config\":{\"a\":{\"deep\":{\"aenv\":\"value\"}}}}}";
Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
// 结果: [value]
```

---

### 嵌套同名路径（a 套 a）

当存在 `pathKey` 嵌套 `pathKey` 的情况时，只取最内层的值：

```java
String json = "{\"a\":{\"aenv\":\"outer\",\"a\":{\"aenv\":\"inner\"}}}";
Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
// 结果: [inner]  （outer 不计入，因为外层 a 包含内层 a）
```

**多层嵌套：**
```java
String json = "{\"a\":{\"aenv\":\"lv1\",\"a\":{\"aenv\":\"lv2\",\"a\":{\"aenv\":\"lv3\"}}}}";
Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
// 结果: [lv3]  （只取最内层）
```

---

### 数组索引

#### `extractAllValuesWithArrayIndex(json, pathKey, targetKey, arrayIndex)`

支持指定只取数组中的第n个元素：

```java
String json = "{\"a\":{\"items\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"},{\"aenv\":\"third\"}]}}";

// 只取每个数组的第一个
Set<Object> first = JsonValueExtractor.extractAllValuesWithArrayIndex(json, "a", "aenv", 0);
// 结果: [first]

// 只取每个数组的第二个
Set<Object> second = JsonValueExtractor.extractAllValuesWithArrayIndex(json, "a", "aenv", 1);
// 结果: [second]
```

**跨数组独立处理：**
```java
String json = "{\"a\":{\"list1\":[{\"aenv\":\"a1\"},{\"aenv\":\"a2\"}],"
            + "\"list2\":[{\"aenv\":\"b1\"},{\"aenv\":\"b2\"}]}}";
Set<Object> result = JsonValueExtractor.extractAllValuesWithArrayIndex(json, "a", "aenv", 0);
// 结果: [a1, b1]  （两个数组各取第一个）
```

---

### 批量提取

```java
List<String[]> mappings = Arrays.asList(
    new String[]{"a", "aenv"},
    new String[]{"b", "benv"}
);
Map<String, Set<Object>> result = JsonValueExtractor.batchExtract(json, mappings);
// result.get("aenv") -> a 下所有 aenv 的值
// result.get("benv") -> b 下所有 benv 的值
```

---

### 字符串专用方法

```java
// 只提取字符串类型的值
Set<String> strings = JsonValueExtractor.extractStringValues(json, "a", "aenv");

// 返回 List 形式
List<String> list = JsonValueExtractor.extractStringValuesAsList(json, "a", "aenv");
```

---

## 使用场景

### 场景1：深层配置提取

```java
String json = "{"
    + "\"application\":{"
    + "  \"profiles\":{"
    + "    \"database\":{"
    + "      \"connection\":{"
    + "        \"host\":\"localhost\","
    + "        \"replicas\":[{\"host\":\"replica1\"},{\"host\":\"replica2\"}]"
    + "      }"
    + "    }"
    + "  }"
    + "}"
    + "}";

// 提取 database 下所有 host（包括深层的 replicas）
Set<Object> hosts = JsonValueExtractor.extractAllValues(json, "database", "host");
// 结果: [localhost, replica1, replica2]
```

### 场景2：多个同名节点

```java
String json = "{"
    + "\"section1\":{\"a\":{\"aenv\":\"s1\"}},"
    + "\"section2\":{\"nested\":{\"a\":{\"aenv\":\"s2\"}}}"
    + "}";

Set<Object> values = JsonValueExtractor.extractAllValues(json, "a", "aenv");
// 结果: [s1, s2]  （两个 a 节点的值都提取）
```

---

## 构建与测试

```bash
cd utils/json-extractor

# 编译
mvn clean compile

# 运行测试
mvn test

# 打包
mvn clean package
```

---

## 版本历史

### v1.3.0 (2026-01-15)
- 🐛 **重要修复**：pathKey 现在支持在 JSON 的任意深度位置
- 🐛 **重要修复**：targetKey 现在支持在 pathKey 下的任意深度（子、孙、曾孙等）
- ✨ 重构核心算法，分离搜索 pathKey 和搜索 targetKey 的逻辑
- 📝 增加 48 个测试用例覆盖各种场景

### v1.2.0 (2026-01-15)
- 🔧 重构代码，将嵌套层数控制在4层以内
- 🐛 修复三元运算符导致的数字类型错误

### v1.1.0 (2026-01-15)
- 🆕 新增数组索引支持
- 🆕 新增嵌套同名路径处理

### v1.0.0 (2026-01-15)
- 初始版本

---

## 版本信息

- **版本:** 1.3.0
- **作者:** GLM
- **JDK:** 1.8+
- **License:** MIT
