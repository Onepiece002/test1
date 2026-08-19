sed -i '738,739c\                        Text("Selected Apps", color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))\
                        Text("${packages.size} selected", color = AccentCyan, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

sed -i '762c\                                Text("Add App", color = Color.Gray, style = MaterialTheme.typography.labelSmall)' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

sed -i '794c\                                    Text(app.appName, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelSmall)' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

sed -i '803c\                            Text("Cancel", color = Color.Gray)' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

sed -i '812c\                            Text("Save Filter", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt
