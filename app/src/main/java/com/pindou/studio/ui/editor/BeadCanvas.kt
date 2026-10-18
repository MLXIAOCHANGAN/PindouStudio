package com.pindou.studio.ui.editor

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import com.pindou.studio.core.ColorMath
import com.pindou.studio.core.DisplayResolver
import com.pindou.studio.core.PaletteColor
import com.pindou.studio.core.Tool
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * 拼豆画布：负责渲染网格、豆子、色号、选区与底图，并处理单指绘制 / 双指缩放平移。
 */
@Composable
fun BeadCanvas(vm: EditorViewModel, modifier: Modifier = Modifier) {
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val textPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.MONOSPACE } }
    val rulerPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.MONOSPACE } }
    val rect = remember { RectF() }

    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { vm.updateViewport(it.width, it.height) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var transforming = false
                    var prevCentroid: Offset? = null
                    var prevDistance = 0f
                    vm.onPointerDown(down.position.x, down.position.y)
                    down.consume()

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        if (pressed.size >= 2) {
                            if (!transforming) {
                                transforming = true
                                vm.cancelStroke()
                            }
                            var cx = 0f
                            var cy = 0f
                            for (p in pressed) {
                                cx += p.position.x
                                cy += p.position.y
                            }
                            cx /= pressed.size
                            cy /= pressed.size
                            val centroid = Offset(cx, cy)
                            val a = pressed[0].position
                            val b = pressed[1].position
                            val distance = kotlin.math.hypot(
                                (a.x - b.x).toDouble(), (a.y - b.y).toDouble(),
                            ).toFloat()
                            val prev = prevCentroid
                            if (prev != null && prevDistance > 1f && distance > 1f) {
                                vm.zoomAt(centroid, distance / prevDistance)
                                vm.pan(centroid.x - prev.x, centroid.y - prev.y)
                            }
                            prevCentroid = centroid
                            prevDistance = distance
                            pressed.forEach { it.consume() }
                        } else {
                            val change = pressed.first()
                            if (!transforming) {
                                val movement = change.position - change.previousPosition
                                if (vm.tool == Tool.PAN) {
                                    vm.pan(movement.x, movement.y)
                                } else {
                                    vm.onPointerMove(change.position.x, change.position.y)
                                }
                                change.consume()
                            } else {
                                change.consume()
                            }
                            prevCentroid = null
                            prevDistance = 0f
                        }
                    }
                    vm.onPointerUp()
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // 订阅状态，保证网格变化 / 视图变化时重绘
            val tick = vm.renderTick
            val zoom = vm.zoom
            val offsetX = vm.offsetX
            val offsetY = vm.offsetY
            val grid = vm.project.grid
            val palette = vm.palette
            val showCodes = vm.showCodes
            val showGrid = vm.showGrid
            val showMajor = vm.showMajorGrid
            val showRuler = vm.showRuler
            val round = vm.roundBeads
            val displayMode = vm.displayMode
            val selection = vm.selection
            val base = vm.baseImage
            val baseVisible = vm.baseVisible
            val baseOpacity = vm.baseOpacity
            val baseScale = vm.baseScale
            @Suppress("UNUSED_EXPRESSION") tick

            drawIntoCanvas { canvas ->
                val nc = canvas.nativeCanvas
                nc.save()
                nc.clipRect(0f, 0f, size.width, size.height)
                nc.drawColor(0xFFF7F5F1.toInt())

                // ---- 底图 ----
                if (base != null && baseVisible && !base.isRecycled) {
                    val boardW = grid.width * zoom
                    val boardH = grid.height * zoom
                    val fit = min(boardW / base.width, boardH / base.height) * baseScale
                    val dw = base.width * fit
                    val dh = base.height * fit
                    val dx = offsetX + (boardW - dw) / 2f
                    val dy = offsetY + (boardH - dh) / 2f
                    paint.reset()
                    paint.isAntiAlias = true
                    paint.alpha = (baseOpacity * 255).toInt().coerceIn(0, 255)
                    rect.set(dx, dy, dx + dw, dy + dh)
                    nc.drawBitmap(base, null, rect, paint)
                }

                // ---- 画布底 ----
                paint.reset()
                paint.isAntiAlias = false
                paint.style = Paint.Style.FILL
                paint.color = 0xFFFFFFFF.toInt()
                nc.drawRect(offsetX, offsetY, offsetX + grid.width * zoom, offsetY + grid.height * zoom, paint)

                // ---- 可见格子范围 ----
                val c0 = max(0, floor((0f - offsetX) / zoom).toInt())
                val c1 = min(grid.width - 1, ceil((size.width - offsetX) / zoom).toInt())
                val r0 = max(0, floor((0f - offsetY) / zoom).toInt())
                val r1 = min(grid.height - 1, ceil((size.height - offsetY) / zoom).toInt())

                // ---- 豆子（缩得很小时按 LOD 合并绘制，避免成千上万次 drawRect） ----
                val tintAlpha = if (displayMode == com.pindou.studio.core.DisplayMode.NIGHT) 0xC0 else 0xFF
                val lod = max(1, ceil(EditorViewModel.MIN_CELL_PX / zoom).toInt())
                if (lod == 1) {
                    for (r in r0..r1) {
                        val top = offsetY + r * zoom
                        for (c in c0..c1) {
                            val idx = grid.get(c, r)
                            if (idx == 0) continue
                            val color = palette.colorAt(idx) ?: continue
                            var argb = DisplayResolver.resolve(color, displayMode)
                            if (tintAlpha != 0xFF) {
                                argb = (argb and 0x00FFFFFF) or (tintAlpha shl 24)
                            }
                            paint.color = argb
                            paint.isAntiAlias = round
                            val left = offsetX + c * zoom
                            if (round) {
                                nc.drawCircle(
                                    left + zoom / 2f, top + zoom / 2f,
                                    (zoom / 2f - 0.5f).coerceAtLeast(0.6f), paint,
                                )
                            } else {
                                nc.drawRect(left, top, left + zoom + 0.35f, top + zoom + 0.35f, paint)
                            }
                        }
                    }
                } else {
                    val counts = IntArray(palette.size + 1)
                    val touched = IntArray(lod * lod + 1)
                    paint.isAntiAlias = false
                    var br = r0 / lod
                    val brEnd = r1 / lod
                    val bcStart = c0 / lod
                    val bcEnd = c1 / lod
                    while (br <= brEnd) {
                        var bc = bcStart
                        while (bc <= bcEnd) {
                            var touchedCount = 0
                            var best = 0
                            var bestCount = 0
                            val rowEnd = min(grid.height, (br + 1) * lod)
                            val colEnd = min(grid.width, (bc + 1) * lod)
                            for (rr in br * lod until rowEnd) {
                                for (cc in bc * lod until colEnd) {
                                    val idx = grid.get(cc, rr)
                                    if (idx == 0 || idx >= counts.size) continue
                                    val n = ++counts[idx]
                                    if (n == 1) touched[touchedCount++] = idx
                                    if (n > bestCount) { bestCount = n; best = idx }
                                }
                            }
                            for (i in 0 until touchedCount) counts[touched[i]] = 0
                            if (best != 0) {
                                val color = palette.colorAt(best)
                                if (color != null) {
                                    var argb = DisplayResolver.resolve(color, displayMode)
                                    if (tintAlpha != 0xFF) argb = (argb and 0x00FFFFFF) or (tintAlpha shl 24)
                                    paint.color = argb
                                    val left = offsetX + bc * lod * zoom
                                    val top = offsetY + br * lod * zoom
                                    nc.drawRect(left, top, left + lod * zoom + 0.5f, top + lod * zoom + 0.5f, paint)
                                }
                            }
                            bc++
                        }
                        br++
                    }
                }

                // ---- 网格线 ----
                if (showGrid && zoom >= 4f) {
                    paint.style = Paint.Style.STROKE
                    paint.isAntiAlias = false
                    paint.color = 0x33000000
                    paint.strokeWidth = if (zoom >= 16f) 1f else 0.7f
                    for (c in c0..c1 + 1) {
                        val x = offsetX + c * zoom
                        nc.drawLine(x, offsetY, x, offsetY + grid.height * zoom, paint)
                    }
                    for (r in r0..r1 + 1) {
                        val y = offsetY + r * zoom
                        nc.drawLine(offsetX, y, offsetX + grid.width * zoom, y, paint)
                    }
                }
                if (showMajor && zoom >= 5f) {
                    paint.style = Paint.Style.STROKE
                    paint.isAntiAlias = false
                    paint.color = 0x99000000.toInt()
                    paint.strokeWidth = if (zoom >= 16f) 1.8f else 1.2f
                    var c = (c0 / 8) * 8
                    while (c <= c1 + 1) {
                        val x = offsetX + c * zoom
                        nc.drawLine(x, offsetY, x, offsetY + grid.height * zoom, paint)
                        c += 8
                    }
                    var r = (r0 / 8) * 8
                    while (r <= r1 + 1) {
                        val y = offsetY + r * zoom
                        nc.drawLine(offsetX, y, offsetX + grid.width * zoom, y, paint)
                        r += 8
                    }
                }

                // ---- 外框 ----
                paint.style = Paint.Style.STROKE
                paint.isAntiAlias = true
                paint.color = 0xFF9AA3AE.toInt()
                paint.strokeWidth = 2f
                nc.drawRect(
                    offsetX, offsetY,
                    offsetX + grid.width * zoom, offsetY + grid.height * zoom, paint,
                )

                // ---- 色号 ----
                if (showCodes && zoom >= 20f && (c1 - c0 + 1) * (r1 - r0 + 1) <= 9000) {
                    textPaint.textAlign = Paint.Align.CENTER
                    val textSize = min(zoom * 0.52f, 26f)
                    textPaint.textSize = textSize
                    val metrics = textPaint.fontMetrics
                    val vo = -(metrics.ascent + metrics.descent) / 2f
                    for (r in r0..r1) {
                        val top = offsetY + r * zoom
                        for (c in c0..c1) {
                            val idx = grid.get(c, r)
                            if (idx == 0) continue
                            val color = palette.colorAt(idx) ?: continue
                            val text = color.code
                            if (text.length > 5) continue
                            textPaint.color =
                                if (ColorMath.luminance(color.argb) > 0.45f) 0xDD111111.toInt()
                                else 0xEEFFFFFF.toInt()
                            nc.drawText(
                                text,
                                offsetX + c * zoom + zoom / 2f,
                                top + zoom / 2f + vo,
                                textPaint,
                            )
                        }
                    }
                }

                // ---- 选区 ----
                if (selection != null) {
                    val left = offsetX + selection.left * zoom
                    val top = offsetY + selection.top * zoom
                    val right = offsetX + (selection.right + 1) * zoom
                    val bottom = offsetY + (selection.bottom + 1) * zoom
                    paint.style = Paint.Style.FILL
                    paint.color = 0x222B7CFF
                    nc.drawRect(left, top, right, bottom, paint)
                    paint.style = Paint.Style.STROKE
                    paint.color = 0xFF2B7CFF.toInt()
                    paint.strokeWidth = 2.5f
                    paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 8f), 0f)
                    nc.drawRect(left, top, right, bottom, paint)
                    paint.pathEffect = null
                }

                // ---- 坐标尺 ----
                if (showRuler && zoom >= 9f) {
                    rulerPaint.textAlign = Paint.Align.CENTER
                    rulerPaint.textSize = min(zoom * 0.5f, 20f)
                    rulerPaint.color = 0xFF6B7280.toInt()
                    val metrics = rulerPaint.fontMetrics
                    val vo = -(metrics.ascent + metrics.descent) / 2f
                    val step = when {
                        zoom >= 26f -> 5
                        zoom >= 16f -> 10
                        else -> 20
                    }
                    var c = (c0 / step) * step
                    while (c <= c1) {
                        if (c >= 0) {
                            nc.drawText(
                                (c + 1).toString(),
                                offsetX + c * zoom + zoom / 2f,
                                offsetY - 4f,
                                rulerPaint,
                            )
                        }
                        c += step
                    }
                    rulerPaint.textAlign = Paint.Align.RIGHT
                    var r = (r0 / step) * step
                    while (r <= r1) {
                        if (r >= 0) {
                            nc.drawText(
                                (r + 1).toString(),
                                offsetX - 4f,
                                offsetY + r * zoom + zoom / 2f + vo,
                                rulerPaint,
                            )
                        }
                        r += step
                    }
                }

                nc.restore()
            }
        }
    }
}
