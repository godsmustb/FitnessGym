package com.nunna.fitnessgym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.nunna.fitnessgym.anim.Anatomy
import com.nunna.fitnessgym.core.Gamification
import com.nunna.fitnessgym.core.Generator
import com.nunna.fitnessgym.core.PlanSlot
import com.nunna.fitnessgym.core.Role
import com.nunna.fitnessgym.core.Tracking
import com.nunna.fitnessgym.data.AppClock
import com.nunna.fitnessgym.data.GymRepo
import com.nunna.fitnessgym.data.db.PlanSlotEntity
import com.nunna.fitnessgym.ui.Fmt
import com.nunna.fitnessgym.ui.Gap
import com.nunna.fitnessgym.ui.PrimaryButton
import com.nunna.fitnessgym.ui.StatTile
import com.nunna.fitnessgym.ui.theme.Tokens
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

@Composable
fun TodayScreen(repo: GymRepo, onWorkout: () -> Unit, onExercise: (String) -> Unit, onBeforeStart: () -> Unit = {}) {
    val profile by repo.profileFlow.collectAsState(null)
    val settings by repo.settingsFlow.collectAsState(null)
    val plan by repo.planFlow.collectAsState(emptyList())
    val active by repo.activeFlow.collectAsState(null)
    val done by repo.doneFlow.collectAsState(emptyList())
    val scope = rememberCoroutineScope()

    var starting by remember { mutableStateOf(false) }

    val today = AppClock.today()
    val days = done.map { AppClock.dateOf(it.session.startedAt) }
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val thisWeek = days.count { !it.isBefore(weekStart) }
    val streak = Gamification.weeklyStreak(days, today)
    val xp = profile?.xp ?: 0
    val s = settings
    val nextIdx = if (plan.isEmpty()) 0 else (profile?.nextDay ?: 0).coerceIn(0, plan.size - 1)
    // The chosen day follows the plan rotation: after a workout it moves on to the next day.
    var picked by remember(nextIdx, plan.size) { mutableIntStateOf(nextIdx) }
    val dayIdx = if (picked in plan.indices) picked else nextIdx
    val day = plan.getOrNull(dayIdx)

    Column(Modifier.fillMaxSize().background(Tokens.Bg).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(greeting(s?.name.orEmpty()), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Level · ${xp} XP", "Lv ${Gamification.level(xp)}", Modifier.weight(1f))
            StatTile("week streak", "🔥 $streak", Modifier.weight(1f).testTag("streak"))
            StatTile("this week", "$thisWeek / ${s?.daysPerWeek ?: 3}", Modifier.weight(1f).testTag("week_count"))
        }
        LinearProgressIndicator(progress = { Gamification.progress(xp).toFloat() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), color = Tokens.Synergist)

        // Equipment profile switch (only when the owner trains in more than one place).
        if (s != null && s.profiles.size > 1) {
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Training at", color = Tokens.TextDim)
                for ((i, p) in s.profiles.withIndex()) FilterChip(
                    selected = s.activeProfile == i,
                    onClick = { if (s.activeProfile != i && active == null) scope.launch { repo.updateSettings(s.copy(activeProfile = i)) } },
                    label = { Text(p.name) }, modifier = Modifier.testTag("profile_${p.name}"),
                )
            }
        }

        val a = active
        if (a != null) {
            Gap(16)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Tokens.Prime.copy(alpha = 0.18f)).padding(16.dp)) {
                Text("Workout in progress", color = Tokens.Prime, fontWeight = FontWeight.Bold)
                Text(a.session.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("${a.doneSets} of ${a.totalSets} sets logged · everything is saved", color = Tokens.TextDim)
                Gap(10)
                PrimaryButton("Resume workout", onWorkout, tag = "resume")
            }
        } else if (day != null) {
            Gap(16)
            if (plan.size > 1) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((i, pd) in plan.withIndex()) FilterChip(
                    selected = i == dayIdx, onClick = { picked = i },
                    label = { Text(pd.day.name + if (i == nextIdx) " · next" else "") }, modifier = Modifier.testTag("day_$i"),
                )
            }
            Gap(8)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Tokens.Surface).padding(16.dp)) {
                Text(if (dayIdx == nextIdx) "UP NEXT" else "CHOSEN", color = Tokens.Prime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(day.day.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("day_title"))
                val slots = day.ordered
                Text("${day.day.focus} · ${slots.size} exercises · ~${Generator.estimateMinutes(slots.map { it.toCore() })} min", color = Tokens.TextDim)
                Gap(8)
                for (sl in slots) {
                    val ex = repo.exercise(sl.exerciseId) ?: continue
                    Row(Modifier.fillMaxWidth().clickable { onExercise(ex.id) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ex.name, fontWeight = FontWeight.Medium)
                            Text(ex.primary.joinToString { Anatomy.LABELS[it] ?: it }, color = Tokens.Prime, fontSize = 12.sp)
                        }
                        Text(target(sl), color = Tokens.TextDim, fontSize = 14.sp, textAlign = TextAlign.End)
                    }
                }
                Gap(12)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GoButton(enabled = !starting) {
                        if (starting) return@GoButton
                        starting = true
                        onBeforeStart()
                        scope.launch { repo.startPlanDay(dayIdx); starting = false; onWorkout() }
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        Text("Tap GO to start", fontWeight = FontWeight.SemiBold)
                        Text("Weights are pre-filled from last time.", color = Tokens.TextDim, fontSize = 13.sp)
                    }
                }
            }
        } else {
            Gap(24)
            Text("No plan yet. Open Me › Edit goals to build one.", color = Tokens.TextDim)
        }
        if (a == null) {
            Gap(12)
            OutlinedButton(onClick = {
                onBeforeStart()
                scope.launch { repo.startEmpty(); onWorkout() }
            }, modifier = Modifier.fillMaxWidth().testTag("quick")) { Text("Quick workout (pick exercises as you go)") }
        }
        Gap(24)
    }
}

@Composable
private fun GoButton(enabled: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.size(92.dp).clip(CircleShape).background(if (enabled) Tokens.Prime else Tokens.SurfaceHi)
            .clickable(enabled = enabled, onClick = onClick).testTag("go"),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) { Text("GO", fontSize = 30.sp, fontWeight = FontWeight.Black) }
}

fun target(sl: PlanSlotEntity): String = when (Tracking.valueOf(sl.tracking)) {
    Tracking.CARDIO -> Fmt.secs(sl.seconds)
    Tracking.TIME, Tracking.WEIGHT_TIME -> "${sl.sets} × ${Fmt.secs(sl.seconds)}"
    else -> "${sl.sets} × ${sl.repMin}–${sl.repMax}"
}

fun PlanSlotEntity.toCore() = PlanSlot(exerciseId, sets, repMin, repMax, rir, restSec, seconds, Role.valueOf(role), Tracking.valueOf(tracking))

private fun greeting(name: String): String {
    val h = java.time.Instant.ofEpochMilli(AppClock.now()).atZone(AppClock.zone).hour
    val g = when (h) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }
    return if (name.isBlank()) g else "$g, $name"
}

