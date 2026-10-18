package com.pindou.studio.ui.editor

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pindou.studio.core.ChartExporter
import com.pindou.studio.core.ChartOptions
import com.pindou.studio.core.SvgExporter
import com.pindou.studio.core.SvgOptions
import com.pindou.studio.ui.components.ChipButton
import com.pindou.studio.ui.components.LabeledSlider
import com.pindou.studio.ui.components.SectionTitle
import com.pindou.studio.ui.components.SwitchRow
import com.pindou.studio.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** 导出图纸面板。 */
@Composable
fun ExportSheetContent(vm: EditorViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    var cellPx by remember { mutableStateOf(24) }
    var showCodes by remember { mutableStateOf(false) }
    var showCoords by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(true) }
    var showMajor by remember { mutableStateOf(true) }
    var showStats by remember { mutableStateOf(true) }
    var roundBeads by remember { mutableStateOf(false) }
    var tileCount by remember { mutableStateOf(1) }
    var title by remember { mutableStateOf(vm.project.name) }
    var busy by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var previewing by remember { mutableStateOf(true) }

    fun buildOptions(px: Int) = ChartOptions(
        cellPx = px,
        showCodes = showCodes,
        showCoordinates = showCoords,
        showGrid = showGrid,
        showMajorGrid = showMajor,
        showBeadStats = showStats,
        title = title,
        tileCount = tileCount,
        roundBeads = roundBeads,
    )

    LaunchedEffect(cellPx, showCodes, showCoords, showGrid, showMajor, showStats, roundBeads, tileCount, title) {
        previewing = true
        delay(120)
        val bmp = withContext(Dispatchers.Default) {
            runCatching {
                ChartExporter.renderWindow(
                    vm.project.grid, vm.palette, buildOptions(7),
                    0, 0, vm.project.width, vm.project.height,
                )
            }.getOrNull()
        }
        preview = bmp
        previewing = false
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
                .heightIn(min = 160.dp, max = 300.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF3F4F6)),
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
            if (previewing) CircularProgressIndicator(Modifier.size(26.dp))
        }

        SectionTitle("图纸设置")
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            singleLine = true,
            label = { Text("图纸标题") },
            modifier = Modifier.fillMaxWidth(),
        )
        LabeledSlider(
            title = "每颗豆子像素",
            value = cellPx.toFloat(),
            valueRange = 8f..64f,
            valueText = "${cellPx}px",
        ) { cellPx = it.toInt() }
        SwitchRow("显示色号", checked = showCodes) { showCodes = it }
        SwitchRow("显示坐标编号", checked = showCoords) { showCoords = it }
        SwitchRow("显示网格线", checked = showGrid) { showGrid = it }
        SwitchRow("加粗主网格（每 8 格）", checked = showMajor) { showMajor = it }
        SwitchRow("附加用量统计", checked = showStats) { showStats = it }
        SwitchRow("豆子画成圆形", checked = roundBeads) { roundBeads = it }

        SectionTitle("分块导出")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 4, 9, 16).forEach { n ->
                ChipButton(if (n == 1) "整张" else "$n 张", tileCount == n) { tileCount = n }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    busy = true
                    Thread {
                        try {
                            val tiles = ChartExporter.render(vm.project.grid, vm.palette, buildOptions(cellPx))
                            var saved = 0
                            tiles.forEach { tile ->
                                val name = FileUtils.timestampName(
                                    sanitize(vm.project.name) + if (tile.count > 1) "_${tile.index}of${tile.count}" else "",
                                    "png",
                                )
                                if (FileUtils.saveBitmapToGallery(context, tile.bitmap, name) != null) saved++
                                tile.bitmap.recycle()
                            }
                            vm.message = "已保存 $saved 张图纸到相册 Pictures/拼豆工坊"
                        } catch (e: Exception) {
                            vm.message = "导出失败：${e.message}"
                        } finally {
                            busy = false
                        }
                    }.start()
                },
                enabled = !busy,
                modifier = Modifier.weight(1f),
            ) { Text(if (busy) "导出中…" else "保存到相册") }
            ChipButton("分享", false) {
                busy = true
                Thread {
                    try {
                        val tiles = ChartExporter.render(vm.project.grid, vm.palette, buildOptions(cellPx))
                        val file = FileUtils.writeTempPng(context, tiles.first().bitmap, FileUtils.timestampName("pindou", "png"))
                        val uri = FileUtils.shareUri(context, file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "分享图纸"))
                        tiles.forEach { it.bitmap.recycle() }
                    } catch (e: Exception) {
                        vm.message = "分享失败：${e.message}"
                    } finally {
                        busy = false
                    }
                }.start()
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChipButton("导出 SVG 矢量图", false) {
                Thread {
                    try {
                        val svg = SvgExporter.render(
                            vm.project.grid,
                            vm.palette,
                            SvgOptions(
                                cellPx = cellPx,
                                showCodes = showCodes,
                                showGrid = showGrid,
                                showCoordinates = showCoords,
                                showMajorGrid = showMajor,
                                title = title,
                            ),
                        )
                        val dir = java.io.File(context.cacheDir, "export").apply { mkdirs() }
                        val file = java.io.File(dir, FileUtils.timestampName(sanitize(vm.project.name), "svg"))
                        file.writeText(svg)
                        FileUtils.saveFileToDownloads(context, file, file.name, "image/svg+xml")
                        vm.message = "SVG 已保存到下载目录"
                    } catch (e: Exception) {
                        vm.message = "SVG 导出失败：${e.message}"
                    }
                }.start()
            }
            ChipButton("用量清单 CSV", false) {
                Thread {
                    try {
                        val csv = SvgExporter.usageCsv(vm.project.grid, vm.palette)
                        val dir = java.io.File(context.cacheDir, "export").apply { mkdirs() }
                        val file = java.io.File(dir, FileUtils.timestampName(sanitize(vm.project.name) + "_用量", "csv"))
                        file.writeText("\uFEFF" + csv)
                        FileUtils.saveFileToDownloads(context, file, file.name, "text/csv")
                        vm.message = "用量清单已保存到下载目录"
                    } catch (e: Exception) {
                        vm.message = "清单导出失败：${e.message}"
                    }
                }.start()
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChipButton("关闭", false) { onDone() }
        }
        Spacer(Modifier.height(28.dp))
    }
}

private fun sanitize(name: String): String =
    name.replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "pindou" }
