package com.primaraya.inspectra.ui.screens.masterdata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.primaraya.inspectra.ui.screens.checksheet.EmptyStateCard
import com.primaraya.inspectra.ui.screens.checksheet.FriendlyInfoDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDataScreen(
    onBackClick: () -> Unit,
    viewModel: MasterDataViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Master Data Management", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A365D),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = Color(0xFF1A365D),
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                        color = Color(0xFFD97706)
                    )
                }
            ) {
                Tab(selected = uiState.selectedTab == 0, onClick = { viewModel.setTab(0) }) {
                    Text("Parts", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = uiState.selectedTab == 1, onClick = { viewModel.setTab(1) }) {
                    Text("Materials", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = uiState.selectedTab == 2, onClick = { viewModel.setTab(2) }) {
                    Text("Defects", modifier = Modifier.padding(16.dp))
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF1A365D))
                    }
                } else {
                    when (uiState.selectedTab) {
                        0 -> PartList(uiState.parts)
                        1 -> MaterialList(uiState.materials)
                        2 -> DefectList(uiState.defects)
                    }
                }
            }
        }
    }

    uiState.userMessage?.let { msg ->
        FriendlyInfoDialog(
            title = msg.title,
            message = msg.body,
            buttonText = msg.actionLabel ?: "Mengerti",
            onDismiss = { viewModel.clearUserMessage() }
        )
    }
}

@Composable
fun PartList(parts: List<com.primaraya.inspectra.masterdata.domain.MasterPartDto>) {
    if (parts.isEmpty()) {
        EmptyStateCard(title = "No Parts Found", message = "Add some parts to Supabase.")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(parts) { part ->
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text(part.uniq_no, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(part.nama_part, style = MaterialTheme.typography.bodyLarge)
                        Text("Model: ${part.model ?: "-"}", style = MaterialTheme.typography.bodySmall)
                        Text("Komoditas: ${part.komoditas}", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD97706))
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialList(materials: List<com.primaraya.inspectra.masterdata.domain.MasterMaterialDto>) {
    if (materials.isEmpty()) {
        EmptyStateCard(title = "No Materials Found", message = "Add some materials to Supabase.")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(materials) { material ->
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text(material.nama_material, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Supplier: ${material.supplier ?: "-"}", style = MaterialTheme.typography.bodyMedium)
                        Text("Spec: ${material.spec ?: "-"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun DefectList(defects: List<com.primaraya.inspectra.masterdata.domain.MasterDefectDto>) {
    if (defects.isEmpty()) {
        EmptyStateCard(title = "No Defects Found", message = "Add some defects to Supabase.")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(defects) { defect ->
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(defect.nama_defect, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("ID: ${defect.id_defect}", style = MaterialTheme.typography.bodySmall)
                        }
                        SuggestionChip(onClick = {}, label = { Text(defect.kategori) })
                    }
                }
            }
        }
    }
}
