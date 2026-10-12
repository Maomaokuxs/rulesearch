"""爱盼 (aipan.me)：影视资源站，search API 取条目，再调 detail API 取磁链/网盘链接。"""
import json

from engine.http import new_session


def _detail(session, movie_id):
    try:
        r = session.get(
            f"https://www.aipan.me/api/movies/detail/{movie_id}", timeout=20
        )
        r.raise_for_status()
        return r.json()
    except Exception:  # noqa: BLE001
        return {}


def parse(html: str, keyword: str, rule) -> list[dict]:
    try:
        data = json.loads(html)
    except Exception:  # noqa: BLE001
        return []
    movies = data.get("movies") or []
    if not movies:
        return []
    session = new_session()
    out = []
    for m in movies[:10]:  # 限制详情请求数，避免太慢
        mid = m.get("id")
        title = (m.get("title") or "").strip()
        if not mid or not title:
            continue
        d = _detail(session, mid)
        resources = (d.get("resources") or []) + (d.get("panLinks") or [])
        for res in resources:
            url = (res.get("url") or "").strip()
            if not url:
                continue
            kind = (res.get("kind") or "").strip()
            label = (res.get("label") or "").strip()
            t = title
            if label:
                t += f" [{label}]"
            elif kind and kind != "magnet":
                t += f" [{kind}]"
            pwd = res.get("password") or ""
            if pwd:
                t += f"（提取码：{pwd}）"
            out.append(
                {
                    "title": t,
                    "url": url,
                    "time": str(m.get("year") or ""),
                }
            )
    return out
