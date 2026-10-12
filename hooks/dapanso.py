"""大盘搜 (dapanso.com)：夸克/阿里/百度/UC/迅雷等网盘聚合搜索。

搜索页直出网盘分享链接：div.item > a.title 取标题，
div.content-info 内 a[href] 即各网盘真实地址（一项可能有多个）。
"""
from bs4 import BeautifulSoup

PAN_HINTS = (
    "pan.baidu.com",
    "aliyundrive.com",
    "alipan.com",
    "pan.quark.cn",
    "quark.cn",
    "drive.uc.cn",
    "uc.cn",
    "115.com",
    "115cdn.com",
    "pan.xunlei.com",
    "xunlei.com",
    "123pan.com",
    "123pan.cn",
    "cloud.189.cn",
    "yun.139.com",
    "pan.139.com",
    "caiyun.139.com",
    "pikpak",
    "mypikpak.com",
    "lanzou",
    "lanzn.com",
    "lanzoui.com",
    "lanzoux.com",
    "tianyi",
    "189.cn",
)


def _is_pan(url: str) -> bool:
    u = url.lower()
    return u.startswith("http") and any(h in u for h in PAN_HINTS)


def parse(html: str, keyword: str, rule) -> list[dict]:
    soup = BeautifulSoup(html, "html.parser")
    out = []
    for item in soup.select("div.item"):
        title_el = item.select_one("a.title")
        title = title_el.get_text(strip=True) if title_el else ""
        if not title:
            continue
        for a in item.select("div.content-info a[href]"):
            url = (a.get("href") or "").strip()
            if _is_pan(url):
                out.append({"title": title, "url": url})
    return out
