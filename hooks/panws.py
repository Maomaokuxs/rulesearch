"""盘万搜 (panws.net)：解码 /link/{base64url} 站内跳转，得到真实网盘链接。"""
import base64
from urllib.parse import unquote

from bs4 import BeautifulSoup


def _decode_link(href: str) -> str:
    # href 形如 /link/{base64url}；base64url 解码后是百分号编码的真实 URL
    token = href.rsplit("/", 1)[-1]
    token += "=" * (-len(token) % 4)
    decoded = base64.urlsafe_b64decode(token).decode("utf-8", errors="replace")
    return unquote(decoded)


def parse(html: str, keyword: str, rule) -> list[dict]:
    soup = BeautifulSoup(html, "html.parser")
    out = []
    for a in soup.select("a.card"):
        name_el = a.select_one("div.left > div.name")
        title = name_el.get_text(strip=True) if name_el else ""
        href = a.get("href", "") or ""
        if not title or not href:
            continue
        try:
            url = _decode_link(href)
        except Exception:
            continue
        # 网盘类型：img.pan-logo 的 src 如 /drive/quark.webp
        pan = ""
        logo = a.select_one("img.pan-logo")
        if logo and logo.get("src"):
            pan = str(logo["src"]).rsplit("/", 1)[-1].split(".")[0]
        out.append(
            {
                "title": f"{title} [{pan}]" if pan else title,
                "url": url,
            }
        )
    return out
