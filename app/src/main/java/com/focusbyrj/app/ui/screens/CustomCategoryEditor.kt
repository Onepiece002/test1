package com.focusbyrj.app.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.focusbyrj.app.ui.theme.MidnightBlack
import com.focusbyrj.app.ui.theme.SurfaceVariantDark
import com.focusbyrj.app.ui.theme.SurfaceDark
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
    var selectedCategory by remember { mutableStateOf<AppCategory>(AppCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(installedApps, selectedCategory, searchQuery) {
        var list = if (selectedCategory == AppCategory.ALL) {
            installedApps
        } else {
            installedApps.filter { it.category == selectedCategory }
        }
        if (searchQuery.isNotBlank()) {
            list = list.filter { it.appName.contains(searchQuery, ignoreCase = true) }
        }
        list
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (editingCategory != null) "Edit Filter" else "New Filter",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MidnightBlack,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = MidnightBlack,
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MidnightBlack)
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name, selectedPackages)
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Save Filter (${selectedPackages.size} Apps)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Filter Name (e.g. Work Apps)", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BorderGlass,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BorderGlass,
                        unfocusedBorderColor = BorderGlass,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                // Category Tabs
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AppCategory.entries.toTypedArray()) { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else SurfaceVariantDark)
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category.title,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // App List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filteredApps.size} apps available",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )

                            val allPackages = filteredApps.map { it.packageName }.toSet()
                            val allSelected = filteredApps.isNotEmpty() && selectedPackages.containsAll(allPackages)
                            
                            if (allSelected) {
                                TextButton(
                                    onClick = { selectedPackages = selectedPackages - allPackages }
                                ) {
                                    Text("Deselect All", color = Color(0xFFF44336))
                                }
                            } else {
                                TextButton(
                                    onClick = { selectedPackages = selectedPackages + allPackages }
                                ) {
                                    Text("Select All", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = selectedPackages.contains(app.packageName)
                        val icon = remember(app.packageName) { ImageUtils.getAppIcon(context.packageManager, app.packageName) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else SurfaceDark)
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else BorderGlass,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    selectedPackages = if (isSelected) {
                                        selectedPackages - app.packageName
                                    } else {
                                        selectedPackages + app.packageName
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (icon != null) {
                                    Image(
                                        bitmap = icon,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = app.category.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.Gray
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .border(2.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}
