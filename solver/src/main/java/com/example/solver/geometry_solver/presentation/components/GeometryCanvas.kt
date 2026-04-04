package com.example.solver.geometry_solver.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.common.R
import net.objecthunter.exp4j.ExpressionBuilder
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class GeometryDrawingStep(
    val title: String,
    val type: String,
    val shapes: List<Map<String, Any>>,
)

@Composable
fun GeometryStepCard(
    step: GeometryDrawingStep,
    prevShapes: List<Map<String, Any>> = emptyList(),
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Text(step.title)
            GeometryInteractiveCanvas(step, prevShapes)
        }
    }
}

@Composable
fun GeometryInteractiveCanvas(
    step: GeometryDrawingStep,
    prevShapes: List<Map<String, Any>> = emptyList(),
) {
    val zoomState = remember { mutableFloatStateOf(1f) }
    val yawState = remember { mutableFloatStateOf(30f) }
    val pitchState = remember { mutableFloatStateOf(15f) }

    Column {
        GeometryControls(
            is3D = step.type.equals("3D", ignoreCase = true),
            zoomState = zoomState,
            yawState = yawState,
            pitchState = pitchState,
        )

        val hasAxes = remember(step.shapes) { step.shapes.any { (it["kind"] as? String).orEmpty().equals("axes", ignoreCase = true) } }
        val axesShape =
            remember(step.shapes) { step.shapes.firstOrNull { (it["kind"] as? String).orEmpty().equals("axes", ignoreCase = true) } }
        val shapesToDraw =
            remember(step.shapes) { step.shapes.filterNot { (it["kind"] as? String).orEmpty().equals("axes", ignoreCase = true) } }

        val onSurfaceColor = MaterialTheme.colorScheme.onSurface
        val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
        val primaryColor = MaterialTheme.colorScheme.primary

        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(300.dp),
        ) {
            val base = min(size.width, size.height) / 20f
            val scale = base * zoomState.floatValue
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val yaw = yawState.floatValue
            val pitch = pitchState.floatValue

            if (hasAxes && axesShape != null) {
                drawAxesConfig(centerX, centerY, scale, axesShape, outlineVariantColor)
            } else {
                drawAxes(centerX, centerY, scale, outlineVariantColor)
            }
            prevShapes.forEach { shape -> drawShape(shape, centerX, centerY, scale, onSurfaceColor, primaryColor, step.type, yaw, pitch) }
            shapesToDraw.forEach { shape -> drawShape(shape, centerX, centerY, scale, onSurfaceColor, primaryColor, step.type, yaw, pitch) }
        }
    }
}

@Composable
fun GeometryControls(
    is3D: Boolean,
    zoomState: MutableFloatState,
    yawState: MutableFloatState,
    pitchState: MutableFloatState,
) {
    Column {
        Row {
            Text(stringResource(R.string.zoom))
            Slider(value = zoomState.floatValue, onValueChange = { zoomState.floatValue = it }, valueRange = 0.5f..2.5f)
        }
        if (is3D) {
            Row {
                Text(stringResource(com.example.common.R.string.yaw_angle))
                Slider(value = yawState.floatValue, onValueChange = { yawState.floatValue = it }, valueRange = -90f..90f)
            }
            Row {
                Text(stringResource(com.example.common.R.string.pitch_angle))
                Slider(value = pitchState.floatValue, onValueChange = { pitchState.floatValue = it }, valueRange = -60f..60f)
            }
        }
    }
}

private fun DrawScope.drawShape(
    shape: Map<String, Any>,
    centerX: Float,
    centerY: Float,
    scale: Float,
    onSurfaceColor: Color,
    primaryColor: Color,
    type: String,
    yaw: Float,
    pitch: Float,
) {
    val kind = shape["kind"] as? String ?: return
    val color = parseColor(shape["color"], onSurfaceColor) ?: primaryColor
    val strokeWidth = (shape["strokeWidth"] as? Number)?.toFloat() ?: 2f
    val dashed = (shape["style"] as? String)?.equals("dashed", ignoreCase = true) == true
    try {
        when (kind) {
            "point" -> {
                val x = (shape["x"] as Number).toFloat()
                val y = (shape["y"] as Number).toFloat()
                drawPoint2D(centerX, centerY, scale, x, y, color)
                val label = shape["label"] as? String
                if (!label.isNullOrBlank()) drawTextLabel(centerX, centerY, scale, x, y, label, color)
            }
            "polyline" -> {
                @Suppress("UNCHECKED_CAST")
                val points = shape["points"] as? List<Map<String, Any>> ?: emptyList()
                drawPolyline2D(centerX, centerY, scale, points, color, strokeWidth, dashed)
            }
            "line" -> {
                val x1 = (shape["x1"] as Number).toFloat()
                val y1 = (shape["y1"] as Number).toFloat()
                val x2 = (shape["x2"] as Number).toFloat()
                val y2 = (shape["y2"] as Number).toFloat()
                drawLine2D(centerX, centerY, scale, x1, y1, x2, y2, color, strokeWidth, dashed)
            }
            "circle" -> {
                val cx = (shape["cx"] as Number).toFloat()
                val cy = (shape["cy"] as Number).toFloat()
                val r = (shape["r"] as Number).toFloat()
                drawCircle2D(centerX, centerY, scale, cx, cy, r, color, strokeWidth, dashed)
            }
            "polygon" -> {
                @Suppress("UNCHECKED_CAST")
                val points = shape["points"] as? List<Map<String, Any>> ?: emptyList()
                drawPolygon2D(centerX, centerY, scale, points, color, strokeWidth, dashed, fill = (shape["fill"] as? Boolean) == true)
            }
            "arc" -> {
                val cx = (shape["cx"] as Number).toFloat()
                val cy = (shape["cy"] as Number).toFloat()
                val r = (shape["r"] as Number).toFloat()
                val start = (shape["startDeg"] as Number).toFloat()
                val sweep = (shape["sweepDeg"] as Number).toFloat()
                drawArc2D(centerX, centerY, scale, cx, cy, r, start, sweep, color, strokeWidth, dashed)
            }
            "angle" -> {
                val ax = (shape["ax"] as Number).toFloat()
                val ay = (shape["ay"] as Number).toFloat()
                val bx = (shape["bx"] as Number).toFloat()
                val by = (shape["by"] as Number).toFloat()
                val cx = (shape["cx"] as Number).toFloat()
                val cy = (shape["cy"] as Number).toFloat()
                drawAngleMarker(centerX, centerY, scale, ax, ay, bx, by, cx, cy, color)
            }
            "text" -> {
                val x = (shape["x"] as Number).toFloat()
                val y = (shape["y"] as Number).toFloat()
                val text = shape["text"] as? String ?: ""
                if (text.isNotBlank()) drawTextLabel(centerX, centerY, scale, x, y, text, color)
            }
            "ellipse" -> {
                val cx = (shape["cx"] as Number).toFloat()
                val cy = (shape["cy"] as Number).toFloat()
                val rx = (shape["rx"] as Number).toFloat()
                val ry = (shape["ry"] as Number).toFloat()
                val rot = (shape["rotationDeg"] as? Number)?.toFloat() ?: 0f
                drawEllipse2D(centerX, centerY, scale, cx, cy, rx, ry, rot, color, strokeWidth, dashed)
            }
            "rect" -> {
                val x = (shape["x"] as Number).toFloat()
                val y = (shape["y"] as Number).toFloat()
                val w = (shape["width"] as Number).toFloat()
                val h = (shape["height"] as Number).toFloat()
                val rot = (shape["rotationDeg"] as? Number)?.toFloat() ?: 0f
                drawRect2D(centerX, centerY, scale, x, y, w, h, rot, color, strokeWidth, dashed, fill = (shape["fill"] as? Boolean) == true)
            }
            "arrow" -> {
                val x1 = (shape["x1"] as Number).toFloat()
                val y1 = (shape["y1"] as Number).toFloat()
                val x2 = (shape["x2"] as Number).toFloat()
                val y2 = (shape["y2"] as Number).toFloat()
                drawArrow2D(centerX, centerY, scale, x1, y1, x2, y2, color, strokeWidth, dashed)
            }
            "quadraticBezier" -> {
                val x0 = (shape["x0"] as Number).toFloat()
                val y0 = (shape["y0"] as Number).toFloat()
                val x1 = (shape["x1"] as Number).toFloat()
                val y1 = (shape["y1"] as Number).toFloat()
                val x2 = (shape["x2"] as Number).toFloat()
                val y2 = (shape["y2"] as Number).toFloat()
                drawQuadraticBezier2D(centerX, centerY, scale, x0, y0, x1, y1, x2, y2, color, strokeWidth, dashed)
            }
            "cubicBezier" -> {
                val x0 = (shape["x0"] as Number).toFloat()
                val y0 = (shape["y0"] as Number).toFloat()
                val x1 = (shape["x1"] as Number).toFloat()
                val y1 = (shape["y1"] as Number).toFloat()
                val x2 = (shape["x2"] as Number).toFloat()
                val y2 = (shape["y2"] as Number).toFloat()
                val x3 = (shape["x3"] as Number).toFloat()
                val y3 = (shape["y3"] as Number).toFloat()
                drawCubicBezier2D(centerX, centerY, scale, x0, y0, x1, y1, x2, y2, x3, y3, color, strokeWidth, dashed)
            }
            "function" -> {
                val expr = shape["expr"] as? String ?: return
                val xMin = (shape["xMin"] as? Number)?.toFloat() ?: -10f
                val xMax = (shape["xMax"] as? Number)?.toFloat() ?: 10f
                drawFunction2D(centerX, centerY, scale, expr, xMin, xMax, color, strokeWidth, dashed)
            }
            "shadedRegion" -> {
                val expr1 = shape["expr1"] as? String ?: "0"
                val expr2 = shape["expr2"] as? String
                val xMin = (shape["xMin"] as? Number)?.toFloat() ?: -10f
                val xMax = (shape["xMax"] as? Number)?.toFloat() ?: 10f
                drawShadedRegion2D(centerX, centerY, scale, expr1, expr2, xMin, xMax, color.copy(alpha = 0.3f))
            }
            "tangent" -> {
                val expr = shape["expr"] as? String ?: return
                val x0 = (shape["x0"] as? Number)?.toFloat() ?: 0f
                val length = (shape["length"] as? Number)?.toFloat() ?: 5f
                drawTangent2D(centerX, centerY, scale, expr, x0, length, color, strokeWidth, dashed)
            }
            "axes" -> {
                drawAxesConfig(centerX, centerY, scale, shape, color)
            }
            "point3D" -> {
                val x = (shape["x"] as Number).toFloat()
                val y = (shape["y"] as Number).toFloat()
                val z = (shape["z"] as Number).toFloat()
                val p = project3D(x, y, z, yaw, pitch)
                drawPoint2D(centerX, centerY, scale, p.first, p.second, color)
            }
            "line3D" -> {
                val x1 = (shape["x1"] as Number).toFloat()
                val y1 = (shape["y1"] as Number).toFloat()
                val z1 = (shape["z1"] as Number).toFloat()
                val x2 = (shape["x2"] as Number).toFloat()
                val y2 = (shape["y2"] as Number).toFloat()
                val z2 = (shape["z2"] as Number).toFloat()
                val p1 = project3D(x1, y1, z1, yaw, pitch)
                val p2 = project3D(x2, y2, z2, yaw, pitch)
                drawLine2D(centerX, centerY, scale, p1.first, p1.second, p2.first, p2.second, color, strokeWidth, dashed)
            }
            "polygon3D" -> {
                @Suppress("UNCHECKED_CAST")
                val points = shape["points"] as? List<Map<String, Any>> ?: emptyList()
                val projected =
                    points.map {
                        val x = (it["x"] as Number).toFloat()
                        val y = (it["y"] as Number).toFloat()
                        val z = (it["z"] as Number).toFloat()
                        val p = project3D(x, y, z, yaw, pitch)
                        mapOf<String, Any>("x" to p.first, "y" to p.second)
                    }
                drawPolygon2D(centerX, centerY, scale, projected, color, strokeWidth, dashed, fill = (shape["fill"] as? Boolean) == true)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        // Skip drawing this shape if attributes are malformed to prevent Compose crash
    }
}

private fun DrawScope.drawPoint2D(
    cx: Float,
    cy: Float,
    s: Float,
    x: Float,
    y: Float,
    color: Color = Color.Red,
) {
    val px = cx + x * s
    val py = cy - y * s
    drawCircle(color, radius = 3f, center = Offset(px, py))
}

private fun DrawScope.drawLine2D(
    cx: Float,
    cy: Float,
    s: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    color: Color = Color.Blue,
    strokeWidth: Float = 2f,
    dashed: Boolean = false,
) {
    val p1 = Offset(cx + x1 * s, cy - y1 * s)
    val p2 = Offset(cx + x2 * s, cy - y2 * s)
    val style = if (dashed) Stroke(strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))) else Stroke(strokeWidth)
    drawLine(color, p1, p2, style.width, pathEffect = style.pathEffect)
}

private fun DrawScope.drawCircle2D(
    cx: Float,
    cy: Float,
    s: Float,
    x: Float,
    y: Float,
    r: Float,
    color: Color = Color.Green,
    strokeWidth: Float = 2f,
    dashed: Boolean = false,
) {
    val center = Offset(cx + x * s, cy - y * s)
    val style = if (dashed) Stroke(strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))) else Stroke(strokeWidth)
    drawCircle(color, radius = r * s, center = center, style = style)
}

private fun DrawScope.drawPolygon2D(
    cx: Float,
    cy: Float,
    s: Float,
    points: List<Map<String, Any>>,
    color: Color = Color.Magenta,
    strokeWidth: Float = 2f,
    dashed: Boolean = false,
    fill: Boolean = false,
) {
    if (points.isEmpty()) return
    val path = Path()
    points.firstOrNull()?.let {
        val x = (it["x"] as Number).toFloat()
        val y = (it["y"] as Number).toFloat()
        path.moveTo(cx + x * s, cy - y * s)
    }
    points.drop(1).forEach {
        val x = (it["x"] as Number).toFloat()
        val y = (it["y"] as Number).toFloat()
        path.lineTo(cx + x * s, cy - y * s)
    }
    path.close()
    if (fill) {
        drawPath(path, color)
    } else {
        val style = if (dashed) Stroke(strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))) else Stroke(strokeWidth)
        drawPath(path, color, style = style)
    }
}

private fun DrawScope.drawPolyline2D(
    cx: Float,
    cy: Float,
    s: Float,
    points: List<Map<String, Any>>,
    color: Color,
    strokeWidth: Float,
    dashed: Boolean,
) {
    if (points.size < 2) return
    val path = Path()
    val first = points.first()
    path.moveTo(cx + (first["x"] as Number).toFloat() * s, cy - (first["y"] as Number).toFloat() * s)
    points.drop(1).forEach {
        val x = (it["x"] as Number).toFloat()
        val y = (it["y"] as Number).toFloat()
        path.lineTo(cx + x * s, cy - y * s)
    }
    val style = if (dashed) Stroke(strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))) else Stroke(strokeWidth)
    drawPath(path, color, style = style)
}

private fun DrawScope.drawArc2D(
    cx: Float,
    cy: Float,
    s: Float,
    x: Float,
    y: Float,
    r: Float,
    startDeg: Float,
    sweepDeg: Float,
    color: Color = Color.Cyan,
    strokeWidth: Float = 2f,
    dashed: Boolean = false,
) {
    val left = cx + (x - r) * s
    val top = cy - (y + r) * s
    val right = cx + (x + r) * s
    val bottom = cy - (y - r) * s
    val style = if (dashed) Stroke(strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))) else Stroke(strokeWidth)
    drawArc(
        color = color,
        startAngle = -startDeg,
        sweepAngle = -sweepDeg,
        useCenter = false,
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
        style = style,
    )
}

private fun DrawScope.drawAngleMarker(
    cx: Float,
    cy: Float,
    s: Float,
    ax: Float,
    ay: Float,
    bx: Float,
    by: Float,
    cx2: Float,
    cy2: Float,
    color: Color = Color.Yellow,
) {
    val v1 = Pair(ax - bx, ay - by)
    val v2 = Pair(cx2 - bx, cy2 - by)
    val a1 = atan2(v1.second, v1.first)
    val a2 = atan2(v2.second, v2.first)
    var start = Math.toDegrees(a1.toDouble()).toFloat()
    var end = Math.toDegrees(a2.toDouble()).toFloat()
    var sweep = end - start
    if (sweep < 0) sweep += 360f
    val r = 2f
    drawArc2D(cx, cy, s, bx, by, r, start, sweep, color)
}

private fun DrawScope.drawEllipse2D(
    cx: Float,
    cy: Float,
    s: Float,
    x: Float,
    y: Float,
    rx: Float,
    ry: Float,
    rotationDeg: Float,
    color: Color,
    strokeWidth: Float,
    dashed: Boolean,
) {
    val center = Offset(cx + x * s, cy - y * s)
    val w = rx * 2 * s
    val h = ry * 2 * s
    val style = if (dashed) Stroke(strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))) else Stroke(strokeWidth)
    rotate(rotationDeg, pivot = center) {
        drawOval(color, topLeft = Offset(center.x - w / 2, center.y - h / 2), size = androidx.compose.ui.geometry.Size(w, h), style = style)
    }
}

private fun DrawScope.drawRect2D(
    cx: Float,
    cy: Float,
    s: Float,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    rotationDeg: Float,
    color: Color,
    strokeWidth: Float,
    dashed: Boolean,
    fill: Boolean,
) {
    val center = Offset(cx + x * s, cy - y * s)
    val rw = w * s
    val rh = h * s
    val style = if (dashed) Stroke(strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))) else Stroke(strokeWidth)
    rotate(rotationDeg, pivot = center) {
        if (fill) {
            drawRect(color, topLeft = Offset(center.x - rw / 2, center.y - rh / 2), size = androidx.compose.ui.geometry.Size(rw, rh))
        } else {
            drawRect(
                color,
                topLeft = Offset(center.x - rw / 2, center.y - rh / 2),
                size = androidx.compose.ui.geometry.Size(rw, rh),
                style = style,
            )
        }
    }
}

private fun DrawScope.drawArrow2D(
    cx: Float,
    cy: Float,
    s: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    color: Color,
    strokeWidth: Float,
    dashed: Boolean,
) {
    drawLine2D(cx, cy, s, x1, y1, x2, y2, color, strokeWidth, dashed)
    val p1 = Offset(cx + x1 * s, cy - y1 * s)
    val p2 = Offset(cx + x2 * s, cy - y2 * s)
    val angle = atan2(p1.y - p2.y, p1.x - p2.x)
    val arrowLen = 10f + strokeWidth * 2
    val angle1 = angle + PI.toFloat() / 6f
    val angle2 = angle - PI.toFloat() / 6f
    val h1 = Offset(p2.x + arrowLen * cos(angle1), p2.y + arrowLen * sin(angle1))
    val h2 = Offset(p2.x + arrowLen * cos(angle2), p2.y + arrowLen * sin(angle2))
    val style = if (dashed) Stroke(strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))) else Stroke(strokeWidth)
    drawLine(color, p2, h1, style.width, pathEffect = style.pathEffect)
    drawLine(color, p2, h2, style.width, pathEffect = style.pathEffect)
}

private fun DrawScope.drawQuadraticBezier2D(
    cx: Float,
    cy: Float,
    s: Float,
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    color: Color,
    strokeWidth: Float,
    dashed: Boolean,
) {
    val path = Path()
    path.moveTo(cx + x0 * s, cy - y0 * s)
    path.quadraticBezierTo(cx + x1 * s, cy - y1 * s, cx + x2 * s, cy - y2 * s)
    val style = if (dashed) Stroke(strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))) else Stroke(strokeWidth)
    drawPath(path, color, style = style)
}

private fun DrawScope.drawCubicBezier2D(
    cx: Float,
    cy: Float,
    s: Float,
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    x3: Float,
    y3: Float,
    color: Color,
    strokeWidth: Float,
    dashed: Boolean,
) {
    val path = Path()
    path.moveTo(cx + x0 * s, cy - y0 * s)
    path.cubicTo(cx + x1 * s, cy - y1 * s, cx + x2 * s, cy - y2 * s, cx + x3 * s, cy - y3 * s)
    val style = if (dashed) Stroke(strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))) else Stroke(strokeWidth)
    drawPath(path, color, style = style)
}

private fun DrawScope.drawFunction2D(
    cx: Float,
    cy: Float,
    s: Float,
    exprStr: String,
    xMin: Float,
    xMax: Float,
    color: Color,
    strokeWidth: Float,
    dashed: Boolean,
) {
    try {
        val expr = ExpressionBuilder(exprStr).variables("x").build()
        val path = Path()
        val steps = 200
        val step = (xMax - xMin) / steps
        var first = true
        for (i in 0..steps) {
            val x = xMin + i * step
            expr.setVariable("x", x.toDouble())
            val y = expr.evaluate().toFloat()
            if (y.isNaN() || y.isInfinite()) {
                first = true
                continue
            }
            if (first) {
                path.moveTo(cx + x * s, cy - y * s)
                first = false
            } else {
                path.lineTo(cx + x * s, cy - y * s)
            }
        }
        val style = if (dashed) Stroke(strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))) else Stroke(strokeWidth)
        drawPath(path, color, style = style)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun DrawScope.drawShadedRegion2D(
    cx: Float,
    cy: Float,
    s: Float,
    expr1Str: String,
    expr2Str: String?,
    xMin: Float,
    xMax: Float,
    color: Color,
) {
    try {
        val expr1 = ExpressionBuilder(expr1Str).variables("x").build()
        val expr2 = expr2Str?.let { ExpressionBuilder(it).variables("x").build() }
        val path = Path()
        val steps = 100
        val step = (xMax - xMin) / steps

        // Draw top edge (expr1)
        var first = true
        for (i in 0..steps) {
            val x = xMin + i * step
            expr1.setVariable("x", x.toDouble())
            val y = expr1.evaluate().toFloat()
            if (first) {
                path.moveTo(cx + x * s, cy - y * s)
                first = false
            } else {
                path.lineTo(cx + x * s, cy - y * s)
            }
        }

        // Draw bottom edge (expr2 or y=0)
        if (expr2 != null) {
            for (i in steps downTo 0) {
                val x = xMin + i * step
                expr2.setVariable("x", x.toDouble())
                val y = expr2.evaluate().toFloat()
                path.lineTo(cx + x * s, cy - y * s)
            }
        } else {
            path.lineTo(cx + xMax * s, cy)
            path.lineTo(cx + xMin * s, cy)
        }

        path.close()
        drawPath(path, color)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun DrawScope.drawTangent2D(
    cx: Float,
    cy: Float,
    s: Float,
    exprStr: String,
    x0: Float,
    length: Float,
    color: Color,
    strokeWidth: Float,
    dashed: Boolean,
) {
    try {
        val expr = ExpressionBuilder(exprStr).variables("x").build()
        expr.setVariable("x", x0.toDouble())
        val y0 = expr.evaluate().toFloat()

        val h = 0.001
        expr.setVariable("x", x0.toDouble() + h)
        val y1 = expr.evaluate()
        expr.setVariable("x", x0.toDouble() - h)
        val y2 = expr.evaluate()
        val m = ((y1 - y2) / (2 * h)).toFloat()

        // Tangent line: y - y0 = m * (x - x0) => y = m*(x - x0) + y0
        val xMin = x0 - length / 2
        val xMax = x0 + length / 2
        val yMin = m * (xMin - x0) + y0
        val yMax = m * (xMax - x0) + y0

        drawLine2D(cx, cy, s, xMin, yMin, xMax, yMax, color, strokeWidth, dashed)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun project3D(
    x: Float,
    y: Float,
    z: Float,
    yawDeg: Float,
    pitchDeg: Float,
): Pair<Float, Float> {
    val yaw = yawDeg * PI.toFloat() / 180f
    val pitch = pitchDeg * PI.toFloat() / 180f
    val x1 = x * cos(yaw) - z * sin(yaw)
    val z1 = x * sin(yaw) + z * cos(yaw)
    val y1 = y * kotlin.math.cos(pitch) - z1 * kotlin.math.sin(pitch)
    val px = x1 + 0.2f * z1
    val py = y1 + 0.1f * z1
    return Pair(px, py)
}

private fun DrawScope.drawAxes(
    cx: Float,
    cy: Float,
    s: Float,
    color: Color,
) {
    drawLine(color, Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1f)
    drawLine(color, Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1f)
    for (i in -20..20) {
        val x = cx + i * s
        drawLine(color.copy(alpha = 0.3f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        drawLine(color, Offset(x, cy - 4f), Offset(x, cy + 4f), strokeWidth = 1f)
        val y = cy - i * s
        drawLine(color.copy(alpha = 0.3f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        drawLine(color, Offset(cx - 4f, y), Offset(cx + 4f, y), strokeWidth = 1f)
    }
}

private fun DrawScope.drawAxesConfig(
    cx: Float,
    cy: Float,
    s: Float,
    shape: Map<String, Any>,
    color: Color,
) {
    val xMin = (shape["xMin"] as? Number)?.toInt() ?: -10
    val xMax = (shape["xMax"] as? Number)?.toInt() ?: 10
    val yMin = (shape["yMin"] as? Number)?.toInt() ?: -10
    val yMax = (shape["yMax"] as? Number)?.toInt() ?: 10
    val grid = (shape["grid"] as? Boolean) ?: true
    drawLine(color, Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1f)
    drawLine(color, Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1f)
    for (i in xMin..xMax) {
        val x = cx + i * s
        if (grid) drawLine(color.copy(alpha = 0.3f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        drawLine(color, Offset(x, cy - 4f), Offset(x, cy + 4f), strokeWidth = 1f)
    }
    for (j in yMin..yMax) {
        val y = cy - j * s
        if (grid) drawLine(color.copy(alpha = 0.3f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        drawLine(color, Offset(cx - 4f, y), Offset(cx + 4f, y), strokeWidth = 1f)
    }
}

private fun DrawScope.drawTextLabel(
    cx: Float,
    cy: Float,
    s: Float,
    x: Float,
    y: Float,
    text: String,
    color: Color,
) {
    val native = drawContext.canvas.nativeCanvas
    val paint =
        android.graphics.Paint().apply {
            this.color =
                android.graphics.Color.argb(
                    (color.alpha * 255).toInt(),
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt(),
                )
            textSize = 28f
            isAntiAlias = true
        }
    native.drawText(text, cx + x * s + 6f, cy - y * s - 6f, paint)
}

private fun parseColor(
    value: Any?,
    onSurfaceColor: Color,
): Color? {
    val s = value as? String ?: return null
    return try {
        if (s.startsWith("#")) {
            val c = android.graphics.Color.parseColor(s)
            Color(c)
        } else {
            when (s.lowercase()) {
                "red" -> Color.Red
                "green" -> Color.Green
                "blue" -> Color.Blue
                "black" -> onSurfaceColor // Adapt black to current theme's onSurface (white in dark mode)
                "gray", "grey" -> Color.Gray
                "yellow" -> Color.Yellow
                "magenta" -> Color.Magenta
                "cyan" -> Color.Cyan
                "white" -> if (onSurfaceColor == Color.White) Color.Black else Color.White // Reverse for dark mode
                else -> null
            }
        }
    } catch (_: Exception) {
        null
    }
}
