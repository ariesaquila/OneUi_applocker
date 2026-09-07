package com.oneui.applocker.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oneui.applocker.core.theme.OneUiBgAmoled
import com.oneui.applocker.core.theme.OneUiShapes

/**
 * Samsung One UI signature rounded container card.
 * Groups related settings or list items cleanly.
 */
@Composable
fun OneUiCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = 0.dp,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isAmoled = MaterialTheme.colorScheme.background == OneUiBgAmoled
    val effectiveBorder = border ?: if (isAmoled) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)) else null

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(OneUiShapes.large)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = OneUiShapes.large,
        color = backgroundColor,
        tonalElevation = elevation,
        shadowElevation = elevation,
        border = effectiveBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}
