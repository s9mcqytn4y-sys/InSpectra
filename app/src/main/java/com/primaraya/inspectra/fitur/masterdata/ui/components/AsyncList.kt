package com.primaraya.inspectra.fitur.masterdata.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.primaraya.inspectra.core.common.AsyncData
import com.primaraya.inspectra.core.ui.component.AppEmptyState
import com.primaraya.inspectra.core.ui.component.AppLoading

@Composable
fun <T> AsyncList(
    data: AsyncData<List<T>>,
    canLoadMore: Boolean,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (List<T>) -> Unit
) {
    when (data) {
        is AsyncData.Loading -> AppLoading(Modifier.fillMaxSize())
        is AsyncData.Success -> {
            content(data.data)
        }
        is AsyncData.Error -> Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(data.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Text(data.message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onLoadMore) { Text("Coba Lagi") }
        }
        is AsyncData.Empty -> AppEmptyState(
            title = data.title,
            message = data.message,
            modifier = Modifier.fillMaxSize()
        )
        else -> Unit
    }
}
