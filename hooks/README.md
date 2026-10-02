# hooks/

复杂站点的 Python 解析钩子。规则 YAML 里写 `hook: "模块名:函数名"`，例如：

```yaml
hook: "mysite:parse"
```

对应 `hooks/mysite.py`：

```python
def parse(html, keyword, rule):
    # 返回 [{"title":..., "url":..., "size":..., "time":...}, ...]
    ...
```

`hooks/` 会被自动加入 `sys.path`（见 `engine/loader.py`），Android 构建时也会同步进 APK。
