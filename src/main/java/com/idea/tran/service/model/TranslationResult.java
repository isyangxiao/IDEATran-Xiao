/*
 * 渣渣逍 - IntelliJ IDEA 离线翻译插件
 * 作者: xiao
 */

package com.idea.tran.service.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 翻译结果数据类
 *
 * 用于封装翻译操作的返回结果，包含:
 * - success: 是否成功
 * - text: 翻译文本（成功时）
 * - error: 错误信息（失败时）
 * - timestamp: 时间戳
 * - durationMs: 耗时（毫秒）
 *
 * @author xiao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationResult {

    /** 是否成功 */
    private boolean success;

    /** 翻译结果文本 */
    private String text;

    /** 错误信息（失败时） */
    private String error;

    /** 时间戳 */
    private long timestamp;

    /** 翻译耗时（毫秒） */
    private int durationMs;

    /**
     * 创建成功结果
     *
     * @param text     翻译文本
     * @param startTime 开始时间（用于计算耗时）
     * @return 成功结果对象
     */
    public static TranslationResult success(String text, long startTime) {
        return TranslationResult.builder()
                .success(true)
                .text(text)
                .timestamp(System.currentTimeMillis())
                .durationMs((int)(System.currentTimeMillis() - startTime))
                .build();
    }

    /**
     * 创建错误结果
     *
     * @param message 错误信息
     * @return 错误结果对象
     */
    public static TranslationResult error(String message) {
        return TranslationResult.builder()
                .success(false)
                .error(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 获取翻译文本或 null
     *
     * @return 成功时返回翻译文本，失败时返回 null
     */
    public String getTextOrNull() {
        return success ? text : null;
    }
}
