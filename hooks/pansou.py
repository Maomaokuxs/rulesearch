"""PanSou (pansou.app) JSON API 解析。"""
import json


def parse(html: str, keyword: str, rule) -> list[dict]:
    data = json.loads(html)
    out = []
    groups = (data.get("data") or {}).get("merged_by_type") or {}
    for pan_type, items in groups.items():
        for it in items or []:
            url = it.get("url", "") or ""
            note = it.get("note", "") or ""
            if not url or not note:
                continue
            title = note
            pwd = it.get("password") or ""
            if pwd:
                title += f"（提取码：{pwd}）"
            out.append(
                {
                    "title": f"{title} [{pan_type}]",
                    "url": url,
                    "time": (it.get("datetime") or "")[:10],
                }
            )
    return out
