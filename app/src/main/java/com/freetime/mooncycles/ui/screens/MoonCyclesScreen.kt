package com.freetime.mooncycles.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.freetime.mooncycles.MoonPhaseCalculator
import com.freetime.mooncycles.MoonPhaseData
import com.freetime.mooncycles.MonthlyMoonDay
import com.freetime.mooncycles.ui.components.MoonCanvas
import com.freetime.mooncycles.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

@Composable
fun MoonCyclesScreen() {
    val moonData = remember { MoonPhaseCalculator.getMoonPhase() }
    val calendar = remember { Calendar.getInstance() }
    var currentMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    val monthDays by remember(currentMonth, currentYear) {
        derivedStateOf { MoonPhaseCalculator.getMonthlyCalendar(currentYear, currentMonth) }
    }
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MoonDeepBlack)
    ) {
        StarfieldBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            MoonHeader()

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MoonPurpleLight,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MoonPurple
                        )
                    }
                },
                divider = {}
            ) {
                listOf("HEUTE", "KALENDER", "INFO").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selectedTab == index) MoonPurpleLight else MoonGray
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> TodayScreen(moonData)
                    1 -> CalendarScreen(
                        monthDays = monthDays,
                        currentMonth = currentMonth,
                        currentYear = currentYear,
                        onPrevMonth = {
                            if (currentMonth == 0) { currentMonth = 11; currentYear-- } else currentMonth--
                        },
                        onNextMonth = {
                            if (currentMonth == 11) { currentMonth = 0; currentYear++ } else currentMonth++
                        }
                    )
                    2 -> InfoScreen()
                }
            }
        }
    }
}

@Composable
private fun MoonHeader() {
    val dateFormatter = remember { SimpleDateFormat("EEEE, d. MMMM yyyy", Locale.GERMAN) }
    val today = remember { dateFormatter.format(Date()) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("MOON CYCLES", style = MaterialTheme.typography.titleLarge, color = MoonPurpleLight.copy(alpha = 0.7f), letterSpacing = 6.sp)
        Spacer(Modifier.height(4.dp))
        Text(today, style = MaterialTheme.typography.bodyMedium, color = MoonGray)
    }
}

// ─── TODAY ───────────────────────────────────────────────────────────────────

@Composable
private fun TodayScreen(moonData: MoonPhaseData) {
    val scrollState = rememberScrollState()
    val animatedPhase by animateFloatAsState(targetValue = moonData.phase.toFloat(), animationSpec = tween(1500, easing = EaseOutCubic), label = "phase")

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        MoonCanvas(phase = animatedPhase.toDouble(), size = 200.dp)
        Spacer(Modifier.height(20.dp))
        Text(moonData.phaseNameDe.uppercase(), style = MaterialTheme.typography.headlineMedium, color = MoonSilver, letterSpacing = 3.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        IlluminationBar(illumination = moonData.illumination)
        Spacer(Modifier.height(24.dp))
        StatsGrid(moonData)
        Spacer(Modifier.height(20.dp))
        DescriptionCard(moonData)
        Spacer(Modifier.height(20.dp))
        UpcomingCard(moonData)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun IlluminationBar(illumination: Double) {
    val animatedIll by animateFloatAsState(targetValue = (illumination / 100f).toFloat(), animationSpec = tween(1200, easing = EaseOutCubic), label = "ill")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Beleuchtung: ${illumination.toInt()}%", style = MaterialTheme.typography.bodyMedium, color = MoonGray)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.width(220.dp).height(6.dp).clip(CircleShape).background(MoonNavy)) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animatedIll).clip(CircleShape).background(Brush.horizontalGradient(listOf(MoonSlate, MoonPurple, MoonGoldLight))))
        }
    }
}

@Composable
private fun StatsGrid(moonData: MoonPhaseData) {
    val dateFormatter = remember { SimpleDateFormat("d. MMM", Locale.GERMAN) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("MONDTAG", "${moonData.age.toInt()}", "/ 29", Modifier.weight(1f))
        StatCard("STERNZEICHEN", moonData.zodiacSignDe, "", Modifier.weight(1.4f))
        StatCard("ZYKLUS", if (moonData.isWaxing) "Zunehmend" else "Abnehmend", "", Modifier.weight(1.4f))
    }
    Spacer(Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("VOLLMOND", "in ${moonData.daysUntilNextFullMoon}", "Tagen", Modifier.weight(1f))
        StatCard("NEUMOND", "in ${moonData.daysUntilNextNewMoon}", "Tagen", Modifier.weight(1f))
        StatCard("DATUM 🌕", dateFormatter.format(moonData.nextFullMoonDate), "", Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MoonNavy),
        border = BorderStroke(1.dp, MoonSlate.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MoonGray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = MoonSilver, textAlign = TextAlign.Center, maxLines = 2)
            if (unit.isNotEmpty()) Text(unit, style = MaterialTheme.typography.labelSmall, color = MoonGold.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun DescriptionCard(moonData: MoonPhaseData) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MoonNavy),
        border = BorderStroke(1.dp, MoonPurple.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(moonData.emoji, fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Text("Mondenergie", style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp), color = MoonPurpleLight)
            }
            Spacer(Modifier.height(12.dp))
            Text(moonData.description, style = MaterialTheme.typography.bodyMedium, color = MoonCream.copy(alpha = 0.9f), lineHeight = 22.sp)
        }
    }
}

@Composable
private fun UpcomingCard(moonData: MoonPhaseData) {
    val dateFormatter = remember { SimpleDateFormat("EEEE, d. MMMM", Locale.GERMAN) }
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MoonNavy),
        border = BorderStroke(1.dp, MoonGold.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("NÄCHSTE MONDPHASEN", style = MaterialTheme.typography.labelLarge, color = MoonGold.copy(alpha = 0.8f))
            Spacer(Modifier.height(16.dp))
            UpcomingRow("🌕", "Vollmond", dateFormatter.format(moonData.nextFullMoonDate), "in ${moonData.daysUntilNextFullMoon} Tagen")
            HorizontalDivider(color = MoonSlate.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 10.dp))
            UpcomingRow("🌑", "Neumond", dateFormatter.format(moonData.nextNewMoonDate), "in ${moonData.daysUntilNextNewMoon} Tagen")
        }
    }
}

@Composable
private fun UpcomingRow(emoji: String, label: String, date: String, countdown: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MoonSilver)
            Text(date, style = MaterialTheme.typography.labelSmall, color = MoonGray)
        }
        Text(countdown, style = MaterialTheme.typography.labelLarge, color = MoonGold)
    }
}

// ─── CALENDAR ────────────────────────────────────────────────────────────────

@Composable
private fun CalendarScreen(monthDays: List<MonthlyMoonDay>, currentMonth: Int, currentYear: Int, onPrevMonth: () -> Unit, onNextMonth: () -> Unit) {
    val monthNames = listOf("Januar","Februar","März","April","Mai","Juni","Juli","August","September","Oktober","November","Dezember")
    val cal = Calendar.getInstance().also { it.set(currentYear, currentMonth, 1) }
    val firstDayOffset = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevMonth) { Icon(Icons.Default.ChevronLeft, null, tint = MoonPurpleLight) }
            Text("${monthNames[currentMonth]} $currentYear", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp), color = MoonSilver)
            IconButton(onClick = onNextMonth) { Icon(Icons.Default.ChevronRight, null, tint = MoonPurpleLight) }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Mo","Di","Mi","Do","Fr","Sa","So").forEach { day ->
                Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MoonGray)
            }
        }
        Spacer(Modifier.height(8.dp))
        val rows = ceil((firstDayOffset + monthDays.size) / 7f).toInt()
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val dayIndex = row * 7 + col - firstDayOffset
                    if (dayIndex < 0 || dayIndex >= monthDays.size) Box(modifier = Modifier.weight(1f))
                    else CalendarDayCell(day = monthDays[dayIndex], modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.height(16.dp))
        PhaseLegend()
    }
}

@Composable
private fun CalendarDayCell(day: MonthlyMoonDay, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.aspectRatio(1f).padding(2.dp)
            .then(if (day.isToday) Modifier.background(MoonPurple.copy(alpha = 0.25f), RoundedCornerShape(8.dp)) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day.emoji, fontSize = if (day.isToday) 18.sp else 16.sp)
            Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = if (day.isToday) MoonGold else MoonGray)
        }
    }
}

@Composable
private fun PhaseLegend() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MoonNavy),
        border = BorderStroke(1.dp, MoonSlate.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("MONDPHASEN", style = MaterialTheme.typography.labelLarge, color = MoonGray)
            Spacer(Modifier.height(12.dp))
            listOf("🌑" to "Neumond","🌒" to "Zunehmende Sichel","🌓" to "Erstes Viertel","🌔" to "Zunehmend","🌕" to "Vollmond","🌖" to "Abnehmend","🌗" to "Letztes Viertel","🌘" to "Abnehmende Sichel")
                .chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { (emoji, name) ->
                            Row(modifier = Modifier.weight(1f).padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(emoji, fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(name, style = MaterialTheme.typography.labelSmall, color = MoonCream.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
        }
    }
}

// ─── INFO ─────────────────────────────────────────────────────────────────────

@Composable
private fun InfoScreen() {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp)) {
        Text("MONDPHASEN & BEDEUTUNG", style = MaterialTheme.typography.titleLarge.copy(fontSize = 13.sp), color = MoonGold.copy(alpha = 0.8f))
        Spacer(Modifier.height(16.dp))
        listOf(
            InfoPhase("🌑","Neumond","Neuanfänge & Intention","Der Neumond markiert den Beginn eines neuen Zyklus. Es ist die ideale Zeit für neue Projekte, das Setzen von Absichten und innere Reflexion. Die Energie ist ruhig und nach innen gerichtet."),
            InfoPhase("🌒","Zunehmende Sichel","Aufbau & Wachstum","In dieser Phase beginnt die Energie zu steigen. Es ist eine gute Zeit, erste Schritte zu unternehmen und Pläne in die Tat umzusetzen. Bleibe offen für neue Möglichkeiten."),
            InfoPhase("🌓","Erstes Viertel","Entscheidung & Handlung","Das erste Viertel bringt Entscheidungsdruck. Hindernisse können auftauchen, die überwunden werden müssen. Es ist Zeit für mutiges Handeln und Engagement."),
            InfoPhase("🌔","Zunehmender Mond","Verfeinerung & Fokus","Die Energie steigt weiter. Verfeinere deine Pläne und bleibe fokussiert auf deine Ziele. Details werden wichtig und die Ausdauer zahlt sich aus."),
            InfoPhase("🌕","Vollmond","Höhepunkt & Erfüllung","Der Vollmond ist die Phase höchster Energie und Intensität. Emotionen sind verstärkt, Intuition ist geschärft. Feiere Errungenschaften und lass los, was nicht mehr dient."),
            InfoPhase("🌖","Abnehmender Mond","Dankbarkeit & Teilen","Nach dem Höhepunkt beginnt die Energie abzunehmen. Es ist Zeit für Reflexion, Dankbarkeit und das Weitergeben von Wissen an andere."),
            InfoPhase("🌗","Letztes Viertel","Loslassen & Reinigen","Das letzte Viertel lädt zum Loslassen und Reinigen ein. Beende, was nicht mehr dient. Miste aus, vergib und schaffe Raum für das Neue."),
            InfoPhase("🌘","Abnehmende Sichel","Ruhe & Rückzug","Die letzte Phase vor dem Neumond ist eine Zeit der Stille und des Rückzugs. Ruhe dich aus, meditiere und bereite dich innerlich auf den nächsten Zyklus vor.")
        ).forEach { p -> InfoCard(p); Spacer(Modifier.height(12.dp)) }

        Spacer(Modifier.height(20.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MoonNavy), border = BorderStroke(1.dp, MoonGold.copy(alpha = 0.25f))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("DER MONDZYKLUS", style = MaterialTheme.typography.labelLarge, color = MoonGold.copy(alpha = 0.8f))
                Spacer(Modifier.height(12.dp))
                Text("Ein vollständiger Mondzyklus (synodischer Monat) dauert etwa 29 Tage, 12 Stunden und 44 Minuten. Die Mondphasen entstehen durch die relative Position von Erde, Mond und Sonne. Nicht der Erdschatten erzeugt die Phasen – das ist ein häufiger Irrtum – sondern der Blickwinkel, aus dem wir die beleuchtete Hälfte des Mondes sehen.", style = MaterialTheme.typography.bodyMedium, color = MoonCream.copy(alpha = 0.85f), lineHeight = 22.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

private data class InfoPhase(val emoji: String, val name: String, val subtitle: String, val text: String)

@Composable
private fun InfoCard(phase: InfoPhase) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MoonNavy),
        border = BorderStroke(1.dp, MoonSlate.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(phase.emoji, fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(phase.name, style = MaterialTheme.typography.bodyMedium, color = MoonSilver)
                    Text(phase.subtitle, style = MaterialTheme.typography.labelSmall, color = MoonGold.copy(alpha = 0.7f))
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MoonGray)
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    HorizontalDivider(color = MoonSlate.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 12.dp))
                    Text(phase.text, style = MaterialTheme.typography.bodyMedium, color = MoonCream.copy(alpha = 0.85f), lineHeight = 22.sp)
                }
            }
        }
    }
}

// ─── STARFIELD ────────────────────────────────────────────────────────────────

@Composable
private fun StarfieldBackground() {
    val stars = remember {
        (1..80).map { Triple((0..1000).random() / 1000f, (0..1000).random() / 1000f, (2..6).random() / 10f) }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val twinkle by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "twinkle")

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEachIndexed { i, (x, y, s) ->
            val alpha = (sin((twinkle + i * 0.13f) * Math.PI).toFloat().absoluteValue * 0.6f + 0.2f)
            drawCircle(color = Color.White.copy(alpha = (alpha * s * 2f).coerceIn(0f, 1f)), radius = s * 3f, center = Offset(x * size.width, y * size.height))
        }
    }
}