sed -i '187,188c\                            Text("Enable Shield", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))\
                        }' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

sed -i '219,220c\                        Text("Enable Shield for ${selectedApps.size} Apps", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))\
                    }' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

sed -i '362,363c\                                            Text("Deselect All", color = Color(0xFFF44336))\
                                        }' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

sed -i '368,369c\                                            Text("Select All", color = AccentCyan)\
                                        }' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

sed -i '849d' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

sed -i '900,902c\                                onClick = { selection = selection - filteredPackages }\
                            ) {\
                                Text("Deselect All", color = Color(0xFFF44336))' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

sed -i '906,908c\                                onClick = { selection = selection + filteredPackages }\
                            ) {\
                                Text("Select All", color = AccentCyan)' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

sed -i '959,960c\                        Text("Confirm ${selection.size} Apps", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

