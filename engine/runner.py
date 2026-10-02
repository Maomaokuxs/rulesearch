"""并发执行规则并聚合结果。"""
from __future__ import annotations

import importlib
from concurrent.futures import ThreadPoolExecutor
from urllib.parse import quote_plus, urljoin

from bs4 import BeautifulSoup

from .http import new_session
from .models import Rule, SearchResult


def build_url(rule: Rule, keyword: str) -> str:
    return rule.request["url"].replace("{keyword}", quote_plus(keyword))


def _extract_field(item, spec: dict) -> str:
    sel = spec.get("select")
    attr = spec.get("attr", "text")
    target = item.select_one(sel) if sel else item
    if target is None:
        return ""
    if attr == "text":
        return target.get_text(strip=True)
    return (target.get(attr) or "").strip()


def _parse_html(rule: Rule, html: str) -> list[SearchResult]:
    soup = BeautifulSoup(html, "html.parser")
    base = rule.parse.get("url_base", "")
    fields = rule.parse["fields"]
    results: list[SearchResult] = []
    for el in soup.select(rule.parse["list"]):
        try:
            title = _extract_field(el, fields["title"])
            url = _extract_field(el, fields["url"])
            if not title or not url:
                continue
            if base:
                url = urljoin(base, url)
            results.append(
                SearchResult(
                    title=title,
                    url=url,
                    size=_extract_field(el, fields["size"]) if "size" in fields else "",
                    time=_extract_field(el, fields["time"]) if "time" in fields else "",
                    source=rule.name,
                    kind=rule.type,
                )
            )
        except Exception:
            continue  # 单条解析失败跳过，不中断整页
    return results


def _run_hook(rule: Rule, html: str, keyword: str) -> list[SearchResult]:
    mod_name, func_name = rule.hook.split(":")
    fn = getattr(importlib.import_module(mod_name), func_name)
    out: list[SearchResult] = []
    for d in fn(html, keyword, rule) or []:
        r = SearchResult(
            title=d.get("title", ""),
            url=d.get("url", ""),
            size=d.get("size", ""),
            time=d.get("time", ""),
            source=rule.name,
            kind=rule.type,
        )
        if r.title and r.url:
            out.append(r)
    return out


def run_rule(rule: Rule, keyword: str) -> tuple[list[SearchResult], str | None]:
    """执行单条规则，返回 (结果, 错误信息)。永不抛异常。"""
    try:
        req = rule.request
        resp = new_session(req.get("headers")).request(
            req.get("method", "GET").upper(),
            build_url(rule, keyword),
            timeout=req.get("timeout", 15),
            params=req.get("params"),
            data=req.get("data"),
        )
        resp.raise_for_status()
        encoding = req.get("encoding") or resp.apparent_encoding or "utf-8"
        html = resp.content.decode(encoding, errors="replace")
        if rule.hook:
            return _run_hook(rule, html, keyword), None
        return _parse_html(rule, html), None
    except Exception as e:  # noqa: BLE001
        return [], f"{rule.name}: {type(e).__name__}: {e}"


def search_all(
    keyword: str, rules: list[Rule], max_workers: int = 8
) -> tuple[list[dict], list[str]]:
    """并发跑所有规则，返回 (去重后的结果 dict 列表, 错误列表)。"""
    results: list[SearchResult] = []
    errors: list[str] = []
    with ThreadPoolExecutor(max_workers=max_workers) as pool:
        for res, err in pool.map(lambda r: run_rule(r, keyword), rules):
            results.extend(res)
            if err:
                errors.append(err)
    seen: set[str] = set()
    uniq: list[dict] = []
    for r in results:
        if r.url not in seen:
            seen.add(r.url)
            uniq.append(r.to_dict())
    return uniq, errors
