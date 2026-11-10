# 规则格式说明

规则是 YAML 文件，放在 `rules/` 目录下，文件名任意（`*.yaml` / `*.yml`）。

## 最小示例

```yaml
name: "示例盘搜"          # 规则名，会显示在结果的来源里
type: pan                # pan（网盘）| magnet（磁力）
enabled: true            # false 则跳过

request:
  url: "https://example.com/s?q={keyword}"  # {keyword} 会被 URL 编码替换
  method: GET            # GET | POST，默认 GET
  timeout: 15            # 秒
  # encoding: utf-8      # 不写则自动检测
  # headers:             # 需要时覆盖默认 UA
  #   Cookie: "..."

parse:
  list: "div.result"     # CSS 选择器：每条结果的容器
  url_base: "https://example.com"  # 相对链接补全用，可省略
  fields:
    title: {select: "a.t", attr: text}   # attr: text 取文本，其他值取该属性
    url:   {select: "a.t", attr: href}   # title/url 必填
    size:  {select: "span.s", attr: text} # 可选字段
    time:  {select: "span.t", attr: text} # 可选字段

# hook: "mymodule:parse_func"  # 复杂站点：用 hooks/mymodule.py 的 parse_func(html, keyword, rule)
```

## 字段说明

- `request.url` 必须包含 `{keyword}`，引擎会做 `quote_plus` 编码后替换。
- `parse.fields` 的每个字段：`select` 是相对 `list` 容器的 CSS 选择器（省略则直接用容器本身），`attr: text` 取文本内容，其他如 `href`/`src` 取对应属性。找不到的字段返回空字符串，不报错。
- 单条结果解析失败会被跳过，不影响整页；整条规则请求失败会记入 `errors`，不影响其他规则。

## 规则更新（清单）

`rules/manifest.json` 是规则清单，App 检查更新时先拉这一个文件，再按需下载
变了的条目：清单里有、本地没有的是新规则，清单里 `version` 比本地新的是更新，
清单里没有、本地有的不动。

- 改规则后必须升 `version`（如 1 → 2），否则 App 认为无更新。
- 改完规则跑一遍 `python3 gen_manifest.py`（仓库根目录）重新生成清单再提交；
  CI 可用 `python3 gen_manifest.py --check` 校验清单是否最新。
- 清单里的 `update_url` 指向 GitHub raw 上的对应 yaml，App 直接从那里下载。

## 调试

```bash
python -m engine search "测试词" --rules rules/
```

输出 JSON：`results` 是聚合去重后的结果，`errors` 列出加载/请求失败的原因。规则挂了先看 `errors`。
