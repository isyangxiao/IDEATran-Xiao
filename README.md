# 渣渣逍 - IntelliJ IDEA 离线翻译插件

## 目录

- [简介](#简介)
- [功能特性](#功能特性)
- [快速部署](#快速部署)
  - [1. 解压 Python 环境](#1-解压-python-环境)
  - [2. 配置环境变量](#2-配置环境变量)
  - [3. 验证 Python 安装](#3-验证-python-安装)
  - [4. 安装 IDEA 插件](#4-安装-idea-插件)
  - [5. 开始使用](#5-开始使用)
  - [6. 修改快捷键](#6-修改快捷键)
- [项目结构](#项目结构)
- [技术架构](#技术架构)
- [调用原理](#调用原理)
- [核心组件详解](#核心组件详解)
- [并发处理机制](#并发处理机制)
- [线程模型](#线程模型)
- [翻译日志](#翻译日志)
- [故障排除](#故障排除)
- [构建插件](#构建插件)
- [版本信息](#版本信息)
- [许可证](#许可证)

---

## 简介

渣渣逍是一款基于本地 AI 模型的 IntelliJ IDEA 翻译插件，支持英文↔中文离线翻译。
苦内网久已，需要各位哥姐自行编译

- **作者**: xiao
- **模型**: NLLB-200-distilled-600M (约 2.4GB)
- **Python 环境**: 3.14.3 (Portable 版本)
- **插件大小**: 约 85MB

---

## 功能特性

| 特性 | 说明 |
|------|------|
| 离线运行 | 无需网络，完全本地翻译 |
| 双模式翻译 | 气泡显示 / 替换原文 |
| 自动语言检测 | 根据文本内容自动判断翻译方向 |
| 撤销支持 | 替换翻译支持 Ctrl+Z 撤销 |
| 翻译日志 | 自动记录所有翻译记录 |

---

## 快速部署

### 1. 解压 Python 环境

将 `portable-python.zip` 解压到 `D:\` 盘根目录：

```
D:\portable-python\
├── Python314\           # Python 运行时 (1.3GB)
├── models\              # 翻译模型 (2.4GB)
│   └── nllb200\
│       ├── model.safetensors
│       └── ...
├── python\              # 翻译脚本
│   └── translation_server.py
└── translation.log      # 翻译日志
```

### 2. 配置环境变量

打开系统环境变量设置，添加以下路径到 `PATH`：

```
D:\portable-python\Python314
```

或者在命令行中临时设置：

```cmd
set PATH=D:\portable-python\Python314;%PATH%
```

### 3. 验证 Python 安装

```cmd
python --version
# 应输出: Python 3.14.3
```

### 4. 安装 IDEA 插件

1. 打开 IntelliJ IDEA
2. 进入 `File → Settings → Plugins`
3. 点击右上角齿轮图标，选择 `Install Plugin from Disk...`
4. 选择 `idea-tran-1.0.0.jar`
5. 重启 IDEA

### 5. 开始使用

| 快捷键 | 功能 | 说明 |
|--------|------|------|
| `Ctrl+Alt+M` | 气泡翻译 | 翻译结果显示在气泡提示中 |
| `Ctrl+Alt+R` | 替换翻译 | 翻译结果直接替换选中的原文 |

---

### 6. 修改快捷键

#### 方法一：通过 IDEA 设置界面（推荐）

1. 进入 `File → Settings → Keymap`
2. 在搜索框中输入 `渣渣逍`
3. 可以看到两个动作：
   - `渣渣逍翻译（气泡）` - 气泡翻译模式
   - `渣渣逍翻译（替换）` - 替换翻译模式
4. 右键点击动作名称，选择 `Add Keyboard Shortcut`
5. 输入你想要的快捷键组合
6. 点击 `OK` 保存

#### 方法二：通过插件配置文件

插件的默认快捷键定义在 `src/main/resources/META-INF/plugin.xml` 中：

```xml
<actions>
    <action id="IdeaTranTranslateAction"
            class="com.idea.tran.action.TranslateAction"
            text="渣渣逍翻译（气泡）">
        <add-to-group group-id="EditorPopupMenu" anchor="LAST"/>
        <keyboard-shortcut keymap="$default" first-keystroke="control alt m"/>
    </action>
    <action id="IdeaTranTranslateReplaceAction"
            class="com.idea.tran.action.TranslateActionReplace"
            text="渣渣逍翻译（替换）">
        <add-to-group group-id="EditorPopupMenu" anchor="LAST"/>
        <keyboard-shortcut keymap="$default" first-keystroke="control alt r"/>
    </action>
</actions>
```

修改快捷键语法：
- `control alt m` = Ctrl+Alt+M
- `control shift m` = Ctrl+Shift+M
- `alt m` = Alt+M
- `ctrl shift alt m` = Ctrl+Shift+Alt+M

修改后需要重新构建插件：

```cmd
.\gradlew.bat build
```

---

## 项目结构

```
IDEATran-Xiao/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/idea/tran/
│       │       ├── action/                    # 用户动作类
│       │       │   ├── TranslateAction.java           # 气泡翻译
│       │       │   └── TranslateActionReplace.java     # 替换翻译
│       │       ├── service/                   # 核心服务类
│       │       │   ├── TranslationService.java          # 翻译服务门面
│       │       │   ├── TranslationServiceManager.java   # 单例管理器
│       │       │   ├── PythonTranslationService.java    # Python 子进程管理
│       │       │   └── model/                          # 数据模型
│       │       │       ├── TranslationResult.java       # 翻译结果
│       │       │       └── TranslationHistory.java      # 历史记录
│       │       ├── ui/                         # 界面组件
│       │       │   ├── BalloonPanelFactory.java        # 气泡提示工厂
│       │       │   ├── TranslationHistoryPanel.java     # 历史记录面板
│       │       │   └── TranslationToolWindowFactory.java # 工具窗口
│       │       └── settings/                    # 设置页面
│       │           ├── TranslationSettingsAction.java
│       │           └── TranslationSettingsConfigurable.java
│       └── resources/
│           ├── META-INF/
│           │   └── plugin.xml                  # 插件配置
│           ├── config.properties               # 应用配置
│           └── python/
│               └── translation_server.py       # Python 翻译服务器
├── portable-python/                           # 便携 Python 环境
│   ├── Python314/                            # Python 运行时
│   ├── models/                               # 翻译模型
│   └── python/                               # 翻译脚本
├── build.gradle.kts                           # Gradle 构建配置
└── settings.gradle.kts                        # Gradle 设置
```

---

## 技术架构

### 整体架构图

```
┌────────────────────────────────────────────────────────────────────┐
│                          IntelliJ IDEA                              │
│                                                                    │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐       │
│  │TranslateAction│    │TranslateAction│    │TranslationTool│       │
│  │   (M模式)    │    │   (R模式)     │    │  Window      │       │
│  └──────┬──────┘    └──────┬──────┘    └─────────────┘       │
│         │                   │                                     │
│         └─────────┬─────────┘                                     │
│                   ▼                                                │
│         ┌─────────────────────┐                                    │
│         │ TranslationService    │  ← 门面类                         │
│         └──────────┬──────────┘                                    │
│                    │                                                │
│                    ▼                                                │
│         ┌─────────────────────┐                                    │
│         │PythonTranslationService│  ← 子进程管理                     │
│         │  (stdin/stdout)      │                                    │
│         └──────────┬──────────┘                                    │
└─────────────────────│──────────────────────────────────────────────┘
                      │ 管道通信
                      ▼
         ┌─────────────────────┐
         │  Python Subprocess   │
         │ translation_server.py │
         └──────────┬──────────┘
                    │
                    ▼
         ┌─────────────────────┐
         │  NLLB-200 Model     │
         │  (本地模型文件)       │
         └─────────────────────┘
```

---

## 调用原理

### 气泡翻译流程 (Ctrl+Alt+M)

```
┌─────────────────────────────────────────────────────────────────┐
│ 步骤 1: 用户操作                                                 │
│ 用户选中文本 "Hello world"，按下 Ctrl+Alt+M                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 步骤 2: Action 接收事件                                          │
│ TranslateAction.actionPerformed() 被调用                         │
│   - 获取选中文本范围 [startOffset, endOffset]                   │
│   - 提取选中文本内容                                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 步骤 3: 语言检测                                                 │
│ 使用正则 [\\u4e00-\\u9fff] 检测是否包含中文字符                    │
│   - 包含中文 → 翻译为英文 (zh → en)                              │
│   - 纯英文   → 翻译为中文 (en → zh)                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 步骤 4: 异步翻译请求                                              │
│ 后台线程调用 TranslationService.translateToChinese(text)         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 步骤 5: 发送 JSON 请求到 Python                                  │
│ PythonTranslationService 构建并发送:                              │
│ {                                                                  │
│   "type": "translate",                                           │
│   "text": "Hello world",                                        │
│   "source_lang": "en",                                          │
│   "target_lang": "zh",                                          │
│   "id": "uuid-xxxx-xxxx"  ← 用于响应匹配                        │
│ }                                                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 步骤 6: Python 处理翻译                                           │
│ translation_server.py:                                           │
│   - 解析 JSON 请求                                                │
│   - 加载/使用缓存的 NLLB 模型                                     │
│   - 设置 src_lang = "eng_Latn"                                  │
│   - 设置 forced_bos_token_id = zho_Hans token ID                │
│   - model.generate() 生成翻译                                    │
│   - tokenizer.decode() 解码为文本                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 步骤 7: 返回 JSON 响应                                            │
│ {                                                                  │
│   "success": true,                                                │
│   "result": "你好,世界",                                          │
│   "id": "uuid-xxxx-xxxx"  ← 与请求 ID 匹配                      │
│ }                                                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 步骤 8: Java 端接收并匹配响应                                      │
│ ResponseReaderThread 根据 UUID 分发到对应请求                      │
│ PendingRequest.setResponse() 解除阻塞                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 步骤 9: UI 线程显示结果                                           │
│ Application.invokeLater() 在 UI 线程执行:                         │
│ BalloonPanelFactory.showInfo() 显示气泡提示                       │
└─────────────────────────────────────────────────────────────────┘
```

### 替换翻译流程 (Ctrl+Alt+R)

```
┌─────────────────────────────────────────────────────────────────┐
│ 步骤 1-7: 同气泡翻译流程                                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 步骤 8: 替换文档内容                                              │
│ TranslateActionReplace.replaceSelection():                       │
│                                                                   │
│   WriteCommandAction.runWriteCommandAction(project, () => {      │
│       document.replaceString(startOffset, endOffset, result);    │
│       primaryCaret.removeSelection();                            │
│   });                                                            │
│                                                                   │
│   - 使用 WriteCommandAction 支持撤销                               │
│   - 替换选中的原文为翻译结果                                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 核心组件详解

### Java 层

#### 1. TranslateAction (气泡翻译)

```java
/**
 * 职责:
 * - 处理 Ctrl+Alt+M 快捷键
 * - 检测语言方向
 * - 调用翻译服务
 * - 显示气泡结果
 */
public class TranslateAction extends AnAction {

    // 中文字符检测正则
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]");

    // actionPerformed() → 检测语言 → 异步翻译 → 显示气泡
}
```

#### 2. TranslateActionReplace (替换翻译)

```java
/**
 * 职责:
 * - 处理 Ctrl+Alt+R 快捷键
 * - 检测语言方向
 * - 调用翻译服务
 * - 替换文档内容（支持撤销）
 */
public class TranslateActionReplace extends AnAction {

    // 使用 WriteCommandAction 包装文档修改
    WriteCommandAction.runWriteCommandAction(project, () -> {
        document.replaceString(start, end, translatedText);
    });
}
```

#### 3. TranslationService (门面服务)

```java
/**
 * 职责:
 * - 对外提供统一的翻译接口
 * - 管理 Python 子进程生命周期
 * - 处理配置
 */
public class TranslationService {

    public TranslationResult translateToEnglish(String text);  // 中→英
    public TranslationResult translateToChinese(String text);   // 英→中
    public boolean isReady();                                  // 服务就绪检查
}
```

#### 4. PythonTranslationService (子进程管理)

```java
/**
 * 职责:
 * - 启动/管理 Python 子进程
 * - 通过 stdin/stdout 通信
 * - 处理并发请求-响应匹配
 *
 * 关键机制:
 * - UUID 请求 ID: 确保响应正确匹配
 * - CountDownLatch: 阻塞等待响应
 * - ConcurrentHashMap: 存储待处理请求
 */
public class PythonTranslationService {

    // 后台线程持续读取 Python 输出
    private void startResponseReader() {
        new Thread(() -> {
            while (process.isAlive()) {
                String line = reader.readLine();
                String reqId = extractId(line);
                pendingRequests.get(reqId).setResponse(line);
            }
        }).start();
    }

    // 发送请求并等待响应
    public TranslationResult translate(...) {
        String requestId = UUID.randomUUID().toString();
        PendingRequest pending = new PendingRequest();
        pendingRequests.put(requestId, pending);

        writer.write(request);  // 发送请求
        String response = pending.getResponse();  // 阻塞等待

        return parseResponse(response);
    }
}
```

#### 5. PendingRequest (请求容器)

```java
/**
 * 用于在后台线程和主线程之间传递响应
 *
 * 使用 CountDownLatch 实现线程同步:
 * - 主线程调用 latch.await() 阻塞等待
 * - 后台线程调用 latch.countDown() 解除阻塞
 */
private static class PendingRequest {
    String response;
    CountDownLatch latch = new CountDownLatch(1);

    void setResponse(String response) {
        this.response = response;
        latch.countDown();  // 解除阻塞
    }

    String getResponse() throws InterruptedException {
        latch.await();  // 阻塞等待
        return response;
    }
}
```

### Python 层

#### translation_server.py (翻译服务器)

```python
"""
主循环:
1. 从 stdin 读取 JSON 请求
2. 解析 type, text, source_lang, target_lang, id
3. 执行翻译
4. 输出 JSON 响应到 stdout
"""
def main():
    # 主循环
    while True:
        line = sys.stdin.readline()
        request = json.loads(line)

        if request["type"] == "translate":
            result = translate(
                request["text"],
                request["source_lang"],
                request["target_lang"]
            )
            # 输出响应
            print(json.dumps({
                "success": True,
                "result": result,
                "id": request["id"]  # 原样返回 ID
            }))

def translate(text, source_lang, target_lang):
    """使用 NLLB 模型翻译"""
    tokenizer.src_lang = LANG_CODES[source_lang]

    inputs = tokenizer(text, return_tensors="pt")
    inputs["forced_bos_token_id"] = tokenizer.convert_tokens_to_ids(
        LANG_CODES[target_lang]
    )

    outputs = model.generate(**inputs, max_length=256, num_beams=4)
    return tokenizer.decode(outputs[0], skip_special_tokens=True)
```

---

## 并发处理机制

### 问题背景

当多个翻译请求同时发生时，可能出现响应错乱：

```
时间线:
T1: 线程A 发送 "Hello"     → Python 返回 "你好"
T2: 线程B 发送 "World"      → Python 返回 "世界"
T3: 线程A 收到 "世界" (错误!)
T4: 线程B 收到 "你好" (错误!)
```

### 解决方案: UUID 请求-响应匹配

```
┌─────────────────────────────────────────────────────────────────┐
│                     请求-响应匹配流程                               │
│                                                                   │
│  线程A ── UUID-A ──► ┌─────────┐ ──► UUID-A ──► 线程A           │
│                      │  Map    │                                 │
│  线程B ── UUID-B ──► │<UUID,Req>│ ──► UUID-B ──► 线程B           │
│                      └─────────┘                                 │
│                         ▲                                        │
│                         │                                        │
│              ResponseReaderThread                               │
│                  读取 Python 输出                                │
│                  提取 ID, 分发响应                                │
└─────────────────────────────────────────────────────────────────┘

请求数据结构:
{
    "id": "uuid-xxxx-xxxx",  // 唯一标识
    "text": "Hello",
    "source_lang": "en",
    "target_lang": "zh"
}

响应数据结构:
{
    "id": "uuid-xxxx-xxxx",  // 与请求相同
    "success": true,
    "result": "你好"
}
```

### 代码实现

```java
// 1. 发送请求时创建 PendingRequest
String requestId = UUID.randomUUID().toString();
PendingRequest pending = new PendingRequest();
pendingRequests.put(requestId, pending);  // 注册

writer.write(request);  // 发送时包含 ID
pending.getResponse();  // 阻塞等待

// 2. 后台线程收到响应后分发
String reqId = extractId(responseLine);
PendingRequest req = pendingRequests.remove(reqId);
req.setResponse(responseLine);  // 唤醒等待线程
```

---

## 线程模型

```
┌─────────────────────────────────────────────────────────────────┐
│                        主线程 (UI)                               │
│                                                                   │
│  TranslateAction.actionPerformed()                               │
│    └─ invokeLater() → 显示气泡/替换文档                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ invokeLater
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     后台工作线程                                   │
│                                                                   │
│  new Thread(() -> {                                              │
│      result = translationService.translate(text);                │
│      invokeLater(() -> showResult(result));                     │
│  }).start();                                                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ pending.getResponse() (阻塞)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  响应读取线程 (Daemon)                             │
│                                                                   │
│  while (process.isAlive()) {                                     │
│      line = reader.readLine();                                   │
│      // 提取 ID，分发响应                                          │
│      pendingRequests.get(id).setResponse(line);                 │
│  }                                                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 翻译日志

翻译过程会自动记录到 `D:\portable-python\translation.log`

### 日志格式

```json
{"timestamp": "2026-04-03 10:30:15.123", "input": "Hello", "source_lang": "en", "target_lang": "zh", "success": true, "output": "你好"}
{"timestamp": "2026-04-03 10:30:25.456", "input": "你好", "source_lang": "zh", "target_lang": "en", "success": true, "output": "Hello"}
{"timestamp": "2026-04-03 10:30:35.789", "input": "Error case", "source_lang": "en", "target_lang": "zh", "success": false, "error": "模型加载失败"}
```

### 日志字段说明

| 字段 | 说明 |
|------|------|
| timestamp | 翻译时间 (格式: YYYY-MM-DD HH:mm:ss.SSS) |
| input | 原文 |
| source_lang | 源语言 |
| target_lang | 目标语言 |
| success | 是否成功 |
| output | 翻译结果 (成功时) |
| error | 错误信息 (失败时) |

---

## 故障排除

### 1. 翻译服务未就绪

**症状**: 提示"翻译服务未就绪"

**检查步骤**:
```cmd
# 1. 确认目录存在
dir D:\portable-python\

# 2. 确认 Python 可用
python --version

# 3. 确认模型存在
dir D:\portable-python\models\nllb200\
```

**解决方案**:
1. 确认 `D:\portable-python\` 目录完整
2. 重新解压 `portable-python.zip`
3. 重启 IDEA

### 2. 模型加载失败

**症状**: Python 进程启动后立即退出

**检查日志**:
```cmd
# 检查 IDEA 日志或 Python 错误输出
```

**解决方案**:
1. 确认模型文件完整 (model.safetensors 约 2.4GB)
2. 确认 transformers 和 torch 已安装

### 3. 翻译结果为旧内容

**症状**: 翻译显示的是之前请求的结果

**原因**: 旧的并发问题

**解决方案**:
1. 已通过 UUID 请求-响应匹配修复
2. 检查 `translation.log` 确认请求顺序

### 4. 替换翻译报错

**症状**: `Must not change document outside command`

**解决方案**:
1. 已使用 `WriteCommandAction` 修复
2. 重新安装插件

---

## 构建插件

### 开发环境构建

```cmd
cd IDEATran-Xiao
.\gradlew.bat build
```

产物位置: `build/libs/idea-tran-1.0.0.jar`

### 清理构建

```cmd
.\gradlew.bat clean
```

---

## 版本信息

| 组件 | 版本 | 说明 |
|------|------|------|
| 插件 | 1.0.0 | 初始版本 |
| NLLB 模型 | 200-distilled-600M | Facebook 多语言翻译模型 |
| Python | 3.14.3 | 便携版 |
| transformers | 4.57.6 | HuggingFace |
| torch | 2.10.0+cpu | PyTorch CPU 版 |

---

## 许可证

MIT License

---

## 联系方式

**作者**: xiao

**GitHub**: https://github.com/isyangxiao/IDEATran-Xiao
