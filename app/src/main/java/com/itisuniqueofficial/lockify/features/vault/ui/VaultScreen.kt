package com.itisuniqueofficial.lockify.features.vault.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.itisuniqueofficial.lockify.features.vault.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(navController: NavHostController) {
    val context = LocalContext.current
    val repo = remember { VaultRepository(context) }
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf(repo.list()) }
    var pendingExport by remember { mutableStateOf<File?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) scope.launch {
            withContext(Dispatchers.IO) { repo.import(uri) }
            entries = repo.list()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val src = pendingExport
        if (uri != null && src != null) scope.launch {
            withContext(Dispatchers.IO) { repo.export(src, uri) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { importLauncher.launch("*/*") }) {
                        Icon(Icons.Default.Add, contentDescription = "Import file")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn {
                items(entries, key = { it.file.path }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.displayName) },
                        supportingContent = { Text("${entry.sizeBytes} bytes") },
                        trailingContent = {
                            androidx.compose.foundation.layout.Row {
                                IconButton(onClick = {
                                    pendingExport = entry.file
                                    exportLauncher.launch(entry.displayName)
                                }) { Icon(Icons.Default.Download, contentDescription = "Export") }
                                IconButton(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { repo.delete(entry.file) }
                                        entries = repo.list()
                                    }
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                            }
                        }
                    )
                }
            }
        }
    }
}
