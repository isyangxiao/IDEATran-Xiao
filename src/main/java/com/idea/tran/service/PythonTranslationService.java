/*
 * 渣渣逍 - IntelliJ IDEA 离线翻译插件
 * 作者: xiao
 *
 * 功能说明:
 * - Python 子进程管理
 * - 与 Python 翻译服务器通信
 * - 请求-响应匹配（支持并发）
 */

package com.idea.tran.service;

import com.idea.tran.service.model.TranslationResult;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Python 翻译服务 - 管理 Python 子进程
 *
 * 职责:
 * 1. 查找并启动 Python 解释器
 * 2. 加载 NLLB-200 翻译模型
 * 3. 通过 stdin/stdout 与 Python 服务器通信
 * 4. 处理请求-响应匹配（解决并发问题）
 *
 * 技术细节:
 * - Python 服务器以子进程运行
 * - 使用 UUID 作为请求 ID，确保响应匹配正确
 * - 后台线程持续读取 Python 输出
 * - PendingRequest Map 存储待处理的请求
 *
 * @author xiao
 */
@Slf4j
public class PythonTranslationService {

    /** Python 子进程 */
    private Process process;

    /** 向 Python 进程写入的 writer */
    private BufferedWriter writer;

    /** 从 Python 进程读取的 reader */
    private BufferedReader reader;

    /** 服务是否就绪 */
    private volatile boolean ready = false;

    /** Python 脚本临时文件路径 */
    private Path scriptPath;

    // ==================== JSON 解析正则 ====================
    /** 成功响应匹配 */
    private static final Pattern SUCCESS_PATTERN = Pattern.compile("\"success\"\\s*:\\s*true");
    /** 结果提取匹配 */
    private static final Pattern RESULT_PATTERN = Pattern.compile("\"result\"\\s*:\\s*\"([^\"]+)\"");
    /** 错误信息匹配 */
    private static final Pattern ERROR_PATTERN = Pattern.compile("\"error\"\\s*:\\s*\"([^\"]+)\"");
    /** 请求 ID 匹配 */
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");

    /**
     * 待处理请求容器
     * Key: 请求 UUID
     * Value: 包含响应和 CountDownLatch 的请求对象
     */
    private final Map<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    /**
     * 待处理请求内部类
     * 用于在后台线程和主线程之间传递响应
     */
    private static class PendingRequest {
        String response;      // 服务器响应
        CountDownLatch latch; // 倒计时门闩

        PendingRequest() {
            this.latch = new CountDownLatch(1);
        }

        /** 设置响应并解锁 */
        void setResponse(String response) {
            this.response = response;
            latch.countDown();
        }

        /** 等待响应（阻塞直到收到响应） */
        String getResponse() throws InterruptedException {
            latch.await();
            return response;
        }
    }

    /** Python 脚本资源路径 */
    private static final String PYTHON_SCRIPT = "/python/translation_server.py";

    /**
     * 构造函数 - 启动 Python 服务器
     */
    public PythonTranslationService() {
        startServer();
        startResponseReader();
    }

    /**
     * 启动 Python 翻译服务器
     *
     * 流程:
     * 1. 查找 Python 解释器
     * 2. 确认模型路径存在
     * 3. 从 JAR 提取 Python 脚本到临时文件
     * 4. 启动 Python 子进程
     * 5. 等待模型加载（5秒）
     */
    private void startServer() {
        try {
            // 1. 查找 Python 解释器
            String pythonPath = findPython();
            log.info("使用 Python: {}", pythonPath);

            // 2. 确认模型路径
            String modelPath = "D:/portable-python/models/nllb200";
            if (Files.exists(Paths.get(modelPath))) {
                log.info("模型路径: {}", modelPath);
            } else {
                log.error("模型未找到: {}，请确保 portable-python 已解压到 D:/", modelPath);
                return;
            }

            // 3. 从 JAR 提取 Python 脚本到临时文件
            scriptPath = extractResourceToTemp(PYTHON_SCRIPT, "translation_server.py");
            log.info("Python 脚本已提取: {}", scriptPath);

            // 4. 启动 Python 子进程，传递模型路径作为参数
            ProcessBuilder pb = new ProcessBuilder(
                pythonPath,
                scriptPath.toString(),
                modelPath
            );
            pb.redirectErrorStream(true);
            process = pb.start();

            // 5. 设置输入输出流
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

            // 6. 等待模型加载（首次加载需要 10-20 秒）
            Thread.sleep(5000);

            // 7. 检查进程是否存活
            if (process.isAlive()) {
                ready = true;
                log.info("Python 翻译服务器启动成功");
            } else {
                log.error("Python 翻译服务器启动失败，退出码: {}", process.exitValue());
                try {
                    String errorOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    log.error("服务器错误输出: {}", errorOutput);
                } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            log.error("启动翻译服务器失败", e);
            ready = false;
        }
    }

    /**
     * 启动后台响应读取线程
     *
     * 持续从 Python stdout 读取响应
     * 根据请求 ID 分发到对应的 PendingRequest
     */
    private void startResponseReader() {
        Thread readerThread = new Thread(() -> {
            try {
                String line;
                // 只要进程存活，持续读取响应
                while (process != null && process.isAlive() && (line = reader.readLine()) != null) {
                    log.debug("收到响应: {}", line);
                    String reqId = extractId(line);
                    if (reqId != null && pendingRequests.containsKey(reqId)) {
                        PendingRequest req = pendingRequests.remove(reqId);
                        req.setResponse(line);
                    }
                }
            } catch (Exception e) {
                log.error("响应读取线程异常", e);
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * 从 JSON 响应中提取请求 ID
     *
     * @param json JSON 响应字符串
     * @return 请求 ID 或 null
     */
    private String extractId(String json) {
        Matcher matcher = ID_PATTERN.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 从 JAR 资源提取文件到临时目录
     *
     * @param resourcePath 资源路径
     * @param fileName      文件名
     * @return 临时文件路径
     */
    private Path extractResourceToTemp(String resourcePath, String fileName) throws Exception {
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new FileNotFoundException("资源未找到: " + resourcePath);
        }

        Path tempDir = Files.createTempDirectory("idea-tran");
        Path tempFile = tempDir.resolve(fileName);

        try (OutputStream os = Files.newOutputStream(tempFile)) {
            is.transferTo(os);
        }
        is.close();

        // JVM 退出时删除临时文件
        tempFile.toFile().deleteOnExit();
        tempDir.toFile().deleteOnExit();

        return tempFile;
    }

    /**
     * 查找 Python 解释器
     *
     * 按顺序尝试多个可能的路径
     *
     * @return Python 解释器路径
     */
    private String findPython() {
        String[] paths = {
            "python",           // PATH 中的 python
            "python3",          // PATH 中的 python3
            "py",               // Python Launcher
            "C:/Users/Administrator/AppData/Local/Programs/Python/Python314/python.exe",
            "C:/Python312/python.exe",
            "C:/Python311/python.exe"
        };

        for (String path : paths) {
            try {
                ProcessBuilder pb = new ProcessBuilder(path, "--version");
                Process p = pb.start();
                int exitCode = p.waitFor();
                if (exitCode == 0) {
                    return path;
                }
            } catch (Exception ignored) {
            }
        }

        return "python"; // 默认值
    }

    /**
     * 翻译为英文
     */
    public TranslationResult translateToEnglish(String inputText) {
        return translate(inputText, "zh", "en");
    }

    /**
     * 翻译为中文
     */
    public TranslationResult translateToChinese(String inputText) {
        return translate(inputText, "en", "zh");
    }

    /**
     * 执行翻译
     *
     * 流程:
     * 1. 生成唯一请求 ID
     * 2. 构建 JSON 请求
     * 3. 创建 PendingRequest 并注册
     * 4. 发送请求到 Python
     * 5. 等待响应（通过 CountDownLatch 阻塞）
     * 6. 解析响应返回结果
     *
     * @param inputText  输入文本
     * @param sourceLang 源语言代码
     * @param targetLang 目标语言代码
     * @return 翻译结果
     */
    public TranslationResult translate(String inputText, String sourceLang, String targetLang) {
        if (!ready || process == null || !process.isAlive()) {
            return TranslationResult.error("翻译服务未就绪，请重启 IDEA。");
        }

        // 1. 生成唯一请求 ID
        String requestId = UUID.randomUUID().toString();

        try {
            // 2. 构建 JSON 请求
            String request = String.format(
                "{\"type\":\"translate\",\"text\":\"%s\",\"source_lang\":\"%s\",\"target_lang\":\"%s\",\"id\":\"%s\"}\n",
                escapeJson(inputText), sourceLang, targetLang, requestId
            );

            log.info("发送请求 [{}]: {}", requestId, request);

            // 3. 创建并注册待处理请求
            PendingRequest pending = new PendingRequest();
            pendingRequests.put(requestId, pending);

            // 4. 发送请求
            synchronized (writer) {
                writer.write(request);
                writer.flush();
            }

            // 5. 等待响应（阻塞）
            String responseLine = pending.getResponse();
            if (responseLine == null) {
                return TranslationResult.error("服务器关闭了连接");
            }

            log.info("收到响应 [{}]: {}", requestId, responseLine);
            return parseResponse(responseLine);

        } catch (Exception e) {
            log.error("翻译失败", e);
            pendingRequests.remove(requestId);
            return TranslationResult.error("翻译错误: " + e.getMessage());
        }
    }

    /**
     * 转义 JSON 特殊字符
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * 解析 JSON 响应
     *
     * @param json JSON 字符串
     * @return 翻译结果
     */
    private TranslationResult parseResponse(String json) {
        log.debug("解析响应: {}", json);

        // 检查是否成功
        if (!SUCCESS_PATTERN.matcher(json).find()) {
            log.error("响应不包含 success=true: {}", json);
            Matcher errorMatcher = ERROR_PATTERN.matcher(json);
            if (errorMatcher.find()) {
                return TranslationResult.error("翻译错误: " + errorMatcher.group(1));
            }
            return TranslationResult.error("未知错误: " + json.substring(0, Math.min(100, json.length())));
        }

        // 提取翻译结果
        Matcher resultMatcher = RESULT_PATTERN.matcher(json);
        if (resultMatcher.find()) {
            String result = resultMatcher.group(1);
            // 反转义 JSON 字符串
            result = result
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
            return TranslationResult.success(result, 0);
        }

        return TranslationResult.error("无法解析结果");
    }

    /**
     * 检查服务是否就绪
     */
    public boolean isReady() {
        return ready && process != null && process.isAlive();
    }

    /**
     * 关闭翻译服务
     */
    public void close() {
        try {
            if (writer != null) {
                writer.write("{\"type\":\"quit\"}\n");
                writer.flush();
            }
        } catch (Exception ignored) {
        }

        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
    }
}
