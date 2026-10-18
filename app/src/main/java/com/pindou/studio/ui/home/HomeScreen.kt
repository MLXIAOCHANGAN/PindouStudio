package com.pindou.studio.ui.home

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pindou.studio.core.Palette
import com.pindou.studio.core.ProjectSummary
import com.pindou.studio.ui.components.ChipButton
import com.pindou.studio.ui.components.EmptyState
import com.pindou.studio.ui.components.IconSquare
import com.pindou.studio.ui.components.PIcon
import com.pindou.studio.ui.components.PIconView
import com.pindou.studio.ui.components.SectionTitle
import com.pindou.studio.util.FileUtils
import java.io.File

/** 首页：作品列表 + 新建 / 导入入口。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenProject: (projectId: String, openImport: Boolean) -> Unit,
    onOpenPalettes: () -> Unit,
) {
    val vm: HomeViewModel = viewModel()
    val context = LocalContext.current
    var showNewDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ProjectSummary?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<ProjectSummary?>(null) }
    var menuTarget by remember { mutableStateOf<ProjectSummary?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val name = "import_" + System.currentTimeMillis() + ".json"
            val file = FileUtils.copyUriToCache(context, uri, name)
            if (file != null) {
                val id = vm.importProject(file)
                if (id != null) {
                    vm.message = "导入成功"
                    onOpenProject(id, false)
                }
            } else {
                vm.message = "读取文件失败"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PIconView(PIcon.Palette, MaterialTheme.colorScheme.primary, Modifier.size(26.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("拼豆工坊", fontWeight = FontWeight.SemiBold)
                    }
                },
                actions = {
                    IconSquare(PIcon.Palette, size = 42) { onOpenPalettes() }
                    IconSquare(PIcon.Info, size = 42) { showAbout = true }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewDialog = true },
                icon = { PIconView(PIcon.Plus, MaterialTheme.colorScheme.onPrimary, Modifier.size(20.dp)) },
                text = { Text("新建作品") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionCard("从图片生成", PIcon.Image, Modifier.weight(1f)) {
                    val id = vm.createProject(
                        "图片转图纸 " + vm.projects.size,
                        64, 64,
                        vm.defaultPaletteId(),
                    )
                    onOpenProject(id, true)
                }
                ActionCard("导入工程", PIcon.Upload, Modifier.weight(1f)) {
                    importLauncher.launch("*/*")
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle("我的作品", "${vm.projects.size} 个")
            }
            if (vm.projects.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        PIcon.Grid,
                        "还没有作品",
                        "点击右下角「新建作品」开始，\n或从图片一键生成拼豆图纸",
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(vm.projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            onClick = { onOpenProject(project.id, false) },
                            onLongClick = { menuTarget = project },
                        )
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        NewProjectDialog(
            palettes = vm.palettes,
            defaultPaletteId = vm.defaultPaletteId(),
            onDismiss = { showNewDialog = false },
            onCreate = { name, w, h, paletteId ->
                val id = vm.createProject(name, w, h, paletteId)
                showNewDialog = false
                onOpenProject(id, false)
            },
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("关于拼豆工坊") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "一款离线可用的拼豆图纸编辑器。\n\n" +
                            "• 画笔 / 橡皮 / 油漆桶 / 吸管 / 选区\n" +
                            "• 对称绘制、旋转翻转、扩裁画布\n" +
                            "• 以图生草图（OKLab 感知色差 + 抖动 + 合并相近色）\n" +
                            "• 实时光变 / 温变 / 夜光显示模式\n" +
                            "• 导出带坐标与色号的打印图纸（支持 4/9/16 分块）\n" +
                            "• 内置 17 套色卡（MARD / Perler / Artkal / Hama / DMC …）\n" +
                            "• 兼容 pindou-draw 的 .pd 工程文件\n\n" +
                            "v1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("知道了") } },
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名作品") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("作品名称") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.rename(target.id, renameText.trim())
                    renameTarget = null
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("取消") } },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除作品") },
            text = { Text("确定删除「${target.name}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(target.id)
                    deleteTarget = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } },
        )
    }

    menuTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { menuTarget = null },
            title = { Text(target.name) },
            text = {
                Column {
                    MenuRow("重命名") {
                        renameText = target.name
                        renameTarget = target
                        menuTarget = null
                    }
                    MenuRow("创建副本") {
                        vm.duplicate(target.id)
                        menuTarget = null
                    }
                    MenuRow("导出工程文件 (.pdp)") {
                        val dir = File(context.cacheDir, "export").apply { mkdirs() }
                        val file = File(dir, target.name + ".pdp")
                        if (vm.exportProject(target.id, file, false)) {
                            FileUtils.saveFileToDownloads(context, file, target.name + ".pdp", "application/json")
                            vm.message = "已导出到下载目录"
                        } else {
                            vm.message = "导出失败"
                        }
                        menuTarget = null
                    }
                    MenuRow("导出 pindou-draw 工程 (.pd)") {
                        val dir = File(context.cacheDir, "export").apply { mkdirs() }
                        val file = File(dir, target.name + ".pd")
                        if (vm.exportProject(target.id, file, true)) {
                            FileUtils.saveFileToDownloads(context, file, target.name + ".pd", "application/json")
                            vm.message = "已导出到下载目录"
                        } else {
                            vm.message = "导出失败"
                        }
                        menuTarget = null
                    }
                    MenuRow("删除", danger = true) {
                        deleteTarget = target
                        menuTarget = null
                    }
                }
            },
            confirmButton = { TextButton(onClick = { menuTarget = null }) { Text("关闭") } },
        )
    }
}

@Composable
private fun MenuRow(text: String, danger: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
    )
}

@Composable
private fun ActionCard(text: String, icon: PIcon, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                PIconView(icon, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun ProjectCard(project: ProjectSummary, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.12f)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                val path = project.thumbnailPath
                if (path != null) {
                    val bmp = remember(path) {
                        runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        EmptyState(PIcon.Image, "", "")
                    }
                } else {
                    PIconView(PIcon.Grid, MaterialTheme.colorScheme.outline, Modifier.size(38.dp))
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                        .clickable { onLongClick() }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    PIconView(PIcon.More, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(15.dp))
                }
            }
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${project.width}×${project.height} · ${project.beadCount} 颗",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    FileUtils.formatDate(project.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewProjectDialog(
    palettes: List<Palette>,
    defaultPaletteId: String,
    onDismiss: () -> Unit,
    onCreate: (String, Int, Int, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("48") }
    var height by remember { mutableStateOf("48") }
    var paletteId by remember { mutableStateOf(defaultPaletteId) }
    var expanded by remember { mutableStateOf(false) }
    val selected = palettes.firstOrNull { it.id == paletteId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建作品") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("作品名称") },
                    placeholder = { Text("我的拼豆图纸") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = width,
                        onValueChange = { width = it.filter { ch -> ch.isDigit() }.take(4) },
                        singleLine = true,
                        label = { Text("宽(格)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it.filter { ch -> ch.isDigit() }.take(4) },
                        singleLine = true,
                        label = { Text("高(格)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(32, 48, 64, 96, 128).forEach { preset ->
                        ChipButton("${preset}²", width == preset.toString() && height == preset.toString()) {
                            width = preset.toString()
                            height = preset.toString()
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selected?.name ?: "选择色卡",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("色卡") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        palettes.forEach { palette ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        palette.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                    )
                                },
                                onClick = {
                                    paletteId = palette.id
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = width.toIntOrNull()?.coerceIn(1, 1024) ?: 48
                val h = height.toIntOrNull()?.coerceIn(1, 1024) ?: 48
                onCreate(name.trim(), w, h, paletteId)
            }) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
