package com.primaraya.inspectra.core.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppResponsiveContent(
    modifier: Modifier = Modifier,
    content: @Composable (isTablet: Boolean, contentModifier: Modifier) -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 840.dp
        val width = if (isTablet) 1120.dp else maxWidth

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isTablet) 32.dp else 16.dp)
        ) {
            content(
                isTablet,
                Modifier
                    .widthIn(max = width)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
    }
}
