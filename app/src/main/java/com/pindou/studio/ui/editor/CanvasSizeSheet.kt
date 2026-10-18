package com.pindou.studio.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pindou.studio.core.BeadGrid
import com.pindou.studio.ui.components.ChipButton
import com.pindou.studio.ui.components.SectionTitle

/** 画布尺寸面板：扩裁 / 新建。 */
@Composable
fun CanvasSizeSheetContent(vm: EditorViewModel, onDone: () -> Unit) {
    var width by remember(vm.project.width) { mutableStateOf(vm.project.width.toString()) }
    var height by remember(vm.project.height) { mutableStateOf(vm.project.height.toString()) }
    var anchor by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        SectionTitle("当前画布", "${vm.project.width} × ${vm.project.height}")
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
            ChipButton("内容靠左上", anchor == 0) { anchor = 0 }
            ChipButton("内容居中", anchor == 1) { anchor = 1 }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "变大时新增区域为空白；变小时超出部分会被裁掉（可撤销）",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val w = width.toIntOrNull()?.coerceIn(1, BeadGrid.MAX_SIZE) ?: vm.project.width
                    val h = height.toIntOrNull()?.coerceIn(1, BeadGrid.MAX_SIZE) ?: vm.project.height
                    vm.resizeCanvas(w, h, anchor)
                    onDone()
                },
                modifier = Modifier.weight(1f),
            ) { Text("扩裁画布") }
            ChipButton("取消", false) { onDone() }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                val w = width.toIntOrNull()?.coerceIn(1, BeadGrid.MAX_SIZE) ?: 48
                val h = height.toIntOrNull()?.coerceIn(1, BeadGrid.MAX_SIZE) ?: 48
                vm.newCanvas(w, h)
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("清空并新建 $width×$height 画布") }
        Spacer(Modifier.height(28.dp))
    }
}
