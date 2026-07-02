package com.primaraya.inspectra.fitur.masterdata.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.primaraya.inspectra.core.ui.component.SearchBarElite
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
        MasterDataContract.TabMasterData.KARYAWAN -> "Cari nama atau no reg"
    }

    Column(modifier = modifier.padding(vertical = 12.dp).background(MaterialTheme.colorScheme.surface)) {
        SearchBarElite(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = placeholder,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(12.dp))
        
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (tabAktif == MasterDataContract.TabMasterData.PART) {
                items(FilterDataInduk.entries.toTypedArray(), key = { it.name }) { filter ->
                    FilterChip(
                        selected = filterAktif == filter,
                        onClick = { onFilterSelected(filter) },
                        label = { 
                            Text(
                                text = filter.labelIndonesia(),
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            } else {
                // Generic Filter Chip for other tabs to keep UI consistent
                item {
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text("Semua Data", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

private fun FilterDataInduk.labelIndonesia(): String = when (this) {
    FilterDataInduk.SEMUA -> "Semua"
    FilterDataInduk.SIAP_INPUT -> "Siap Input"
    FilterDataInduk.PERLU_VERIFIKASI -> "Perlu Verifikasi"
    FilterDataInduk.TANPA_MATERIAL -> "Tanpa Material"
    FilterDataInduk.TANPA_DEFECT -> "Tanpa Defect"
    FilterDataInduk.NONAKTIF -> "Nonaktif"
}
