#!/usr/bin/env python3
"""从 rules/*.yaml 生成 rules/manifest.json（规则清单）。

清单是 App 检查规则更新的唯一入口：App 先拉这一个文件，就能发现
新增规则、只下载 version 变了的条目，不用逐条去试每条规则的 update_url。

用法（在仓库根目录）：
    python3 gen_manifest.py            # 重新生成 rules/manifest.json
    python3 gen_manifest.py --check    # 只校验：清单与当前 yaml 不一致时 exit 1（供 CI 用）

改完规则后跑一遍本脚本再提交；CI 可用 --check 挡住忘记更新清单的提交。
规则文件本身不用动：name / type / version / update_url 都从 yaml 里读。
"""

from __future__ import annotations

import argparse
import datetime
import glob
import json
import os
import sys

import yaml

ROOT = os.path.dirname(os.path.abspath(__file__))
RULES_DIR = os.path.join(ROOT, "rules")
MANIFEST_PATH = os.path.join(RULES_DIR, "manifest.json")
MANIFEST_URL = (
    "https://raw.githubusercontent.com/Maomaokuxs/rulesearch"
    "/master/rules/manifest.json"
)


def build_manifest() -> dict:
    """扫描 rules/*.yaml，生成清单内容。"""
    rules = []
    for path in sorted(
        glob.glob(os.path.join(RULES_DIR, "*.yaml"))
        + glob.glob(os.path.join(RULES_DIR, "*.yml"))
    ):
        with open(path, encoding="utf-8") as f:
            data = yaml.safe_load(f) or {}
        name = data.get("name")
        if not name:
            print(f"跳过 {os.path.basename(path)}：缺少 name", file=sys.stderr)
            continue
        rules.append(
            {
                "name": name,
                "type": data.get("type", "pan"),
                "version": str(data.get("version") or ""),
                "update_url": data.get("update_url") or "",
                "file": os.path.basename(path),
            }
        )
    return {
        "version": 1,
        "updated_at": datetime.date.today().isoformat(),
        "manifest_url": MANIFEST_URL,
        "rules": rules,
    }


def dump(manifest: dict) -> str:
    return json.dumps(manifest, ensure_ascii=False, indent=2) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description="生成 rules/manifest.json")
    parser.add_argument(
        "--check", action="store_true", help="只校验清单是否最新，不一致时 exit 1"
    )
    args = parser.parse_args()

    manifest = build_manifest()
    new_text = dump(manifest)
    try:
        with open(MANIFEST_PATH, encoding="utf-8") as f:
            old_text = f.read()
    except OSError:
        old_text = None

    if args.check:
        if old_text == new_text:
            print(f"manifest.json 是最新的（{len(manifest['rules'])} 条规则）")
            return 0
        print(
            "manifest.json 已过期，请跑 python3 gen_manifest.py 重新生成",
            file=sys.stderr,
        )
        return 1

    with open(MANIFEST_PATH, "w", encoding="utf-8") as f:
        f.write(new_text)
    print(f"已生成 {MANIFEST_PATH}（{len(manifest['rules'])} 条规则）")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
