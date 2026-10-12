"""Chaquopy 入口：给 Kotlin 层暴露搜索 / 规则列表 / 规则管理 / 单规则测试。

rules/ 目录与本文件同级（构建时由 gradle syncPython 任务从仓库根目录同步）。
用户自建规则放在 Kotlin 层传入的 user_rules_dir，同名时覆盖内置规则。
"""
import json
import os
import sys
import time

_HERE = os.path.dirname(os.path.abspath(__file__))
if _HERE not in sys.path:
    sys.path.insert(0, _HERE)

import yaml

from engine.http import new_session
from engine.loader import load_rules, validate
from engine.runner import run_rule, search_all

_RULES_DIR = os.path.join(_HERE, "rules")
_user_rules_dir = None
_rules_cache = None


def init(user_rules_dir):
    """Kotlin 层启动时调用一次，传入 app filesDir 下的 rules 目录。"""
    global _user_rules_dir
    _user_rules_dir = user_rules_dir
    reload_rules()


def _builtin_blocklist_path():
    """内置过滤词表（打包在 APK 内，用户不可见、不可改）。"""
    return os.path.join(_HERE, "blocked_words.txt")


def _user_blocklist_path():
    """用户自建过滤词表（内容过滤页增删）。"""
    if _user_rules_dir:
        return os.path.join(os.path.dirname(_user_rules_dir), "blocked_words.txt")
    return None


def _load_blocklist():
    """合并内置 + 用户两份过滤词表，去重。"""
    words = []
    seen = set()
    for p in (_builtin_blocklist_path(), _user_blocklist_path()):
        if not p:
            continue
        try:
            with open(p, encoding="utf-8") as f:
                for ln in f:
                    w = ln.strip()
                    if w and not w.startswith("#") and w not in seen:
                        seen.add(w)
                        words.append(w)
        except OSError:
            pass
    return words


def get_builtin_blocklist():
    """返回内置过滤词表 JSON（供 Kotlin 界面过滤显示用，用户不可见）。"""
    words = []
    try:
        with open(_builtin_blocklist_path(), encoding="utf-8") as f:
            for ln in f:
                w = ln.strip()
                if w and not w.startswith("#"):
                    words.append(w)
    except OSError:
        pass
    return json.dumps(words, ensure_ascii=False)


def reload_rules():
    """导入/删除/更新规则后调用，清空缓存。"""
    global _rules_cache
    _rules_cache = None
    return list_rules()


def _rules():
    global _rules_cache
    if _rules_cache is None:
        rules, _ = load_rules(_RULES_DIR)
        if _user_rules_dir and os.path.isdir(_user_rules_dir):
            user_rules, _ = load_rules(_user_rules_dir)
            names = {r.name for r in user_rules}
            # 同名用户规则覆盖内置规则
            rules = user_rules + [r for r in rules if r.name not in names]
        _rules_cache = rules
    return _rules_cache


def _is_user_rule(rule):
    return bool(
        _user_rules_dir
        and os.path.abspath(rule.path).startswith(os.path.abspath(_user_rules_dir))
    )


def _rule_meta(rule):
    """读规则文件的 version / update_url（可选字段）。"""
    try:
        with open(rule.path, encoding="utf-8") as f:
            data = yaml.safe_load(f) or {}
        return {
            "version": str(data.get("version") or ""),
            "update_url": data.get("update_url") or "",
        }
    except Exception:  # noqa: BLE001
        return {"version": "", "update_url": ""}


def list_rules():
    """返回所有规则的 name/type/source/version，供 Kotlin 层做搜索源管理。"""
    items = []
    for r in _rules():
        meta = _rule_meta(r)
        items.append(
            {
                "name": r.name,
                "type": r.type,
                "source": "user" if _is_user_rule(r) else "builtin",
                "version": meta["version"],
                "has_update_url": bool(meta["update_url"]),
            }
        )
    return json.dumps(items, ensure_ascii=False)


def search(keyword, only_names_json=None):
    """only_names_json：只跑这些规则（JSON 数组字符串；None 或空表示全部）。

    注：不用直接传 list——Chaquopy 的 callAttr 不会自动把 Java List
    转成 Python list（'ArrayList' object is not iterable），走 JSON 最稳。
    """
    rules = _rules()
    if only_names_json:
        names = set(json.loads(only_names_json))
        rules = [r for r in rules if r.name in names]
    results, errors, filtered = search_all(
        keyword, rules, blocked_words=_load_blocklist()
    )
    return json.dumps(
        {"results": results, "errors": errors, "filtered": filtered},
        ensure_ascii=False,
    )


def test_rule(name, keyword):
    """单条规则试跑：返回 {results, error, elapsed_ms}，永不抛异常。

    失败会自动重试一次：网站偶发的超时/503/连接重置很常见，
    避免"搜得到但检测显示异常"的误报。
    """
    rule = next((r for r in _rules() if r.name == name), None)
    if rule is None:
        return json.dumps(
            {"results": [], "error": "找不到规则", "elapsed_ms": 0},
            ensure_ascii=False,
        )
    t0 = time.time()
    results, error = run_rule(rule, keyword)
    retried = False
    if error:
        time.sleep(2)
        results, error = run_rule(rule, keyword)
        retried = True
    elapsed_ms = int((time.time() - t0) * 1000)
    return json.dumps(
        {
            "results": [r.to_dict() for r in results],
            "error": error,
            "elapsed_ms": elapsed_ms,
            "retried": retried and error is None,
        },
        ensure_ascii=False,
    )


def validate_rule(yaml_text):
    """导入前校验 YAML，返回 {ok, name, type} 或 {ok:false, error}。"""
    try:
        data = yaml.safe_load(yaml_text)
        rule = validate(data, "<import>")
        return json.dumps(
            {"ok": True, "name": rule.name, "type": rule.type},
            ensure_ascii=False,
        )
    except Exception as e:  # noqa: BLE001
        return json.dumps(
            {"ok": False, "error": f"{type(e).__name__}: {e}"},
            ensure_ascii=False,
        )


def get_rule_yaml(name):
    """取规则原文（导出用），找不到返回空串。"""
    rule = next((r for r in _rules() if r.name == name), None)
    if rule is None:
        return ""
    try:
        with open(rule.path, encoding="utf-8") as f:
            return f.read()
    except OSError:
        return ""


def check_updates():
    """对带 update_url 的规则检查新版本，返回 [{name, current_version,
    new_version, new_yaml} | {name, error}]。"""
    out = []
    for r in _rules():
        meta = _rule_meta(r)
        url = meta["update_url"]
        if not url:
            continue
        try:
            resp = new_session().get(url, timeout=15)
            resp.raise_for_status()
            data = yaml.safe_load(resp.text)
            rule = validate(data, "<update>")
            new_ver = str(data.get("version") or "")
            if rule.name != r.name:
                out.append({"name": r.name, "error": "远端规则名不一致，跳过"})
            elif new_ver and new_ver != meta["version"]:
                out.append(
                    {
                        "name": r.name,
                        "current_version": meta["version"],
                        "new_version": new_ver,
                        "new_yaml": resp.text,
                    }
                )
        except Exception as e:  # noqa: BLE001
            out.append({"name": r.name, "error": f"{type(e).__name__}: {e}"})
    return json.dumps(out, ensure_ascii=False)


def delete_user_rule(name):
    """删除用户自建规则（仅 user 来源），返回是否成功。"""
    rule = next((r for r in _rules() if r.name == name), None)
    if rule is None or not _is_user_rule(rule):
        return False
    try:
        os.remove(rule.path)
    except OSError:
        return False
    reload_rules()
    return True


def apply_rule_update(name, new_yaml):
    """写入新版规则 YAML 并重载，返回是否成功。"""
    rule = next((r for r in _rules() if r.name == name), None)
    if rule is None:
        return False
    try:
        validate(yaml.safe_load(new_yaml), "<update>")
        with open(rule.path, "w", encoding="utf-8") as f:
            f.write(new_yaml)
    except Exception:  # noqa: BLE001
        return False
    reload_rules()
    return True
