# JSON Value Extractor 工具类

## 概述

`JsonValueExtractor` 是一个基于 JDK 8 的 Java 工具类，用于从嵌套的 JSON 结构中递归提取指定路径下特定键的所有值并去重。

### 核心特性

- ✅ **递归遍历** - 支持任意深度的嵌套结构（dict/list/混合嵌套）
- ✅ **路径限定** - 先定位到指定路径节点，再在其子树中搜索
- ✅ **自动去重** - 使用 `LinkedHashSet` 存储结果，自动去除重复值
- ✅ **保留顺序** - 去重时保留插入顺序
- ✅ **数组索引** - 支持指定只取数组中的第n个元素
- ✅ **嵌套同名路径** - 支持处理 a 套 a 的情况，只取最内层子集
- ✅ **类型保持** - 保持原始数据类型（String/Long/Double/Boolean）
- ✅ **批量提取** - 支持一次提取多组路径-键值对

## 环境要求

- JDK 1.8+
- Maven 3.x
- Gson 2.10.1

## 快速开始

### 1. 添加依赖

将以下依赖添加到你的 `pom.xml`：

```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

### 2. 复制工具类

将 `JsonValueExtractor.java` 复制到你的项目中。

### 3. 使用示例

```java
import com.glm.utils.JsonValueExtractor;
import java.util.*;

public class Example {
    public static void main(String[] args) {
        String json = "{"
            + "\"a\":{"
            + "  \"aenv\":\"env1\","
            + "  \"nested\":{\"aenv\":\"env2\"},"
            + "  \"list\":[{\"aenv\":\"env3\"},{\"aenv\":\"env1\"}]"
            + "},"
            + "\"b\":{"
            + "  \"benv\":\"benv1\","
            + "  \"config\":[{\"benv\":\"benv2\"}]"
            + "}"
            + "}";

        // 提取 a 下所有 aenv 的值（去重，保留顺序）
        Set<Object> aenvValues = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        System.out.println("aenv values: " + aenvValues);
        // 输出: aenv values: [env1, env2, env3] （保留顺序）
    }
}
```

## API 文档

### 基础方法

#### `extractValuesUnderPath(String jsonString, String pathKey, String targetKey)`

从 JSON 字符串的指定路径下提取目标键的所有值（去重，保留顺序）。

```java
Set<Object> values = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
```

---

### 数组索引方法 🆕

#### `extractValuesWithArrayIndex(String jsonString, String pathKey, String targetKey, int arrayIndex)`

从 JSON 字符串的指定路径下提取目标键的值，支持指定数组索引。

**参数：**
- `arrayIndex` - 数组索引（0-based），-1 表示遍历所有元素

```java
// 只取每个数组的第一个元素
Set<Object> first = JsonValueExtractor.extractValuesWithArrayIndex(json, "a", "aenv", 0);

// 只取每个数组的第二个元素
Set<Object> second = JsonValueExtractor.extractValuesWithArrayIndex(json, "a", "aenv", 1);
```

**示例场景：**

```java
String json = "{\"a\":{\"list\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"},{\"aenv\":\"third\"}]}}";

// 取所有（默认行为）
Set<Object> all = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
// 结果: [first, second, third]

// 只取第一个
Set<Object> first = JsonValueExtractor.extractValuesWithArrayIndex(json, "a", "aenv", 0);
// 结果: [first]
```

#### `extractFirstValuesFromArrays(String jsonString, String pathKey, String targetKey)`

便捷方法，等价于 `extractValuesWithArrayIndex(json, pathKey, targetKey, 0)`

```java
Set<Object> first = JsonValueExtractor.extractFirstValuesFromArrays(json, "a", "aenv");
```

---

### 嵌套同名路径处理 🆕

当存在 **a 套 a** 的情况时，工具类只处理最内层的子集。

```java
String json = "{\"a\":{\"aenv\":\"parent\",\"a\":{\"aenv\":\"child\"}}}";

Set<Object> values = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
// 结果: [child]  （只取最内层的 a 下的值，parent 不计入）
```

**多层嵌套：**

```java
String json = "{\"a\":{\"aenv\":\"level1\",\"a\":{\"aenv\":\"level2\",\"a\":{\"aenv\":\"level3\"}}}}";

Set<Object> values = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
// 结果: [level3]  （只取最内层）
```

---

### 递归搜索方法

#### `extractAllValues(String jsonString, String pathKey, String targetKey)`

递归搜索 pathKey，然后在其下提取目标键的值。适用于 pathKey 本身也嵌套在任意深度的情况。

```java
String json = "{\"root\":{\"config\":{\"a\":{\"aenv\":\"env1\"}}}}";
Set<Object> values = JsonValueExtractor.extractAllValues(json, "a", "aenv");
// 结果: [env1]
```

#### `extractAllValuesWithArrayIndex(String jsonString, String pathKey, String targetKey, int arrayIndex)`

递归搜索并支持数组索引。

#### `extractAllFirstValues(String jsonString, String pathKey, String targetKey)`

递归搜索，只取每个数组的第一个元素。

---

### 批量提取方法

#### `batchExtract(String jsonString, List<String[]> mappings)`

批量提取多组 pathKey -> targetKey 的值。

```java
List<String[]> mappings = Arrays.asList(
    new String[]{"a", "aenv"},
    new String[]{"b", "benv"}
);
Map<String, Set<Object>> result = JsonValueExtractor.batchExtract(json, mappings);
```

#### `batchExtractWithArrayIndex(String jsonString, List<String[]> mappings, int arrayIndex)`

批量提取，支持数组索引。

---

### 字符串专用方法

| 方法 | 说明 |
|------|------|
| `extractStringValues` | 仅提取字符串类型的值 |
| `extractStringValuesAsList` | 提取字符串值并返回 List |
| `extractFirstStringValues` | 只取每个数组第一个的字符串值 |

---

## 使用场景

### 场景1：提取环境配置（只取第一个）

```java
String json = "{"
    + "\"development\":{"
    + "  \"env\":\"dev\","
    + "  \"services\":[{\"env\":\"dev-api\"},{\"env\":\"dev-web\"}]"
    + "}"
    + "}";

// 取所有
Set<Object> all = JsonValueExtractor.extractValuesUnderPath(json, "development", "env");
// 结果: [dev, dev-api, dev-web]

// 只取每个数组的第一个
Set<Object> first = JsonValueExtractor.extractValuesWithArrayIndex(json, "development", "env", 0);
// 结果: [dev, dev-api]
```

### 场景2：处理嵌套配置覆盖

```java
String json = "{"
    + "\"config\":{"
    + "  \"env\":\"global\","
    + "  \"config\":{"
    + "    \"env\":\"override\""
    + "  }"
    + "}"
    + "}";

Set<Object> values = JsonValueExtractor.extractValuesUnderPath(json, "config", "env");
// 结果: [override]  （内层 config 覆盖外层）
```

### 场景3：跨数组独立处理

```java
String json = "{\"a\":{\"list1\":[{\"aenv\":\"a1\"},{\"aenv\":\"a2\"}],\"list2\":[{\"aenv\":\"b1\"},{\"aenv\":\"b2\"}]}}";

// 每个数组都取第一个（跨数组独立）
Set<Object> result = JsonValueExtractor.extractValuesWithArrayIndex(json, "a", "aenv", 0);
// 结果: [a1, b1]  （两个数组各取第一个）
```

---

## 构建与测试

### 编译

```bash
cd utils/json-extractor
mvn clean compile
```

### 运行测试

```bash
mvn test
```

### 打包

```bash
mvn clean package
```

---

## 项目结构

```
utils/json-extractor/
├── pom.xml
├── README.md
├── TEST_REPORT.md
├── .gitignore
└── src/
    ├── main/java/com/glm/utils/
    │   └── JsonValueExtractor.java
    └── test/java/com/glm/utils/
        └── JsonValueExtractorTest.java
```

---

## 版本历史

### v1.1.0 (2026-01-15)
- 🆕 新增数组索引支持 (`extractValuesWithArrayIndex`)
- 🆕 新增嵌套同名路径处理（a套a只取最内层）
- 🆕 新增便捷方法 `extractFirstValuesFromArrays`、`extractAllFirstValues`
- ✅ 确保去重时保留插入顺序

### v1.0.0 (2026-01-15)
- 初始版本
- 基础的递归提取和去重功能

---

## 版本信息

- **版本:** 1.1.0
- **作者:** GLM
- **JDK:** 1.8+
- **License:** MIT
