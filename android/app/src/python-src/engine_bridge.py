"""Chaquopy 入口：给 Kotlin 层暴露 search(keyword) -> JSON 字符串。

rules/ 目录与本文件同级（构建时由 gradle syncPython 任务从仓库根目录同步）。
"""
import json
import os
import sys

_HERE = os.path.dirname(os.path.abspath(__file__))
if _HERE not in sys.path:
    sys.path.insert(0, _HERE)

from engine.loader import load_rules
from engine.runner import search_all

_RULES_DIR = os.path.join(_HERE, "rules")
_rules_cache = None


def _rules():
    global _rules_cache
    if _rules_cache is None:
        _rules_cache, _ = load_rules(_RULES_DIR)
    return _rules_cache


def search(keyword):
    results, errors = search_all(keyword, _rules())
    return json.dumps({"results": results, "errors": errors}, ensure_ascii=False)
