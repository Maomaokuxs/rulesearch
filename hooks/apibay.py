"""apibay.org：The Pirate Bay 官方 JSON API 解析。"""
import json
from urllib.parse import quote_plus


def _human_size(n: str) -> str:
    try:
        v = int(n)
    except (TypeError, ValueError):
        return ""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if v < 1024 or unit == "TB":
            return f"{v:.1f}{unit}" if unit != "B" else f"{v}B"
        v /= 1024
    return ""


def _fmt_time(ts: str) -> str:
    try:
        import datetime
        return datetime.datetime.fromtimestamp(int(ts)).strftime("%Y-%m-%d")
    except (TypeError, ValueError):
        return ""


def parse(html: str, keyword: str, rule) -> list[dict]:
    data = json.loads(html)
    out = []
    for it in data if isinstance(data, list) else []:
        ih = (it.get("info_hash") or "").strip()
        name = (it.get("name") or "").strip()
        if not ih or not name:
            continue
        magnet = f"magnet:?xt=urn:btih:{ih}&dn={quote_plus(name)}"
        title = name
        try:
            seeders = int(it.get("seeders") or 0)
            if seeders:
                title += f" [↑{seeders}]"
        except (TypeError, ValueError):
            pass
        out.append(
            {
                "title": title,
                "url": magnet,
                "size": _human_size(it.get("size", "")),
                "time": _fmt_time(it.get("added", "")),
            }
        )
    return out
