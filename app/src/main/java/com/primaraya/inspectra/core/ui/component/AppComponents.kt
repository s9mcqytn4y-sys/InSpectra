package com.primaraya.inspectra.core.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Nada status untuk badge dan indikator visual.
 */
enum class NadaStatusAplikasi {
    INFO,
    SUKSES,
    PERINGATAN,
    BAHAYA
}

/**
 * Badge status visual yang mengikuti tema InSpectra.
 */
@Composable
fun AppStatusBadge(
    label: String,
    nada: NadaStatusAplikasi,
    modifier: Modifier = Modifier
) {
    val colors = when (nada) {
        NadaStatusAplikasi.INFO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        NadaStatusAplikasi.SUKSES -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        NadaStatusAplikasi.PERINGATAN -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        NadaStatusAplikasi.BAHAYA -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = colors.first,
        contentColor = colors.second
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Tampilan empty state yang jujur dan bersih.
 */
@Composable
fun AppEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (onRetry != null) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Coba Lagi")
            }
        }
    }
}

/**
 * Indikator loading global.
 */
@Composable
fun AppLoading(
    modifier: Modifier = Modifier,
    label: String? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            if (label != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Dialog konfirmasi/informasi ramah.
 */
@Composable
fun AppFriendlyDialog(
    title: String,
    message: String,
    buttonText: String = "Mengerti",
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(buttonText)
            }
        }
    )
}
