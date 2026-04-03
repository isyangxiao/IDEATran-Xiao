#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
渣渣逍 - Python 翻译服务器
作者: xiao

功能说明:
- 加载 NLLB-200 翻译模型
- 处理 stdin 输入，stdout 输出
- 支持请求-响应匹配（通过 ID）
- 翻译日志记录

调用方式:
    python translation_server.py <model_path>

输入格式 (JSON):
    {"type": "translate", "text": "要翻译的文本", "source_lang": "en", "target_lang": "zh", "id": "请求ID"}

输出格式 (JSON):
    {"success": true, "result": "翻译结果", "id": "请求ID"}
    或
    {"success": false, "error": "错误信息", "id": "请求ID"}
"""

import sys
import json
import os
from datetime import datetime

# 强制 UTF-8 编码（Windows 默认可能是 GBK）
sys.stdin.reconfigure(encoding='utf-8')
sys.stdout.reconfigure(encoding='utf-8')

# 禁用 tokenizers 并行警告
os.environ["TOKENIZERS_PARALLELISM"] = "false"

# 语言代码映射
LANG_CODES = {
    "en": "eng_Latn",
    "zh": "zho_Hans",
}

# 模型缓存
_model = None
_tokenizer = None

# 日志文件路径
LOG_FILE = "D:/portable-python/translation.log"


def log_translation(text, source_lang, target_lang, result, success=True, error_msg=None):
    """
    记录翻译请求和结果到日志文件

    Args:
        text: 原文
        source_lang: 源语言
        target_lang: 目标语言
        result: 翻译结果
        success: 是否成功
        error_msg: 错误信息（失败时）
    """
    try:
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
        log_entry = {
            "timestamp": timestamp,
            "input": text,
            "source_lang": source_lang,
            "target_lang": target_lang,
            "success": success
        }
        if success:
            log_entry["output"] = result
        else:
            log_entry["error"] = error_msg

        with open(LOG_FILE, "a", encoding="utf-8") as f:
            f.write(json.dumps(log_entry, ensure_ascii=False) + "\n")
    except Exception as e:
        print(f"日志写入错误: {e}", file=sys.stderr)


def get_model(model_path):
    """
    加载或获取缓存的 NLLB 翻译模型

    Args:
        model_path: 模型本地路径

    Returns:
        (model, tokenizer) 元组
    """
    global _model, _tokenizer

    if _model is None:
        from transformers import AutoModelForSeq2SeqLM, AutoTokenizer
        _tokenizer = AutoTokenizer.from_pretrained(
            model_path,
            local_files_only=True
        )
        _model = AutoModelForSeq2SeqLM.from_pretrained(
            model_path,
            local_files_only=True
        )
    return _model, _tokenizer


def translate(text, source_lang="en", target_lang="zh", model_path=None):
    """
    使用 NLLB 模型翻译文本

    Args:
        text: 要翻译的文本
        source_lang: 源语言代码 (en/zh)
        target_lang: 目标语言代码 (en/zh)
        model_path: 模型路径

    Returns:
        翻译结果文本
    """
    model, tokenizer = get_model(model_path)

    src_lang = LANG_CODES.get(source_lang, source_lang)
    tgt_lang = LANG_CODES.get(target_lang, target_lang)

    # 设置源语言
    tokenizer.src_lang = src_lang

    # 获取目标语言的 token ID
    tgt_lang_id = tokenizer.convert_tokens_to_ids(tgt_lang)

    # Tokenize 输入文本
    inputs = tokenizer(text, return_tensors="pt", padding=True)

    # 强制指定目标语言
    inputs["forced_bos_token_id"] = tgt_lang_id

    # 生成翻译
    translated = model.generate(
        **inputs,
        max_length=256,
        num_beams=4,
        early_stopping=True,
    )

    # 解码翻译结果
    result = tokenizer.decode(translated[0], skip_special_tokens=True)

    return result


def main():
    """
    主循环 - 从 stdin 读取请求，stdout 输出响应
    """
    # 从命令行参数或环境变量获取模型路径
    model_path = None
    if len(sys.argv) > 1:
        model_path = sys.argv[1]
    elif "TRAN_MODEL_PATH" in os.environ:
        model_path = os.environ["TRAN_MODEL_PATH"]

    if model_path is None:
        error_response = {
            "success": False,
            "error": "未提供模型路径，请通过命令行参数或 TRAN_MODEL_PATH 环境变量指定"
        }
        print(json.dumps(error_response, ensure_ascii=False), flush=True)
        return

    # 确认模型路径配置成功
    print(json.dumps({
        "success": True,
        "result": f"模型路径: {model_path}"
    }, ensure_ascii=False), flush=True)

    # 主循环
    while True:
        try:
            line = sys.stdin.readline()
            if not line:
                break

            request = json.loads(line.strip())

            if request.get("type") == "translate":
                text = request.get("text", "")
                source_lang = request.get("source_lang", "en")
                target_lang = request.get("target_lang", "zh")
                req_id = request.get("id", "")

                try:
                    result = translate(text, source_lang, target_lang, model_path)
                    response = {
                        "success": True,
                        "result": result,
                        "id": req_id
                    }
                    log_translation(text, source_lang, target_lang, result)
                except Exception as e:
                    response = {
                        "success": False,
                        "error": str(e),
                        "id": req_id
                    }
                    log_translation(text, source_lang, target_lang, None, success=False, error_msg=str(e))

                print(json.dumps(response, ensure_ascii=False), flush=True)

            elif request.get("type") == "quit":
                break

        except Exception as e:
            error_response = {
                "success": False,
                "error": str(e)
            }
            print(json.dumps(error_response, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    main()
