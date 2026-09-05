package com.focusbyrj.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.focusbyrj.app.util.DailyQuestManager
import com.focusbyrj.app.util.FocusEconomyManager
import com.focusbyrj.app.util.GamificationHaptics
import org.json.JSONObject

@Composable
fun FullscreenDrillView(
    activeSession: DrillSession,
    latestQuestionMessage: ChatMessage,
    allQuestions: List<ChatMessage> = emptyList(),
    onNextQuestion: (() -> Unit)? = null,
    onAnswerSubmitted: (Boolean, QuestionRecord) -> Unit,
    onEndSession: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val parsedQuestions = remember(activeSession.preGeneratedQuestions, allQuestions) {
        if (activeSession.preGeneratedQuestions.isNotEmpty()) {
            activeSession.preGeneratedQuestions.mapIndexed { idx, json ->
                ChatMessage(
                    id = "pre_$idx",
                    text = "Arithmetic Drill",
                    isUser = false,
                    isArithmetic = true,
                    arithmeticJson = json
                )
            }
        } else {
            if (allQuestions.isNotEmpty()) allQuestions else listOf(latestQuestionMessage)
        }
    }

    val questionsList = parsedQuestions
    var questionIndex by remember(questionsList.size) { mutableIntStateOf(activeSession.highestSeenIndex.coerceIn(0, (questionsList.size - 1).coerceAtLeast(0))) }

    LaunchedEffect(questionIndex) {
        if (questionIndex > activeSession.highestSeenIndex) {
            activeSession.highestSeenIndex = questionIndex
        }
    }

    val currentQuestionMessage = questionsList.getOrElse(questionIndex) { latestQuestionMessage }

    // Parse current question
    val json = currentQuestionMessage.arithmeticJson
    var title by remember(currentQuestionMessage.id) { mutableStateOf("Mental Arithmetic") }
    var direction by remember(currentQuestionMessage.id) { mutableStateOf("") }
    var questionText by remember(currentQuestionMessage.id) { mutableStateOf("") }
    var options by remember(currentQuestionMessage.id) { mutableStateOf<List<String>>(emptyList()) }
    var correctIndex by remember(currentQuestionMessage.id) { mutableStateOf(0) }
    var explanation by remember(currentQuestionMessage.id) { mutableStateOf("") }
    var isBookmarked by remember(currentQuestionMessage.id) { mutableStateOf(false) }
    var timeOnQuestionSec by remember(currentQuestionMessage.id) { mutableIntStateOf(0) }
    var showGridDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentQuestionMessage.id, json) {
        if (json != null) {
            try {
                val obj = JSONObject(json)
                title = obj.optString("title", "Mental Arithmetic")
                direction = obj.optString("direction", "Read the arithmetic problem carefully and select the single correct option based on fundamental principles.")
                questionText = obj.optString("questionText", "")
                val arr = obj.optJSONArray("options")
                val list = mutableListOf<String>()
                if (arr != null) {
                    for (i in 0 until arr.length()) list.add(arr.getString(i))
                }
                options = list
                correctIndex = obj.optInt("correctIndex", 0)
                explanation = obj.optString("explanation", "")
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
    }

    LaunchedEffect(currentQuestionMessage.id) {
        timeOnQuestionSec = 0
        while (true) {
            kotlinx.coroutines.delay(1000)
            timeOnQuestionSec++
        }
    }

    // Local state for question selection
    var selectedIndex by remember(currentQuestionMessage.id) { 
        mutableStateOf<Int?>(activeSession.questionRecords.find { it.questionNumber == questionIndex + 1 }?.userSelectedIndex) 
    }

    // Duolingo Brand Theme Colors
    val duolingoGreen = Color(0xFF58CC02)
    val duolingoGreenBevel = Color(0xFF46A302)
    val duolingoGreenBg = Color(0xFF58CC02).copy(alpha = 0.14f)

    val duolingoRed = Color(0xFFFF4B4B)
    val duolingoRedBevel = Color(0xFFD11919)
    val duolingoRedBg = Color(0xFFFF4B4B).copy(alpha = 0.14f)

    val duolingoYellow = Color(0xFFFFC800)
    val duolingoYellowBevel = Color(0xFFD8A800)

    val duolingoBlue = Color(0xFF1CB0F6)
    val duolingoBlueBevel = Color(0xFF1899D6)

    val duolingoOrange = Color(0xFFFF9600)
    val duolingoOrangeBevel = Color(0xFFD47800)

    val duolingoPurple = Color(0xFFCE82FF)
    val duolingoPurpleBevel = Color(0xFFA855F7)

    // Canvas background
    val darkCanvasBg = Color(0xFF0C1418) // Deep Duolingo dark slate
    val cardDarkBg = Color(0xFF131F24)

    // Gentle pulse animation for high combos
    val infiniteTransition = rememberInfiniteTransition(label = "combo_pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (activeSession.combo >= 3) 1.04f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (activeSession.combo >= 5) 450 else 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "combo_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) darkCanvasBg else Color(0xFFF7F9FA))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            // -------------------------------------------------------------
            // TOP HUD: Close Button, Glossy Progress Bar & Blitz Timer / Q Count
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exit Button
                IconButton(
                    onClick = onEndSession,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Exit Drill",
                        tint = if (isDark) Color(0xFF839EAB) else Color(0xFF64748B),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Duolingo Glossy Progress Bar (Fills up gradually as session progresses)
                val progressFraction = if (activeSession.isBlitz) {
                    val totalTime = 300f
                    val elapsedSeconds = (300 - activeSession.blitzSecondsRemaining).coerceAtLeast(0)
                    (elapsedSeconds.toFloat() / totalTime).coerceIn(0.03f, 1f)
                } else {
                    val target = if (activeSession.targetQuestions > 0) activeSession.targetQuestions else 10
                    (activeSession.total.toFloat() / target.toFloat()).coerceIn(0.03f, 1f)
                }

                val animatedProgress by animateFloatAsState(
                    targetValue = progressFraction,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    label = "drill_progress"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (isDark) Color(0xFF20343D) else Color(0xFFE5E5E5))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (activeSession.isBlitz) {
                                    if (activeSession.blitzSecondsRemaining <= 30) duolingoRed
                                    else duolingoYellow
                                } else {
                                    duolingoGreen
                                }
                            )
                    ) {
                        // Specular gloss shine on top half
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.5.dp)
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.35f))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right HUD: Blitz Timer or Question Number Pill
                if (activeSession.isBlitz) {
                    val mins = activeSession.blitzSecondsRemaining / 60
                    val secs = activeSession.blitzSecondsRemaining % 60
                    val timerText = if (mins > 0) String.format("%d:%02d", mins, secs) else "${secs}s"
                    val isUrgent = activeSession.blitzSecondsRemaining <= 30

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isUrgent) duolingoRed.copy(alpha = 0.15f) else duolingoYellow.copy(alpha = 0.15f),
                        border = BorderStroke(2.dp, if (isUrgent) duolingoRed else duolingoYellow)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Filled.ElectricBolt,
                                contentDescription = null,
                                tint = if (isUrgent) duolingoRed else duolingoYellow,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timerText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
                                color = if (isUrgent) duolingoRed else duolingoYellow
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) cardDarkBg else Color(0xFFF1F5F9),
                        border = BorderStroke(2.dp, if (isDark) Color(0xFF20343D) else Color(0xFFCBD5E1))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (activeSession.targetQuestions > 0) "${questionIndex + 1}/${activeSession.targetQuestions}" else "Q${questionIndex + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 12.5.sp),
                                color = if (isDark) Color(0xFF839EAB) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // TOP IN-LINE ROW (In line with Chat Bubble Icon to the right)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeSession.targetQuestions > 0) {
                    Button(
                        onClick = { showGridDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF2B3A4A) else Color(0xFFE2E8F0),
                            contentColor = if (isDark) Color(0xFFA0AEC0) else Color(0xFF475569)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Grid View",
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                        Text(
                            text = "Overview",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        )
                    }
                } else {
                    Button(
                        onClick = onEndSession,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF5350),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "End Session",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // DUOLINGO 3D COMBO STREAK CAPSULE
            // -------------------------------------------------------------
            AnimatedVisibility(
                visible = activeSession.combo > 0,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -10 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -10 })
            ) {
                val combo = activeSession.combo
                val (comboLabel, boostLabel, streakColor, streakBevelColor, streakIcon) = when {
                    combo >= 8 -> Quintuple("GODLIKE STREAK", "2.0x XP ACTIVE", duolingoPurple, duolingoPurpleBevel, "⚡")
                    combo >= 5 -> Quintuple("ON FIRE (x$combo)", "1.5x XP ACTIVE", duolingoRed, duolingoRedBevel, "🔥")
                    combo >= 3 -> Quintuple("STREAK x$combo", "+50 XP BONUS", duolingoOrange, duolingoOrangeBevel, "🔥")
                    combo == 2 -> Quintuple("STREAK x$combo", "+40 XP", duolingoYellow, duolingoYellowBevel, "⚡")
                    else -> Quintuple("STREAK x1", "+40 XP", duolingoBlue, duolingoBlueBevel, "🎯")
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .wrapContentWidth()
                            .scale(if (combo >= 3) pulseScale else 1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(19.dp),
                        color = streakBevelColor // 3D Bottom shadow bevel
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 3.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(if (isDark) cardDarkBg else Color.White)
                                .border(BorderStroke(2.dp, streakColor), RoundedCornerShape(17.dp))
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = streakIcon,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = comboLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = streakColor
                                )

                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(3.5.dp)
                                        .clip(CircleShape)
                                        .background(streakColor.copy(alpha = 0.7f))
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = if (activeSession.isBlitz) "$boostLabel • +30s" else boostLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.5.sp
                                    ),
                                    color = streakColor
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // MAIN QUESTION & OPTIONS AREA (Solution Testbook Format)
            // -------------------------------------------------------------
            val activeBlue = Color(0xFF2196F3)
            val correctGreen = Color(0xFF00C853)
            val wrongRed = Color(0xFFEF5350)
            val cardBackground = if (isDark) Color(0xFF22262C) else Color(0xFFF1F5F9)
            val cardStrokeColor = if (isDark) Color(0xFF2E333B) else Color(0xFFE2E8F0)
            val textPrimary = if (isDark) Color(0xFFF1F3F5) else Color(0xFF0F172A)
            val textSecondary = if (isDark) Color(0xFF9EA7B4) else Color(0xFF64748B)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 70.dp, bottom = 12.dp)
            ) {
                // Top Question Meta Row: Number Badge Circle + Timer + Marks (+1.0 -0.25) + Warning + Bookmark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Question Number Circle Badge
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(correctGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${questionIndex + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Timer + Time formatted e.g. "28sec"
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${timeOnQuestionSec}sec",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = textSecondary
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Marks: +1.0  -0.25
                    Text(
                        text = "+1.0",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = correctGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "-0.25",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                        color = textSecondary
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Report Warning Icon
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Report",
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Bookmark Icon
                    IconButton(
                        onClick = { isBookmarked = !isBookmarked },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) activeBlue else textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Direction line
                if (direction.isNotEmpty()) {
                    Text(
                        text = "Direction: $direction",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp
                        ),
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Question Prompt
                Text(
                    text = questionText.ifBlank { "..." },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Normal,
                        lineHeight = 24.sp,
                        fontSize = 17.sp
                    ),
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Options List (1. 2. 3. 4. 5.)
                options.forEachIndexed { optIndex, optText ->
                    val isCorrectOption = optIndex == correctIndex
                    val isUserChosen = optIndex == selectedIndex

                    val (optBorderColor, optBgColor, optNumberColor) = when {
                        selectedIndex != null && isCorrectOption -> Triple(correctGreen, correctGreen.copy(alpha = 0.12f), correctGreen)
                        selectedIndex != null && isUserChosen && !isCorrectOption -> Triple(wrongRed, wrongRed.copy(alpha = 0.12f), wrongRed)
                        else -> Triple(Color.Transparent, cardBackground, textSecondary)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable(enabled = selectedIndex == null) {
                                selectedIndex = optIndex
                                val isCorrectAnswer = (optIndex == correctIndex)

                                if (isCorrectAnswer) {
                                    DailyQuestManager.recordCorrectAnswer()
                                    val nextCombo = activeSession.combo + 1
                                    DailyQuestManager.recordCombo(nextCombo)
                                    GamificationHaptics.playCombo(context, nextCombo)
                                } else {
                                    GamificationHaptics.playWrong(context)
                                }

                                val qRecord = QuestionRecord(
                                    questionNumber = questionIndex + 1,
                                    title = title,
                                    questionText = questionText,
                                    options = options,
                                    correctIndex = correctIndex,
                                    userSelectedIndex = optIndex,
                                    status = if (isCorrectAnswer) "correct" else "wrong",
                                    explanation = explanation
                                )
                                activeSession.attemptedIndices.add(questionIndex)
                                onAnswerSubmitted(isCorrectAnswer, qRecord)
                                
                                if (activeSession.targetQuestions > 0) {
                                    coroutineScope.launch {
                                        delay(700)
                                        if (questionIndex < questionsList.size - 1) {
                                            // Auto advance to next question
                                            // Only if the next question is not attempted
                                            if (!activeSession.attemptedIndices.contains(questionIndex + 1)) {
                                                questionIndex++
                                            }
                                        } else {
                                            // Reached end of test, show grid
                                            showGridDialog = true
                                        }
                                    }
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = optBgColor,
                        border = if (optBorderColor != Color.Transparent) BorderStroke(1.5.dp, optBorderColor) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${optIndex + 1}.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = optNumberColor
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = optText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = textPrimary,
                                modifier = Modifier.weight(1f)
                            )

                            if (selectedIndex != null) {
                                if (isCorrectOption) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Correct",
                                        tint = correctGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else if (isUserChosen) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Incorrect",
                                        tint = wrongRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Solution & Explanation Card on answer submission
                AnimatedVisibility(
                    visible = selectedIndex != null && explanation.isNotBlank()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = cardBackground,
                        border = BorderStroke(1.dp, cardStrokeColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = activeBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Explanation & Method",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = textPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = cardStrokeColor)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 22.sp,
                                    fontSize = 14.sp
                                ),
                                color = textPrimary.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // BOTTOM BAR: Prev & Next Navigation Buttons
            // -------------------------------------------------------------
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = cardBackground,
                border = BorderStroke(1.dp, cardStrokeColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (questionIndex > 0) {
                                questionIndex--
                            }
                        },
                        enabled = questionIndex > 0,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (questionIndex > 0) cardStrokeColor else cardStrokeColor.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (questionIndex > 0) textPrimary else textSecondary.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Prev",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Button(
                        onClick = {
                            if (questionIndex < questionsList.size - 1) {
                                questionIndex++
                            } else {
                                onNextQuestion?.invoke()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text(
                            text = "Next",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        GridNavigationDialog(
            showDialog = showGridDialog,
            onDismiss = { showGridDialog = false },
            totalQuestions = if (activeSession.targetQuestions > 0) activeSession.targetQuestions else questionsList.size,
            highestSeenIndex = activeSession.highestSeenIndex,
            attemptedIndices = activeSession.attemptedIndices,
            onQuestionSelected = { idx ->
                questionIndex = idx
            },
            onSubmitClick = onEndSession,
            isDark = isDark
        )
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GridNavigationDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    totalQuestions: Int,
    highestSeenIndex: Int,
    attemptedIndices: Set<Int>,
    onQuestionSelected: (Int) -> Unit,
    onSubmitClick: () -> Unit,
    isDark: Boolean
) {
    if (!showDialog) return

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1B1E23) else Color.White,
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Grid View",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) Color.White else Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem("Attempted", Color(0xFF3B82F6), true, isDark)
                LegendItem("Unattempted", Color(0xFF64748B), true, isDark)
                LegendItem("Unseen", Color(0xFF64748B), false, isDark)
            }

            Text(
                text = "Numerical Ability",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) Color(0xFFA0AEC0) else Color(0xFF64748B),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(48.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(totalQuestions) { index ->
                    val isAttempted = attemptedIndices.contains(index)
                    val isUnseen = index > highestSeenIndex
                    val isUnattempted = !isAttempted && !isUnseen

                    val bgColor = when {
                        isAttempted -> Color(0xFF3B82F6)
                        isUnattempted -> if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                        else -> Color.Transparent
                    }
                    val strokeColor = when {
                        isAttempted -> Color.Transparent
                        isUnattempted -> Color.Transparent
                        else -> if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                    }
                    val textColor = when {
                        isAttempted -> Color.White
                        isUnattempted -> if (isDark) Color.White else Color.Black
                        else -> if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .border(1.dp, strokeColor, CircleShape)
                            .clickable {
                                onQuestionSelected(index)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = textColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { 
                    onDismiss()
                    onSubmitClick() 
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "SUBMIT TEST",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color, filled: Boolean, isDark: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (filled) color else Color.Transparent)
                .border(1.dp, if (!filled) color else Color.Transparent, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
        )
    }
}
