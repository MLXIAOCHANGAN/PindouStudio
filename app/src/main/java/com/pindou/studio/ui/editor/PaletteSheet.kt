package com.pindou.studio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pindou.studio.PindouApp
import com.pindou.studio.core.DisplayResolver
import com.pindou.studio.core.Palette
import com.pindou.studio.core.PaletteColor
import com.pindou.studio.ui.components.ChipButton
import com.pindou.studio.ui.components.ColorSwatch
import com.pindou.studio.ui.components.SectionTitle

/** 调色板面板。 */
@Composable
fun PaletteSheetContent(vm: EditorViewModel) {
    val context = LocalContext.current
    val repo = remember { PindouApp.from(context).paletteRepository }
    val palettes = remember(vm.palette.id) { repo.all() }
    var filter by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) } // 0 全部 1 普通 2 特殊
    var showPalettePicker by remember { mutableStateOf(false) }

    val colors = remember(filter, tab, vm.palette.id, vm.displayMode) {
        vm.palette.colors.withIndex()
            .filter { (_, c) ->
                (filter.isBlank() || c.code.contains(filter, ignoreCase = true)) &&
                    when (tab) {
                        1 -> !c.isSpecial
                        2 -> c.isSpecial
                        else -> true
                    }
            }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showPalettePicker = true }
                    .padding(vertical = 6.dp),
            ) {
                Text("当前色卡", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${vm.palette.name}  ·  ${vm.palette.size} 色", style = MaterialTheme.typography.titleSmall)
            }
            TextButton(onClick = { showPalettePicker = true }) { Text("切换") }
        }

        OutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            singleLine = true,
            label = { Text("搜索色号") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipButton("全部", tab == 0) { tab = 0 }
            ChipButton("普通色", tab == 1) { tab = 1 }
            ChipButton("特殊色", tab == 2) { tab = 2 }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "共 ${colors.size} 个色号",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(52.dp),
            state = rememberLazyGridState(),
            modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(colors, key = { it.value.code }) { indexed ->
                val index = indexed.index + 1
                val color = indexed.value
                val display = DisplayResolver.resolve(color, vm.displayMode)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ColorSwatch(
                        argb = display,
                        code = "",
                        selected = index == vm.colorIndex,
                        size = 40,
                    ) { vm.chooseColor(index) }
                    Text(
                        color.code,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        color = if (index == vm.colorIndex) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (color.isSpecial) {
                        Text(
                            PaletteColor.typeLabel(color.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showPalettePicker) {
        AlertDialog(
            onDismissRequest = { showPalettePicker = false },
            title = { Text("选择色卡") },
            text = {
                LazyVerticalGrid(
                    columns = GridColumns1,
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(palettes, key = { it.id }) { palette ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (palette.id == vm.palette.id) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .clickable {
                                    vm.switchPalette(palette)
                                    showPalettePicker = false
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(palette.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                Text(
                                    "${palette.size} 色" + if (palette.builtin) " · 内置" else " · 我的",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row {
                                palette.colors.take(5).forEach { c ->
                                    Box(
                                        Modifier
                                            .padding(start = 2.dp)
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(c.argb))
                                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp)),
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPalettePicker = false }) { Text("关闭") } },
        )
    }
}

private val GridColumns1 = GridCells.Fixed(1)
