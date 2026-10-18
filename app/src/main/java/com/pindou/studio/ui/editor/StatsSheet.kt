package com.pindou.studio.ui.editor

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 用量统计面板。 */
@Composable
fun StatsSheetContent(vm: EditorViewModel) {
    val rows = remember(vm.renderTick) { vm.usageRows() }
    val total = remember(vm.renderTick) { rows.sumOf { it.count } }
    val bounds = remember(vm.renderTick) { vm.project.grid.bounds() }
    val paintedW = if (bounds != null) bounds[2] - bounds[0] + 1 else 0
    val paintedH = if (bounds != null) bounds[3] - bounds[1] + 1 else 0

    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("画布", "${vm.project.width}×${vm.project.height}", Modifier.weight(1f))
            StatCard("图案", "${paintedW}×${paintedH}", Modifier.weight(1f))
            StatCard("豆子", "$total", Modifier.weight(1f))
            StatCard("颜色", "${rows.size}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text("色号用量", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        if (rows.isEmpty()) {
            Text(
                "画布还没有内容",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(rows, key = { it.index }) { row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(Color.parseColorSafe(row.hex))),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            row.code,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(64.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(
                                        (row.count.toFloat() / total).coerceIn(0.02f, 1f)
                                    )
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${row.count}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun Color.Companion.parseColorSafe(hex: String): Int = runCatching {
    android.graphics.Color.parseColor(hex)
}.getOrDefault(0xFF000000.toInt())
