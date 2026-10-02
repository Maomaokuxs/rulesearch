package dev.rulesearch.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.rulesearch.app.SearchViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField

@Composable
fun SearchScreen(vm: SearchViewModel = viewModel()) {
    val context = LocalContext.current
    val fieldState = remember { TextFieldState() }

    Scaffold { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    state = fieldState,
                    label = "搜索资源",
                    useLabelAsPlaceholder = true,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                    vm.keyword = fieldState.text.toString()
                    vm.search()
                }) {
                    Text("搜索")
                }
            }

            if (vm.loading) {
                Text("搜索中…")
            }
            vm.error?.let {
                Text("出错：$it")
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.results) { r ->
                    Card(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, r.url.toUri()))
                    }) {
                        Text(r.title)
                        if (r.size.isNotEmpty() || r.time.isNotEmpty()) {
                            Text(listOf(r.size, r.time).filter { it.isNotEmpty() }.joinToString(" · "))
                        }
                        Text("来源：${r.source}")
                    }
                }
            }
        }
    }
}
