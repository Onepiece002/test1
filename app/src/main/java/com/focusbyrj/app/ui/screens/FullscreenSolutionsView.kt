package com.focusbyrj.app.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import kotlin.math.roundToInt

data class SolutionQuestionItem(
    val qNum: Int,
    val title: String,
    val direction: String = "",
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val userSelectedIndex: Int, // -1 if not attempted
    val status: String, // "correct", "wrong", "unattempted"
    val explanation: String,
    val timeTakenSec: Int = 24,
    val accuracyPct: Int = 80,
    val positiveMarks: Double = 1.0,
    val negativeMarks: Double = 0.25
)

enum class FilterTab {
    ALL, INCORRECT, CORRECT, OVERTIME, UNATTEMPTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenSolutionsView(
    summaryJson: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        context.sendBroadcast(android.content.Intent("com.focusbyrj.app.HIDE_BUBBLE"))
        onDispose {
            context.sendBroadcast(android.content.Intent("com.focusbyrj.app.SHOW_BUBBLE"))
        }
    }

    // Palette strictly matching the reference Testbook / Exam dark interface
    val darkBackground = Color(0xFF131518)
    val darkSurface = Color(0xFF1B1E23)
    val cardBackground = Color(0xFF22262C)
    val cardStrokeColor = Color(0xFF2E333B)
    val activeBlue = Color(0xFF2196F3)
    val correctGreen = Color(0xFF00C853)
    val wrongRed = Color(0xFFEF5350)
    val unattemptedGrey = Color(0xFF5A626E)
    val textPrimary = Color(0xFFF1F3F5)
    val textSecondary = Color(0xFF9EA7B4)
    val timerRed = Color(0xFFFF5252)

    var examTitle by remember { mutableStateOf("Arithmetic & Speed Drill") }
    val questionsList = remember(summaryJson) { mutableStateListOf<SolutionQuestionItem>() }
    val bookmarkedQuestions = remember { mutableStateMapOf<Int, Boolean>() }

    // Screen State
    var showFilterOverview by remember { mutableStateOf(false) }
    var selectedFilterTab by remember { mutableStateOf(FilterTab.ALL) }
    var selectedQuestionIndex by remember { mutableStateOf(0) }
    var isReattemptMode by remember { mutableStateOf(false) }
    val reattemptUserAnswers = remember { mutableStateMapOf<Int, Int>() } // qNum -> selectedOptIndex

    LaunchedEffect(summaryJson) {
        questionsList.clear()
        try {
            val obj = JSONObject(summaryJson)
            val isBlitz = obj.optBoolean("isBlitz", false)
            val categoryTitle = obj.optString("title", "Arithmetic & Speed Drill")
            examTitle = if (isBlitz) "⚡ Speed Blitz Review" else "$categoryTitle Solutions"

            val totalQuestions = obj.optInt("total", 0)
            val correctQuestions = obj.optInt("correct", 0)
            val elapsedSeconds = obj.optInt("elapsedSeconds", 60)

            val qArr = obj.optJSONArray("questions")
            if (qArr != null && qArr.length() > 0) {
                val count = qArr.length()
                val avgSec = (elapsedSeconds / count.coerceAtLeast(1)).coerceAtLeast(5)
                for (i in 0 until count) {
                    val qObj = qArr.getJSONObject(i)
                    val optsArr = qObj.optJSONArray("options")
                    val opts = mutableListOf<String>()
                    if (optsArr != null) {
                        for (j in 0 until optsArr.length()) {
                            opts.add(optsArr.getString(j))
                        }
                    }
                    val status = qObj.optString("status", "unattempted")
                    val estimatedSec = when (i % 3) {
                        0 -> (avgSec * 1.4).toInt()
                        1 -> (avgSec * 0.7).toInt().coerceAtLeast(4)
                        else -> avgSec
                    }
                    val accuracy = when (status) {
                        "correct" -> 75 + (i * 3) % 20
                        "wrong" -> 45 + (i * 7) % 35
                        else -> 50 + (i * 5) % 30
                    }.coerceIn(35, 95)

                    val item = SolutionQuestionItem(
                        qNum = qObj.optInt("qNum", i + 1),
                        title = qObj.optString("title", "Arithmetic Drill"),
                        direction = if (i == 0) "Read the arithmetic problem carefully and select the single correct option based on fundamental principles." else "",
                        questionText = qObj.optString("questionText", ""),
                        options = opts,
                        correctIndex = qObj.optInt("correctIndex", 0),
                        userSelectedIndex = qObj.optInt("userSelectedIndex", -1),
                        status = status,
                        explanation = qObj.optString("explanation", "Step-by-step mathematical breakdown and shortcut analysis."),
                        timeTakenSec = estimatedSec,
                        accuracyPct = accuracy,
                        positiveMarks = 1.0,
                        negativeMarks = 0.25
                    )
                    questionsList.add(item)
                }
            } else {
                // Fallback demo questions if array was empty
                val totalCount = if (totalQuestions > 0) totalQuestions else 5
                for (i in 1..totalCount) {
                    val isCorr = i <= correctQuestions
                    questionsList.add(
                        SolutionQuestionItem(
                            qNum = i,
                            title = "Arithmetic Drill",
                            direction = if (i == 1) "Solve following problem using optimal arithmetic formulas." else "",
                            questionText = "Sample Problem $i: Find the evaluated result of the required calculation.",
                            options = listOf("None", "Two", "One", "Three", "More than three"),
                            correctIndex = 0,
                            userSelectedIndex = if (isCorr) 0 else 1,
                            status = if (isCorr) "correct" else "wrong",
                            explanation = "Step 1: Expand terms.\nStep 2: Simplify equation.\nHence, the correct option is Option 1.",
                            timeTakenSec = 15 + (i * 8),
                            accuracyPct = 78,
                            positiveMarks = 1.0,
                            negativeMarks = 0.25
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }

    val totalCount = questionsList.size.coerceAtLeast(1)
    val correctCount = questionsList.count { it.status == "correct" }
    val incorrectCount = questionsList.count { it.status == "wrong" }
    val unattemptedCount = questionsList.count { it.status == "unattempted" }
    val overtimeCount = questionsList.count { it.timeTakenSec > 35 }

    val currentQuestion = questionsList.getOrNull(selectedQuestionIndex) ?: questionsList.firstOrNull()
    val scrollState = rememberScrollState()

    // Question number strip list state
    val stripListState = rememberLazyListState()
    LaunchedEffect(selectedQuestionIndex) {
        if (selectedQuestionIndex in 0 until questionsList.size) {
            stripListState.animateScrollToItem(
                (selectedQuestionIndex - 2).coerceAtLeast(0)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (showFilterOverview) {
            // =========================================================================
            // SCREEN 2: ALL QUESTIONS / FILTER OVERVIEW LIST (Exact match to Screenshot 2)
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(darkBackground)
            ) {
                // Top App Bar for Question Overview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(darkSurface)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showFilterOverview = false }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Question Overview",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = textPrimary
                        )
                        Text(
                            text = "$totalCount Questions Total",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = textSecondary
                        )
                    }
                }

                HorizontalDivider(color = cardStrokeColor, thickness = 1.dp)

                // Top Filter Tabs (Horizontal Pills)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterTabPill(
                        text = "All ($totalCount)",
                        isSelected = selectedFilterTab == FilterTab.ALL,
                        activeColor = activeBlue,
                        onClick = { selectedFilterTab = FilterTab.ALL }
                    )
                    FilterTabPill(
                        text = "Incorrect ($incorrectCount)",
                        isSelected = selectedFilterTab == FilterTab.INCORRECT,
                        activeColor = activeBlue,
                        onClick = { selectedFilterTab = FilterTab.INCORRECT }
                    )
                    FilterTabPill(
                        text = "Overtime ($overtimeCount)",
                        isSelected = selectedFilterTab == FilterTab.OVERTIME,
                        activeColor = activeBlue,
                        onClick = { selectedFilterTab = FilterTab.OVERTIME }
                    )
                    FilterTabPill(
                        text = "Unattempted ($unattemptedCount)",
                        isSelected = selectedFilterTab == FilterTab.UNATTEMPTED,
                        activeColor = activeBlue,
                        onClick = { selectedFilterTab = FilterTab.UNATTEMPTED }
                    )
                    FilterTabPill(
                        text = "Correct ($correctCount)",
                        isSelected = selectedFilterTab == FilterTab.CORRECT,
                        activeColor = activeBlue,
                        onClick = { selectedFilterTab = FilterTab.CORRECT }
                    )
                }

                // Section Title & Question Count
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "QUANTITATIVE & REASONING",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontSize = 13.sp
                        ),
                        color = textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filtered List of Question Cards
                val filteredQuestions = remember(selectedFilterTab, questionsList) {
                    when (selectedFilterTab) {
                        FilterTab.ALL -> questionsList.mapIndexed { idx, item -> Pair(idx, item) }
                        FilterTab.INCORRECT -> questionsList.mapIndexed { idx, item -> Pair(idx, item) }.filter { it.second.status == "wrong" }
                        FilterTab.CORRECT -> questionsList.mapIndexed { idx, item -> Pair(idx, item) }.filter { it.second.status == "correct" }
                        FilterTab.OVERTIME -> questionsList.mapIndexed { idx, item -> Pair(idx, item) }.filter { it.second.timeTakenSec > 35 }
                        FilterTab.UNATTEMPTED -> questionsList.mapIndexed { idx, item -> Pair(idx, item) }.filter { it.second.status == "unattempted" }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    itemsIndexed(filteredQuestions) { _, (originalIndex, qItem) ->
                        val isBookmarked = bookmarkedQuestions[qItem.qNum] == true
                        val statusColor = when (qItem.status) {
                            "correct" -> correctGreen
                            "wrong" -> wrongRed
                            else -> unattemptedGrey
                        }
                        val timeStr = formatSecondsToMmSs(qItem.timeTakenSec)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedQuestionIndex = originalIndex
                                    showFilterOverview = false
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = cardBackground,
                            border = BorderStroke(1.dp, cardStrokeColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                // Top row: Number circle + Timer + Bookmark
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Number Badge Circle
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(statusColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${qItem.qNum}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Stopwatch Icon & Time
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = if (qItem.status == "wrong" || qItem.timeTakenSec > 35) timerRed else textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = if (qItem.status == "wrong" || qItem.timeTakenSec > 35) timerRed else textSecondary
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                     // Bookmark Icon
                                    IconButton(
                                        onClick = {
                                            if (isBookmarked) bookmarkedQuestions.remove(qItem.qNum)
                                            else bookmarkedQuestions[qItem.qNum] = true
                                        },
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

                                Spacer(modifier = Modifier.height(10.dp))

                                // Question snippet
                                Text(
                                    text = qItem.questionText.ifEmpty { "Problem #${qItem.qNum}" },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = 20.sp
                                    ),
                                    color = textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Floating Pill: "Back to Question View"
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                Surface(
                    onClick = { showFilterOverview = false },
                    shape = RoundedCornerShape(28.dp),
                    color = activeBlue,
                    shadowElevation = 8.dp,
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Back to Questions",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            // =========================================================================
            // SCREEN 1: DETAILED SOLUTION QUESTION VIEW (Exact match to Screenshot 1)
            // =========================================================================
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. TOP APP BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(darkSurface)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = examTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showFilterOverview = true }
                        ) {
                            Text(
                                text = "All Sections",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = activeBlue
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = activeBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Right icons: Language/Translate + Menu
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF2C323B),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Language",
                                tint = textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("E/अ", color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = { showFilterOverview = true }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = textPrimary
                        )
                    }
                }

                // 2. QUESTION NUMBER STRIP (Horizontal Scrollable + Filters Pill pinned at right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(darkSurface)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        state = stripListState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(questionsList) { index, item ->
                            val isSelected = index == selectedQuestionIndex
                            val statusColor = when (item.status) {
                                "correct" -> correctGreen
                                "wrong" -> wrongRed
                                else -> unattemptedGrey
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.dp, Color.White, CircleShape)
                                        } else Modifier
                                    )
                                    .clickable {
                                        selectedQuestionIndex = index
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${item.qNum}",
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Pinned "Filters" Button on the right
                    Surface(
                        onClick = { showFilterOverview = true },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF262C34),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filters",
                                tint = textPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Filters",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = cardStrokeColor, thickness = 1.dp)

                // 3. MAIN SCROLLABLE QUESTION BODY
                if (currentQuestion != null) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        val isBookmarked = bookmarkedQuestions[currentQuestion.qNum] == true
                        val statusColor = when (currentQuestion.status) {
                            "correct" -> correctGreen
                            "wrong" -> wrongRed
                            else -> unattemptedGrey
                        }

                        // Question Header Row: Number Badge + Time Taken + Marks + Warning/Bookmark
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Badge
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(statusColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${currentQuestion.qNum}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Timer + Time formatted
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (currentQuestion.status == "wrong" || currentQuestion.timeTakenSec > 35) timerRed else textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatSecondsToMinutesSec(currentQuestion.timeTakenSec),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = if (currentQuestion.status == "wrong" || currentQuestion.timeTakenSec > 35) timerRed else textSecondary
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            // Marks: +1.0  -0.25
                            Text(
                                text = "+${currentQuestion.positiveMarks}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = correctGreen
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "-${currentQuestion.negativeMarks}",
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
                                onClick = {
                                    if (isBookmarked) bookmarkedQuestions.remove(currentQuestion.qNum)
                                    else bookmarkedQuestions[currentQuestion.qNum] = true
                                },
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

                        // Direction (if present)
                        if (currentQuestion.direction.isNotEmpty()) {
                            Text(
                                text = "Direction: ${currentQuestion.direction}",
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
                            text = currentQuestion.questionText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                lineHeight = 24.sp,
                                fontSize = 16.sp
                            ),
                            color = textPrimary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Options List (1. 2. 3. 4. 5.)
                        val currentReattemptChoice = reattemptUserAnswers[currentQuestion.qNum]

                        currentQuestion.options.forEachIndexed { optIndex, optText ->
                            val isCorrectOption = optIndex == currentQuestion.correctIndex
                            val isUserChosen = optIndex == currentQuestion.userSelectedIndex

                            // Highlighting Logic
                            val (optBorderColor, optBgColor, optNumberColor) = if (isReattemptMode) {
                                val isSelectedInReattempt = currentReattemptChoice == optIndex
                                if (isSelectedInReattempt) {
                                    Triple(activeBlue, activeBlue.copy(alpha = 0.12f), activeBlue)
                                } else {
                                    Triple(Color.Transparent, cardBackground, textSecondary)
                                }
                            } else {
                                when {
                                    isCorrectOption -> Triple(
                                        correctGreen,
                                        correctGreen.copy(alpha = 0.12f),
                                        correctGreen
                                    )
                                    isUserChosen && !isCorrectOption -> Triple(
                                        wrongRed,
                                        wrongRed.copy(alpha = 0.12f),
                                        wrongRed
                                    )
                                    else -> Triple(Color.Transparent, cardBackground, textSecondary)
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clickable(enabled = isReattemptMode) {
                                        reattemptUserAnswers[currentQuestion.qNum] = optIndex
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
                                    // Option index prefix e.g. "1. ", "2. "
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

                                    // Option text
                                    Text(
                                        text = optText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        color = textPrimary,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Trailing status indicator in non-reattempt mode
                                    if (!isReattemptMode) {
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

                        // Detailed Solution & Explanation Section (Shown when not in reattempt mode)
                        if (!isReattemptMode) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = cardBackground,
                                border = BorderStroke(1.dp, cardStrokeColor),
                                modifier = Modifier.fillMaxWidth()
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
                                            text = "Solution & Detailed Explanation",
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
                                        text = currentQuestion.explanation,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            lineHeight = 22.sp,
                                            fontSize = 14.sp
                                        ),
                                        color = textPrimary.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }

                // 4. BOTTOM BAR: Reattempt Mode Toggle + Next Floating Arrow Button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = darkSurface,
                    border = BorderStroke(1.dp, cardStrokeColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Reattempt Mode Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reattempt Mode",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Switch(
                                checked = isReattemptMode,
                                onCheckedChange = { isReattemptMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = activeBlue,
                                    uncheckedThumbColor = textSecondary,
                                    uncheckedTrackColor = cardBackground
                                )
                            )
                        }

                        // Right Navigation Controls (Prev & Next buttons)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (selectedQuestionIndex > 0) {
                                IconButton(
                                    onClick = {
                                        selectedQuestionIndex--
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(cardBackground, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous",
                                        tint = textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Circular Blue Forward Button
                            IconButton(
                                onClick = {
                                    if (selectedQuestionIndex < questionsList.size - 1) {
                                        selectedQuestionIndex++
                                    } else {
                                        showFilterOverview = true
                                    }
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(activeBlue, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTabPill(
    text: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) activeColor else Color(0xFF262C34),
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) Color.White else Color(0xFFADB5BD),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}

private fun formatSecondsToMinutesSec(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) {
        "${mins}min ${secs}sec"
    } else {
        "${secs}sec"
    }
}

private fun formatSecondsToMmSs(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}
