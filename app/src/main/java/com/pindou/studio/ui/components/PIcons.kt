package com.pindou.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** 应用内使用的线条图标集合（用代码绘制，避免额外依赖）。 */
enum class PIcon {
    Pen, Eraser, Bucket, Dropper, Select, Move,
    Undo, Redo, Grid, Tag, Symmetry, Stats, Palette,
    Download, Upload, Image, Resize, More, Close, Trash,
    FlipH, FlipV, RotateCW, RotateCCW, Save, Check, Plus, Back,
    Settings, Eye, Fit, ZoomIn, ZoomOut, Copy, Cut, Paste, Star, Info, Light,
}

@Composable
fun PIconView(
    icon: PIcon,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        drawIcon(icon, tint)
    }
}

private fun DrawScope.drawIcon(icon: PIcon, tint: Color) {
    val s = size.minDimension
    val sw = s * 0.085f
    fun p(x: Float, y: Float) = Offset(x * s, y * s)
    fun stroke(width: Float = sw) = Stroke(width = width, cap = StrokeCap.Round)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float = sw) =
        drawLine(tint, p(x1, y1), p(x2, y2), strokeWidth = width, cap = StrokeCap.Round)

    when (icon) {
        PIcon.Pen -> {
            line(0.22f, 0.78f, 0.78f, 0.22f)
            line(0.22f, 0.78f, 0.30f, 0.62f)
            line(0.22f, 0.78f, 0.38f, 0.70f)
            line(0.30f, 0.62f, 0.38f, 0.70f, sw * 0.8f)
            drawCircle(tint, s * 0.05f, p(0.2f, 0.82f))
        }
        PIcon.Eraser -> {
            val path = Path().apply {
                moveTo(0.22f * s, 0.68f * s)
                lineTo(0.52f * s, 0.24f * s)
                lineTo(0.80f * s, 0.44f * s)
                lineTo(0.50f * s, 0.84f * s)
                close()
            }
            drawPath(path, tint, style = stroke(sw * 0.9f))
            line(0.36f, 0.74f, 0.66f, 0.52f, sw * 0.7f)
        }
        PIcon.Bucket -> {
            val path = Path().apply {
                moveTo(0.24f * s, 0.44f * s)
                lineTo(0.50f * s, 0.18f * s)
                lineTo(0.78f * s, 0.46f * s)
                lineTo(0.52f * s, 0.72f * s)
                close()
            }
            drawPath(path, tint, style = stroke(sw * 0.9f))
            drawCircle(tint, s * 0.10f, p(0.80f, 0.76f))
        }
        PIcon.Dropper -> {
            line(0.28f, 0.74f, 0.62f, 0.40f, sw)
            line(0.56f, 0.24f, 0.78f, 0.46f, sw * 1.4f)
            drawCircle(tint, sw * 0.7f, p(0.24f, 0.78f))
        }
        PIcon.Select -> {
            val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(s * 0.10f, s * 0.08f))
            drawRect(
                tint,
                topLeft = p(0.20f, 0.20f),
                size = Size(s * 0.60f, s * 0.60f),
                style = Stroke(width = sw, pathEffect = dash),
            )
        }
        PIcon.Move -> {
            line(0.5f, 0.18f, 0.5f, 0.82f)
            line(0.18f, 0.5f, 0.82f, 0.5f)
            line(0.5f, 0.18f, 0.42f, 0.28f)
            line(0.5f, 0.18f, 0.58f, 0.28f)
            line(0.5f, 0.82f, 0.42f, 0.72f)
            line(0.5f, 0.82f, 0.58f, 0.72f)
            line(0.18f, 0.5f, 0.28f, 0.42f)
            line(0.18f, 0.5f, 0.28f, 0.58f)
            line(0.82f, 0.5f, 0.72f, 0.42f)
            line(0.82f, 0.5f, 0.72f, 0.58f)
        }
        PIcon.Undo, PIcon.Redo -> {
            val flip = icon == PIcon.Redo
            fun fx(x: Float) = if (flip) 1f - x else x
            drawArc(
                tint,
                startAngle = if (flip) -60f else 240f,
                sweepAngle = if (flip) -250f else 250f,
                useCenter = false,
                topLeft = p(0.22f, 0.26f),
                size = Size(s * 0.56f, s * 0.52f),
                style = stroke(sw),
            )
            line(fx(0.24f), 0.44f, fx(0.24f), 0.24f)
            line(fx(0.24f), 0.44f, fx(0.44f), 0.44f)
        }
        PIcon.Grid -> {
            drawRect(tint, p(0.18f, 0.18f), Size(s * 0.64f, s * 0.64f), style = stroke(sw))
            line(0.39f, 0.18f, 0.39f, 0.82f, sw * 0.8f)
            line(0.61f, 0.18f, 0.61f, 0.82f, sw * 0.8f)
            line(0.18f, 0.39f, 0.82f, 0.39f, sw * 0.8f)
            line(0.18f, 0.61f, 0.82f, 0.61f, sw * 0.8f)
        }
        PIcon.Tag -> {
            val path = Path().apply {
                moveTo(0.20f * s, 0.24f * s)
                lineTo(0.56f * s, 0.24f * s)
                lineTo(0.82f * s, 0.50f * s)
                lineTo(0.56f * s, 0.76f * s)
                lineTo(0.20f * s, 0.76f * s)
                close()
            }
            drawPath(path, tint, style = stroke(sw))
            drawCircle(tint, sw * 0.9f, p(0.36f, 0.5f))
        }
        PIcon.Symmetry -> {
            line(0.5f, 0.14f, 0.5f, 0.86f, sw * 0.7f)
            val path = Path().apply {
                moveTo(0.42f * s, 0.30f * s); lineTo(0.16f * s, 0.50f * s); lineTo(0.42f * s, 0.70f * s); close()
            }
            drawPath(path, tint)
            val path2 = Path().apply {
                moveTo(0.58f * s, 0.30f * s); lineTo(0.84f * s, 0.50f * s); lineTo(0.58f * s, 0.70f * s); close()
            }
            drawPath(path2, tint.copy(alpha = 0.45f))
        }
        PIcon.Stats -> {
            drawRect(tint, p(0.20f, 0.56f), Size(s * 0.14f, s * 0.26f))
            drawRect(tint, p(0.43f, 0.36f), Size(s * 0.14f, s * 0.46f))
            drawRect(tint, p(0.66f, 0.22f), Size(s * 0.14f, s * 0.60f))
        }
        PIcon.Palette -> {
            drawCircle(tint, s * 0.30f, p(0.5f, 0.5f), style = stroke(sw))
            drawCircle(tint, s * 0.055f, p(0.38f, 0.40f))
            drawCircle(tint, s * 0.055f, p(0.62f, 0.40f))
            drawCircle(tint, s * 0.055f, p(0.50f, 0.62f))
        }
        PIcon.Download -> {
            line(0.5f, 0.18f, 0.5f, 0.62f)
            line(0.5f, 0.62f, 0.34f, 0.46f)
            line(0.5f, 0.62f, 0.66f, 0.46f)
            val path = Path().apply {
                moveTo(0.22f * s, 0.68f * s)
                lineTo(0.22f * s, 0.80f * s)
                lineTo(0.78f * s, 0.80f * s)
                lineTo(0.78f * s, 0.68f * s)
            }
            drawPath(path, tint, style = stroke(sw))
        }
        PIcon.Upload -> {
            line(0.5f, 0.62f, 0.5f, 0.18f)
            line(0.5f, 0.18f, 0.34f, 0.34f)
            line(0.5f, 0.18f, 0.66f, 0.34f)
            val path = Path().apply {
                moveTo(0.22f * s, 0.68f * s)
                lineTo(0.22f * s, 0.80f * s)
                lineTo(0.78f * s, 0.80f * s)
                lineTo(0.78f * s, 0.68f * s)
            }
            drawPath(path, tint, style = stroke(sw))
        }
        PIcon.Image -> {
            drawRect(tint, p(0.16f, 0.22f), Size(s * 0.68f, s * 0.56f), style = stroke(sw))
            drawCircle(tint, s * 0.06f, p(0.34f, 0.38f))
            val path = Path().apply {
                moveTo(0.20f * s, 0.72f * s)
                lineTo(0.42f * s, 0.48f * s)
                lineTo(0.58f * s, 0.66f * s)
                lineTo(0.70f * s, 0.54f * s)
                lineTo(0.80f * s, 0.72f * s)
            }
            drawPath(path, tint, style = stroke(sw * 0.9f))
        }
        PIcon.Resize -> {
            line(0.20f, 0.20f, 0.44f, 0.20f)
            line(0.20f, 0.20f, 0.20f, 0.44f)
            line(0.80f, 0.80f, 0.56f, 0.80f)
            line(0.80f, 0.80f, 0.80f, 0.56f)
            line(0.34f, 0.34f, 0.66f, 0.66f, sw * 0.8f)
        }
        PIcon.More -> {
            drawCircle(tint, s * 0.06f, p(0.5f, 0.24f))
            drawCircle(tint, s * 0.06f, p(0.5f, 0.50f))
            drawCircle(tint, s * 0.06f, p(0.5f, 0.76f))
        }
        PIcon.Close -> {
            line(0.26f, 0.26f, 0.74f, 0.74f)
            line(0.74f, 0.26f, 0.26f, 0.74f)
        }
        PIcon.Trash -> {
            line(0.18f, 0.30f, 0.82f, 0.30f)
            line(0.40f, 0.30f, 0.42f, 0.18f)
            line(0.60f, 0.30f, 0.58f, 0.18f)
            line(0.42f, 0.18f, 0.58f, 0.18f)
            val path = Path().apply {
                moveTo(0.26f * s, 0.30f * s)
                lineTo(0.30f * s, 0.82f * s)
                lineTo(0.70f * s, 0.82f * s)
                lineTo(0.74f * s, 0.30f * s)
            }
            drawPath(path, tint, style = stroke(sw * 0.9f))
        }
        PIcon.FlipH -> {
            line(0.5f, 0.14f, 0.5f, 0.86f, sw * 0.7f)
            val path = Path().apply {
                moveTo(0.42f * s, 0.26f * s); lineTo(0.42f * s, 0.74f * s); lineTo(0.16f * s, 0.50f * s); close()
            }
            drawPath(path, tint)
            val path2 = Path().apply {
                moveTo(0.58f * s, 0.26f * s); lineTo(0.58f * s, 0.74f * s); lineTo(0.84f * s, 0.50f * s); close()
            }
            drawPath(path2, tint, style = stroke(sw * 0.8f))
        }
        PIcon.FlipV -> {
            line(0.14f, 0.5f, 0.86f, 0.5f, sw * 0.7f)
            val path = Path().apply {
                moveTo(0.26f * s, 0.42f * s); lineTo(0.74f * s, 0.42f * s); lineTo(0.50f * s, 0.16f * s); close()
            }
            drawPath(path, tint)
            val path2 = Path().apply {
                moveTo(0.26f * s, 0.58f * s); lineTo(0.74f * s, 0.58f * s); lineTo(0.50f * s, 0.84f * s); close()
            }
            drawPath(path2, tint, style = stroke(sw * 0.8f))
        }
        PIcon.RotateCW, PIcon.RotateCCW -> {
            val flip = icon == PIcon.RotateCCW
            drawArc(
                tint,
                startAngle = if (flip) 100f else -10f,
                sweepAngle = if (flip) 250f else -250f,
                useCenter = false,
                topLeft = p(0.20f, 0.20f),
                size = Size(s * 0.60f, s * 0.60f),
                style = stroke(sw),
            )
            if (flip) {
                line(0.50f, 0.20f, 0.30f, 0.24f)
                line(0.50f, 0.20f, 0.46f, 0.40f)
            } else {
                line(0.50f, 0.20f, 0.70f, 0.24f)
                line(0.50f, 0.20f, 0.54f, 0.40f)
            }
        }
        PIcon.Save -> {
            drawRect(tint, p(0.20f, 0.20f), Size(s * 0.60f, s * 0.60f), style = stroke(sw))
            drawRect(tint, p(0.34f, 0.20f), Size(s * 0.32f, s * 0.20f))
            drawRect(tint, p(0.32f, 0.54f), Size(s * 0.36f, s * 0.26f), style = stroke(sw * 0.8f))
        }
        PIcon.Check -> {
            line(0.24f, 0.52f, 0.42f, 0.70f, sw * 1.2f)
            line(0.42f, 0.70f, 0.78f, 0.30f, sw * 1.2f)
        }
        PIcon.Plus -> {
            line(0.5f, 0.22f, 0.5f, 0.78f, sw * 1.1f)
            line(0.22f, 0.5f, 0.78f, 0.5f, sw * 1.1f)
        }
        PIcon.Back -> {
            line(0.60f, 0.22f, 0.34f, 0.50f, sw * 1.1f)
            line(0.34f, 0.50f, 0.60f, 0.78f, sw * 1.1f)
        }
        PIcon.Settings -> {
            line(0.20f, 0.30f, 0.80f, 0.30f, sw * 0.9f)
            line(0.20f, 0.50f, 0.80f, 0.50f, sw * 0.9f)
            line(0.20f, 0.70f, 0.80f, 0.70f, sw * 0.9f)
            drawCircle(tint, s * 0.075f, p(0.38f, 0.30f))
            drawCircle(tint, s * 0.075f, p(0.62f, 0.50f))
            drawCircle(tint, s * 0.075f, p(0.44f, 0.70f))
        }
        PIcon.Eye -> {
            val path = Path().apply {
                moveTo(0.14f * s, 0.50f * s)
                quadraticBezierTo(0.50f * s, 0.16f * s, 0.86f * s, 0.50f * s)
                quadraticBezierTo(0.50f * s, 0.84f * s, 0.14f * s, 0.50f * s)
                close()
            }
            drawPath(path, tint, style = stroke(sw * 0.9f))
            drawCircle(tint, s * 0.10f, p(0.5f, 0.5f))
        }
        PIcon.Fit -> {
            line(0.18f, 0.34f, 0.18f, 0.18f)
            line(0.18f, 0.18f, 0.34f, 0.18f)
            line(0.82f, 0.34f, 0.82f, 0.18f)
            line(0.82f, 0.18f, 0.66f, 0.18f)
            line(0.18f, 0.66f, 0.18f, 0.82f)
            line(0.18f, 0.82f, 0.34f, 0.82f)
            line(0.82f, 0.66f, 0.82f, 0.82f)
            line(0.82f, 0.82f, 0.66f, 0.82f)
        }
        PIcon.ZoomIn, PIcon.ZoomOut -> {
            drawCircle(tint, s * 0.24f, p(0.44f, 0.44f), style = stroke(sw))
            line(0.62f, 0.62f, 0.84f, 0.84f, sw)
            line(0.32f, 0.44f, 0.56f, 0.44f, sw * 0.8f)
            if (icon == PIcon.ZoomIn) line(0.44f, 0.32f, 0.44f, 0.56f, sw * 0.8f)
        }
        PIcon.Copy -> {
            drawRect(tint, p(0.30f, 0.30f), Size(s * 0.46f, s * 0.46f), style = stroke(sw * 0.9f))
            drawRect(tint, p(0.20f, 0.20f), Size(s * 0.46f, s * 0.46f), style = stroke(sw * 0.9f))
        }
        PIcon.Cut -> {
            line(0.28f, 0.20f, 0.72f, 0.72f, sw * 0.9f)
            line(0.72f, 0.20f, 0.28f, 0.72f, sw * 0.9f)
            drawCircle(tint, s * 0.08f, p(0.26f, 0.80f), style = stroke(sw * 0.8f))
            drawCircle(tint, s * 0.08f, p(0.74f, 0.80f), style = stroke(sw * 0.8f))
        }
        PIcon.Paste -> {
            drawRect(tint, p(0.22f, 0.22f), Size(s * 0.56f, s * 0.62f), style = stroke(sw))
            drawRect(tint, p(0.36f, 0.14f), Size(s * 0.28f, s * 0.14f))
        }
        PIcon.Star -> {
            val path = Path().apply {
                for (i in 0 until 10) {
                    val angle = (-90f + i * 36f) * Math.PI.toFloat() / 180f
                    val radius = if (i % 2 == 0) 0.34f else 0.15f
                    val x = (0.5f + radius * kotlin.math.cos(angle)) * s
                    val y = (0.5f + radius * kotlin.math.sin(angle)) * s
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(path, tint)
        }
        PIcon.Info -> {
            drawCircle(tint, s * 0.32f, p(0.5f, 0.5f), style = stroke(sw))
            drawCircle(tint, s * 0.055f, p(0.5f, 0.32f))
            line(0.5f, 0.44f, 0.5f, 0.68f, sw)
        }
        PIcon.Light -> {
            drawCircle(tint, s * 0.18f, p(0.5f, 0.40f), style = stroke(sw * 0.9f))
            line(0.5f, 0.62f, 0.5f, 0.72f, sw * 0.8f)
            line(0.38f, 0.76f, 0.62f, 0.76f, sw * 0.8f)
            line(0.5f, 0.12f, 0.5f, 0.20f, sw * 0.7f)
            line(0.20f, 0.24f, 0.26f, 0.30f, sw * 0.7f)
            line(0.80f, 0.24f, 0.74f, 0.30f, sw * 0.7f)
        }
    }
}
