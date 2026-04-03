/*
 * 渣渣逍 - IntelliJ IDEA 离线翻译插件
 * 作者: xiao
 */

package com.idea.tran.ui;

import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.JBTable;
import com.idea.tran.service.model.TranslationHistory;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 翻译历史面板
 *
 * 功能:
 * - 以表格形式显示翻译历史
 * - 每行包含：时间、原文、译文、翻译方向
 * - 支持清空历史记录
 *
 * @author xiao
 */
@Slf4j
public class TranslationHistoryPanel {

    /** 主面板 */
    private JPanel mainPanel;

    /** 历史记录表格 */
    private JBTable historyTable;

    /** 表格数据模型 */
    private DefaultTableModel tableModel;

    /** 历史记录列表 */
    private List<TranslationHistory> historyList = new ArrayList<>();

    /**
     * 构造函数 - 初始化面板
     */
    public TranslationHistoryPanel() {
        initialize();
    }

    /**
     * 初始化界面组件
     */
    private void initialize() {
        mainPanel = new JPanel(new BorderLayout());

        // 定义表格列
        String[] columnNames = {"时间", "原文", "译文", "方向"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        historyTable = new JBTable(tableModel);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // 设置列宽
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(80);

        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(historyTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton clearButton = new JButton("清空历史");
        clearButton.addActionListener(e -> clearHistory());
        buttonPanel.add(clearButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 添加历史记录
     *
     * @param history 翻译历史对象
     */
    public void addHistory(TranslationHistory history) {
        historyList.add(history);
        Object[] row = new Object[]{
            formatTime(history.getTimestamp()),
            truncate(history.getOriginalText(), 50),
            truncate(history.getTranslatedText(), 50),
            history.getSourceLang() + " -> " + history.getTargetLang()
        };
        tableModel.addRow(row);
    }

    /**
     * 清空所有历史记录
     */
    private void clearHistory() {
        historyList.clear();
        tableModel.setRowCount(0);
    }

    /**
     * 格式化时间戳
     *
     * @param timestamp 时间戳（毫秒）
     * @return 格式化的时间字符串 HH:mm:ss
     */
    private String formatTime(long timestamp) {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(timestamp));
    }

    /**
     * 截断过长的文本
     *
     * @param text   原文
     * @param maxLen 最大长度
     * @return 截断后的文本
     */
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    /**
     * 获取主面板组件
     *
     * @return JPanel 面板
     */
    public JPanel getComponent() {
        return mainPanel;
    }
}
