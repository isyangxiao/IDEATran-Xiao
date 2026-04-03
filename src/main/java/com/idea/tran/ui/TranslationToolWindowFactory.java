/*
 * 渣渣逍 - IntelliJ IDEA 离线翻译插件
 * 作者: xiao
 */

package com.idea.tran.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;

/**
 * 翻译历史工具窗口工厂
 *
 * 职责:
 * - 创建翻译历史工具窗口
 * - 管理历史记录面板
 *
 * @author xiao
 */
public class TranslationToolWindowFactory implements ToolWindowFactory {

    /** 历史记录面板 */
    private TranslationHistoryPanel historyPanel;

    /**
     * 创建工具窗口内容
     *
     * @param project    项目
     * @param toolWindow 工具窗口
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        historyPanel = new TranslationHistoryPanel();
        toolWindow.getContentManager().addContent(
            toolWindow.getContentManager().getFactory().createContent(
                historyPanel.getComponent(),
                "历史记录",
                false
            )
        );
    }

    /**
     * 添加历史记录
     *
     * @param originalText   原文
     * @param translatedText 翻译结果
     * @param sourceLang     源语言
     * @param targetLang     目标语言
     */
    public void addHistory(String originalText, String translatedText, String sourceLang, String targetLang) {
        if (historyPanel != null) {
            com.idea.tran.service.model.TranslationHistory history =
                new com.idea.tran.service.model.TranslationHistory(
                    originalText, translatedText, System.currentTimeMillis(), sourceLang, targetLang
                );
            historyPanel.addHistory(history);
        }
    }
}
