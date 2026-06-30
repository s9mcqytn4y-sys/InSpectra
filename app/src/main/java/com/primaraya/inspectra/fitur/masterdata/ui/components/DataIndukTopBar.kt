package com.primaraya.inspectra.fitur.masterdata.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.primaraya.inspectra.fitur.masterdata.ui.MasterDataContract

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataIndukTopBar(
    tabAktif: MasterDataContract.TabMasterData,
    onTabSelected: (MasterDataContract.TabMasterData) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Data Induk",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kelola referensi QC aktif",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            TabRow(
                selectedTabIndex = tabAktif.ordinal,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) },
                indicator = { tabPositions ->
                    if (tabAktif.ordinal < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[tabAktif.ordinal]),
                            color = MaterialTheme.colorScheme.primary,
                            height = 3.dp
                        )
                    }
                }
            ) {
                MasterDataContract.TabMasterData.entries.forEach { tab ->
                    Tab(
                        selected = tabAktif == tab,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Text(
                                text = tab.name.lowercase().capitalize(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (tabAktif == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }
    }
}

// Extension to capitalize
fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
