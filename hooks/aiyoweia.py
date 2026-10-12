"""哎哟喂啊 (aiyoweia.com)：网盘资源聚合搜索。

流程：列表页 /search/{kw} -> 每条进详情页 .shtml ->
找 /down/xxx/{base64} 链接，尾部 base64 解码即真实网盘分享地址。
详情页并发抓取（默认取前 12 条）。
"""
import base64
from concurrent.futures import ThreadPoolExecutor
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup

BASE = "https://www.aiyoweia.com"
UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)
DETAIL_LIMIT = 12


def _decode_token(token: str) -> str:
    token = token.strip().rstrip("/")
    token += "=" * (-len(token) % 4)
    return base64.b64decode(token).decode("utf-8", errors="replace").strip()


def _fetch_detail(href: str) -> str | None:
    """抓详情页，返回真实网盘链接，失败返回 None。"""
    try:
        r = requests.get(href, headers={"User-Agent": UA}, timeout=10)
        r.encoding = "gb18030"  # 站点为 GBK 编码
        soup = BeautifulSoup(r.text, "html.parser")
        for a in soup.find_all("a", href=True):
            h = a["href"]
            if "/down/" in h:
                token = h.rstrip("/").rsplit("/", 1)[-1]
                try:
                    url = _decode_token(token)
                except Exception:  # noqa: BLE001
                    continue
                if url.startswith("http"):
                    return url
    except Exception:  # noqa: BLE001
        return None
    return None


def parse(html: str, keyword: str, rule) -> list[dict]:
    soup = BeautifulSoup(html, "html.parser")
    items: list[tuple[str, str]] = []
    for li in soup.select("li.catlist-li"):
        a = li.select_one("div.catlist-li-title > a[href]")
        if not a:
            continue
        href = a.get("href", "") or ""
        if not href.endswith(".shtml"):
            continue
        title = (a.get("title") or a.get_text(strip=True)).strip()
        if not title:
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
