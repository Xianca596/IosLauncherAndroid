package com.example.ioslauncheronandroid.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ioslauncheronandroid.ui.glass.liquidGlass
import kotlin.math.abs

@Composable
fun PageDots(state: PagerState, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .liquidGlass(null, elevation = 4)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(count) { i ->
            Row(
                Modifier
                    .size(7.dp)
                    .graphicsLayer {
                        val dist = abs(state.currentPage - i + state.currentPageOffsetFraction).coerceIn(0f, 1f)
                        val s = 1.25f - 0.25f * dist
                        scaleX = s; scaleY = s
                        alpha = 1f - 0.6f * dist
                    }
                    .background(Color.White, CircleShape)
            ) {}
        }
    }
}
