package com.nunna.fitnessgym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunna.fitnessgym.core.Gamification
import com.nunna.fitnessgym.core.Tracking
import com.nunna.fitnessgym.data.AppClock
import com.nunna.fitnessgym.data.GymRepo
import com.nunna.fitnessgym.data.db.SessionFull
import com.nunna.fitnessgym.data.db.SetLogEntity
import com.nunna.fitnessgym.ui.Dot
import com.nunna.fitnessgym.ui.Fmt
import com.nunna.fitnessgym.ui.Gap
import com.nunna.fitnessgym.ui.SectionTitle
import com.nunna.fitnessgym.ui.StatTile
import com.nunna.fitnessgym.ui.theme.Tokens
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun fmtSet(s: SetLogEntity, t: Tracking): String {
    val u = s.unit.lowercase()
    return when (t) {
        Tracking.WEIGHT_REPS -> "${Fmt.w(s.weight)} $u × ${s.reps ?: "-"}"
        Tracking.REPS -> "${s.reps ?: "-"} reps"
        Tracking.TIME -> Fmt.secs(s.seconds ?: 0)
        Tracking.CARDIO -> Fmt.secs(s.seconds ?: 0)
        Tracking.WEIGHT_TIME -> "${Fmt.w(s.weight)} $u × ${Fmt.secs(s.seconds ?: 0)}"
    } + (s.rir?.let { " @RIR $it" } ?: "")
}

@Composable
fun ProgressScreen(repo: GymRepo, onSession: (Long) -> Unit) {
    val done by repo.doneFlow.collectAsState(emptyList())
    val profile by repo.profileFlow.collectAsState(null)
    val today = AppClock.today()
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf(today) }
    val byDay = done.groupBy { AppClock.dateOf(it.session.startedAt) }
    val streak = Gamification.weeklyStreak(byDay.keys, today)

    Column(Modifier.fillMaxSize().background(Tokens.Bg).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Progress", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("workouts", "${done.size}", Modifier.weight(1f).testTag("total_workouts"))
            StatTile("sets logged", "${done.sumOf { it.doneSets }}", Modifier.weight(1f))
            StatTile("week streak", "$streak", Modifier.weight(1f))
            StatTile("level", "${Gamification.level(profile?.xp ?: 0)}", Modifier.weight(1f))
        }

        // Month calendar with a dot on every workout day.
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }, modifier = Modifier.testTag("prev_month")) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month") }
            Text(month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.year, modifier = Modifier.weight(1f).testTag("month_title"),
                textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            IconButton(onClick = { month = month.plusMonths(1) }, modifier = Modifier.testTag("next_month")) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month") }
        }
        Row(Modifier.fillMaxWidth()) {
            for (d in listOf("M", "T", "W", "T", "F", "S", "S")) Text(d, Modifier.weight(1f), textAlign = TextAlign.Center, color = Tokens.TextDim, fontSize = 12.sp)
        }
        val first = month.atDay(1)
        val lead = first.dayOfWeek.value - 1
        val cells = lead + month.lengthOfMonth()
        for (row in 0 until (cells + 6) / 7) Row(Modifier.fillMaxWidth()) {
            for (col in 0 until 7) {
                val n = row * 7 + col - lead + 1
                Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                    if (n in 1..month.lengthOfMonth()) {
                        val date = month.atDay(n)
                        val has = byDay.containsKey(date)
                        val isSel = date == selected
                        Column(
                            Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) Tokens.Prime.copy(alpha = 0.25f) else Tokens.Surface)
                                .let { if (date == today) it.border(1.dp, Tokens.Prime, RoundedCornerShape(10.dp)) else it }
                                .clickable { selected = date }.testTag("cal_$date"),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                        ) {
                            Text("$n", fontSize = 13.sp)
                            if (has) Dot(Tokens.Prime, 6)
                        }
                    }
                }
            }
        }

        SectionTitle(if (selected == today) "Today" else selected.format(DateTimeFormatter.ofPattern("EEE d MMM")))
        val sessions = byDay[selected].orEmpty()
        if (sessions.isEmpty()) Text("No workout logged on this day.", color = Tokens.TextDim, modifier = Modifier.testTag("day_empty"))
        for (s in sessions) DayLog(repo, s) { onSession(s.session.id) }

        SectionTitle("Recent personal records")
        val prs = done.flatMap { s -> s.ordered.flatMap { e -> e.orderedSets.filter { it.pr != null }.map { Triple(s.session.startedAt, e.ex, it) } } }
            .sortedByDescending { it.first }.take(10)
        if (prs.isEmpty()) Text("Records appear after your second workout of an exercise.", color = Tokens.TextDim)
        for ((at, ex, set) in prs) Row(Modifier.padding(vertical = 3.dp)) {
            Text(AppClock.dateOf(at).format(DateTimeFormatter.ofPattern("d MMM")), color = Tokens.TextDim, modifier = Modifier.size(width = 60.dp, height = 20.dp))
            Text("${repo.exercise(ex.exerciseId)?.name}: ${fmtSet(set, Tracking.valueOf(ex.tracking))} (${set.pr})", fontSize = 14.sp)
        }
        Gap(30)
    }
}

@Composable
private fun DayLog(repo: GymRepo, s: SessionFull, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(14.dp)).background(Tokens.Surface).clickable(onClick = onOpen).padding(12.dp).testTag("daylog")) {
        Text(s.session.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        val mins = s.session.endedAt?.let { ((it - s.session.startedAt) / 60000).toInt() } ?: 0
        Text("$mins min · ${s.doneSets} set${if (s.doneSets == 1) "" else "s"} · +${s.session.xpEarned} XP", color = Tokens.TextDim, fontSize = 13.sp)
        for (e in s.ordered) {
            val t = Tracking.valueOf(e.ex.tracking)
            val sets = e.orderedSets.filter { it.done }
            if (sets.isEmpty()) continue
            Text(repo.exercise(e.ex.exerciseId)?.name ?: e.ex.exerciseId, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
            Text(sets.joinToString("  ·  ") { fmtSet(it, t) }, color = Tokens.Text, fontSize = 14.sp)
        }
    }
}

@Composable
fun SessionScreen(repo: GymRepo, id: Long, back: () -> Unit) {
    val full by repo.sessionFlow(id).collectAsState(null)
    var confirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Tokens.Bg).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(full?.session?.title ?: "Workout", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { confirm = true }, modifier = Modifier.testTag("delete_session")) { Text("Delete", color = Tokens.Prime) }
        }
        val f = full ?: return@Column
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(AppClock.dateOf(f.session.startedAt).format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")), color = Tokens.TextDim)
            DayLog(repo, f) {}
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Delete this workout?") },
        text = { Text("Its sets are removed from your history and records. This can't be undone.") },
        confirmButton = { TextButton(onClick = { confirm = false; scope.launch { repo.deleteSession(id); back() } }, modifier = Modifier.testTag("confirm_delete")) { Text("Delete", color = Tokens.Prime) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
    )
}
