"""CLI：python -m engine search "关键词" [--rules DIR]"""
from __future__ import annotations

import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from engine.loader import load_rules  # noqa: E402
from engine.runner import search_all  # noqa: E402


def main() -> None:
    ap = argparse.ArgumentParser(prog="rulesearch", description="规则引擎：多源聚合搜索")
    sub = ap.add_subparsers(dest="cmd", required=True)

    s = sub.add_parser("search", help="执行聚合搜索")
    s.add_argument("keyword", help="搜索关键词")
    s.add_argument(
        "--rules",
        default=os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "rules"
        ),
        help="规则目录",
    )
    s.add_argument("--workers", type=int, default=8, help="并发数")

    args = ap.parse_args()
    rules, load_errors = load_rules(args.rules)
    results, errors = search_all(args.keyword, rules, max_workers=args.workers)
    print(
        json.dumps(
            {
                "keyword": args.keyword,
                "count": len(results),
                "results": results,
                "errors": load_errors + errors,
            },
            ensure_ascii=False,
            indent=2,
        )
    )


if __name__ == "__main__":
    main()
