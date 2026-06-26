package com.primaraya.inspectra.fitur.masterdata.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.primaraya.inspectra.core.common.AsyncData

@Composable
fun <T> AsyncRelationList(
    data: AsyncData<T>,
    content: @Composable (T) -> Unit
) {
    when (data) {
        is AsyncData.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
        is AsyncData.Success -> content(data.data)
        is AsyncData.Error -> Text(
            text = "Gagal: ${data.message}",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
        else -> Unit
    }
}
