# JSON Value Extractor 工具类

## 概述

`JsonValueExtractor` 是一个基于 JDK 8 的 Java 工具类，用于从嵌套的 JSON 结构中递归提取指定路径下特定键的所有值并去重。

### 核心特性

- ✅ **pathKey 任意深度**：pathKey 可以在 JSON 的任意深度位置
- ✅ **targetKey 任意深度**：targetKey 可以在 pathKey 下的任意深度
- ✅ **路径链支持**：支持 `a -> a1 -> aenv` 这样的多级路径
- ✅ **字符串JSON解析**：支持解析字符串类型的JSON字段值
- ✅ **嵌套同名路径**：a 套 a 时只取最内层子集的值
- ✅ **去重保序**：使用 LinkedHashSet，自动去重并保留插入顺序
- ✅ **数组索引**：支持指定只取数组中的第n个元素

## 环境要求

- JDK 1.8+
- Maven 3.x
- Gson 2.10.1

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

---

## API 文档

### 1. 基础提取方法

#### `extractAllValues(json, pathKey, targetKey)`

从 JSON 中提取指定路径下目标键的所有值。

```java
// pathKey "a" 可以在任意深度，targetKey "aenv" 可以在 a 下任意深度
String json = "{\"root\":{\"config\":{\"a\":{\"deep\":{\"aenv\":\"value\"}}}}}";
Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
// 结果: [value]
```

---

### 2. 路径链提取（新增）

#### `extractWithPathChain(json, pathChain, targetKey)`

按顺序依次进入每个路径节点，最后在最终路径下搜索目标键。

**用途**：指定精确路径，如只取 `a` 下的 `a1` 里的 `aenv`，忽略 `a` 下直接的 `aenv`。

```java
// 只取 a -> a1 下的 aenv，忽略 a 下直接的 aenv
String json = "{\"a\":{\"aenv\":\"ignored\",\"a1\":{\"aenv\":\"found\"}}}";
Set<Object> result = JsonValueExtractor.extractWithPathChain(
    json, 
    Arrays.asList("a", "a1"),  // 路径链
    "aenv"
);
// 结果: [found]
```

**深层路径链：**
```java
// a -> b -> c -> target
String json = "{\"root\":{\"a\":{\"b\":{\"c\":{\"target\":\"deepValue\"}}}}}";
Set<Object> result = JsonValueExtractor.extractWithPathChain(
    json, 
    Arrays.asList("a", "b", "c"), 
    "target"
);
// 结果: [deepValue]
```

**相关方法：**
- `extractWithPathChainAndArrayIndex(json, pathChain, targetKey, arrayIndex)` - 支持数组索引
- `extractFirstWithPathChain(json, pathChain, targetKey)` - 只取第一个元素
- `extractStringWithPathChain(json, pathChain, targetKey)` - 只提取字符串值

---

### 3. 字符串JSON字段解析（新增）

#### `extractFromStringField(json, stringFieldKey, pathKey, targetKey)`

某些JSON中，一个字段的值本身是JSON字符串（而不是JSON对象）。此方法会先找到该字段，解析其字符串值为JSON，然后在里面搜索目标。

**用途**：处理嵌入的JSON字符串字段。

```java
// sa 字段的值是一个JSON字符串
String json = "{\"sa\":\"{\\\"a\\\":{\\\"aenv\\\":\\\"innerValue\\\"}}\"}";
Set<Object> result = JsonValueExtractor.extractFromStringField(
    json, 
    "sa",      // 字符串JSON字段的键名
    "a",       // 在解析后的JSON中搜索的路径键
    "aenv"     // 目标键
);
// 结果: [innerValue]
```

**多个字符串JSON字段：**
```java
String json = "{\"item1\":{\"sa\":\"{\\\"a\\\":{\\\"aenv\\\":\\\"v1\\\"}}\"},"
            + "\"item2\":{\"sa\":\"{\\\"a\\\":{\\\"aenv\\\":\\\"v2\\\"}}\"}}";
Set<Object> result = JsonValueExtractor.extractFromStringField(json, "sa", "a", "aenv");
// 结果: [v1, v2]
```

**相关方法：**
- `extractFromStringFieldWithArrayIndex(...)` - 支持数组索引
- `extractFirstFromStringField(...)` - 只取第一个元素
- `extractStringFromStringField(...)` - 只提取字符串值

---

### 4. 字符串JSON + 路径链组合（新增）

#### `extractFromStringFieldWithPathChain(json, stringFieldKey, pathChain, targetKey)`

结合字符串JSON解析和路径链功能。

```java
// sa 字段的值是JSON字符串，里面用路径链 a -> a1 查找 aenv
String json = "{\"sa\":\"{\\\"a\\\":{\\\"a1\\\":{\\\"aenv\\\":\\\"pathChainValue\\\"}}}\"}";
Set<Object> result = JsonValueExtractor.extractFromStringFieldWithPathChain(
    json, 
    "sa", 
    Arrays.asList("a", "a1"), 
    "aenv"
);
// 结果: [pathChainValue]
```

---

### 5. 嵌套同名路径（a 套 a）

当存在 pathKey 嵌套 pathKey 的情况时，只取最内层的值：

```java
String json = "{\"a\":{\"aenv\":\"outer\",\"a\":{\"aenv\":\"inner\"}}}";
Set<Object> result = JsonValueExtractor.extractAllValues(json, "a", "aenv");
// 结果: [inner]  （outer 不计入）
```

---

### 6. 数组索引

```java
String json = "{\"a\":{\"items\":[{\"aenv\":\"first\"},{\"aenv\":\"second\"}]}}";

// 只取每个数组的第一个
Set<Object> first = JsonValueExtractor.extractAllValuesWithArrayIndex(json, "a", "aenv", 0);
// 结果: [first]

// 便捷方法
Set<Object> same = JsonValueExtractor.extractAllFirstValues(json, "a", "aenv");
// 结果: [first]
```

---

### 7. 批量提取

```java
List<String[]> mappings = Arrays.asList(
    new String[]{"a", "aenv"},
    new String[]{"b", "benv"}
);
Map<String, Set<Object>> result = JsonValueExtractor.batchExtract(json, mappings);
```

---

## 使用场景

### 场景1：精确路径提取

```java
// 只取 database -> connection 下的 host
String json = "{"
    + "\"database\":{"
    + "  \"host\":\"main-host\","  // 这个会被忽略
    + "  \"connection\":{"
    + "    \"host\":\"conn-host\""  // 只取这个
    + "  }"
    + "}"
    + "}";

Set<Object> hosts = JsonValueExtractor.extractWithPathChain(
    json, 
    Arrays.asList("database", "connection"), 
    "host"
);
// 结果: [conn-host]
```

### 场景2：嵌入JSON字符串

```java
// API响应中某个字段是JSON字符串
String apiResponse = "{"
    + "\"data\":{"
    + "  \"config\":\"{\\\"settings\\\":{\\\"env\\\":\\\"production\\\"}}\""
    + "}"
    + "}";

Set<Object> env = JsonValueExtractor.extractFromStringField(
    apiResponse, 
    "config",   // 字符串JSON字段
    "settings", // 路径键
    "env"       // 目标键
);
// 结果: [production]
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

### v1.4.0 (2026-01-15)
- ✨ **新增**：路径链支持 - `extractWithPathChain` 系列方法
- ✨ **新增**：字符串JSON字段解析 - `extractFromStringField` 系列方法
- ✨ **新增**：字符串JSON + 路径链组合 - `extractFromStringFieldWithPathChain`
- 📝 增加到 76 个测试用例

### v1.3.0 (2026-01-15)
- 🐛 修复 pathKey 任意深度搜索
- 🐛 修复 targetKey 任意深度搜索

### v1.2.0 (2026-01-15)
- 🔧 代码重构优化

### v1.1.0 (2026-01-15)
- 🆕 数组索引支持
- 🆕 嵌套同名路径处理

### v1.0.0 (2026-01-15)
- 初始版本

---

## 版本信息

- **版本:** 1.4.0
- **作者:** GLM
- **JDK:** 1.8+
- **License:** MIT
