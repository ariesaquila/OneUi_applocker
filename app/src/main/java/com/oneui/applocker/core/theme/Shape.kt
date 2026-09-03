package com.oneui.applocker.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val OneUiCardCornerRadius = 26.dp
val OneUiButtonCornerRadius = 20.dp
val OneUiInputCornerRadius = 18.dp
val OneUiPillCornerRadius = 100.dp

val OneUiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(OneUiInputCornerRadius),
    large = RoundedCornerShape(OneUiCardCornerRadius),
    extraLarge = RoundedCornerShape(32.dp)
)
