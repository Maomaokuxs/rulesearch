package dev.rulesearch.app

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** 链接可访问性检测结果 */
sealed interface LinkStatus {
    data object Checking : LinkStatus
    data object Reachable : LinkStatus
    data object Unreachable : LinkStatus
    /** 磁力等非 http(s) 链接：不检测 */
    data object Skipped : LinkStatus
}

/** HEAD 优先、失败降级 GET，只取响应头不下载正文。 */
suspend fun checkLink(url: String): LinkStatus = withContext(Dispatchers.IO) {
    fun probe(method: String): Int? = try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36",
        )
        conn.setRequestProperty("Range", "bytes=0-0")
        val code = conn.responseCode
        conn.disconnect()
        code
    } catch (e: Exception) {
        null
    }
    val code = probe("HEAD") ?: probe("GET")
    if (code != null && code in 200..399) LinkStatus.Reachable else LinkStatus.Unreachable
}

data class SearchResultUi(
    val title: String,
    val url: String,
    val size: String,
    val time: String,
    val source: String,
    val kind: String, // pan | magnet
)

/** 一条搜索源（规则）。 */
data class RuleUi(
    val name: String,
    val type: String, // pan | magnet
    val source: String = "builtin", // builtin | user
    val version: String = "",
    val hasUpdateUrl: Boolean = false,
)

/** 收藏的一条结果。 */
data class FavoriteUi(
    val title: String,
    val url: String,
    val size: String,
    val time: String,
    val source: String,
    val kind: String,
)

/** 足迹：点开过的一条结果。 */
data class FootprintUi(
    val title: String,
    val url: String,
    val source: String,
    val kind: String,
    val clickedAt: Long,
)

/** 单条规则的试跑结果。 */
data class RuleTestResult(
    val results: List<SearchResultUi>,
    val error: String?,
    val elapsedMs: Int,
)

/** 单条搜索源健康状态（一键检测全部的结果）。 */
data class RuleHealth(
    val ok: Boolean,
    val count: Int = 0,
    val elapsedMs: Int = 0,
    val error: String? = null,
)

/** 规则更新信息。 */
data class RuleUpdate(
    val name: String,
    val currentVersion: String = "",
    val newVersion: String = "",
    val newYaml: String = "",
    val error: String? = null,
)

/** 规则导入结果。 */
data class ImportResult(
    val ok: Boolean,
    val name: String = "",
    val error: String = "",
)

/** 网盘品牌定义：key、显示名、域名子串 */
val PAN_BRAND_DEFS = listOf(
    Triple("quark", "夸克网盘", listOf("pan.quark.cn")),
    Triple("baidu", "百度网盘", listOf("pan.baidu.com")),
    Triple("ali", "阿里云盘", listOf("alipan.com", "aliyundrive.com")),
    Triple("p123", "123云盘", listOf("123pan.com", "123912.com", "123pan.cn")),
    Triple("uc", "UC网盘", listOf("drive.uc.cn")),
    Triple("xunlei", "迅雷云盘", listOf("pan.xunlei.com")),
    Triple("ct189", "天翼云盘", listOf("cloud.189.cn")),
    Triple("cmcc139", "移动云盘", listOf("yun.139.com", "caiyun.139.com")),
    Triple("weiyun", "腾讯微云", listOf("share.weiyun.com")),
    Triple("lanzou", "蓝奏云", listOf("lanzou")),
    Triple("yun115", "115网盘", listOf("115.com")),
    Triple("cow", "奶牛快传", listOf("cowtransfer.com")),
    Triple("ctfile", "城通网盘", listOf("ctfile.com")),
    Triple("wo", "联通沃云盘", listOf("pan.wo.cn")),
)

/** 返回链接所属网盘品牌 key；磁力/未知返回 null */
fun panBrandOf(url: String): String? {
    val u = url.trim().lowercase()
    if (u.startsWith("magnet:")) return null
    val host = try {
        android.net.Uri.parse(url).host?.lowercase().orEmpty()
    } catch (_: Exception) {
        ""
    }
    if (host.isEmpty()) return null
    return PAN_BRAND_DEFS.firstOrNull { (_, _, domains) ->
        domains.any { host.contains(it) }
    }?.first
}

/** 结果排序方式。 */
enum class SortMode(val label: String) {
    DEFAULT("默认"),
    SIZE_DESC("大小 ↓"),
    SIZE_ASC("大小 ↑"),
    TIME_DESC("时间 ↓"),
    SOURCE("来源"),
}

/** 文件大小筛选。 */
enum class SizeFilter(val label: String) {
    ALL("全部"),
    SMALL("< 1GB"),
    MEDIUM("1-5GB"),
    LARGE("> 5GB"),
}

/** 更新时间筛选。 */
enum class TimeFilter(val label: String) {
    ALL("全部"),
    TODAY("今天"),
    LAST_3_DAYS("近 3 天"),
    LAST_7_DAYS("近 7 天"),
}

/** 把 "1.2GB"/"500 MB"/"800KB" 转成字节数，解析不出返回 null。 */
fun parseSizeBytes(s: String): Long? {
    val t = s.trim().uppercase(Locale.ROOT).replace(" ", "")
    if (t.isEmpty()) return null
    val m = Regex("""([\d.]+)(GB|MB|KB|B|G|M|K)$""").find(t) ?: return null
    val num = m.groupValues[1].toDoubleOrNull() ?: return null
    val mult = when (m.groupValues[2]) {
        "GB", "G" -> 1024L * 1024 * 1024
        "MB", "M" -> 1024L * 1024
        "KB", "K" -> 1024L
        else -> 1L
    }
    return (num * mult).toLong()
}

/** 尽力解析常见时间格式，解析不出返回 null。 */
fun parseTimeMs(s: String): Long? {
    val t = s.trim()
    if (t.isEmpty()) return null
    val formats = listOf(
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
        "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd",
        "yyyy年MM月dd日",
    )
    for (f in formats) {
        try {
            SimpleDateFormat(f, Locale.getDefault()).parse(t)?.time?.let { return it }
        } catch (_: Exception) {
        }
    }
    return null
}

/** 薄 ViewModel：调 Python 引擎 + 解析 JSON + 本地状态持久化。 */
/** 检查更新状态 */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val latest: String, val url: String, val notes: String) : UpdateState
    data object Error : UpdateState
}

/** 比较版本号：返回 >0 表示 latest 更新，0 表示相等，<0 表示 current 更新 */
fun compareVersions(current: String, latest: String): Int {
    val cur = current.trimStart('v').split(".").map { it.toIntOrNull() ?: 0 }
    val lat = latest.trimStart('v').split(".").map { it.toIntOrNull() ?: 0 }
    val n = maxOf(cur.size, lat.size)
    for (i in 0 until n) {
        val c = cur.getOrElse(i) { 0 }
        val l = lat.getOrElse(i) { 0 }
        if (l != c) return l - c
    }
    return 0
}

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    // 搜索框 FLIP 动画是否已播过（切 tab 不重播）
    var flipPlayedForKeyword: String? = null
    private val prefs = app.getSharedPreferences("rulesearch", Context.MODE_PRIVATE)

    var keyword by mutableStateOf("")
    var results by mutableStateOf<List<SearchResultUi>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    private var searchJob: Job? = null
    private var searchGen = 0

    // 一键检测：url -> true(可访问)/false(疑似失效)，磁力不参与
    var linkStatus by mutableStateOf<Map<String, Boolean>>(emptyMap())
    var checkingLinks by mutableStateOf(false)
    var checkProgress by mutableStateOf<Pair<Int, Int>?>(null) // (已测, 总数)
    /** 当前选中的 tab（按分组顺序：网盘在前，磁力在后） */
    var selectedTab by mutableIntStateOf(0)
        private set
    /** 全部搜索源（从 Python 规则加载） */
    var rules by mutableStateOf<List<RuleUi>>(emptyList())
        private set
    /** 被禁用的搜索源名 */
    var disabledRules by mutableStateOf<Set<String>>(emptySet())
        private set
    /** 一键检测全部搜索源的结果：规则名 -> 健康状态 */
    var ruleHealth by mutableStateOf<Map<String, RuleHealth>>(emptyMap())
    var testingAllRules by mutableStateOf(false)
    var ruleTestProgress by mutableStateOf<Pair<Int, Int>?>(null) // (已测, 总数)

    /** 是否已发起过搜索（用于区分首页/结果页） */
    var searched by mutableStateOf(false)
        private set
    /** 搜索历史（新在前，最多 20 条） */
    var history by mutableStateOf<List<String>>(emptyList())
        private set
    /** 排序方式 */
    var sortMode by mutableStateOf(SortMode.DEFAULT)
    /** 只显示可访问的链接（筛选框选项，开启后自动检测；持久化，重启仍保持） */
    var onlyReachable by mutableStateOf(prefs.getBoolean("only_reachable", false))
        private set
    /** 被筛掉的网盘品牌（在筛选框里取消勾选的） */
    var hiddenPanBrands by mutableStateOf<Set<String>>(emptySet())
        private set
    /** 文件大小筛选 */
    var sizeFilter by mutableStateOf(SizeFilter.ALL)
    /** 更新时间筛选 */
    var timeFilter by mutableStateOf(TimeFilter.ALL)
    /** 收藏列表 */
    var favorites by mutableStateOf<List<FavoriteUi>>(emptyList())
        private set
    /** 足迹：点开过的结果，新在前，最多 100 条 */
    var footprints by mutableStateOf<List<FootprintUi>>(emptyList())
        private set
    /** 用户自建内容过滤词（标题命中即过滤），存 filesDir/blocked_words.txt；
     * 另有一份内置词表打包在 APK 内（用户不可见），搜索时两份合并生效。
     * 界面展示时过滤掉内置词（文件本身不动）。 */
    var blockedWords by mutableStateOf<List<String>>(emptyList())
        private set
    /** 用户词表文件的完整内容（含历史残留的内置词，仅用于存盘） */
    private var allBlockedWords: List<String> = emptyList()
    /** 内置词表（Python 侧读取，用于界面过滤显示） */
    private var builtinWords: Set<String> = emptySet()

    /** 内置词表（只读，供内容过滤页展示；不可删除）。 */
    val builtinBlockedWords: List<String> get() = builtinWords.sorted()
    /** 本次搜索被过滤掉的条数 */
    var filteredCount by mutableIntStateOf(0)
        private set
    /** 累计过滤掉的条数（持久化，设置页内容过滤中展示） */
    var totalFiltered by mutableStateOf(0L)
        private set

    /** 检查更新状态 */
    var updateState by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set

    /** 检查 GitHub 最新 Release，与当前版本比较 */
    fun checkForUpdate() {
        if (updateState is UpdateState.Checking) return
        updateState = UpdateState.Checking
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val conn = URL("https://api.github.com/repos/Maomaokuxs/rulesearch/releases/latest").openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("User-Agent", "RuleSearch")
                val code = conn.responseCode
                if (code != 200) {
                    conn.disconnect()
                    withContext(Dispatchers.Main) { updateState = UpdateState.Error }
                    return@launch
                }
                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "")
                val htmlUrl = json.optString("html_url", "https://github.com/Maomaokuxs/rulesearch/releases")
                val notes = json.optString("body", "")
                val current = try {
                    val info = getApplication<Application>().packageManager.getPackageInfo(
                        getApplication<Application>().packageName, 0
                    )
                    @Suppress("DEPRECATION")
                    info.versionName ?: "0.0.0"
                } catch (e: Exception) { "0.0.0" }
                val result: UpdateState = if (tagName.isBlank()) {
                    UpdateState.Error
                } else if (compareVersions(current, tagName) > 0) {
                    UpdateState.Available(tagName, htmlUrl, notes)
                } else {
                    UpdateState.UpToDate
                }
                withContext(Dispatchers.Main) { updateState = result }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { updateState = UpdateState.Error }
            }
        }
    }

    /** 重置更新状态（弹窗关闭后调用） */
    fun resetUpdateState() {
        updateState = UpdateState.Idle
    }
    /** 多选模式 */
    var selectionMode by mutableStateOf(false)
    /** 多选选中的 url */
    var selectedUrls by mutableStateOf<Set<String>>(emptySet())
        private set

    init {
        disabledRules = prefs.getStringSet(KEY_DISABLED, emptySet()) ?: emptySet()
        history = loadHistory()
        favorites = loadFavorites()
        footprints = loadFootprints()
        totalFiltered = prefs.getLong(KEY_TOTAL_FILTERED, 0L)
        viewModelScope.launch {
            // 先把用户规则目录告诉 Python 引擎，再加载规则列表
            withContext(Dispatchers.IO) {
                val mod = Python.getInstance().getModule("engine_bridge")
                mod.callAttr("init", userRulesDir().absolutePath)
                // 读内置词表，供界面过滤显示（失败就不过滤，不影响搜索）
                builtinWords = try {
                    val json = mod.callAttr("get_builtin_blocklist").toString()
                    JSONArray(json).let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }.toSet()
                    }
                } catch (e: Exception) {
                    emptySet()
                }
            }
            loadRules()
            allBlockedWords = loadBlockedWords()
            refreshBlockedWordsDisplay()
        }
    }

    private fun userRulesDir(): File =
        File(getApplication<Application>().filesDir, "rules").also { it.mkdirs() }

    fun selectTab(i: Int) {
        selectedTab = i
    }

    fun isRuleEnabled(name: String) = name !in disabledRules

    fun setRuleEnabled(name: String, enabled: Boolean) {
        disabledRules = if (enabled) disabledRules - name else disabledRules + name
        prefs.edit().putStringSet(KEY_DISABLED, disabledRules).apply()
    }

    /** 当前启用的搜索源名；规则未加载完时返回 null（表示全部）。 */
    private fun enabledRuleNames(): List<String>? {
        if (rules.isEmpty()) return null
        return rules.filter { isRuleEnabled(it.name) }.map { it.name }
    }

    fun search() {
        val kw = keyword.trim()
        if (kw.isEmpty() || loading) return
        val enabled = enabledRuleNames()
        if (enabled != null && enabled.isEmpty()) {
            error = "请至少启用一个搜索源"
            return
        }
        addToHistory(kw)
        searched = true
        selectionMode = false
        selectedUrls = emptySet()
        hiddenPanBrands = emptySet()
        searchJob?.cancel()
        val gen = ++searchGen
        searchJob = viewModelScope.launch {
            loading = true
            error = null
            try {
                // 传 JSON 字符串：Chaquopy 不会自动转换 Java List
                val enabledJson = enabled?.let { JSONArray(it).toString() }
                val jsonStr = withContext(Dispatchers.IO) {
                    Python.getInstance()
                        .getModule("engine_bridge")
                        .callAttr("search", kw, enabledJson)
                        .toString()
                }
                results = parseResults(jsonStr)
                selectedTab = 0 // 新搜索回到第一个 tab
                linkStatus = emptyMap()
                checkProgress = null
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 用户主动取消，不显示错误
                throw e
            } catch (e: Exception) {
                error = e.message ?: e.toString()
            } finally {
                // 只有最新任务才清 loading，避免旧任务覆盖新搜索状态
                if (searchGen == gen) loading = false
            }
        }
    }

    private suspend fun loadRules() {
        try {
            val jsonStr = withContext(Dispatchers.IO) {
                Python.getInstance()
                    .getModule("engine_bridge")
                    .callAttr("list_rules")
                    .toString()
            }
            val arr = JSONArray(jsonStr)
            rules = List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                RuleUi(
                    name = o.optString("name"),
                    type = o.optString("type", "pan"),
                    source = o.optString("source", "builtin"),
                    version = o.optString("version"),
                    hasUpdateUrl = o.optBoolean("has_update_url"),
                )
            }
        } catch (e: Exception) {
            // 规则加载失败就不展示管理界面，不阻塞搜索
        }
    }

    private fun parseResults(jsonStr: String): List<SearchResultUi> {
        val o = JSONObject(jsonStr)
        filteredCount = o.optInt("filtered")
        if (filteredCount > 0) {
            totalFiltered += filteredCount
            prefs.edit().putLong(KEY_TOTAL_FILTERED, totalFiltered).apply()
        }
        val arr = o.optJSONArray("results") ?: return emptyList()
        return List(arr.length()) { i ->
            val r = arr.getJSONObject(i)
            SearchResultUi(
                title = r.optString("title"),
                url = r.optString("url"),
                size = r.optString("size"),
                time = r.optString("time"),
                source = r.optString("source"),
                kind = r.optString("kind", "pan"),
            )
        }
    }

    /** 返回首页：清空结果，回到空状态 */
    fun clearResults() {
        searchJob?.cancel()
        searchJob = null
        loading = false
        results = emptyList()
        error = null
        filteredCount = 0
        selectedTab = 0
        searched = false
        selectionMode = false
        selectedUrls = emptySet()
        linkStatus = emptyMap()
        checkProgress = null
    }

    /** 一键检测：并发测当页所有 http(s) 链接的可访问性，磁力跳过。 */
    fun checkAllLinks() {
        val targets = results.filter { it.url.startsWith("http") }
        if (targets.isEmpty() || checkingLinks) return
        checkingLinks = true
        linkStatus = emptyMap()
        viewModelScope.launch {
            val total = targets.size
            val done = AtomicInteger(0)
            val out = ConcurrentHashMap<String, Boolean>()
            // 进度条节流：最多 300ms 更新一次，避免逐条触发重组卡顿
            val lastProg = AtomicLong(0)
            try {
                withContext(Dispatchers.IO.limitedParallelism(8)) {
                    targets.map { r ->
                        async {
                            out[r.url] = checkLink(r.url) == LinkStatus.Reachable
                            val d = done.incrementAndGet()
                            val now = SystemClock.uptimeMillis()
                            if (d == total || now - lastProg.get() >= 300) {
                                lastProg.set(now)
                                checkProgress = d to total
                            }
                        }
                    }.awaitAll()
                }
                linkStatus = HashMap(out)
            } finally {
                checkProgress = null
                checkingLinks = false
            }
        }
    }

    /** 输入联想：历史记录中包含输入串的（空输入时返回最近 8 条） */
    fun suggestionsFor(input: String): List<String> {
        val h = history
        if (h.isEmpty()) return emptyList()
        return if (input.isBlank()) h.take(8)
        else h.filter { it.contains(input, ignoreCase = true) }.take(8)
    }

    fun clearHistory() {
        history = emptyList()
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun deleteHistoryItem(item: String) {
        history = history.filter { it != item }
        prefs.edit().putString(KEY_HISTORY, JSONArray(history).toString()).apply()
    }

    /** 在线实时搜索建议（百度 sugrec），失败返回空（只用历史联想）。 */
    suspend fun onlineSuggestions(input: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://www.baidu.com/sugrec?prod=pc&wd=" + URLEncoder.encode(input, "UTF-8"),
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36",
            )
            val text = conn.inputStream.bufferedReader().readText()
            val arr = JSONObject(text).optJSONArray("g") ?: return@withContext emptyList()
            List(arr.length()) { i -> arr.getJSONObject(i).optString("q") }
                .filter { it.isNotBlank() }
                .take(6)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ===== 收藏 =====

    fun isFavorite(url: String) = favorites.any { it.url == url }

    fun toggleFavorite(r: SearchResultUi) {
        favorites = if (isFavorite(r.url)) {
            favorites.filter { it.url != r.url }
        } else {
            favorites + FavoriteUi(r.title, r.url, r.size, r.time, r.source, r.kind)
        }
        saveFavorites()
    }

    fun removeFavorite(url: String) {
        favorites = favorites.filter { it.url != url }
        saveFavorites()
    }

    private fun saveFavorites() {
        val arr = JSONArray()
        for (f in favorites) {
            arr.put(
                JSONObject().apply {
                    put("title", f.title)
                    put("url", f.url)
                    put("size", f.size)
                    put("time", f.time)
                    put("source", f.source)
                    put("kind", f.kind)
                },
            )
        }
        prefs.edit().putString(KEY_FAVORITES, arr.toString()).apply()
    }

    private fun loadFavorites(): List<FavoriteUi> {
        val json = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                FavoriteUi(
                    title = o.optString("title"),
                    url = o.optString("url"),
                    size = o.optString("size"),
                    time = o.optString("time"),
                    source = o.optString("source"),
                    kind = o.optString("kind", "pan"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ===== 足迹 =====

    /** 点开一条结果时记录足迹（去重，最多 100 条）。 */
    fun addFootprint(r: SearchResultUi) {
        footprints = (
            listOf(
                FootprintUi(
                    title = r.title,
                    url = r.url,
                    source = r.source,
                    kind = r.kind,
                    clickedAt = System.currentTimeMillis(),
                ),
            ) + footprints.filter { it.url != r.url }
        ).take(100)
        saveFootprints()
    }

    fun removeFootprint(url: String) {
        footprints = footprints.filter { it.url != url }
        saveFootprints()
    }

    fun clearFootprints() {
        footprints = emptyList()
        prefs.edit().remove(KEY_FOOTPRINTS).apply()
    }

    private fun saveFootprints() {
        val arr = JSONArray()
        for (f in footprints) {
            arr.put(
                JSONObject().apply {
                    put("title", f.title)
                    put("url", f.url)
                    put("source", f.source)
                    put("kind", f.kind)
                    put("clickedAt", f.clickedAt)
                },
            )
        }
        prefs.edit().putString(KEY_FOOTPRINTS, arr.toString()).apply()
    }

    private fun loadFootprints(): List<FootprintUi> {
        val json = prefs.getString(KEY_FOOTPRINTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                FootprintUi(
                    title = o.optString("title"),
                    url = o.optString("url"),
                    source = o.optString("source"),
                    kind = o.optString("kind", "pan"),
                    clickedAt = o.optLong("clickedAt", 0L),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ===== 内容过滤 =====

    private fun blocklistFile(): File =
        File(getApplication<Application>().filesDir, "blocked_words.txt")

    private fun loadBlockedWords(): List<String> {
        val f = blocklistFile()
        if (!f.exists()) return emptyList()
        return f.readLines().map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
    }

    /** 界面展示用：过滤掉内置词，文件本身不动。 */
    private fun refreshBlockedWordsDisplay() {
        blockedWords = allBlockedWords.filter { it !in builtinWords }
    }

    private fun saveBlockedWords() {
        blocklistFile().writeText(allBlockedWords.joinToString("\n"))
    }

    /** 加过滤词（去重去空；已在内置词表中的不再重复添加），下次搜索生效。 */
    fun addBlockedWord(w: String) {
        val word = w.trim()
        if (word.isEmpty() || word in allBlockedWords || word in builtinWords) return
        allBlockedWords = allBlockedWords + word
        saveBlockedWords()
        refreshBlockedWordsDisplay()
    }

    fun removeBlockedWord(w: String) {
        allBlockedWords = allBlockedWords - w
        saveBlockedWords()
        refreshBlockedWordsDisplay()
    }

    /** 清空用户自建过滤词（内置词表不受影响，仍在后台生效）。 */
    fun clearBlockedWords() {
        allBlockedWords = emptyList()
        saveBlockedWords()
        refreshBlockedWordsDisplay()
    }

    /** 批量删除用户过滤词。 */
    fun removeBlockedWords(words: Set<String>) {
        if (words.isEmpty()) return
        allBlockedWords = allBlockedWords - words
        saveBlockedWords()
        refreshBlockedWordsDisplay()
    }

    // ===== 多选 =====

    fun toggleSelectionMode() {
        selectionMode = !selectionMode
        if (!selectionMode) selectedUrls = emptySet()
    }

    fun toggleSelect(url: String) {
        selectedUrls = if (url in selectedUrls) selectedUrls - url else selectedUrls + url
    }

    /** 选中的结果（按当前列表顺序） */
    fun selectedResults(): List<SearchResultUi> {
        val sel = selectedUrls
        return currentTabResults.filter { it.url in sel }
    }

    // ===== 规则管理（导入/删除/测试/更新） =====

    /** 单条规则试跑。 */
    suspend fun testRule(name: String, keyword: String): RuleTestResult =
        withContext(Dispatchers.IO) {
            val jsonStr = Python.getInstance()
                .getModule("engine_bridge")
                .callAttr("test_rule", name, keyword)
                .toString()
            val o = JSONObject(jsonStr)
            val arr = o.optJSONArray("results") ?: JSONArray()
            val parsedError = if (o.isNull("error")) null else o.optString("error").ifEmpty { null }
            if (parsedError != null) {
                android.util.Log.e(
                    "RuleSearch",
                    "test_rule原始返回 [$name]: $jsonStr".take(2000),
                )
            }
            RuleTestResult(
                results = List(arr.length()) { i ->
                    val r = arr.getJSONObject(i)
                    SearchResultUi(
                        title = r.optString("title"),
                        url = r.optString("url"),
                        size = r.optString("size"),
                        time = r.optString("time"),
                        source = r.optString("source"),
                        kind = r.optString("kind", "pan"),
                    )
                },
                error = parsedError,
                elapsedMs = o.optInt("elapsed_ms"),
            )
        }

    /** 一键检测全部搜索源：逐个试跑，实时更新每条的健康状态。 */
    fun testAllRules(keyword: String = "三体") {
        if (testingAllRules || rules.isEmpty()) return
        testingAllRules = true
        ruleHealth = emptyMap()
        viewModelScope.launch {
            val total = rules.size
            val done = AtomicInteger(0)
            val out = ConcurrentHashMap<String, RuleHealth>()
            try {
                withContext(Dispatchers.IO.limitedParallelism(4)) {
                    rules.map { rule ->
                        async {
                            val h = try {
                                val r = testRule(rule.name, keyword)
                                RuleHealth(
                                    ok = r.error == null,
                                    count = r.results.size,
                                    elapsedMs = r.elapsedMs,
                                    error = r.error,
                                )
                            } catch (e: Exception) {
                                android.util.Log.e(
                                    "RuleSearch",
                                    "规则检测异常 ${rule.name}",
                                    e,
                                )
                                RuleHealth(
                                    ok = false,
                                    error = "${e.javaClass.simpleName}: ${e.message}",
                                )
                            }
                            out[rule.name] = h
                            ruleTestProgress = done.incrementAndGet() to total
                            ruleHealth = HashMap(out) // 逐步刷新 UI
                            if (!h.ok) {
                                android.util.Log.e(
                                    "RuleSearch",
                                    "规则检测失败 ${rule.name}: ${h.error}",
                                )
                            }
                        }
                    }.awaitAll()
                }
            } finally {
                ruleTestProgress = null
                testingAllRules = false
            }
        }
    }

    /** 导入 YAML 规则：先校验，通过后写入用户规则目录并重载。 */
    suspend fun importRule(fileName: String, yamlText: String): ImportResult =
        withContext(Dispatchers.IO) {
            val mod = Python.getInstance().getModule("engine_bridge")
            val v = JSONObject(mod.callAttr("validate_rule", yamlText).toString())
            if (!v.optBoolean("ok")) {
                val importErr = if (v.isNull("error")) null else v.optString("error").ifEmpty { null }
                return@withContext ImportResult(false, error = importErr.orEmpty())
            }
            val name = v.optString("name")
            val safeName = fileName.ifBlank { "rule-${System.currentTimeMillis()}.yaml" }
                .let { if (it.endsWith(".yaml") || it.endsWith(".yml")) it else "$it.yaml" }
            File(userRulesDir(), safeName).writeText(yamlText)
            mod.callAttr("reload_rules")
            loadRules()
            ImportResult(true, name = name)
        }

    /** 删除用户自建规则。 */
    suspend fun deleteUserRule(name: String): Boolean = withContext(Dispatchers.IO) {
        val ok = Python.getInstance()
            .getModule("engine_bridge")
            .callAttr("delete_user_rule", name)
            .toString().toBoolean()
        if (ok) loadRules()
        ok
    }

    /** 取规则原文（导出用）。 */
    suspend fun getRuleYaml(name: String): String = withContext(Dispatchers.IO) {
        Python.getInstance()
            .getModule("engine_bridge")
            .callAttr("get_rule_yaml", name)
            .toString()
    }

    /** 检查带 update_url 的规则是否有新版。 */
    suspend fun checkRuleUpdates(): List<RuleUpdate> = withContext(Dispatchers.IO) {
        val arr = JSONArray(
            Python.getInstance()
                .getModule("engine_bridge")
                .callAttr("check_updates")
                .toString(),
        )
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            RuleUpdate(
                name = o.optString("name"),
                currentVersion = o.optString("current_version"),
                newVersion = o.optString("new_version"),
                newYaml = o.optString("new_yaml"),
                error = if (o.isNull("error")) null else o.optString("error").ifEmpty { null },
            )
        }
    }

    /** 写入新版规则并重载。 */
    suspend fun applyRuleUpdate(name: String, newYaml: String): Boolean =
        withContext(Dispatchers.IO) {
            val ok = Python.getInstance()
                .getModule("engine_bridge")
                .callAttr("apply_rule_update", name, newYaml)
                .toString().toBoolean()
            if (ok) loadRules()
            ok
        }

    private fun loadHistory(): List<String> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i -> arr.getString(i) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun addToHistory(kw: String) {
        val updated = (listOf(kw) + history.filter { it != kw }).take(20)
        history = updated
        prefs.edit().putString(KEY_HISTORY, JSONArray(updated).toString()).apply()
    }

    /** 按 kind 分组：网盘在前，磁力在后（derivedStateOf 缓存，避免每次重组都重算） */
    val groupedResults: List<Pair<String, List<SearchResultUi>>> by derivedStateOf {
        val groups = results.groupBy { it.kind }
        listOfNotNull(
            groups["pan"]?.let { "网盘" to it },
            groups["magnet"]?.let { "磁力" to it },
            // 未知 kind 的兜底
            *groups.filterKeys { it != "pan" && it != "magnet" }
                .map { (k, v) -> k to v }.toTypedArray(),
        )
    }

    /** tab 标题（按实际有结果的分组） */
    val tabTitles: List<String> by derivedStateOf { groupedResults.map { it.first } }

    /** 筛选框"只显示可访问的链接"开关：开启时若还没检测过就自动跑一遍。 */
    fun setOnlyReachableFilter(v: Boolean) {
        onlyReachable = v
        prefs.edit().putBoolean("only_reachable", v).apply()
        if (v && linkStatus.isEmpty() && !checkingLinks) {
            checkAllLinks()
        }
    }

    /** 筛选框里切换网盘品牌的勾选 */
    fun togglePanBrandHidden(brand: String) {
        hiddenPanBrands =
            if (brand in hiddenPanBrands) hiddenPanBrands - brand else hiddenPanBrands + brand
    }

    /** 当前 tab 出现过的网盘品牌（筛选框用） */
    val tabPanBrands: List<String> by derivedStateOf {
        groupedResults.getOrNull(selectedTab)?.second
            ?.mapNotNull { panBrandOf(it.url) }?.distinct().orEmpty()
    }

    /** 当前 tab 的结果（含可访问筛选、网盘品牌筛选和排序） */
    val currentTabResults: List<SearchResultUi> by derivedStateOf {
        val list = (groupedResults.getOrNull(selectedTab)?.second ?: emptyList())
            // 只显示可访问：检测失败的隐藏，未检测的（磁力等）保留
            .filter { !onlyReachable || linkStatus[it.url] != false }
            // 网盘品牌筛选：非网盘链接（磁力等）不受影响
            .filter { r ->
                val brand = panBrandOf(r.url)
                brand == null || brand !in hiddenPanBrands
            }
            // 文件大小筛选：无大小信息的不过滤
            .filter { r ->
                val bytes = parseSizeBytes(r.size)
                bytes == null || when (sizeFilter) {
                    SizeFilter.ALL -> true
                    SizeFilter.SMALL -> bytes < 1L * 1024 * 1024 * 1024
                    SizeFilter.MEDIUM -> bytes in (1L * 1024 * 1024 * 1024)..(5L * 1024 * 1024 * 1024)
                    SizeFilter.LARGE -> bytes > 5L * 1024 * 1024 * 1024
                }
            }
            // 更新时间筛选：解析失败的不过滤
            .filter { r ->
                val ms = parseTimeMs(r.time)
                ms == null || when (timeFilter) {
                    TimeFilter.ALL -> true
                    TimeFilter.TODAY -> {
                        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        fmt.format(java.util.Date(ms)) == fmt.format(java.util.Date())
                    }
                    TimeFilter.LAST_3_DAYS -> System.currentTimeMillis() - ms <= 3L * 24 * 60 * 60 * 1000
                    TimeFilter.LAST_7_DAYS -> System.currentTimeMillis() - ms <= 7L * 24 * 60 * 60 * 1000
                }
            }
        when (sortMode) {
            SortMode.DEFAULT -> list
            SortMode.SIZE_DESC ->
                list.sortedByDescending { parseSizeBytes(it.size) ?: Long.MIN_VALUE }
            SortMode.SIZE_ASC ->
                list.sortedBy { parseSizeBytes(it.size) ?: Long.MAX_VALUE }
            SortMode.TIME_DESC ->
                list.sortedByDescending { parseTimeMs(it.time) ?: Long.MIN_VALUE }
            SortMode.SOURCE -> list.sortedBy { it.source }
        }
    }

    companion object {
        private const val KEY_DISABLED = "disabled_rules"
        private const val KEY_HISTORY = "search_history"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_FOOTPRINTS = "footprints"
        private const val KEY_TOTAL_FILTERED = "total_filtered"
    }
}
