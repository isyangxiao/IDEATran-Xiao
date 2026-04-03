/*
 * 渣渣逍 - IntelliJ IDEA 离线翻译插件
 * 作者: xiao
 *
 * 功能说明:
 * - 核心翻译服务类
 * - 管理 Python 子进程生命周期
 * - 提供同步/异步翻译接口
 */

package com.idea.tran.service;

import com.idea.tran.service.model.TranslationResult;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 翻译服务 - 核心服务类
 *
 * 职责:
 * 1. 管理 Python 子进程的创建和生命周期
 * 2. 提供翻译接口（英→中、中→英）
 * 3. 处理配置文件
 *
 * 调用流程:
 * TranslationService (Java) → PythonTranslationService → Python 子进程 → NLLB 模型
 *
 * @author xiao
 */
@Slf4j
public class TranslationService {

    /** 配置文件资源路径 */
    private static final String CONFIG_RESOURCE = "/config.properties";

    /** 配置文件名称 */
    private static final String CONFIG_FILE_NAME = "idea.tran.properties";

    /** Python 翻译服务实例 */
    private final PythonTranslationService pythonTranslator;

    /** 模型是否加载成功 */
    private boolean modelLoaded = false;

    /**
     * 构造函数 - 初始化 Python 翻译服务
     */
    public TranslationService() {
        this.pythonTranslator = new PythonTranslationService();
        this.modelLoaded = pythonTranslator.isReady();
        if (modelLoaded) {
            log.info("Python 翻译服务初始化成功");
        } else {
            log.warn("Python 翻译服务初始化失败 - 将使用模拟模式");
        }
    }

    /**
     * 获取配置文件
     * 配置存储在 D:\ 盘根目录
     *
     * @return 配置文件对象
     */
    private File getConfigFile() {
        // 使用 D:\ 作为配置目录
        Path configPath = Paths.get("D:/", CONFIG_FILE_NAME);
        File configFile = configPath.toFile();

        // 如果配置文件不存在，从资源中创建
        if (!configFile.exists()) {
            try {
                try (InputStream is = getClass().getResourceAsStream(CONFIG_RESOURCE)) {
                    if (is != null) {
                        try (OutputStream os = Files.newOutputStream(configPath)) {
                            is.transferTo(os);
                        }
                        log.info("已创建配置文件: {}", configPath);
                    }
                }
            } catch (IOException e) {
                log.error("创建配置文件失败", e);
            }
        }
        return configFile;
    }

    /**
     * 翻译为英文
     *
     * @param inputText 输入文本（应为中文）
     * @return 英文翻译结果
     */
    public TranslationResult translateToEnglish(String inputText) {
        if (!modelLoaded) {
            // 模拟模式：返回带前缀的原文
            return TranslationResult.success("[模拟 EN] " + inputText, 0);
        }
        return pythonTranslator.translateToEnglish(inputText);
    }

    /**
     * 翻译为中文
     *
     * @param inputText 输入文本（应为英文）
     * @return 中文翻译结果
     */
    public TranslationResult translateToChinese(String inputText) {
        if (!modelLoaded) {
            // 模拟模式：返回带前缀的原文
            return TranslationResult.success("[模拟 ZH] " + inputText, 0);
        }
        return pythonTranslator.translateToChinese(inputText);
    }

    /**
     * 检查翻译服务是否就绪
     *
     * @return true 表示服务可用
     */
    public boolean isReady() {
        return modelLoaded && pythonTranslator.isReady();
    }

    /**
     * 关闭翻译服务
     * 终止 Python 子进程
     */
    public void close() {
        try {
            if (pythonTranslator != null) {
                pythonTranslator.close();
            }
        } catch (Exception e) {
            log.error("关闭翻译服务失败", e);
        }
    }
}
