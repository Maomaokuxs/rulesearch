"""从 rules/*.yaml 加载并校验规则。"""
from __future__ import annotations

import glob
import os
import sys

import yaml

from .models import Rule

_REQUIRED_TOP = ("name", "request", "parse")


def _ensure_hooks_on_path() -> None:
    # hooks/ 目录允许规则用 "module:func" 挂 Python 解析钩子
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    hooks_dir = os.path.join(root, "hooks")
    if os.path.isdir(hooks_dir) and hooks_dir not in sys.path:
        sys.path.insert(0, hooks_dir)


def validate(data: dict, path: str) -> Rule:
    if not isinstance(data, dict):
        raise ValueError("规则必须是 YAML 映射")
    for key in _REQUIRED_TOP:
        if key not in data:
            raise ValueError(f"缺少必要字段: {key}")
    req = data["request"]
    if "url" not in req:
        raise ValueError("request.url 必填")
    if "{keyword}" not in req["url"]:
        raise ValueError("request.url 必须包含 {keyword} 占位符")
    parse = data["parse"]
    if "list" not in parse or "fields" not in parse:
        raise ValueError("parse.list / parse.fields 必填")
    fields = parse["fields"]
    if "title" not in fields or "url" not in fields:
        raise ValueError("parse.fields 必须定义 title 和 url")
    return Rule(
        name=data["name"],
        type=data.get("type", "pan"),
        enabled=data.get("enabled", True),
        request=req,
        parse=parse,
        hook=data.get("hook"),
        path=path,
    )


def load_rules(rules_dir: str) -> tuple[list[Rule], list[str]]:
    """返回 (启用的规则, 加载错误列表)。"""
    _ensure_hooks_on_path()
    rules: list[Rule] = []
    errors: list[str] = []
    patterns = (
        glob.glob(os.path.join(rules_dir, "*.yaml"))
        + glob.glob(os.path.join(rules_dir, "*.yml"))
    )
    for path in sorted(patterns):
        try:
            with open(path, encoding="utf-8") as f:
                data = yaml.safe_load(f)
            rule = validate(data, path)
            if rule.enabled:
                rules.append(rule)
        except Exception as e:  # noqa: BLE001 —— 单条规则坏了不影响其他规则
            errors.append(f"{os.path.basename(path)}: {e}")
    return rules, errors
