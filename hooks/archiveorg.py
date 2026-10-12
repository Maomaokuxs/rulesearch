"""Internet Archive：开放典籍/影视/软件的合法资源，torrent 直链。"""
import json
from urllib.parse import quote_plus


def _human_size(n) -> str:
    try:
        v = int(n)
    except (TypeError, ValueError):
        return ""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if v < 1024 or unit == "TB":
            return f"{v:.1f}{unit}" if unit != "B" else f"{v}B"
        v /= 1024
    return ""


def parse(html: str, keyword: str, rule) -> list[dict]:
    data = json.loads(html)
    docs = (data.get("response") or {}).get("docs") or []
    out = []
    for d in docs:
        ident = (d.get("identifier") or "").strip()
        title = d.get("title") or ident
        if isinstance(title, list):
            title = title[0] if title else ident
        if not ident:
            continue
        url = f"https://archive.org/download/{ident}/{ident}_archive.torrent"
        out.append(
            {
                "title": str(title),
                "url": url,
                "time": str(d.get("date") or "")[:10],
            }
        )
    return out
