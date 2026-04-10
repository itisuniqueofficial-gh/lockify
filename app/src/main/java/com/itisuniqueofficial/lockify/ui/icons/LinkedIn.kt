package com.itisuniqueofficial.lockify.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val LinkedIn: ImageVector
    get() {
        if (_LinkedIn != null) return _LinkedIn!!
        _LinkedIn = ImageVector.Builder(
            name = "LinkedIn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 3f)
                arcTo(2f, 2f, 0f, false, true, 21f, 5f)
                verticalLineTo(19f)
                arcTo(2f, 2f, 0f, false, true, 19f, 21f)
                horizontalLineTo(5f)
                arcTo(2f, 2f, 0f, false, true, 3f, 19f)
                verticalLineTo(5f)
                arcTo(2f, 2f, 0f, false, true, 5f, 3f)
                horizontalLineTo(19f)
                moveTo(18.5f, 18.5f)
                verticalLineTo(13.2f)
                arcTo(3.26f, 3.26f, 0f, false, false, 15.24f, 9.94f)
                curveToRelative(-0.85f, 0f, -1.84f, 0.52f, -2.32f, 1.3f)
                verticalLineTo(10.13f)
                horizontalLineTo(10.13f)
                verticalLineTo(18.5f)
                horizontalLineTo(12.92f)
                verticalLineTo(13.57f)
                curveToRelative(0f, -0.77f, 0.62f, -1.4f, 1.39f, -1.4f)
                arcTo(1.4f, 1.4f, 0f, false, true, 15.71f, 13.57f)
                verticalLineTo(18.5f)
                horizontalLineTo(18.5f)
                moveTo(6.88f, 8.56f)
                arcTo(1.68f, 1.68f, 0f, false, false, 8.56f, 6.88f)
                curveToRelative(0f, -0.93f, -0.75f, -1.68f, -1.68f, -1.68f)
                arcTo(1.68f, 1.68f, 0f, false, false, 5.2f, 6.88f)
                curveToRelative(0f, 0.93f, 0.75f, 1.68f, 1.68f, 1.68f)
                moveTo(8.27f, 18.5f)
                verticalLineTo(10.13f)
                horizontalLineTo(5.5f)
                verticalLineTo(18.5f)
                horizontalLineTo(8.27f)
                close()
            }
        }.build()
        return _LinkedIn!!
    }

private var _LinkedIn: ImageVector? = null
