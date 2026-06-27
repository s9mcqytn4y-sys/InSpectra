package com.primaraya.inspectra.core.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed interface FeedbackType {
    data object Success : FeedbackType
    data object Error : FeedbackType
    data object Warning : FeedbackType
    data object Info : FeedbackType
}

@Composable
fun InspectraFeedback(
    message: String,
    type: FeedbackType = FeedbackType.Info,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val (backgroundColor, contentColor, icon) = when (type) {
        FeedbackType.Success -> Triple(Color(0xFFDCFCE7), Color(0xFF166534), Icons.Default.CheckCircle)
        FeedbackType.Error -> Triple(Color(0xFFFEF2F2), Color(0xFF991B1B), Icons.Default.Error)
        FeedbackType.Warning -> Triple(Color(0xFFFFFBEB), Color(0xFF92400E), Icons.Default.Warning)
        FeedbackType.Info -> Triple(Color(0xFFEFF6FF), Color(0xFF1E40AF), Icons.Default.Info)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = message,
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = contentColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
