package com.focusbyrj.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusbyrj.app.util.DailyQuestManager
import java.util.Calendar

@Composable
fun MysteryBoxChatCard(
    onOpenBox: () -> Unit
) {
    val questState by DailyQuestManager.stateFlow.collectAsState()
    val isAvailable = questState.isEarlyBirdAvailable || questState.isNightOwlAvailable
    val isDaytime = remember {
        val cal = Calendar.getInstance()
        cal.get(Calendar.HOUR_OF_DAY) in 6..17
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF131F24),
        border = BorderStroke(2.dp, if (isAvailable) Color(0xFFFFB300).copy(alpha = 0.6f) else Color(0xFF20333D)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(enabled = isAvailable) { onOpenBox() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDaytime) {
                    EarlyBirdChestGraphic(modifier = Modifier.size(72.dp))
                } else {
                    NightOwlChestGraphic(modifier = Modifier.size(72.dp))
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isAvailable) Color(0xFFFFB300).copy(alpha = 0.2f) else Color(0xFF58CC02).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isAvailable) "DAILY REWARD READY 🎁" else "CLAIMED TODAY ✓",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp
                            ),
                            color = if (isAvailable) Color(0xFFFFB300) else Color(0xFF58CC02),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isAvailable) "Mystery Box Unlocked!" else "Mystery Box Claimed",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        ),
                        color = Color.White
                    )

                    Text(
                        text = if (isAvailable) {
                            "1 test completed! Tap the box to crack it open & reveal your rewards."
                        } else {
                            "You unlocked and claimed today's mystery reward! Next chest available tomorrow."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8B9EAA)
                    )
                }
            }

            if (isAvailable) {
                Duolingo3DButton(
                    text = "OPEN MYSTERY BOX 🎁",
                    buttonColor = Color(0xFFFFB300),
                    bevelColor = Color(0xFFFF8F00),
                    textColor = Color(0xFF3E2723),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenBox
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF58CC02),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rewards Added To Wallet & XP",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF58CC02)
                    )
                }
            }
        }
    }
}
