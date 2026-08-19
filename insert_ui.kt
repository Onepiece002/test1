                        Text(
                            text = "CUSTOM RESTRICTIONS",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentCyan,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModeSelector(
                                title = "Simple Restriction",
                                description = "Always block (Default)",
                                isSelected = restrictionMode == "SIMPLE",
                                onClick = { restrictionMode = "SIMPLE" },
                                modifier = Modifier.fillMaxWidth()
                            )
                            ModeSelector(
                                title = "Time Limit",
                                description = "Restrict app by usage time",
                                isSelected = restrictionMode == "TIME_LIMIT",
                                onClick = { restrictionMode = "TIME_LIMIT" },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (restrictionMode == "TIME_LIMIT") {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text("Allowed Time: $timeLimitMinutes mins", color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    androidx.compose.material3.Slider(
                                        value = timeLimitMinutes.toFloat(),
                                        onValueChange = { timeLimitMinutes = it.toInt() },
                                        valueRange = 1f..120f,
                                        modifier = Modifier.weight(2f),
                                        colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan)
                                    )
                                }
                            }
                            ModeSelector(
                                title = "Click Bypass",
                                description = "Unlock with manual clicks",
                                isSelected = restrictionMode == "CLICK_LIMIT",
                                onClick = { restrictionMode = "CLICK_LIMIT" },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (restrictionMode == "CLICK_LIMIT") {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text("Clicks Required: $clickLimitCount", color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    androidx.compose.material3.Slider(
                                        value = clickLimitCount.toFloat(),
                                        onValueChange = { clickLimitCount = it.toInt() },
                                        valueRange = 10f..100f,
                                        steps = 9,
                                        modifier = Modifier.weight(2f),
                                        colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))

