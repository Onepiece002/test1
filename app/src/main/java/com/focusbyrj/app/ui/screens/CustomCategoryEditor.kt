package com.focusbyrj.app.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.focusbyrj.app.ui.theme.MidnightBlack
import com.focusbyrj.app.ui.theme.SurfaceVariantDark
import com.focusbyrj.app.ui.theme.BorderGlass
import com.focusbyrj.app.util.CustomCategory
import com.focusbyrj.app.util.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCategoryEditor(
    context: Context,
    installedApps: List<InstalledApp>,
    editingCategory: CustomCategory?,
    onSave: (String, Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(editingCategory?.name ?: "") }
    var selectedPackages by remember { mutableStateOf(editingCategory?.packages ?: emptySet()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightBlack),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text(
                    text = if (editingCategory != null) "Edit Category" else "New Category",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BorderGlass,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Select Apps",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(installedApps) { app ->
                        val isSelected = selectedPackages.contains(app.packageName)
                        val icon = remember(app.packageName) { ImageUtils.getAppIcon(context.packageManager, app.packageName) }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else SurfaceVariantDark)
                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else BorderGlass, RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedPackages = if (isSelected) {
                                        selectedPackages - app.packageName
                                    } else {
                                        selectedPackages + app.packageName
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            if (icon != null) {
                                Image(
                                    bitmap = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name, selectedPackages)
                            }
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
