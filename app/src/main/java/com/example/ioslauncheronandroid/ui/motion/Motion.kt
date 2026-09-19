package com.example.ioslauncheronandroid.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object Motion {
    val Settle = spring<Float>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow,
    )

    val Jump = spring<Float>(
        dampingRatio = 0.58f,
        stiffness = 320f,
    )
    val Snappy = spring<Float>(
        dampingRatio = 0.78f,
        stiffness = Spring.StiffnessMedium,
    )
    val Bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )
    val Soft = spring<Float>(
        dampingRatio = 0.92f,
        stiffness = 280f,
    )
    val Dp = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow,
    )
    val Dock = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = 380f,
    )
}
