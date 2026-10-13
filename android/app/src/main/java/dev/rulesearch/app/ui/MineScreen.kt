package dev.rulesearch.app.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.core.graphics.drawable.toBitmap
import dev.rulesearch.app.SearchViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** "我的"页（底部 dock 的"我的"标签）：足迹 / 搜索源 / 内容过滤 / 关于，二级页面结构。 */
@Composable
fun MineTab(vm: SearchViewModel) {
    var page by remember { mutableStateOf<String?>(null) } // null=菜单
    var selectedLicense by remember { mutableStateOf<LicenseEntry?>(null) }

    // 层级返回：详情 -> 许可列表 -> 关于 -> 我的菜单
    BackHandler(enabled = page != null || selectedLicense != null) {
        when {
            selectedLicense != null -> selectedLicense = null
            page == "license" -> page = "about"
            else -> page = null
        }
    }

    // 许可详情页单独渲染（MineSubPage 提供返回）
    if (selectedLicense != null) {
        MineSubPage(onBack = { selectedLicense = null }) {
            LicenseDetailPage(entry = selectedLicense!!)
        }
        return
    }

    AnimatedContent(
        targetState = page,
        transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(180))) },
        label = "mine_nav",
    ) { p ->
        when (p) {
            "footprints" -> MineSubPage(onBack = { page = null }) { FootprintsTab(vm = vm) }
            "rules" -> MineSubPage(onBack = { page = null }) { ManageTab(vm = vm) }
            "blocklist" -> MineSubPage(onBack = { page = null }) { BlocklistPage(vm = vm) }
            "about" -> MineSubPage(onBack = { page = null }) { AboutPage(vm = vm, onLicense = { page = "license" }) }
            "license" -> MineSubPage(onBack = { page = "about" }) {
                LicensePage(onEntryClick = { selectedLicense = it })
            }
            else -> MineMenu(
                vm = vm,
                onFootprints = { page = "footprints" },
                onRules = { page = "rules" },
                onBlocklist = { page = "blocklist" },
                onAbout = { page = "about" },
            )
        }
    }
}

/** "我的"一级菜单：页底浅灰 + 三组白卡片（mock 样式）。 */
@Composable
private fun MineMenu(
    vm: SearchViewModel,
    onFootprints: () -> Unit,
    onRules: () -> Unit,
    onBlocklist: () -> Unit,
    onAbout: () -> Unit,
) {
    val health = vm.ruleHealth
    val healthSub = if (health.isNotEmpty()) {
        " · ${health.count { it.value.ok }}/${health.size} 正常"
    } else {
        ""
    }
    Column(
        modifier = Modifier.fillMaxSize().background(pageBackground()),
    ) {
        Text(
            text = "我的",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 24.dp, end = 20.dp, top = 64.dp, bottom = 16.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. 足迹
            MineCard(onClick = onFootprints) {
                MineMenuRow(
                    title = "足迹",
                    trailing = {
                        Box(
                            modifier = Modifier.clip(CircleShape)
                                .background(Color(0xFFC7C7CC))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "${vm.footprints.size}",
                                fontSize = 12.sp,
                                color = Color.White,
                            )
                        }
                    },
                )
            }
            // 2. 规则管理 + 内容过滤（一张卡片两行）
            MineCard(onClick = null) {
                MineMenuRow(
                    title = "规则管理",
                    trailing = {
                        Text(
                            text = "${vm.rules.size} 条规则$healthSub",
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.secondary,
                        )
                    },
                    onClick = onRules,
                )
                Box(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp)
                        .height(0.5.dp).background(MiuixTheme.colorScheme.outline),
                )
                MineMenuRow(
                    title = "内容过滤",
                    trailing = {
                        Text(
                            text = "${vm.blockedWords.size} 个词",
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.secondary,
                        )
                    },
                    onClick = onBlocklist,
                )
            }
            // 3. 关于
            MineCard(onClick = onAbout) {
                MineMenuRow(title = "关于", trailing = null)
            }
        }
    }
}

/** 白色圆角卡片容器。 */
@Composable
private fun MineCard(
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val mod = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(cardBackground())
    Column(
        modifier = if (onClick != null) mod.clickable(onClick = onClick) else mod,
    ) {
        content()
    }
}

@Composable
private fun MineMenuRow(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val mod = Modifier.fillMaxWidth().heightIn(min = 54.dp)
        .padding(horizontal = 16.dp, vertical = 12.dp)
    Row(
        modifier = if (onClick != null) mod.clickable(onClick = onClick) else mod,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Box(modifier = Modifier.padding(start = 8.dp)) { trailing() }
        } else {
            Text(
                text = "〉",
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.secondary,
            )
        }
    }
}

/** "我的"二级页面：顶部返回行 + 内容。 */
@Composable
private fun MineSubPage(onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "〈 返回",
                color = MiuixTheme.colorScheme.primary,
                fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            content()
        }
    }
}

/**
 * 关于页（按 mock）。
 */
@Composable
private fun AboutPage(vm: SearchViewModel, onLicense: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "关于",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 16.dp),
        )
        // 第一张卡片：RuleSearch / 规则搜索
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(cardBackground())
                .padding(16.dp),
        ) {
            Text(
                text = "RuleSearch",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "规则搜索",
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 第二张卡片：检查更新 / 开源许可 / 问题反馈
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(cardBackground()),
        ) {
            MineMenuRow(
                title = "检查更新",
                trailing = {
                    if (vm.updateState is dev.rulesearch.app.UpdateState.Checking) {
                        Text(
                            text = "检查中…",
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.secondary,
                        )
                    }
                },
                onClick = { vm.checkForUpdate() },
            )
            Box(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp)
                    .height(0.5.dp).background(MiuixTheme.colorScheme.outline),
            )
            MineMenuRow(
                title = "开源许可",
                onClick = onLicense,
            )
            Box(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp)
                    .height(0.5.dp).background(MiuixTheme.colorScheme.outline),
            )
            MineMenuRow(
                title = "问题反馈",
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/Maomaokuxs/rulesearch/issues".toUri(),
                        ),
                    )
                },
            )
        }
    }
    // 检查更新：有更新弹窗 / 已是最新 / 失败 Toast
    val updateState = vm.updateState
    when (updateState) {
        is dev.rulesearch.app.UpdateState.Available -> {
            Dialog(onDismissRequest = { vm.resetUpdateState() }) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBackground())
                        .padding(20.dp),
                ) {
                    Text(
                        text = "发现新版本 ${updateState.latest}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    if (updateState.notes.isNotBlank()) {
                        Text(
                            text = updateState.notes.take(500),
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MiuixTheme.colorScheme.primary)
                                .clickable {
                                    vm.resetUpdateState()
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, updateState.url.toUri()),
                                    )
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "去下载", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MiuixTheme.colorScheme.outline)
                                .clickable { vm.resetUpdateState() }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "稍后", fontSize = 15.sp)
                        }
                    }
                }
            }
        }
        is dev.rulesearch.app.UpdateState.UpToDate -> {
            androidx.compose.runtime.LaunchedEffect(Unit) {
                android.widget.Toast.makeText(context, "已是最新版本", android.widget.Toast.LENGTH_SHORT).show()
                vm.resetUpdateState()
            }
        }
        is dev.rulesearch.app.UpdateState.Error -> {
            androidx.compose.runtime.LaunchedEffect(Unit) {
                android.widget.Toast.makeText(context, "检查失败，请稍后再试", android.widget.Toast.LENGTH_SHORT).show()
                vm.resetUpdateState()
            }
        }
        else -> {}
    }
}

/** 开源许可条目。 */
private data class LicenseEntry(
    val name: String,
    val author: String,
    val license: String,
    val licenseText: String,
)

private const val APACHE_2_0_TEXT = """Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/

TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

1. Definitions.

"License" shall mean the terms and conditions for use, reproduction,
and distribution as defined by Sections 1 through 9 of this document.

"Licensor" shall mean the copyright owner or entity authorized by
the copyright owner that is granting the License.

"Legal Entity" shall mean the union of the acting entity and all
other entities that control, are controlled by, or are under common
control with that entity. For the purposes of this definition,
"control" means (i) the power, direct or indirect, to cause the
direction or management of such entity, whether by contract or
otherwise, or (ii) ownership of fifty percent (50%) or more of the
outstanding shares, or (iii) beneficial ownership of such entity.

"You" (or "Your") shall mean an individual or Legal Entity
exercising permissions granted by this License.

"Source" form shall mean the preferred form for making modifications,
including but not limited to software source code, documentation
source, and configuration files.

"Object" form shall mean any form resulting from mechanical
transformation or translation of a Source form, including but not
limited to compiled object code, generated documentation,
and conversions to other media types.

"Work" shall mean the work of authorship, whether in Source or
Object form, made available under the License, as indicated by a
copyright notice that is included in or attached to the work
(an example is provided in the Appendix below).

"Derivative Works" shall mean any work, whether in Source or Object
form, that is based on (or derived from) the Work and for which the
editorial revisions, annotations, elaborations, or other modifications
represent, as a whole, an original work of authorship. For the purposes
of this License, Derivative Works shall not include works that remain
separable from, or merely link (or bind by name) to the interfaces of,
the Work and Derivative Works thereof.

"Contribution" shall mean any work of authorship, including
the original version of the Work and any modifications or additions
to that Work or Derivative Works thereof, that is intentionally
submitted to Licensor for inclusion in the Work by the copyright owner
or by an individual or Legal Entity authorized to submit on behalf of
the copyright owner. For the purposes of this definition, "submitted"
means any form of electronic, verbal, or written communication sent
to the Licensor or its representatives, including but not limited to
communication on electronic mailing lists, source code control systems,
and issue tracking systems that are managed by, or on behalf of, the
Licensor for the purpose of discussing and improving the Work, but
excluding communication that is conspicuously marked or otherwise
designated in writing by the copyright owner as "Not a Contribution."

"Contributor" shall mean Licensor and any individual or Legal Entity
on behalf of whom a Contribution has been received by Licensor.

2. Grant of Copyright License. Subject to the terms and conditions of
this License, each Contributor hereby grants to You a perpetual,
worldwide, non-exclusive, no-charge, royalty-free, irrevocable
copyright license to reproduce, prepare Derivative Works of,
publicly display, publicly perform, sublicense, and distribute the
Work and such Derivative Works in Source or Object form.

3. Grant of Patent License. Subject to the terms and conditions of
this License, each Contributor hereby grants to You a perpetual,
worldwide, non-exclusive, no-charge, royalty-free, irrevocable
(except as stated in this section) patent license to make, have made,
use, offer to sell, sell, import, and otherwise transfer the Work,
where such license applies only to those patent claims licensable
by such Contributor that are necessarily infringed by their
Contribution(s) alone or by combination of their Contribution(s)
with the Work to which such Contribution(s) was submitted. If You
institute patent litigation against any entity (including a
cross-claim or counterclaim in a lawsuit) alleging that the Work
or a Contribution incorporated within the Work constitutes direct
or contributory patent infringement, then any patent licenses
granted to You under this License for that Work shall terminate
as of the date such litigation is filed.

4. Redistribution. You may reproduce and distribute copies of the
Work or Derivative Works thereof in any medium, with or without
modifications, and in Source or Object form, provided that You
meet the following conditions:

(a) You must give any other recipients of the Work or
Derivative Works a copy of this License; and

(b) You must cause any modified files to carry prominent notices
stating that You changed the files; and

(c) You must retain, in the Source form of any Derivative Works
that You distribute, all copyright, patent, trademark, and
attribution notices from the Source form of the Work,
excluding those notices that do not pertain to any part of
the Derivative Works; and

(d) If the Work includes a "NOTICE" text file as part of its
distribution, then any Derivative Works that You distribute must
include a readable copy of the attribution notices contained
within such NOTICE file, excluding those notices that do not
pertain to any part of the Derivative Works, in at least one
of the following places: within a NOTICE text file distributed
as part of the Derivative Works; within the Source form or
documentation, if provided along with the Derivative Works; or,
within a display generated by the Derivative Works, if and
wherever such third-party notices normally appear. The contents
of the NOTICE file are for informational purposes only and
do not modify the License. You may add Your own attribution
notices within Derivative Works that You distribute, alongside
or as an addendum to the NOTICE text from the Work, provided
that such additional attribution notices cannot be construed
as modifying the License.

You may add Your own copyright statement to Your modifications and
may provide additional or different license terms and conditions
for use, reproduction, or distribution of Your modifications, or
for any such Derivative Works as a whole, provided Your use,
reproduction, or distribution of the Work otherwise complies with
the conditions stated in this License.

5. Submission of Contributions. Unless You explicitly state otherwise,
any Contribution intentionally submitted for inclusion in the Work
by You to the Licensor shall be under the terms and conditions of
this License, without any additional terms or conditions.
Notwithstanding the above, nothing herein shall supersede or modify
the terms of any separate license agreement you may have executed
with Licensor regarding such Contributions.

6. Trademarks. This License does not grant permission to use the trade
names, trademarks, service marks, or product names of the Licensor,
except as required for reasonable and customary use in describing the
origin of the Work and reproducing the content of the NOTICE file.

7. Disclaimer of Warranty. Unless required by applicable law or
agreed to in writing, Licensor provides the Work (and each
Contributor provides its Contributions) on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied, including, without limitation, any warranties or conditions
of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
PARTICULAR PURPOSE. You are solely responsible for determining the
appropriateness of using or redistributing the Work and assume any
risks associated with Your exercise of permissions under this License.

8. Limitation of Liability. In no event and under no legal theory,
whether in tort (including negligence), contract, or otherwise,
unless required by applicable law (such as deliberate and grossly
negligent acts) or agreed to in writing, shall any Contributor be
liable to You for damages, including any direct, indirect, incidental,
special, exemplary, or consequential damages (including, but not
limited to, procurement of substitute goods or services; loss of use,
data, or profits; or business interruption) however caused and on any
theory of liability, whether in contract, strict liability, or tort
(including negligence or otherwise) arising in any way out of the use
or inability to use the Work (including but not limited to any
liability arising from Your use of any third-party software or
services), even if such Contributor has been advised of the
possibility of such damages.

9. Accepting Warranty or Additional Liability. While redistributing
the Work or Derivative Works thereof, You may choose to offer,
and charge a fee for, acceptance of support, warranty, indemnity,
or other liability obligations and/or rights consistent with this
License. However, in accepting such obligations, You may act only
on Your own behalf and on Your sole responsibility, not on behalf
of any other Contributor, and only if You agree to indemnify,
defend, and hold each Contributor harmless for any liability
incurred by, or claims asserted against, such Contributor by reason
of your accepting any such warranty or additional liability.

END OF TERMS AND CONDITIONS

APPENDIX: How to apply the Apache License to your work.

To apply the Apache License to your work, attach the following
boilerplate notice, with the fields enclosed by brackets "[]"
replaced with your own identifying information. (Don't include
the brackets!)  The text should be enclosed in the appropriate
comment syntax for the file format. We also recommend that a
file or class name and description of purpose be included on the
same "printed page" as the copyright notice for easier
identification within third-party archives.

Copyright [yyyy] [name of copyright owner]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License."""

private const val MIT_TEXT = """MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE."""

private const val CHAQUOPY_TEXT = """Chaquopy License

Chaquopy is free for open-source projects and for personal use.
Commercial use requires a Chaquopy license.

See https://chaquo.com/chaquopy/ for the full license terms
and pricing."""

/** 开源许可列表（按实际依赖，见 app/build.gradle.kts）。 */
private fun licenseEntries(): List<LicenseEntry> = listOf(
    LicenseEntry("Miuix", "YuKongA", "Apache-2.0", APACHE_2_0_TEXT),
    LicenseEntry("Kotlin", "JetBrains", "Apache-2.0", APACHE_2_0_TEXT),
    LicenseEntry("Jetpack Compose", "Google", "Apache-2.0", APACHE_2_0_TEXT),
    LicenseEntry("AndroidX Core", "Google", "Apache-2.0", APACHE_2_0_TEXT),
    LicenseEntry("AndroidX Lifecycle", "Google", "Apache-2.0", APACHE_2_0_TEXT),
    LicenseEntry("AndroidX Activity", "Google", "Apache-2.0", APACHE_2_0_TEXT),
    LicenseEntry("Material 3", "Google", "Apache-2.0", APACHE_2_0_TEXT),
    LicenseEntry("Material Icons Core", "Google", "Apache-2.0", APACHE_2_0_TEXT),
    LicenseEntry("Compose Animation", "Google", "Apache-2.0", APACHE_2_0_TEXT),
    LicenseEntry("requests", "Kenneth Reitz", "Apache-2.0", APACHE_2_0_TEXT),
    LicenseEntry("BeautifulSoup4", "Leonard Richardson", "MIT", MIT_TEXT),
    LicenseEntry("PyYAML", "Kirill Simonov", "MIT", MIT_TEXT),
    LicenseEntry("Chaquopy", "Chaquopy Ltd", "Chaquopy License", CHAQUOPY_TEXT),
)

/**
 * 开源许可页：按许可证分组，点行进详情页看完整许可文本。
 */
@Composable
private fun LicensePage(onEntryClick: (LicenseEntry) -> Unit) {
    val groups = licenseEntries().groupBy { it.license }
    val order = listOf("Apache-2.0", "MIT", "Chaquopy License")
    Column(modifier = Modifier.fillMaxSize().background(pageBackground())) {
        Text(
            text = "开源许可",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 24.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            for (license in order) {
                val entries = groups[license] ?: continue
                item {
                Column {
                    Text(
                        text = license,
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBackground()),
                    ) {
                        entries.forEachIndexed { i, e ->
                            if (i > 0) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp)
                                        .height(0.5.dp).background(MiuixTheme.colorScheme.outline),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                                    .clickable { onEntryClick(e) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = e.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MiuixTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = e.author,
                                        fontSize = 14.sp,
                                        color = MiuixTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                                Text(
                                    text = "〉",
                                    fontSize = 14.sp,
                                    color = MiuixTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

/** 许可详情页：完整许可文本。 */
@Composable
private fun LicenseDetailPage(entry: LicenseEntry) {
    Column(modifier = Modifier.fillMaxSize().background(pageBackground())) {
        Text(
            text = entry.name,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 24.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
        )
        Text(
            text = entry.license,
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(cardBackground())
                .padding(16.dp),
        ) {
            Text(
                text = entry.licenseText,
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}
