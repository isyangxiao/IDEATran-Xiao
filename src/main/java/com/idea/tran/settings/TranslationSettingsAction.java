/*
 * 渣渣逍 - IntelliJ IDEA 离线翻译插件
 * 作者: xiao
 */

package com.idea.tran.settings;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import org.jetbrains.annotations.NotNull;

/**
 * 翻译设置动作
 *
 * 职责:
 * - 在设置对话框中显示翻译插件配置页面
 *
 * @author xiao
 */
public class TranslationSettingsAction extends AnAction {

    /**
     * 执行动作 - 打开设置The page
     *
     * @param e 动作事件
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ShowSettingsUtil.getInstance()
            .showSettingsDialog(e.getProject(), "IdeaTranSettings");
    }
}
