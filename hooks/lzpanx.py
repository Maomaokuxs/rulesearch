"""懒盘搜索 (lzpanx.com)：夸克/阿里/迅雷网盘聚合搜索。

流程：列表页 /search?q={kw} -> 每条进详情页 /doc/{id} ->
a.jump-link 即真实网盘分享地址。详情页并发抓取（默认取前 12 条）。
"""
from concurrent.futures import ThreadPoolExecutor
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup

BASE = "https://www.lzpanx.com"
UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)
DETAIL_LIMIT = 12


def _fetch_detail(href: str) -> str | None:
    """抓详情页，返回真实网盘链接，失败返回 None。"""
    try:
        r = requests.get(href, headers={"User-Agent": UA}, timeout=10)
        soup = BeautifulSoup(r.text, "html.parser")
        a = soup.select_one("a.jump-link[href]")
        if a:
            url = (a.get("href") or "").strip()
            if url.startswith("http"):
                return url
    except Exception:  # noqa: BLE001
        return None
    return None


def parse(html: str, keyword: str, rule) -> list[dict]:
    soup = BeautifulSoup(html, "html.parser")
    items: list[tuple[str, str]] = []
    seen = set()
    for a in soup.select('a[href^="/doc/"]'):
        href = a.get("href", "") or ""
        if href in seen:
            continue
        seen.add(href)
        title = a.get_text(strip=True)
        if not title or title == "详情页":
            continue
        items.append((title, urljoin(BASE, href)))
        if len(items) >= DETAIL_LIMIT:
            break
    if not items:
        return []
    out = []
    with ThreadPoolExecutor(max_workers=6) as ex:
        urls = list(ex.map(_fetch_detail, [h for _, h in items]))
    for (title, _), url in zip(items, urls):
        if url:
            out.append({"title": title, "url": url})
    return out
