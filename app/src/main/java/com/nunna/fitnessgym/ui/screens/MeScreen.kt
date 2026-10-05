package com.nunna.fitnessgym.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunna.fitnessgym.core.Gamification
import com.nunna.fitnessgym.core.Units
import com.nunna.fitnessgym.data.GymRepo
import com.nunna.fitnessgym.ui.Fmt
import com.nunna.fitnessgym.ui.Gap
import com.nunna.fitnessgym.ui.PrimaryButton
import com.nunna.fitnessgym.ui.SectionTitle
import com.nunna.fitnessgym.ui.theme.Tokens
import kotlinx.coroutines.launch

@Composable
fun MeScreen(repo: GymRepo, onEdit: () -> Unit, onStatus: () -> Unit, onExercise: (String) -> Unit, onReset: () -> Unit) {
    val ctx = LocalContext.current
    val settings by repo.settingsFlow.collectAsState(null)
    val profile by repo.profileFlow.collectAsState(null)
    val done by repo.doneFlow.collectAsState(emptyList())
    val active by repo.activeFlow.collectAsState(null)
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val s = settings ?: return

    Column(Modifier.fillMaxSize().background(Tokens.Bg).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(if (s.name.isBlank()) "Me" else s.name, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Level ${Gamification.level(profile?.xp ?: 0)} · ${profile?.xp ?: 0} XP · ${done.size} workouts", color = Tokens.TextDim)

        SectionTitle("Your plan")
        Line("Goal", s.goal.label)
        Line("Experience", s.experience.label)
        Line("Schedule", "${s.daysPerWeek} days a week · ${s.sessionMinutes} min")
        Line("Training at", s.profile.name + if (s.profile.dumbbells.isNotEmpty()) " · dumbbells ${s.profile.dumbbells.joinToString { Fmt.w(it) }} ${s.units.label}" else "")
        Line("Extras", s.interests.joinToString { it.label }.ifEmpty { "None" })
        Line("Protecting", s.avoid.joinToString { it.label }.ifEmpty { "Nothing" })
        if (s.parqFlags != 0) Text("Safety check: please check with a doctor before you push hard.", color = Tokens.Synergist, fontSize = 13.sp)
        Gap(10)
        PrimaryButton("Edit goals & schedule", onEdit, tag = "edit_goals")
        Gap(8)
        OutlinedButton(onClick = {
            if (active != null) { info = "Finish or discard the current workout first."; return@OutlinedButton }
            scope.launch { repo.regeneratePlan(); info = "New plan variation built. Same goal, fresh exercise picks." }
        }, modifier = Modifier.fillMaxWidth().testTag("regen")) { Text("New plan variation") }
        info?.let { Text(it, color = Tokens.Mat, modifier = Modifier.padding(top = 6.dp).testTag("me_info")) }

        SectionTitle("Settings")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Weights in", modifier = Modifier.weight(1f))
            for (u in Units.entries) FilterChip(selected = s.units == u, onClick = {
                if (s.units != u) scope.launch {
                    // Keep owned dumbbells meaningful: convert their weights to the new unit.
                    val conv = s.profiles.map { p -> p.copy(dumbbells = p.dumbbells.map { w -> Math.round((if (u == Units.LB) w * 2.20462 else w / 2.20462) * 2) / 2.0 }, machineStep = u.machineStep) }
                    repo.updateSettings(s.copy(units = u, profiles = conv))
                }
            }, label = { Text(u.label) }, modifier = Modifier.padding(start = 6.dp).testTag("units_${u.name}"))
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Log effort (reps in reserve)", modifier = Modifier.weight(1f))
            Switch(s.logEffort, { v -> scope.launch { repo.updateSettings(s.copy(logEffort = v)) } }, modifier = Modifier.testTag("rir_switch"))
        }

        SectionTitle("Hidden exercises")
        if (s.excluded.isEmpty()) Text("None. Use ⋮ › Never suggest this in a workout to hide one.", color = Tokens.TextDim, fontSize = 14.sp)
        for (id in s.excluded.sorted()) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onExercise(id) }, modifier = Modifier.weight(1f)) { Text(repo.exercise(id)?.name ?: id) }
            TextButton(onClick = { scope.launch { repo.exclude(id, false) } }, modifier = Modifier.testTag("unhide_$id")) { Text("Allow again") }
        }

        SectionTitle("Your data")
        OutlinedButton(onClick = {
            val csv = repo.csv(done)
            val send = Intent(Intent.ACTION_SEND).setType("text/csv").putExtra(Intent.EXTRA_SUBJECT, "Fitness Gym workouts").putExtra(Intent.EXTRA_TEXT, csv)
            runCatching { ctx.startActivity(Intent.createChooser(send, "Export workouts").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                .onFailure { info = "EXP-001: No app can receive the export. Fix: install Google Drive or Gmail." }
        }, modifier = Modifier.fillMaxWidth().testTag("export")) { Text("Export all workouts (CSV)") }
        Gap(6)
        OutlinedButton(onClick = onStatus, modifier = Modifier.fillMaxWidth().testTag("status")) { Text("App status & credits") }
        Gap(6)
        TextButton(onClick = { confirmReset = true }, modifier = Modifier.testTag("reset")) { Text("Reset everything", color = Tokens.Prime) }
        Gap(30)
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Reset everything?") },
        text = { Text("This deletes every workout, your plan and your XP, then starts the setup again. Export first if you want a copy.") },
        confirmButton = { TextButton(onClick = { confirmReset = false; scope.launch { repo.resetAll(); onReset() } }, modifier = Modifier.testTag("confirm_reset")) { Text("Reset", color = Tokens.Prime) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
    )
}

@Composable
private fun Line(k: String, v: String) = Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
    Text(k, color = Tokens.TextDim, modifier = Modifier.weight(0.38f))
    Text(v, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.62f))
}
