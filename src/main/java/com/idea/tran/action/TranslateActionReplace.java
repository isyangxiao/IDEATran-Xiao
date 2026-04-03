/*
 * 渣渣逍 - IntelliJ IDEA 离线翻译插件
 * 作者: xiao
 *
 * 功能说明:
 * - 替换翻译: 选中文本后按 Ctrl+Alt+R，翻译结果直接替换原文
 * - 自动检测语言方向: 中文→英文 或 英文→中文
 *
 * 技术架构:
 * - 使用 Python 子进程调用 NLLB-200 翻译模型
 * - Java 端通过 stdin/stdout 与 Python 服务通信
 * - 使用 UUID 实现请求-响应匹配，支持并发翻译
 */

package com.idea.tran.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.idea.tran.service.TranslationService;
import com.idea.tran.service.TranslationServiceManager;
import com.idea.tran.service.model.TranslationResult;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * 翻译动作 - 替换原文模式
 *
 * 用户触发流程:
 * 1. 用户选中要翻译的文本
 * 2. 按下 Ctrl+Alt+R 快捷键
 * 3. 系统自动检测语言方向（中→英 或 英→中）
 * 4. 调用翻译服务获取结果
 * 5. 翻译结果直接替换选中的原文
 *
 * @author xiao
 */
public class TranslateActionReplace extends AnAction {

    /**
     * 中文字符正则表达式
     * 用于自动检测文本是否为中文
     */
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]");

    /**
     * 动作执行入口
     * 检查项目、编辑器、选中文本是否有效，然后执行翻译并替换
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 获取当前项目
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        // 获取翻译服务实例
        TranslationService service = TranslationServiceManager.getInstance();
        if (service == null) {
            Messages.showMessageDialog(project, "翻译服务未初始化，请重启 IDEA。", "翻译插件", Messages.getWarningIcon());
            return;
        }

        // 获取编辑器
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            Messages.showMessageDialog(project, "请在代码文件中操作。", "翻译插件", Messages.getWarningIcon());
            return;
        }

        // 获取选中文本范围
        CaretModel caretModel = editor.getCaretModel();
        Caret primaryCaret = caretModel.getPrimaryCaret();
        int startOffset = primaryCaret.getSelectionStart();
        int endOffset = primaryCaret.getSelectionEnd();

        // 检查是否有选中文本
        if (startOffset == endOffset) {
            Messages.showMessageDialog(project, "请先选择要翻译的文本。", "翻译插件", Messages.getWarningIcon());
            return;
        }

        // 获取选中的文本
        String selectedText = editor.getDocument().getText().substring(startOffset, endOffset).trim();
        if (selectedText.isEmpty()) {
            Messages.showMessageDialog(project, "选中文本为空。", "翻译插件", Messages.getWarningIcon());
            return;
        }

        // 检查翻译服务是否就绪
        if (!service.isReady()) {
            Messages.showMessageDialog(project,
                "Python 翻译服务未就绪。\n\n请确保 portable-python 已解压到 D:/portable-python",
                "翻译插件", Messages.getInformationIcon());
        } else {
            translateAndReplace(e, selectedText, service, startOffset, endOffset);
        }
    }

    /**
     * 执行翻译并替换原文
     *
     * @param e           动作事件
     * @param text        要翻译的文本
     * @param service     翻译服务实例
     * @param startOffset 选中起始位置
     * @param endOffset   选中结束位置
     */
    private void translateAndReplace(AnActionEvent e, String text, TranslationService service, int startOffset, int endOffset) {
        // 自动检测语言方向：如果文本包含中文，则判定为中文→英文
        boolean containsChinese = CHINESE_PATTERN.matcher(text).find();

        // 在后台线程执行翻译，避免阻塞 UI
        new Thread(() -> {
            TranslationResult result;
            if (containsChinese) {
                // 中文文本 → 英文翻译
                result = service.translateToEnglish(text);
            } else {
                // 英文文本 → 中文翻译
                result = service.translateToChinese(text);
            }

            // 在 UI 线程中替换原文
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                if (result != null && result.isSuccess()) {
                    replaceSelection(e, result.getTextOrNull());
                } else {
                    String errorMsg = result != null ? result.getError() : "未知错误";
                    Messages.showMessageDialog(e.getProject(), errorMsg, "翻译错误", Messages.getErrorIcon());
                }
            });
        }).start();
    }

    /**
     * 用翻译结果替换选中的原文
     *
     * 使用 WriteCommandAction 包装文档修改操作，确保修改被 IDEA 正确管理
     * 可以通过 Ctrl+Z 撤销翻译操作
     *
     * @param e               动作事件
     * @param translatedText   翻译结果
     */
    private void replaceSelection(AnActionEvent e, String translatedText) {
        if (translatedText == null || translatedText.isEmpty()) {
            return;
        }

        Project project = e.getProject();
        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        com.intellij.openapi.editor.Document document = editor.getDocument();

        Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
        int startOffset = primaryCaret.getSelectionStart();
        int endOffset = primaryCaret.getSelectionEnd();

        // 在命令操作中执行文档修改，支持撤销
        WriteCommandAction.runWriteCommandAction(project, () -> {
            document.replaceString(startOffset, endOffset, translatedText);
            primaryCaret.removeSelection();
        });
    }
}
