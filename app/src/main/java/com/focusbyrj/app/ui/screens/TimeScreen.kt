package com.focusbyrj.app.ui.screens

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import com.focusbyrj.app.ui.theme.AccentCyan
import com.focusbyrj.app.ui.theme.AccentViolet
import com.focusbyrj.app.ui.theme.BorderGlass
import com.focusbyrj.app.ui.theme.MidnightBlack
import com.focusbyrj.app.ui.theme.SurfaceDark
import com.focusbyrj.app.ui.theme.SurfaceVariantDark
import com.focusbyrj.app.util.AppUsageData
import com.focusbyrj.app.util.UsageStatsHelper

@Composable
fun TimeScreen() {
    val context = LocalContext.current
    val hasPermission = remember { UsageStatsHelper.hasUsageStatsPermission(context) }
    var usageStats by remember { mutableStateOf<List<AppUsageData>>(emptyList()) }
    var totalTimeMs by remember { mutableStateOf(0L) }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val stats = UsageStatsHelper.getTodayUsageStats(context).filter { it.timeInForegroundMs > 60_000 }
            usageStats = stats
            totalTimeMs = stats.sumOf { it.timeInForegroundMs }
        }
    }

    if (!hasPermission) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Permission Required", style = MaterialTheme.typography.titleLarge, color = Color(0xFFCBD5E1))
            Spacer(modifier = Modifier.height(16.dp))
            Text("FocusLock needs Usage Access to display your screen time.", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { UsageStatsHelper.requestUsageStatsPermission(context) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = MidnightBlack)
            ) {
                Text("Grant Permission", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    val hours = totalTimeMs / (1000 * 60 * 60)
    val minutes = (totalTimeMs / (1000 * 60)) % 60

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Screen Time",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Today's digital footprint",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderGlass, RoundedCornerShape(28.dp))
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("TOTAL TIME", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(hours.toString(), style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp), color = Color(0xFFCBD5E1))
                        Text("h", style = MaterialTheme.typography.titleLarge, color = AccentCyan, modifier = Modifier.padding(bottom = 10.dp, start = 4.dp, end = 8.dp))
                        Text(minutes.toString(), style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp), color = Color(0xFFCBD5E1))
                        Text("m", style = MaterialTheme.typography.titleLarge, color = AccentCyan, modifier = Modifier.padding(bottom = 10.dp, start = 4.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (usageStats.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            val colors = listOf(Color(0xFF60A5FA), Color(0xFF34D399), Color(0xFFFBBF24), Color(0xFFF87171), Color(0xFFA78BFA), Color(0xFF2DD4BF), Color(0xFFFB923C))
                            usageStats.take(4).forEachIndexed { index, stat ->
                                val weight = (stat.timeInForegroundMs.toFloat() / totalTimeMs.toFloat()).coerceAtLeast(0.01f)
                                Box(modifier = Modifier.weight(weight).fillMaxHeight().background(colors[index % colors.size]))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "App Breakdown",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        val colors = listOf(Color(0xFF60A5FA), Color(0xFF34D399), Color(0xFFFBBF24), Color(0xFFF87171), Color(0xFFA78BFA), Color(0xFF2DD4BF), Color(0xFFFB923C))
        items(usageStats.size) { index ->
            val stat = usageStats[index]
            val percentage = if (totalTimeMs > 0) stat.timeInForegroundMs.toFloat() / totalTimeMs else 0f
            val h = stat.timeInForegroundMs / (1000 * 60 * 60)
            val m = (stat.timeInForegroundMs / (1000 * 60)) % 60
            val timeStr = if (h > 0) "${h}h ${m}m" else "${m}m"
            
            val pm = context.packageManager
            val icon = remember(stat.packageName) { com.focusbyrj.app.util.ImageUtils.getAppIcon(pm, stat.packageName) }
            
            AppUsageItem(
                appName = stat.appName, 
                icon = icon,
                time = timeStr, 
                percentage = percentage, 
                color = colors[index % colors.size]
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun AppUsageItem(appName: String, icon: androidx.compose.ui.graphics.ImageBitmap?, time: String, percentage: Float, color: Color) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.2f))
                    .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                                androidx.compose.foundation.Image(bitmap = icon, contentDescription = null, modifier = Modifier.fillMaxSize().padding(8.dp))
                } else {
                    Text(appName.take(1).uppercase(), color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(appName, style = MaterialTheme.typography.titleMedium, color = Color(0xFFCBD5E1), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(time, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFCBD5E1))
                Text("${(percentage * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
    }
}
