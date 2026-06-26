package com.primaraya.inspectra.fitur.masterdata.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Immutable
data class PickerItemUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val badges: List<String> = emptyList(),
    val description: String? = null,
    val enabled: Boolean = true
)

@Composable
fun InspectraAdvancedPickerSheet(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    items: List<PickerItemUi>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = "Pilih",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Cari data") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = MaterialTheme.shapes.medium
        )

        LazyColumn(
            modifier = Modifier.heightIn(max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.id }) { item ->
                ElevatedCard(
                    onClick = { if (item.enabled) onSelect(item.id) },
                    enabled = item.enabled,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (item.id == selectedId) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            item.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (item.badges.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                item.badges.forEach { badge ->
                                    SuggestionChip(onClick = {}, label = { Text(badge) })
                                }
                            }
                        }

                        item.description?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onConfirm,
                enabled = selectedId != null,
                shape = MaterialTheme.shapes.small
            ) {
                Text(confirmText)
            }
        }
    }
}
