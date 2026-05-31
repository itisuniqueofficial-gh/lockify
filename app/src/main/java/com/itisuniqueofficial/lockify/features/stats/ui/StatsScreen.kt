package com.itisuniqueofficial.lockify.features.stats.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.itisuniqueofficial.lockify.features.stats.AppUsage
import com.itisuniqueofficial.lockify.features.stats.UsageStatsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val usage by produceState(initialValue = emptyList<AppUsage>()) {
        value = withContext(Dispatchers.IO) { UsageStatsProvider.topApps(context) }
    }
    val barColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usage (7 days)") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val max = (usage.maxOfOrNull { it.totalTimeMs } ?: 1L).coerceAtLeast(1L)
            Canvas(
                modifier = Modifier.fillMaxWidth().height(180.dp).padding(16.dp)
            ) {
                if (usage.isEmpty()) return@Canvas
                val gap = 8.dp.toPx()
                val barWidth = (size.width - gap * (usage.size - 1)) / usage.size
                usage.forEachIndexed { i, u ->
                    val h = size.height * (u.totalTimeMs.toFloat() / max)
                    drawRect(
                        color = barColor,
                        topLeft = Offset(i * (barWidth + gap), size.height - h),
                        size = Size(barWidth, h)
                    )
                }
            }
            LazyColumn {
                items(usage, key = { it.packageName }) { u ->
                    ListItem(
                        headlineContent = { Text(u.packageName) },
                        supportingContent = { Text(formatDuration(u.totalTimeMs)) }
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val minutes = ms / 60000
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
}
