sed -i '949,$c\                    Button(\
                        onClick = { onConfirm(selection) },\
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),\
                        shape = RoundedCornerShape(16.dp),\
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = MidnightBlack)\
                    ) {\
                        Text("Confirm ${selection.size} Apps", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))\
                    }\
                }\
            }\
        }\
    }\
}' app/src/main/java/com/focusbyrj/app/ui/screens/AddRestrictionScreen.kt
