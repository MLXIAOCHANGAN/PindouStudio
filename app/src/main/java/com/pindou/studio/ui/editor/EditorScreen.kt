package com.pindou.studio.ui.editor

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pindou.studio.core.DisplayMode
import com.pindou.studio.core.SymmetryMode
import com.pindou.studio.core.Tool
import com.pindou.studio.ui.components.IconSquare
import com.pindou.studio.ui.components.PIcon
import com.pindou.studio.ui.components.PIconView
import com.pindou.studio.util.FileUtils
import kotlinx.coroutines.launch

/** 编辑器主界面。 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(projectId: String, autoOpenImport: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
    val vm: EditorViewModel = viewModel(
        key = projectId,
        factory = EditorViewModel.Factory(application, projectId),
    )
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(projectId) { vm.load() }

    var activeSheet by remember { mutableStateOf(EditorSheet.None) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var pendingImage by remember { mutableStateOf<Bitmap?>(null) }

    var autoOpened by remember { mutableStateOf(false) }
    LaunchedEffect(autoOpenImport, vm.loaded) {
        if (autoOpenImport && vm.loaded && !autoOpened) {
            autoOpened = true
            activeSheet = EditorSheet.Import
        }
    }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = FileUtils.decodeSampledBitmap(context, uri, 2048)
            if (bmp != null) {
                pendingImage = bmp
                activeSheet = EditorSheet.Import
            } else {
                vm.message = "图片读取失败"
            }
        }
    }

    val baseLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = FileUtils.decodeSampledBitmap(context, uri, 1600)
            if (bmp != null) {
                vm.setBaseImage(bmp)
                vm.baseVisible = true
                vm.message = "底图已导入"
            } else {
                vm.message = "图片读取失败"
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            EditorTopBar(
                title = vm.project.name,
                subtitle = "${vm.project.width}×${vm.project.height} · ${vm.palette.name}",
                saving = vm.saving,
                canUndo = vm.canUndo,
                canRedo = vm.canRedo,
                onBack = {
                    vm.persistNow()
                    onBack()
                },
                onTitleClick = {
                    renameText = vm.project.name
                    showRename = true
                },
                onUndo = { vm.undo() },
                onRedo = { vm.redo() },
                onMore = { showMoreMenu = true },
                menu = {
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                    ) {
                        MenuItem("以图生草图") { showMoreMenu = false; importLauncher.launch("image/*") }
                        MenuItem("导入底图") { showMoreMenu = false; baseLauncher.launch("image/*") }
                        MenuItem(if (vm.baseImage == null) "底图已空" else "移除底图") {
                            showMoreMenu = false
                            vm.setBaseImage(null)
                        }
                        MenuItem("画布尺寸") { showMoreMenu = false; activeSheet = EditorSheet.CanvasSize }
                        MenuItem("显示设置") { showMoreMenu = false; activeSheet = EditorSheet.Display }
                        MenuItem("导出图纸") { showMoreMenu = false; activeSheet = EditorSheet.Export }
                        MenuItem("图像操作") { showMoreMenu = false; activeSheet = EditorSheet.ImageOps }
                        MenuItem("清空画布") {
                            showMoreMenu = false
                            vm.clearCanvas()
                        }
                    }
                },
            )

            ToolBar(
                vm = vm,
                onOpenPalette = { activeSheet = EditorSheet.Palette },
                onOpenStats = { activeSheet = EditorSheet.Stats },
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                BeadCanvas(vm, Modifier.fillMaxSize())
                if (vm.selection != null) {
                    SelectionBar(
                        vm = vm,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    )
                }
                ZoomControls(
                    vm = vm,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                )
                if (vm.baseImage != null && vm.baseVisible) {
                    BaseImageBadge(
                        vm = vm,
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                    )
                }
            }

            ColorBar(
                vm = vm,
                onOpenPalette = { activeSheet = EditorSheet.Palette },
            )
        }
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("重命名") },
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
                    vm.rename(renameText.trim())
                    showRename = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("取消") } },
        )
    }

    if (activeSheet != EditorSheet.None) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeSheet = EditorSheet.None; pendingImage = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            when (activeSheet) {
                EditorSheet.Palette -> PaletteSheetContent(vm = vm)
                EditorSheet.Stats -> StatsSheetContent(vm = vm)
                EditorSheet.Import -> ImportSheetContent(
                    vm = vm,
                    source = pendingImage,
                    onPickImage = { importLauncher.launch("image/*") },
                    onDone = { activeSheet = EditorSheet.None; pendingImage = null },
                )
                EditorSheet.Export -> ExportSheetContent(vm = vm, onDone = { activeSheet = EditorSheet.None })
                EditorSheet.CanvasSize -> CanvasSizeSheetContent(vm = vm, onDone = { activeSheet = EditorSheet.None })
                EditorSheet.Display -> DisplaySheetContent(vm = vm)
                EditorSheet.ImageOps -> ImageOpsSheetContent(vm = vm, onDone = { activeSheet = EditorSheet.None })
                EditorSheet.None -> Unit
            }
        }
    }
}

/** 底部面板类型。 */
enum class EditorSheet {
    None, Palette, Stats, Import, Export, CanvasSize, Display, ImageOps,
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text) }, onClick = onClick)
}

@Composable
private fun EditorTopBar(
    title: String,
    subtitle: String,
    saving: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onBack: () -> Unit,
    onTitleClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onMore: () -> Unit,
    menu: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconSquare(PIcon.Back, size = 40, onClick = onBack)
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onTitleClick() }
                    .padding(horizontal = 6.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (saving) "保存中…" else subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconSquare(PIcon.Undo, enabled = canUndo, size = 40, onClick = onUndo)
            IconSquare(PIcon.Redo, enabled = canRedo, size = 40, onClick = onRedo)
            Box {
                IconSquare(PIcon.More, size = 40, onClick = onMore)
                menu()
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    }
}

@Composable
private fun ToolBar(
    vm: EditorViewModel,
    onOpenPalette: () -> Unit,
    onOpenStats: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconSquare(PIcon.Pen, "画笔", vm.tool == Tool.PEN) { vm.selectTool(Tool.PEN) }
        IconSquare(PIcon.Eraser, "橡皮", vm.tool == Tool.ERASER) { vm.selectTool(Tool.ERASER) }
        IconSquare(PIcon.Bucket, "填充", vm.tool == Tool.FILL) { vm.selectTool(Tool.FILL) }
        IconSquare(PIcon.Dropper, "吸管", vm.tool == Tool.EYEDROPPER) { vm.selectTool(Tool.EYEDROPPER) }
        IconSquare(PIcon.Select, "选区", vm.tool == Tool.SELECT) { vm.selectTool(Tool.SELECT) }
        IconSquare(PIcon.Move, "移动", vm.tool == Tool.PAN) { vm.selectTool(Tool.PAN) }
        VerticalDivider()
        IconSquare(PIcon.Symmetry, vm.symmetry.label, vm.symmetry != SymmetryMode.NONE) {
            vm.symmetry = when (vm.symmetry) {
                SymmetryMode.NONE -> SymmetryMode.HORIZONTAL
                SymmetryMode.HORIZONTAL -> SymmetryMode.VERTICAL
                SymmetryMode.VERTICAL -> SymmetryMode.QUAD
                SymmetryMode.QUAD -> SymmetryMode.DIAGONAL
                SymmetryMode.DIAGONAL -> SymmetryMode.NONE
            }
        }
        IconSquare(PIcon.Grid, "网格", vm.showGrid) { vm.toggleShowGrid() }
        IconSquare(PIcon.Tag, "色号", vm.showCodes) { vm.toggleShowCodes() }
        IconSquare(PIcon.Light, vm.displayMode.label, vm.displayMode != DisplayMode.STANDARD) {
            val modes = DisplayMode.entries
            val next = modes[(modes.indexOf(vm.displayMode) + 1) % modes.size]
            vm.changeDisplayMode(next)
        }
        VerticalDivider()
        IconSquare(PIcon.Palette, "色卡", false) { onOpenPalette() }
        IconSquare(PIcon.Stats, "用量", false) { onOpenStats() }
        IconSquare(PIcon.Fit, "适配", false) { vm.fitToScreen() }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(26.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    )
}

@Composable
private fun ColorBar(vm: EditorViewModel, onOpenPalette: () -> Unit) {
    val color = vm.palette.colorAt(vm.colorIndex)
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onOpenPalette() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(color?.argb ?: 0xFF000000.toInt()), CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                (color?.code ?: "-") + "  " + (color?.hex ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "当前色卡：${vm.palette.name} · 点击打开调色板",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PIconView(PIcon.Palette, MaterialTheme.colorScheme.primary, Modifier.size(22.dp))
    }
}

@Composable
private fun SelectionBar(vm: EditorViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconSquare(PIcon.Bucket, "填充", size = 40) { vm.fillSelection() }
        IconSquare(PIcon.Trash, "删除", size = 40) { vm.deleteSelection() }
        IconSquare(PIcon.Copy, "复制", size = 40) { vm.copySelection() }
        IconSquare(PIcon.Cut, "剪切", size = 40) { vm.cutSelection() }
        IconSquare(PIcon.Paste, "粘贴", size = 40) { vm.pasteSelection() }
        IconSquare(PIcon.Close, "取消", size = 40) { vm.clearSelection() }
    }
}

@Composable
private fun ZoomControls(vm: EditorViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
    ) {
        IconSquare(PIcon.ZoomIn, size = 36) { vm.zoomIn() }
        IconSquare(PIcon.ZoomOut, size = 36) { vm.zoomOut() }
        IconSquare(PIcon.Fit, size = 36) { vm.fitToScreen() }
    }
}

@Composable
private fun BaseImageBadge(vm: EditorViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable { vm.baseVisible = false }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("底图显示中 · 点击隐藏", style = MaterialTheme.typography.labelSmall)
    }
}
