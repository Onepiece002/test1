# Fix AppPicker Text nodes
sed -i '846,847c\                        Text("Select Apps", color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))\
                        Spacer(modifier = Modifier.height(4.dp))\
                        Text("Add apps to your custom filter", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

sed -i '902,903c\                                Text("Deselect All", color = Color(0xFFF44336))' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

sed -i '907,908c\                                Text("Select All", color = AccentCyan)' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

sed -i '937,939c\                                    Text(app.appName, color = Color.White, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))\
                                    Text(app.category.title, color = Color.Gray, style = MaterialTheme.typography.labelMedium)' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

sed -i '960,961c\                        Text("Confirm ${selection.size} Apps", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

