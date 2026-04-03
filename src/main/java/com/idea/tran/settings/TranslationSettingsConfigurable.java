/*
 * 渣渣逍 - IntelliJ IDEA 离线翻译插件
 * 作者: xiao
 */

package com.idea.tran.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import com.idea.tran.service.TranslationService;
import com.idea.tran.service.TranslationServiceManager;

import javax.swing.*;
import java.awt.*;

/**
 * 翻译插件设置配置页面
 *
 * 功能:
 * - 显示翻译服务状态
 * - 显示当前使用的翻译模型
 * - 提供重新加载按钮
 *
 * @author xiao
 */
@Nls(capitalization = Nls.Capitalization.Title)
public class TranslationSettingsConfigurable implements Configurable {

    /** 当前项目 */
    private final Project project;

    /** 状态标签 */
    private JLabel statusLabel;

    /** 模型标签 */
    private JLabel modelLabel;

    /**
     * 构造函数
     *
     * @param project 当前项目
     */
    public TranslationSettingsConfigurable(Project project) {
        this.project = project;
    }

    /**
     * 获取显示名称
     */
    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "渣渣逍翻译";
    }

    /**
     * 创建设置面板
     *
     * @return 设置面板组件
     */
    @Override
    public JComponent createComponent() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 状态面板
        JPanel statusPanel = createStatusPanel();
        panel.add(statusPanel, BorderLayout.NORTH);

        // 使用说明
        JLabel instructionsLabel = new JLabel(
            "<html>渣渣逍翻译插件使用 NLLB-200 模型<br/>" +
            "支持英文 ↔ 中文翻译<br/>" +
            "快捷键:<br/>" +
            "- Ctrl+Alt+M: 气泡翻译<br/>" +
            "- Ctrl+Alt+R: 替换原文翻译",
            SwingConstants.LEFT
        );
        instructionsLabel.setFont(instructionsLabel.getFont().deriveFont(12f));
        instructionsLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        panel.add(instructionsLabel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 创建状态面板
     *
     * @return 状态面板
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("状态"));

        // 检查服务状态
        TranslationService service = TranslationServiceManager.getInstance();
        boolean ready = service != null && service.isReady();

        statusLabel = new JLabel(ready ? "✓ 就绪" : "✗ 未就绪");
        statusLabel.setFont(statusLabel.getFont().deriveFont(16f));
        statusLabel.setForeground(ready ? Color.GREEN.darker() : Color.RED);

        modelLabel = new JLabel("模型: NLLB-200-distilled-600M");
        modelLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        labelPanel.add(statusLabel);
        labelPanel.add(modelLabel);

        panel.add(labelPanel, BorderLayout.NORTH);

        // 重新加载按钮
        JButton reloadButton = new JButton("重新加载");
        reloadButton.addActionListener(e -> {
            // 关闭并重新打开服务
            if (service != null) {
                service.close();
            }
            Messages.showMessageDialog(project,
                "请重启 IDEA 以重新加载翻译服务。",
                "重启", Messages.getInformationIcon());
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        buttonPanel.add(reloadButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 检查设置是否已修改
     */
    @Override
    public boolean isModified() {
        return false;
    }

    /**
     * 应用设置
     */
    @Override
    public void apply() {
        // 无设置需要应用 - Python 子进程自动管理
    }
}
