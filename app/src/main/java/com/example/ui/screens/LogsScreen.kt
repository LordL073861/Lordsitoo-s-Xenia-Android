package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.DiagnosticLogEntry
import com.example.core.model.LogLevel
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logEntries by viewModel.logEntries.collectAsState()
    var selectedTag by remember { mutableStateOf("ALL") }
    var filterQuery by remember { mutableStateOf("") }

    val tags = listOf("ALL", "ANDROID", "VULKAN", "GPU", "CPU", "JIT", "INPUT", "FILESYSTEM", "KERNEL")

    val displayedLogs = remember(logEntries, selectedTag, filterQuery) {
        logEntries.filter { entry ->
            val matchTag = selectedTag == "ALL" || entry.tag.equals(selectedTag, ignoreCase = true)
            val matchQuery = filterQuery.isBlank() || entry.message.contains(filterQuery, ignoreCase = true) || entry.tag.contains(filterQuery, ignoreCase = true)
            matchTag && matchQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Filter & Search Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DIAGNOSTIC LOGCAT STREAM", color = XeniaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                IconButton(
                    onClick = {
                        val fullLog = logEntries.joinToString("\n") {
                            "[${SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(it.timestamp))}] [${it.tag}] [${it.level.name}]: ${it.message}"
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Xenia Diagnostic Logs", fullLog))
                        Toast.makeText(context, "All logs copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Logs", tint = TechTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = { Text("Filter log output...", color = TechTextMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.FilterAlt, contentDescription = null, tint = XeniaGreen) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = XeniaGreen,
                    unfocusedBorderColor = TechBorder,
                    focusedTextColor = TechTextPrimary,
                    unfocusedTextColor = TechTextPrimary,
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("logs_filter_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tag Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tags.take(5).forEach { tag ->
                    val isSelected = selectedTag == tag
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTag = tag },
                        label = { Text(tag, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = XeniaGreenContainer,
                            selectedLabelColor = OnXeniaGreenContainer,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TechTextSecondary
                        )
                    )
                }
            }
        }

        // Log Items Stream
        if (displayedLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No log messages matching filter.", color = TechTextMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(displayedLogs) { log ->
                    LogItemRow(log)
                }
            }
        }
    }
}

@Composable
fun LogItemRow(entry: DiagnosticLogEntry) {
    val levelColor = when (entry.level) {
        LogLevel.DEBUG -> TechTextMuted
        LogLevel.INFO -> TechCyan
        LogLevel.WARN -> WarningAmber
        LogLevel.ERROR -> ErrorRed
    }

    val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(entry.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                timeStr,
                color = TechTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "[${entry.tag}]",
                color = levelColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                entry.message,
                color = TechTextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
