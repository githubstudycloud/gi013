# JSON Value Extractor 工具类

## 概述

`JsonValueExtractor` 是一个基于 JDK 8 的 Java 工具类，用于从嵌套的 JSON 结构中递归提取指定路径下特定键的所有值并去重。

### 核心特性

- ✅ **递归遍历** - 支持任意深度的嵌套结构（dict/list/混合嵌套）
- ✅ **路径限定** - 先定位到指定路径节点，再在其子树中搜索
- ✅ **自动去重** - 使用 `Set` 存储结果，自动去除重复值
- ✅ **类型保持** - 保持原始数据类型（String/Long/Double/Boolean）
- ✅ **批量提取** - 支持一次提取多组路径-键值对
- ✅ **灵活API** - 提供多种方法满足不同使用场景

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

        // 提取 a 下所有 aenv 的值（去重）
        Set<Object> aenvValues = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
        System.out.println("aenv values: " + aenvValues);
        // 输出: aenv values: [env1, env2, env3]

        // 提取 b 下所有 benv 的值（去重）
        Set<Object> benvValues = JsonValueExtractor.extractValuesUnderPath(json, "b", "benv");
        System.out.println("benv values: " + benvValues);
        // 输出: benv values: [benv1, benv2]
    }
}
```

## API 文档

### 基础方法

#### `extractValuesUnderPath(String jsonString, String pathKey, String targetKey)`

从 JSON 字符串的指定路径下提取目标键的所有值。

```java
Set<Object> values = JsonValueExtractor.extractValuesUnderPath(json, "a", "aenv");
```

**参数：**
- `jsonString` - JSON 字符串
- `pathKey` - 限定搜索的路径键名（如 "a"）
- `targetKey` - 要提取的目标键名（如 "aenv"）

**返回：** 去重后的值集合

---

#### `extractAllValues(String jsonString, String pathKey, String targetKey)`

递归搜索 pathKey，然后在其下提取目标键的值。适用于 pathKey 本身也嵌套在任意深度的情况。

```java
// pathKey "a" 嵌套在深层
String json = "{\"root\":{\"config\":{\"a\":{\"aenv\":\"env1\"}}}}";
Set<Object> values = JsonValueExtractor.extractAllValues(json, "a", "aenv");
```

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
// result.get("aenv") -> Set of aenv values
// result.get("benv") -> Set of benv values
```

#### `batchExtractAsList(String jsonString, List<String[]> mappings)`

批量提取并返回 List 形式的结果。

```java
Map<String, List<Object>> result = JsonValueExtractor.batchExtractAsList(json, mappings);
```

---

### 类型专用方法

#### `extractStringValues(String jsonString, String pathKey, String targetKey)`

仅提取字符串类型的值。

```java
Set<String> values = JsonValueExtractor.extractStringValues(json, "a", "aenv");
```

#### `extractStringValuesAsList(String jsonString, String pathKey, String targetKey)`

提取字符串值并返回 List。

```java
List<String> values = JsonValueExtractor.extractStringValuesAsList(json, "a", "aenv");
```

---

## 使用场景

### 场景1：提取环境配置

```java
String json = "{"
    + "\"development\":{"
    + "  \"env\":\"dev\","
    + "  \"services\":[{\"env\":\"dev-api\"},{\"env\":\"dev-web\"}]"
    + "}"
    + "}";

Set<Object> devEnvs = JsonValueExtractor.extractValuesUnderPath(json, "development", "env");
// 结果: [dev, dev-api, dev-web]
```

### 场景2：提取数据库主机列表

```java
String json = "{"
    + "\"database\":{"
    + "  \"host\":\"localhost\","
    + "  \"replicas\":[{\"host\":\"replica1\"},{\"host\":\"replica2\"}]"
    + "}"
    + "}";

Set<Object> hosts = JsonValueExtractor.extractValuesUnderPath(json, "database", "host");
// 结果: [localhost, replica1, replica2]
```

### 场景3：处理动态key

```java
// key可以是任意值，通过参数传入
String pathKey = getPathKeyFromConfig();   // 如 "moduleA"
String targetKey = getTargetKeyFromConfig(); // 如 "endpoint"

Set<Object> values = JsonValueExtractor.extractValuesUnderPath(json, pathKey, targetKey);
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
└── src/
    ├── main/java/com/glm/utils/
    │   └── JsonValueExtractor.java
    └── test/java/com/glm/utils/
        └── JsonValueExtractorTest.java
```

---

## 版本信息

- **版本:** 1.0.0
- **作者:** GLM
- **JDK:** 1.8+
- **License:** MIT

