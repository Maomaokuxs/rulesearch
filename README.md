# RuleSearch（暂定名）

规则引擎式网盘 / 磁力聚合搜索。手机端独立运行，不依赖电脑。

## 架构

```
┌─ android/                 Android 壳（Kotlin + Compose + Miuix）
│   └─ Chaquopy 内嵌 Python → 直接调用 engine/
├─ engine/                  规则引擎核心（Python，手机/桌面共用）
│   ├─ models.py            SearchResult / Rule 数据模型
│   ├─ loader.py            从 rules/*.yaml 加载并校验规则
│   ├─ runner.py            并发执行规则、聚合去重
│   ├─ http.py              请求会话（UA/超时/编码）
│   └─ __main__.py          CLI：python -m engine search "关键词"
├─ rules/                   规则文件（YAML），自维护
│   └─ README.md            规则格式说明
└─ hooks/                   可选：复杂站点的 Python 解析钩子
```

- 规则是 YAML 描述（搜索 URL 模板 + CSS 选择器 + 字段映射），复杂站点可挂 Python 钩子。
- Android 端：Chaquopy 把 `engine/` 打进 APK，Kotlin 薄 UI 壳经 `engine_bridge` 调用。
- 桌面端：同一套 `engine/`，UI 可用 PySide6 或直接跑 CLI。

## 快速开始（桌面验证引擎）

```bash
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
python -m engine search "关键词"
```

## 内置规则（2026-10-03 全部实测出结果）

| 规则文件 | 站点 | 类型 | 方式 |
|---|---|---|---|
| `pan-panws.yaml` | 盘万搜 panws.net | 网盘 | hook 解码 `/link/{base64url}` 跳转 |
| `pan-pansou.yaml` | PanSou pansou.app | 网盘 | JSON API（开源，自带提取码/日期） |
| `magnet-nyaa.yaml` | Nyaa nyaa.si | 磁力 | 纯 CSS 选择器（含大小/时间） |
| `magnet-bitsearch.yaml` | BitSearch bitsearch.to | 磁力 | 纯 CSS 选择器 |

测试：`python -m engine search "三体" --rules rules/` → 120 条，0 错误。

## 状态

- [x] 引擎核心 + CLI（本机实测通过）
- [x] 4 条示例规则（真实站点实测通过）
- [x] Android 壳（Chaquopy + Miuix，待在 Fedora 上构建）
- [ ] 桌面 UI

## 维护指南

1. **站点挂了 / 结构变了**：先 `curl -A "Mozilla/5.0 ..." "搜索URL"` 看返回，
   确认是反爬、改版还是站点死了，再改选择器或换 hook。
2. **新增站点**：在 `rules/` 加一个 yaml，桌面 `python -m engine search` 验证出结果，
   再走 `android/` 重新构建 APK（规则构建时打包，见 BUILD.md）。
3. **被限流**：规则是并发请求的，给单站加 `timeout`、或在 `runner.py` 里加节流。
4. **已知脆弱点**：盘万搜的 `/link/{base64url}` 跳转格式、nyaa 的表格列顺序、
   bitsearch 的卡片 class 名——站点一改版这些最先坏，优先检查。
5. **Miuix**：用 git submodule 锁在 `v0.9.4`（它不在 Maven Central 上，
   不要尝试从 Central 拉）。
