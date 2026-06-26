package com.primaraya.inspectra.fitur.masterdata.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.primaraya.inspectra.fitur.masterdata.domain.FilterDataInduk
import com.primaraya.inspectra.fitur.masterdata.ui.MasterDataContract

@Composable
fun DataIndukSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    tabAktif: MasterDataContract.TabMasterData,
    filterAktif: FilterDataInduk,
    onFilterSelected: (FilterDataInduk) -> Unit,
    modifier: Modifier = Modifier
) {
    val placeholder = when (tabAktif) {
        MasterDataContract.TabMasterData.PART -> "Cari part, nomor part, atau model"
        MasterDataContract.TabMasterData.MATERIAL -> "Cari material, supplier, atau spesifikasi"
        MasterDataContract.TabMasterData.SUPPLIER -> "Cari supplier atau kategori"
        MasterDataContract.TabMasterData.DEFECT -> "Cari defect atau kategori"
    }

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (tabAktif == MasterDataContract.TabMasterData.PART) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterDataInduk.entries.forEach { filter ->
                    FilterChip(
                        selected = filterAktif == filter,
                        onClick = { onFilterSelected(filter) },
                        label = {
                            Text(
                                text = filter.name.replace("_", " ").lowercase().capitalize(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
    }
}
