sed -i 's/import androidx.compose.foundation.border/import androidx.compose.foundation.border\nimport androidx.compose.foundation.Image/g' app/src/main/java/com/focusbyrj/app/ui/components/PermissionsDialog.kt

# Replace the Box layout containing the Icon with just the Image
sed -i '/Box(/,/^                }/c\                Image(\n                    painter = painterResource(id = R.mipmap.ic_launcher),\n                    contentDescription = "App Logo",\n                    modifier = Modifier.size(80.dp).clip(CircleShape)\n                )' app/src/main/java/com/focusbyrj/app/ui/components/PermissionsDialog.kt
