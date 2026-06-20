package com.primaraya.inspectra.core.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Skeleton shimmer minimalis reusable untuk list.
 */
@Composable
fun AppListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 8
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(itemCount) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .alpha(alpha),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.35f)
                            .height(18.dp)
                            .background(Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.75f)
                            .height(14.dp)
                            .background(Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.5f)
                            .height(14.dp)
                            .background(Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}
