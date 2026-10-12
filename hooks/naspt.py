"""NASPT 开放 API：网盘资源聚合，JSON 直出分享链接。"""
import json


def parse(html: str, keyword: str, rule) -> list[dict]:
    try:
        data = json.loads(html)
    except Exception:  # noqa: BLE001
        return []
    if data.get("code") != 0:
        return []
    items = data.get("data") or []
    out = []
    for it in items:
        title = (it.get("title") or "").strip()
        dt = (it.get("datetime") or "")[:10]
        for link in it.get("links") or []:
            url = (link.get("url") or "").strip()
            if not url:
                continue
            t = title
            ltype = (link.get("type") or "").strip()
            if ltype:
                t += f" [{ltype}]"
            pwd = link.get("password") or ""
            if pwd:
                t += f"（提取码：{pwd}）"
            out.append({"title": t, "url": url, "time": dt})
    return out
