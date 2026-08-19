sed -i '523c\                                        colors = androidx.compose.material3.SliderDefaults.colors()' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt
sed -i '543c\                                        colors = androidx.compose.material3.SliderDefaults.colors()' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt

sed -i '899,910c\                        if (allSelected) {\
                            TextButton(onClick = { selection = selection - filteredPackages }) {\
                                Text("Deselect All", color = Color(0xFFF44336))\
                            }\
                        } else {\
                            TextButton(onClick = { selection = selection + filteredPackages }) {\
                                Text("Select All", color = AccentCyan)\
                            }\
                        }' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt
