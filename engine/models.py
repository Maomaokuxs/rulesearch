"""RuleSearch 规则引擎：数据模型。"""
from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class SearchResult:
    """一条搜索结果。"""

    title: str
    url: str
    size: str = ""
    time: str = ""
    source: str = ""  # 规则名
    kind: str = "pan"  # pan | magnet

    def to_dict(self) -> dict:
        return {
            "title": self.title,
            "url": self.url,
            "size": self.size,
            "time": self.time,
            "source": self.source,
            "kind": self.kind,
        }


@dataclass
class Rule:
    """一条搜索规则（由 loader 从 YAML 构建）。"""

    name: str
    type: str = "pan"
    enabled: bool = True
    request: dict = field(default_factory=dict)
    parse: dict = field(default_factory=dict)
    hook: str | None = None  # "module:func"，复杂站点用 Python 钩子解析
    path: str = ""
