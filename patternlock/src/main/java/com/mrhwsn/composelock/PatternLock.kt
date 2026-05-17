package com.mrhwsn.composelock

import android.view.MotionEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

interface LockCallback {
    fun onStart(dot: Dot)
    fun onDotConnected(dot: Dot)
    fun onResult(result: List<Dot>)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PatternLock(
    modifier: Modifier,
    dimension: Int,
    sensitivity: Float,
    dotsColor: Color,
    dotsSize: Float,
    linesColor: Color,
    linesStroke: Float,
    animationDuration: Int = 200,
    animationDelay: Long = 100,
    callback: LockCallback
) {
    val scope = rememberCoroutineScope()
    var canvasSize by remember { mutableStateOf(Offset.Zero) }
    val dotsList = remember(dimension, dotsSize, canvasSize) {
        buildDots(dimension, dotsSize, canvasSize)
    }
    val previewLine = remember {
        mutableStateOf<Line?>(null)
    }
    val connectedLines = remember {
        mutableStateListOf<Line>()
    }
    val connectedDots = remember {
        mutableStateListOf<Dot>()
    }
    val connectedDotIds = remember { mutableStateSetOf<Int>() }

    fun animateDot(dot: Dot) {
        scope.launch {
            dot.size.animateTo(dotsSize * 1.8f, tween(animationDuration))
            delay(animationDelay)
            dot.size.animateTo(dotsSize, tween(animationDuration))
        }
    }

    fun connectDot(dot: Dot) {
        if (!connectedDotIds.add(dot.id)) return
        val previousDot = connectedDots.lastOrNull()
        if (previousDot == null) {
            callback.onStart(dot)
            previewLine.value = Line(start = dot.offset, end = dot.offset)
        } else {
            connectedLines.add(Line(start = previousDot.offset, end = dot.offset))
            callback.onDotConnected(dot)
            previewLine.value = Line(start = dot.offset, end = dot.offset)
        }
        connectedDots.add(dot)
        animateDot(dot)
    }

    fun connectDotWithSkippedDots(dot: Dot) {
        val previousDot = connectedDots.lastOrNull()
        if (previousDot != null) {
            findSkippedDot(previousDot, dot, dotsList, connectedDotIds)?.let { connectDot(it) }
        }
        connectDot(dot)
    }

    Canvas(
        modifier.pointerInteropFilter {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    connectedLines.clear()
                    connectedDots.clear()
                    connectedDotIds.clear()
                    dotsList.firstOrNull { dot -> dot.hitTest(it.x, it.y, sensitivity) }
                        ?.let { dot -> connectDot(dot) }
                }


                MotionEvent.ACTION_MOVE -> {
                    previewLine.value?.let { line ->
                        previewLine.value = line.copy(end = Offset(it.x, it.y))
                    }
                    dotsList.firstOrNull { dot ->
                        dot.id !in connectedDotIds && dot.hitTest(it.x, it.y, sensitivity)
                    }?.let { dot -> connectDotWithSkippedDots(dot) }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    previewLine.value = null
                    callback.onResult(connectedDots.toList())
                    connectedLines.clear()
                    connectedDots.clear()
                    connectedDotIds.clear()
                }
            }
            true
        }) {
        val currentSize = Offset(size.width, size.height)
        if (canvasSize != currentSize) {
            canvasSize = currentSize
        }
        for (line in connectedLines) {
            drawLine(
                color = linesColor,
                start = line.start,
                end = line.end,
                strokeWidth = linesStroke,
                cap = StrokeCap.Round
            )
        }
        if (previewLine.value != null) {
            drawLine(
                color = linesColor,
                start = previewLine.value!!.start,
                end = previewLine.value!!.end,
                strokeWidth = linesStroke,
                cap = StrokeCap.Round
            )
        }
        for (dots in dotsList) {
            drawCircle(
                color = dotsColor,
                radius = dots.size.value,
                center = dots.offset
            )
        }

    }
}

private fun buildDots(dimension: Int, dotsSize: Float, canvasSize: Offset): List<Dot> {
    if (dimension <= 0 || canvasSize == Offset.Zero) return emptyList()

    val realDimension = dimension + 1
    val spaceBetweenWidthDots = canvasSize.x / realDimension
    val spaceBetweenHeightDots = canvasSize.y / realDimension
    return buildList(dimension * dimension) {
        for (y in 1..dimension) {
            for (x in 1..dimension) {
                add(
                    Dot(
                        id = size + 1,
                        offset = Offset(spaceBetweenWidthDots * x, spaceBetweenHeightDots * y),
                        size = Animatable(dotsSize)
                    )
                )
            }
        }
    }
}

private fun Dot.hitTest(x: Float, y: Float, sensitivity: Float): Boolean {
    return abs(x - offset.x) <= sensitivity && abs(y - offset.y) <= sensitivity
}

private fun findSkippedDot(
    start: Dot,
    end: Dot,
    dots: List<Dot>,
    connectedDotIds: Set<Int>
): Dot? {
    val startIndex = start.id - 1
    val endIndex = end.id - 1
    val dimension = kotlin.math.sqrt(dots.size.toDouble()).toInt()
    if (dimension <= 0) return null

    val startRow = startIndex / dimension
    val startCol = startIndex % dimension
    val endRow = endIndex / dimension
    val endCol = endIndex % dimension
    val rowDiff = endRow - startRow
    val colDiff = endCol - startCol

    if (abs(rowDiff) != 2 && abs(colDiff) != 2) return null
    if (!(rowDiff == 0 || colDiff == 0 || abs(rowDiff) == abs(colDiff))) return null

    val middleRow = startRow + rowDiff / 2
    val middleCol = startCol + colDiff / 2
    val middleId = middleRow * dimension + middleCol + 1
    return dots.firstOrNull { it.id == middleId && it.id !in connectedDotIds }
}

@Preview
@Composable
fun PatternLockPreview() {
    PatternLock(
        Modifier,
        4,
        100f,
        Color.Black,
        20f,
        Color.Black,
        30f,
        200,
        100,
        object : LockCallback {
            override fun onStart(dot: Dot) {}
            override fun onDotConnected(dot: Dot) {}
            override fun onResult(result: List<Dot>) {}
        }
    )
}
