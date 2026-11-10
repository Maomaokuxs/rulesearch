"""萌番组 (bangumi.moe) JSON API 解析。"""
import datetime
import json


def _fmt_time(iso: str) -> str:
    try:
        return datetime.datetime.fromisoformat(
            iso.replace("Z", "+00:00")
        ).strftime("%Y-%m-%d")
    except (TypeError, ValueError):
        return ""


def parse(html: str, keyword: str, rule) -> list[dict]:
    data = json.loads(html)
    out = []
    for it in data.get("torrents") or []:
        title = (it.get("title") or "").strip()
        magnet = (it.get("magnet") or "").strip()
        if not title or not magnet:
            continue
        out.append(
            {
                "title": title,
                "url": magnet,
                "size": (it.get("size") or "").strip(),
                "time": _fmt_time(it.get("publish_time") or ""),
            }
        )
    return out
