package com.focusbyrj.app.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.navigation.NavController
import com.focusbyrj.app.service.FocusDeviceAdminReceiver
import com.focusbyrj.app.ui.theme.*

@Composable
fun SecurityScreen(navController: NavController) {
    val context = LocalContext.current
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val prefs = remember { context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE) }
    var secureRecents by remember { mutableStateOf(prefs.getBoolean("secure_recents", true)) }
    val adminComponent = ComponentName(context, FocusDeviceAdminReceiver::class.java)
    var isUninstallProtectionEnabled by remember { mutableStateOf(dpm.isAdminActive(adminComponent)) }
    
    val adminLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isUninstallProtectionEnabled = dpm.isAdminActive(adminComponent)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isUninstallProtectionEnabled = dpm.isAdminActive(adminComponent)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Security",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "APP INTEGRITY & TAMPER DEFENSE",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            SettingsCard(
                title = "Uninstall Protection",
                subtitle = "Uses Device Administrator to prevent accidental app deletion during deep focus sessions.",
                icon = Icons.Filled.Security,
                iconColor = Color(0xFFFFB74D) 
            ) {
                Button(
                    onClick = {
                        if (isUninstallProtectionEnabled) {
                            dpm.removeActiveAdmin(adminComponent)
                            isUninstallProtectionEnabled = false
                        } else {
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Prevent accidental app deletion during deep focus sessions.")
                            }
                            adminLauncher.launch(intent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
                ) {
                    Text(if (isUninstallProtectionEnabled) "Disable" else "Enable", color = Color(0xFFCBD5E1))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsCard(
                title = "Anti-Screenshot Protection",
                subtitle = "Prevents taking screenshots and hides app content in the recent apps menu.",
                icon = Icons.Filled.VisibilityOff,
                iconColor = AccentCyan
            ) {
                Switch(
                    checked = secureRecents,
                    onCheckedChange = { checked ->
                        secureRecents = checked
                        prefs.edit().putBoolean("secure_recents", checked).apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentEmerald,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = SurfaceVariantDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}
@Composable
fun SettingsCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color, trailing: @Composable () -> Unit) {
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
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color(0xFFCBD5E1))
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray, lineHeight = 16.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}
