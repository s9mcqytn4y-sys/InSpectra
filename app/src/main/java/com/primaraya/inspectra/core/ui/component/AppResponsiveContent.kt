package com.primaraya.inspectra.core.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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

/**
 * Responsive host for forms.
 *
 * Phone: ModalBottomSheet
 * Tablet: Right side pane
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveFormHost(
    visible: Boolean,
    isTablet: Boolean,
    onDismiss: () -> Unit,
    form: @Composable () -> Unit
) {
    if (!visible) return

    if (isTablet) {
        Surface(
            modifier = Modifier
                .width(420.dp)
                .fillMaxHeight(),
            tonalElevation = 6.dp
        ) {
            form()
        }
    } else {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            form()
        }
    }
}
