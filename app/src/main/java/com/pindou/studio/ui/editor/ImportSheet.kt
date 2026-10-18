package com.pindou.studio.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.pindou.studio.core.BeadGrid
import com.pindou.studio.core.DitherMode
import com.pindou.studio.core.PaletteColor
import com.pindou.studio.core.QuantizeMode
import com.pindou.studio.core.QuantizeOptions
import com.pindou.studio.core.Quantizer
import com.pindou.studio.ui.components.ChipButton
import com.pindou.studio.ui.components.IconSquare
import com.pindou.studio.ui.components.LabeledSlider
import com.pindou.studio.ui.components.PIcon
import com.pindou.studio.ui.components.SectionTitle
import com.pindou.studio.ui.components.SwitchRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** 以图生草图面板。 */
@Composable
fun ImportSheetContent(
    vm: EditorViewModel,
    source: Bitmap?,
    onPickImage: () -> Unit,
    onDone: () -> Unit,
) {
    if (source == null) {
        Column(
            Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("选择一张图片，自动转成拼豆图纸", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "支持照片与插画，转换在本地完成，不会上传",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onPickImage) { Text("选择图片") }
            Spacer(Modifier.height(24.dp))
        }
        return
    }

    var targetWidth by remember(source) { mutableStateOf(64) }
    var mode by remember(source) { mutableStateOf(QuantizeMode.AVERAGE) }
    var dither by remember(source) { mutableStateOf(DitherMode.NONE) }
    var merge by remember(source) { mutableStateOf(0f) }
    var removeBg by remember(source) { mutableStateOf(false) }
    var brightness by remember(source) { mutableStateOf(0f) }
    var contrast by remember(source) { mutableStateOf(0f) }
    var saturation by remember(source) { mutableStateOf(0f) }
    var excluded by remember(source) { mutableStateOf<Set<String>>(emptySet()) }
    var showExclude by remember { mutableStateOf(false) }

    var preview by remember(source) { mutableStateOf<Bitmap?>(null) }
    var result by remember(source) { mutableStateOf<BeadGrid?>(null) }
    var computing by remember(source) { mutableStateOf(true) }

    LaunchedEffect(source, targetWidth, mode, dither, merge, removeBg, brightness, contrast, saturation, excluded) {
        computing = true
        delay(140)
        val options = QuantizeOptions(
            targetWidth = targetWidth,
            mode = mode,
            dither = dither,
            mergeSimilarity = merge,
            removeBackground = removeBg,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            excludedCodes = excluded,
        )
        val grid = withContext(Dispatchers.Default) {
            Quantizer.quantize(source, vm.palette, options)
        }
        val bmp = withContext(Dispatchers.Default) {
            Quantizer.renderPreview(grid, vm.palette, 3)
        }
        result = grid
        preview = bmp
        computing = false
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 300.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            val shown = preview
            if (shown != null) {
                Image(
                    bitmap = shown.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            if (computing) {
                CircularProgressIndicator(Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                result?.let { "输出 ${it.width}×${it.height} 格 · ${it.paintedCount()} 颗豆子" } ?: "计算中…",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onPickImage) { Text("换一张") }
        }

        SectionTitle("尺寸")
        LabeledSlider(
            title = "目标宽度",
            value = targetWidth.toFloat(),
            valueRange = 8f..256f,
            valueText = "${targetWidth} 格",
        ) { targetWidth = it.toInt() }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(24, 48, 64, 96, 128).forEach { preset ->
                ChipButton("$preset", targetWidth == preset) { targetWidth = preset }
            }
        }

        SectionTitle("模式")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipButton("照片", mode == QuantizeMode.AVERAGE) { mode = QuantizeMode.AVERAGE }
            ChipButton("插画", mode == QuantizeMode.DOMINANT) { mode = QuantizeMode.DOMINANT }
        }
        Text(
            if (mode == QuantizeMode.AVERAGE) "平均：相邻像素混合，过渡更自然，适合照片"
            else "主色：取区域内出现最多的颜色，色块更锐利，适合插画与像素图",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        SectionTitle("抖动")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DitherMode.entries.forEach { d ->
                ChipButton(d.label, dither == d) { dither = d }
            }
        }

        SectionTitle("优化")
        LabeledSlider(
            title = "合并相近色",
            value = merge,
            valueRange = 0f..0.14f,
            valueText = if (merge <= 0f) "关闭" else String.format("%.3f", merge),
        ) { merge = it }
        SwitchRow("移除背景", "从边缘清除主要背景色", removeBg) { removeBg = it }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("排除色号", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = { showExclude = true }) {
                Text(if (excluded.isEmpty()) "未排除" else "已排除 ${excluded.size} 个")
            }
        }

        SectionTitle("色彩调整")
        LabeledSlider("亮度", brightness, -100f..100f, "${brightness.toInt()}") { brightness = it }
        LabeledSlider("对比度", contrast, -100f..100f, "${contrast.toInt()}") { contrast = it }
        LabeledSlider("饱和度", saturation, -100f..100f, "${saturation.toInt()}") { saturation = it }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val grid = result ?: return@Button
                    vm.applyImportedGrid(grid)
                    vm.message = "已生成拼豆图纸"
                    onDone()
                },
                enabled = result != null && !computing,
                modifier = Modifier.weight(1f),
            ) { Text("应用到画布") }
            ChipButton("取消", false) { onDone() }
        }
        Spacer(Modifier.height(28.dp))
    }

    if (showExclude) {
        ExcludeCodesDialog(
            vm = vm,
            excluded = excluded,
            onDismiss = { showExclude = false },
            onChange = { excluded = it },
        )
    }
}

@Composable
private fun ExcludeCodesDialog(
    vm: EditorViewModel,
    excluded: Set<String>,
    onDismiss: () -> Unit,
    onChange: (Set<String>) -> Unit,
) {
    var local by remember { mutableStateOf(excluded) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排除色号") },
        text = {
            Column {
                Text(
                    "被排除的色号不会用于图片转换",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(60.dp),
                    modifier = Modifier.heightIn(max = 380.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(vm.palette.colors) { color ->
                        val isExcluded = color.code.uppercase() in local
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    val code = color.code.uppercase()
                                    local = if (isExcluded) local - code else local + code
                                }
                                .padding(vertical = 3.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(color.argb)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isExcluded) {
                                    IconSquare(PIcon.Close, size = 26) {}
                                }
                            }
                            Text(
                                color.code,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isExcluded) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onChange(local)
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = {
                local = emptySet()
                onChange(emptySet())
                onDismiss()
            }) { Text("全部启用") }
        },
    )
}
