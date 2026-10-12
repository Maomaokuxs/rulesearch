"""AnimeTosho：动漫资源 RSS，magnet 直链（见 hooks 说明）。"""
import re
import warnings

from bs4 import BeautifulSoup, XMLParsedAsHTMLWarning

warnings.filterwarnings("ignore", category=XMLParsedAsHTMLWarning)


def parse(html: str, keyword: str, rule) -> list[dict]:
    soup = BeautifulSoup(html, "html.parser")
    out = []
    for item in soup.select("item"):
        title_el = item.select_one("title")
        title = title_el.get_text(strip=True) if title_el else ""
        desc_el = item.select_one("description")
        desc = desc_el.get_text() if desc_el else ""
        m = re.search(r"magnet:\?xt=urn:btih:[a-zA-Z0-9]+[^\"<>\s]*", desc)
        if not title or not m:
            continue
        magnet = m.group(0).replace("&amp;", "&")
        size_m = re.search(r"Total Size</strong>:\s*([^<]+)", desc)
        out.append(
            {
                "title": title,
                "url": magnet,
                "size": size_m.group(1).strip() if size_m else "",
            }
        )
    return out
