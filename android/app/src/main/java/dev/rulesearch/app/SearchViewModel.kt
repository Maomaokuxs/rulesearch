package dev.rulesearch.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class SearchResultUi(
    val title: String,
    val url: String,
    val size: String,
    val time: String,
    val source: String,
)

/** 薄 ViewModel：只负责调 Python 引擎 + 解析 JSON，不放业务逻辑。 */
class SearchViewModel : ViewModel() {
    var keyword by mutableStateOf("")
    var results by mutableStateOf<List<SearchResultUi>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun search() {
        val kw = keyword.trim()
        if (kw.isEmpty() || loading) return
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val jsonStr = withContext(Dispatchers.IO) {
                    Python.getInstance()
                        .getModule("engine_bridge")
                        .callAttr("search", kw)
                        .toString()
                }
                results = parseResults(jsonStr)
            } catch (e: Exception) {
                error = e.message ?: e.toString()
            } finally {
                loading = false
            }
        }
    }

    private fun parseResults(jsonStr: String): List<SearchResultUi> {
        val arr = JSONObject(jsonStr).optJSONArray("results") ?: return emptyList()
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            SearchResultUi(
                title = o.optString("title"),
                url = o.optString("url"),
                size = o.optString("size"),
                time = o.optString("time"),
                source = o.optString("source"),
            )
        }
    }
}
