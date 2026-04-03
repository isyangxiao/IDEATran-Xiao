#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成渣渣逍翻译插件 PDF 文档
"""

from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT, TA_CENTER
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
import os

# 注册中文字体（Windows 系统）
def register_chinese_font():
    try:
        # 尝试多种中文字体路径
        font_paths = [
            "C:/Windows/Fonts/simhei.ttf",      # 黑体
            "C:/Windows/Fonts/simsun.ttc",     # 宋体
            "C:/Windows/Fonts/msyh.ttc",      # 微软雅黑
        ]
        for path in font_paths:
            if os.path.exists(path):
                pdfmetrics.registerFont(TTFont('ChineseFont', path))
                return 'ChineseFont'
    except Exception as e:
        print(f"Font registration failed: {e}")
    return 'Helvetica'

CHINESE_FONT = register_chinese_font()

def create_styles():
    """创建样式"""
    styles = getSampleStyleSheet()

    styles.add(ParagraphStyle(
        name='ChineseTitle',
        parent=styles['Title'],
        fontName=CHINESE_FONT,
        fontSize=24,
        leading=30,
        alignment=TA_CENTER,
        spaceAfter=30,
    ))

    styles.add(ParagraphStyle(
        name='ChineseH1',
        parent=styles['Heading1'],
        fontName=CHINESE_FONT,
        fontSize=18,
        leading=24,
        spaceAfter=12,
        spaceBefore=20,
    ))

    styles.add(ParagraphStyle(
        name='ChineseH2',
        parent=styles['Heading2'],
        fontName=CHINESE_FONT,
        fontSize=14,
        leading=20,
        spaceAfter=10,
        spaceBefore=15,
    ))

    styles.add(ParagraphStyle(
        name='ChineseBody',
        parent=styles['Normal'],
        fontName=CHINESE_FONT,
        fontSize=11,
        leading=18,
        spaceAfter=8,
    ))

    styles.add(ParagraphStyle(
        name='ChineseCode',
        parent=styles['Code'],
        fontName='Courier',
        fontSize=9,
        leading=12,
        spaceAfter=6,
        leftIndent=20,
        backColor=colors.Color(0.95, 0.95, 0.95),
    ))

    styles.add(ParagraphStyle(
        name='ChineseBullet',
        parent=styles['Normal'],
        fontName=CHINESE_FONT,
        fontSize=11,
        leading=16,
        leftIndent=20,
        bulletIndent=10,
    ))

    return styles

def create_table_style():
    """创建表格样式"""
    return TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.Color(0.2, 0.4, 0.6)),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
        ('ALIGN', (0, 0), (-1, -1), 'LEFT'),
        ('FONTNAME', (0, 0), (-1, 0), CHINESE_FONT),
        ('FONTNAME', (0, 1), (-1, -1), CHINESE_FONT),
        ('FONTSIZE', (0, 0), (-1, 0), 11),
        ('FONTSIZE', (0, 1), (-1, -1), 10),
        ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
        ('BACKGROUND', (0, 1), (-1, -1), colors.Color(0.95, 0.95, 0.95)),
        ('GRID', (0, 0), (-1, -1), 1, colors.Color(0.7, 0.7, 0.7)),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ])

def build_pdf():
    """构建 PDF 文档"""
    doc = SimpleDocTemplate(
        "D:/work/PROJECT/IDEATran-Xiao/渣渣逍翻译插件使用说明.pdf",
        pagesize=A4,
        rightMargin=2*cm,
        leftMargin=2*cm,
        topMargin=2*cm,
        bottomMargin=2*cm,
    )

    styles = create_styles()
    story = []

    # 标题
    story.append(Paragraph("渣渣逍", styles['ChineseTitle']))
    story.append(Paragraph("IntelliJ IDEA 离线翻译插件", styles['ChineseTitle']))
    story.append(Spacer(1, 20))

    # 简介
    story.append(Paragraph("简介", styles['ChineseH1']))
    story.append(Paragraph("渣渣逍是一款基于本地 AI 模型的 IntelliJ IDEA 翻译插件，支持英文↔中文离线翻译。", styles['ChineseBody']))
    story.append(Spacer(1, 10))

    info = [
        ["属性", "说明"],
        ["作者", "xiao"],
        ["模型", "NLLB-200-distilled-600M (约 2.4GB)"],
        ["Python 环境", "3.14.3 (Portable 版本)"],
        ["插件大小", "约 85MB"],
    ]
    table = Table(info, colWidths=[4*cm, 12*cm])
    table.setStyle(create_table_style())
    story.append(table)
    story.append(Spacer(1, 20))

    # 功能特性
    story.append(Paragraph("功能特性", styles['ChineseH1']))
    features = [
        ["特性", "说明"],
        ["离线运行", "无需网络，完全本地翻译"],
        ["双模式翻译", "气泡显示 / 替换原文"],
        ["自动语言检测", "根据文本内容自动判断翻译方向"],
        ["撤销支持", "替换翻译支持 Ctrl+Z 撤销"],
        ["翻译日志", "自动记录所有翻译记录"],
    ]
    table = Table(features, colWidths=[4*cm, 12*cm])
    table.setStyle(create_table_style())
    story.append(table)
    story.append(Spacer(1, 20))

    # 快速部署
    story.append(Paragraph("快速部署", styles['ChineseH1']))

    story.append(Paragraph("1. 解压 Python 环境", styles['ChineseH2']))
    story.append(Paragraph("将 <b>portable-python.zip</b> 解压到 <b>D:\\</b> 盘根目录：", styles['ChineseBody']))
    story.append(Paragraph("D:\\portable-python\\  ├── Python314\\  # Python 运行时", styles['ChineseCode']))
    story.append(Paragraph("  ├── models\\  # 翻译模型 (2.4GB)", styles['ChineseCode']))
    story.append(Paragraph("  │   └── nllb200\\  # NLLB 模型文件", styles['ChineseCode']))
    story.append(Paragraph("  ├── python\\  # 翻译脚本", styles['ChineseCode']))
    story.append(Paragraph("  └── translation.log  # 翻译日志", styles['ChineseCode']))
    story.append(Spacer(1, 10))

    story.append(Paragraph("2. 配置环境变量", styles['ChineseH2']))
    story.append(Paragraph("打开系统环境变量设置，添加以下路径到 PATH：", styles['ChineseBody']))
    story.append(Paragraph("D:\\portable-python\\Python314", styles['ChineseCode']))
    story.append(Spacer(1, 10))

    story.append(Paragraph("3. 验证 Python 安装", styles['ChineseH2']))
    story.append(Paragraph("打开命令提示符，运行：", styles['ChineseBody']))
    story.append(Paragraph("python --version", styles['ChineseCode']))
    story.append(Paragraph("应输出: Python 3.14.3", styles['ChineseBody']))
    story.append(Spacer(1, 10))

    story.append(Paragraph("4. 安装 IDEA 插件", styles['ChineseH2']))
    story.append(Paragraph("1. 打开 IntelliJ IDEA", styles['ChineseBody']))
    story.append(Paragraph("2. 进入 File → Settings → Plugins", styles['ChineseBody']))
    story.append(Paragraph("3. 点击右上角齿轮图标，选择 Install Plugin from Disk...", styles['ChineseBody']))
    story.append(Paragraph("4. 选择 idea-tran-1.0.0.jar", styles['ChineseBody']))
    story.append(Paragraph("5. 重启 IDEA", styles['ChineseBody']))
    story.append(Spacer(1, 10))

    story.append(Paragraph("5. 开始使用", styles['ChineseH2']))
    shortcuts = [
        ["快捷键", "功能", "说明"],
        ["Ctrl+Alt+M", "气泡翻译", "翻译结果显示在气泡提示中"],
        ["Ctrl+Alt+R", "替换翻译", "翻译结果直接替换选中的原文"],
    ]
    table = Table(shortcuts, colWidths=[4*cm, 4*cm, 8*cm])
    table.setStyle(create_table_style())
    story.append(table)
    story.append(Spacer(1, 10))

    story.append(Paragraph("6. 修改快捷键", styles['ChineseH2']))
    story.append(Paragraph("方法一：通过 IDEA 设置界面（推荐）", styles['ChineseH2']))
    story.append(Paragraph("1. 进入 File → Settings → Keymap", styles['ChineseBody']))
    story.append(Paragraph("2. 在搜索框中输入 渣渣逍", styles['ChineseBody']))
    story.append(Paragraph("3. 右键点击动作名称，选择 Add Keyboard Shortcut", styles['ChineseBody']))
    story.append(Paragraph("4. 输入你想要的快捷键组合，点击 OK 保存", styles['ChineseBody']))
    story.append(Spacer(1, 10))

    story.append(Paragraph("方法二：通过插件配置文件", styles['ChineseH2']))
    story.append(Paragraph("插件的默认快捷键定义在 src/main/resources/META-INF/plugin.xml 中", styles['ChineseBody']))
    story.append(Paragraph("修改快捷键语法：control alt m = Ctrl+Alt+M", styles['ChineseCode']))
    story.append(Paragraph("修改后需要重新构建插件：.\\gradlew.bat build", styles['ChineseCode']))
    story.append(Spacer(1, 20))

    # 技术架构
    story.append(Paragraph("技术架构", styles['ChineseH1']))
    story.append(Paragraph("整体架构采用 Java-Python 混合方案：", styles['ChineseBody']))
    story.append(Paragraph("• IntelliJ IDEA 插件（Java）：负责用户交互、UI 显示", styles['ChineseBullet']))
    story.append(Paragraph("• Python 子进程：负责加载翻译模型、执行推理", styles['ChineseBullet']))
    story.append(Paragraph("• NLLB-200 模型：Facebook 开源的多语言翻译模型", styles['ChineseBullet']))
    story.append(Paragraph("• 进程通信：通过 stdin/stdout 进行 JSON 格式的请求响应交互", styles['ChineseBullet']))
    story.append(Spacer(1, 20))

    # 调用流程
    story.append(Paragraph("调用流程", styles['ChineseH1']))
    story.append(Paragraph("气泡翻译流程（Ctrl+Alt+M）：", styles['ChineseH2']))
    story.append(Paragraph("1. 用户选中文本，按下 Ctrl+Alt+M", styles['ChineseBody']))
    story.append(Paragraph("2. TranslateAction 接收事件，检测语言方向", styles['ChineseBody']))
    story.append(Paragraph("3. 异步调用 TranslationService 执行翻译", styles['ChineseBody']))
    story.append(Paragraph("4. Python 服务器加载 NLLB 模型进行推理", styles['ChineseBody']))
    story.append(Paragraph("5. Java 端接收响应，通过 UUID 匹配请求", styles['ChineseBody']))
    story.append(Paragraph("6. 在 UI 线程显示气泡翻译结果", styles['ChineseBody']))
    story.append(Spacer(1, 10))

    story.append(Paragraph("替换翻译流程（Ctrl+Alt+R）：", styles['ChineseH2']))
    story.append(Paragraph("步骤 1-5 同气泡翻译流程", styles['ChineseBody']))
    story.append(Paragraph("6. 使用 WriteCommandAction 替换文档内容", styles['ChineseBody']))
    story.append(Paragraph("7. 支持 Ctrl+Z 撤销", styles['ChineseBody']))
    story.append(Spacer(1, 20))

    # 故障排除
    story.append(Paragraph("故障排除", styles['ChineseH1']))

    story.append(Paragraph("1. 翻译服务未就绪", styles['ChineseH2']))
    story.append(Paragraph("症状: 提示翻译服务未就绪", styles['ChineseBody']))
    story.append(Paragraph("检查步骤:", styles['ChineseBody']))
    story.append(Paragraph("  - 确认目录存在: dir D:\\portable-python\\", styles['ChineseCode']))
    story.append(Paragraph("  - 确认 Python 可用: python --version", styles['ChineseCode']))
    story.append(Paragraph("  - 确认模型存在: dir D:\\portable-python\\models\\nllb200\\", styles['ChineseCode']))
    story.append(Spacer(1, 10))

    story.append(Paragraph("2. 模型加载失败", styles['ChineseH2']))
    story.append(Paragraph("症状: Python 进程启动后立即退出", styles['ChineseBody']))
    story.append(Paragraph("解决方案: 确认模型文件完整 (model.safetensors 约 2.4GB)", styles['ChineseBody']))
    story.append(Spacer(1, 10))

    story.append(Paragraph("3. 替换翻译报错", styles['ChineseH2']))
    story.append(Paragraph("症状: Must not change document outside command", styles['ChineseBody']))
    story.append(Paragraph("解决方案: 已使用 WriteCommandAction 修复，重新安装插件", styles['ChineseBody']))
    story.append(Spacer(1, 20))

    # 版本信息
    story.append(Paragraph("版本信息", styles['ChineseH1']))
    versions = [
        ["组件", "版本", "说明"],
        ["插件", "1.0.0", "初始版本"],
        ["NLLB 模型", "200-distilled-600M", "Facebook 多语言翻译模型"],
        ["Python", "3.14.3", "便携版"],
        ["transformers", "4.57.6", "HuggingFace"],
        ["torch", "2.10.0+cpu", "PyTorch CPU 版"],
    ]
    table = Table(versions, colWidths=[4*cm, 5*cm, 7*cm])
    table.setStyle(create_table_style())
    story.append(table)
    story.append(Spacer(1, 20))

    # 许可证
    story.append(Paragraph("许可证", styles['ChineseH1']))
    story.append(Paragraph("MIT License", styles['ChineseBody']))
    story.append(Spacer(1, 20))

    # 联系方式
    story.append(Paragraph("联系方式", styles['ChineseH1']))
    story.append(Paragraph("作者: xiao", styles['ChineseBody']))
    story.append(Paragraph("GitHub: https://github.com/isyangxiao/IDEATran-Xiao", styles['ChineseBody']))

    # 构建 PDF
    doc.build(story)
    print("PDF 生成完成: D:/work/PROJECT/IDEATran-Xiao/渣渣逍翻译插件使用说明.pdf")

if __name__ == "__main__":
    build_pdf()
