/*
 * 渣渣逍 - IntelliJ IDEA 离线翻译插件
 * 作者: xiao
 */

package com.idea.tran.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 翻译历史记录
 *
 * 用于存储每次翻译的记录，包含:
 * - originalText: 原文
 * - translatedText: 翻译结果
 * - timestamp: 翻译时间
 * - sourceLang: 源语言
 * - targetLang: 目标语言
 *
 * @author xiao
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslationHistory {

    /** 原始文本 */
    private String originalText;

    /** 翻译后的文本 */
    private String translatedText;

    /** 翻译时间戳（毫秒） */
    private long timestamp;

    /** 源语言代码 */
    private String sourceLang;

    /** 目标语言代码 */
    private String targetLang;
}
