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
        color = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Data Induk",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Kelola referensi QC aktif",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )

            ScrollableTabRow(
                selectedTabIndex = tabAktif.ordinal,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                edgePadding = 16.dp,
                divider = {},
                indicator = { tabPositions ->
                    if (tabAktif.ordinal < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[tabAktif.ordinal]),
                            color = Color(0xFFD97706)
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
                                style = MaterialTheme.typography.titleSmall
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
