package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.hardware.HardwareDiagnosticEngine
import com.example.core.model.CompatibilityVerdict
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun DiagnosticsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hardwareReport by viewModel.hardwareReport.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Section: Verdict Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hardwareReport.compatibilityVerdict.isCompatible) DarkSurfaceContainer else Color(0xFF3E1212)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                    if (hardwareReport.compatibilityVerdict.isCompatible) listOf(XeniaGreen, TechCyan) else listOf(ErrorRed, Color(0xFF880000))
                )
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (hardwareReport.compatibilityVerdict.isCompatible) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (hardwareReport.compatibilityVerdict.isCompatible) XeniaGreen else ErrorRed
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        hardwareReport.compatibilityVerdict.statusTitle,
                        color = TechTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    hardwareReport.recommendedSettingsSummary,
                    color = TechTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Action: Export / Copy Hardware Report
        Button(
            onClick = {
                val reportText = HardwareDiagnosticEngine.generateExportableReportText(hardwareReport)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Xenia-Android Hardware Report", reportText))
                Toast.makeText(context, "Hardware diagnostic report copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = XeniaGreen),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("export_diagnostic_button")
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Color(0xFF00391A))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export & Copy Hardware Report (.txt)", color = Color(0xFF00391A), fontWeight = FontWeight.Bold)
        }

        // Section: Processor & Architecture
        Text("SOC & CPU ARCHITECTURE", color = XeniaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(label = "SoC Model", value = "${hardwareReport.socManufacturer} ${hardwareReport.socName}")
                DetailRow(label = "Platform Family", value = hardwareReport.mediaTekFamily)
                DetailRow(label = "CPU Architecture", value = hardwareReport.cpuArchitecture)
                DetailRow(label = "CPU Cores", value = "${hardwareReport.cpuCores} Cores Available")
                DetailRow(label = "ARM64 v8-a ABI", value = if (hardwareReport.supportsArm64V8a) "SUPPORTED" else "UNSUPPORTED")
                DetailRow(label = "LSE Atomics (Fast Locks)", value = if (hardwareReport.supportsLseAtomics) "SUPPORTED (Hardware LSE)" else "FALLBACK (LDXR/STXR)")
                DetailRow(label = "NEON / ASIMD SIMD", value = if (hardwareReport.supportsNeonSimd) "SUPPORTED" else "NO")
                DetailRow(label = "FP16 Half-Precision", value = if (hardwareReport.supportsFp16) "SUPPORTED" else "NO")
                DetailRow(label = "Host RAM", value = "${hardwareReport.availableRamMb} MB Free / ${hardwareReport.totalRamMb} MB Total")
            }
        }

        // Section: GPU & Vulkan Pipeline
        Text("GPU & VULKAN CAPABILITIES", color = XeniaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(label = "GPU Vendor / Family", value = hardwareReport.gpuFamily.displayName)
                DetailRow(label = "GPU Renderer", value = hardwareReport.gpuRenderer)
                DetailRow(label = "Vulkan API Version", value = "${hardwareReport.vulkanVersion} (Level ${hardwareReport.vulkanApiLevel})")
                DetailRow(label = "Thermal Status", value = hardwareReport.thermalStatus)
            }
        }

        // Section: Required Extensions Checklist
        Text("VULKAN EXTENSION COMPLIANCE CHECKLIST", color = XeniaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                hardwareReport.vulkanExtensions.forEach { ext ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ext.name, color = TechTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(ext.description, color = TechTextMuted, fontSize = 10.sp)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (ext.isSupported) XeniaGreenContainer else DarkSurfaceContainerHighest)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (ext.isSupported) "PASS" else if (ext.isRequired) "FAIL" else "N/A",
                                color = if (ext.isSupported) OnXeniaGreenContainer else TechTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
