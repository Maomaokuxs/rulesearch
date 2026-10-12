"""盘搜搜 (www.pansoso.com) HTML 解析。

结果容器 div.pss，标题链接 h2 > a，描述 div.des 形如：
"由网友 zhi****ish 上传百度网盘公开分享，文件大小为 2.60M"
提取网盘类型和文件大小。
"""
import re

from bs4 import BeautifulSoup

_DES_RE = re.compile(r"上传(.+?)公开分享，文件大小为\s*(.+)")

BASE = "https://www.pansoso.com"


def parse(html: str, keyword: str, rule) -> list[dict]:
    soup = BeautifulSoup(html, "html.parser")
    out = []
    for div in soup.select("div.pss"):
        a = div.select_one("h2 > a")
        if not a:
            continue
        title = a.get_text(strip=True)
        href = a.get("href", "") or ""
        if not title or not href:
            continue
        if href.startswith("/"):
            href = BASE + href
        size = ""
        des = div.select_one("div.des")
        if des:
            m = _DES_RE.search(des.get_text(strip=True))
            if m:
                pan_type, size = m.group(1).strip(), m.group(2).strip()
                title = f"{title} [{pan_type}]"
        out.append({"title": title, "url": href, "size": size})
    return out
