package com.nunna.fitnessgym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunna.fitnessgym.AppGraph
import com.nunna.fitnessgym.anim.FigureScene
import com.nunna.fitnessgym.core.Ladder
import com.nunna.fitnessgym.core.Tracking
import com.nunna.fitnessgym.core.Units
import com.nunna.fitnessgym.core.UserSettings
import com.nunna.fitnessgym.data.AppClock
import com.nunna.fitnessgym.data.ContentRepo
import com.nunna.fitnessgym.data.GymRepo
import com.nunna.fitnessgym.data.Summary
import com.nunna.fitnessgym.data.db.SessionExerciseWithSets
import com.nunna.fitnessgym.data.db.SetLogEntity
import com.nunna.fitnessgym.timer.RestTimer
import com.nunna.fitnessgym.ui.FigureView
import com.nunna.fitnessgym.ui.Fmt
import com.nunna.fitnessgym.ui.Gap
import com.nunna.fitnessgym.ui.Pill
import com.nunna.fitnessgym.ui.PrimaryButton
import com.nunna.fitnessgym.ui.theme.Tokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Holds the last finished workout's summary for the Summary screen. */
object SummaryHolder { var value: Summary? = null }

@Composable
fun WorkoutScreen(
    repo: GymRepo,
    onFinished: (Summary?) -> Unit,
    onPick: (swapSeId: Long) -> Unit,
    onExercise: (String) -> Unit,
    onLeave: () -> Unit,
) {
    val ctx = LocalContext.current
    val active by repo.activeFlow.collectAsState(null)
    val settings by repo.settingsFlow.collectAsState(null)
    val rest by RestTimer.state.collectAsState()
    val scope = rememberCoroutineScope()
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(active) { if (active != null) loaded = true }
    LaunchedEffect(banner) { if (banner != null && AppGraph.animations) { delay(4000); banner = null } }

    val a = active
    if (a == null) {
        Column(Modifier.fillMaxSize().background(Tokens.Bg).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (loaded) "Workout closed." else "No workout in progress.", fontSize = 18.sp, modifier = Modifier.testTag("no_workout"))
            Gap(12)
            PrimaryButton("Back to Today", onLeave)
        }
        return
    }
    val s = settings ?: UserSettings()
    val ordered = a.ordered
    // The next-set cursor: first un-ticked set of the first exercise that isn't skipped.
    val cursor = ordered.filter { !it.ex.skipped }.firstNotNullOfOrNull { e -> e.orderedSets.firstOrNull { !it.done } }?.id

    Column(Modifier.fillMaxSize().background(Tokens.Bg).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp, 12.dp, 8.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(a.session.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("workout_title"))
                Elapsed(a.session.startedAt, a.doneSets, a.totalSets)
            }
            TextButton(onClick = { confirmFinish = true }, modifier = Modifier.testTag("finish")) { Text("Finish", fontWeight = FontWeight.Bold, color = Tokens.Prime, fontSize = 17.sp) }
        }
        rest?.let { RestBanner(it.totalSec, it.next, onPlus = { RestTimer.add(ctx, 15) }, onMinus = { RestTimer.add(ctx, -15) }, onSkip = { RestTimer.stop(ctx) }) }
        banner?.let {
            Text(it, color = Color.Black, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(12.dp)).background(Tokens.Synergist)
                    .clickable { banner = null }.padding(12.dp).testTag("pr_banner"))
        }
        error?.let {
            Text(it, color = Color.White, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF8A2B2B)).clickable { error = null }.padding(10.dp).testTag("set_error"))
        }

        LazyColumn(Modifier.weight(1f).testTag("workout_list")) {
            itemsIndexed(ordered, key = { _, e -> e.ex.id }) { pos, e ->
                ExerciseCard(
                    repo, e, pos, ordered.size, s, cursor, onExercise = onExercise,
                    onSwap = { onPick(e.ex.id) },
                    onCheck = { set, w, r, sec, rir ->
                        scope.launch {
                            if (set.done) { repo.uncompleteSet(set.id); return@launch }
                            val res = repo.completeSet(set.id, w, r, sec, rir)
                            if (!res.ok) { error = "SET-001: ${res.error}"; return@launch }
                            error = null
                            if (res.records.isNotEmpty()) banner = "New PR! ${res.records.first()}: ${repo.exercise(e.ex.exerciseId)?.name}"
                            if (res.restSec > 0) RestTimer.start(ctx, res.restSec, res.nextLabel) else RestTimer.stop(ctx)
                        }
                    },
                    onError = { error = it },
                )
            }
            item {
                Column(Modifier.padding(16.dp)) {
                    TextButton(onClick = { onPick(0) }, modifier = Modifier.testTag("add_exercise")) {
                        Icon(Icons.Filled.Add, null); Text("Add exercise", fontSize = 16.sp)
                    }
                    if (ordered.isEmpty()) Text("Empty workout. Add your first exercise.", color = Tokens.TextDim)
                    Gap(8)
                    PrimaryButton("Finish workout", { confirmFinish = true }, tag = "finish_bottom")
                    TextButton(onClick = { confirmDiscard = true }, modifier = Modifier.testTag("discard")) { Text("Discard workout", color = Tokens.TextDim) }
                    Gap(40)
                }
            }
        }
    }

    if (confirmFinish) {
        val none = a.doneSets == 0
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text(if (none) "Nothing logged yet" else "Finish workout?") },
            text = { Text(if (none) "You haven't ticked any sets, so there's nothing to save. Close this workout?" else "${a.doneSets} sets logged. Un-ticked sets won't be saved.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    RestTimer.stop(ctx)
                    scope.launch { onFinished(repo.finish(a.session.id)) }
                }, modifier = Modifier.testTag("confirm_finish")) { Text(if (none) "Close workout" else "Finish") }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }, modifier = Modifier.testTag("keep_going")) { Text("Keep going") } },
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard this workout?") },
            text = { Text("All ${a.doneSets} logged sets in this workout will be deleted. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    RestTimer.stop(ctx)
                    scope.launch { repo.discard(a.session.id); onLeave() }
                }, modifier = Modifier.testTag("confirm_discard")) { Text("Discard", color = Tokens.Prime) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Elapsed(start: Long, done: Int, total: Int) {
    var now by remember { mutableStateOf(AppClock.now()) }
    LaunchedEffect(Unit) { while (AppGraph.animations) { delay(1000); now = AppClock.now() } }
    val min = ((now - start) / 60000).coerceAtLeast(0)
    Text("$min min · $done / $total sets", color = Tokens.TextDim, fontSize = 14.sp, modifier = Modifier.testTag("progress_line"))
}

@Composable
private fun RestBanner(total: Int, next: String?, onPlus: () -> Unit, onMinus: () -> Unit, onSkip: () -> Unit) {
    var left by remember { mutableIntStateOf(RestTimer.remainingSec()) }
    LaunchedEffect(total) {
        left = RestTimer.remainingSec()
        while (AppGraph.animations && left > 0) { delay(250); left = RestTimer.remainingSec() }
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clip(RoundedCornerShape(14.dp)).background(Tokens.Accent.copy(alpha = 0.22f))
            .padding(horizontal = 12.dp, vertical = 6.dp).testTag("rest_banner"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Rest ${Fmt.clock(left)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("rest_left"))
            if (next != null) Text("Next: $next", fontSize = 12.sp, color = Tokens.TextDim)
        }
        TextButton(onClick = onMinus, modifier = Modifier.testTag("rest_minus")) { Text("−15") }
        TextButton(onClick = onPlus, modifier = Modifier.testTag("rest_plus")) { Text("+15") }
        TextButton(onClick = onSkip, modifier = Modifier.testTag("rest_skip")) { Text("Skip") }
    }
}

@Composable
private fun ExerciseCard(
    repo: GymRepo, e: SessionExerciseWithSets, pos: Int, count: Int, s: UserSettings, cursor: Long?,
    onExercise: (String) -> Unit, onSwap: () -> Unit,
    onCheck: (SetLogEntity, Double?, Int?, Int?, Int?) -> Unit, onError: (String) -> Unit,
) {
    val ex = repo.exercise(e.ex.exerciseId)
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val tracking = Tracking.valueOf(e.ex.tracking)
    var menu by remember { mutableStateOf(false) }
    val sets = e.orderedSets
    val anyDone = sets.any { it.done }
    val ladder = remember(ex?.id, s) { Ladder.forExercise(ex?.equipment ?: "", s.profile, s.units) }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clip(RoundedCornerShape(18.dp))
            .background(if (e.ex.skipped) Tokens.Surface.copy(alpha = 0.5f) else Tokens.Surface).padding(12.dp).testTag("card_$pos"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (ex != null) {
                val scene = remember(ex.id) { FigureScene(ContentRepo.get(ctx).motionFor(ex), ex.activation()) }
                FigureView(scene, scene.defaultCamera(), Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(Tokens.Bg).clickable { onExercise(ex.id) }, playing = false, staticPhase = 0.5)
            }
            Column(Modifier.weight(1f).padding(start = 10.dp).clickable { ex?.let { onExercise(it.id) } }) {
                Text(ex?.name ?: e.ex.exerciseId, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("ex_name_$pos"))
                Text(targetText(e, tracking), color = Tokens.TextDim, fontSize = 13.sp)
            }
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.testTag("menu_$pos")) { Icon(Icons.Filled.MoreVert, "Exercise options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(if (anyDone) "Swap (no logged sets only)" else "Swap exercise") }, enabled = !anyDone,
                        onClick = { menu = false; onSwap() }, modifier = Modifier.testTag("menu_swap"))
                    DropdownMenuItem(text = { Text(if (e.ex.skipped) "Un-skip" else "Skip today") },
                        onClick = { menu = false; scope.launch { repo.setSkipped(e.ex.id, !e.ex.skipped) } }, modifier = Modifier.testTag("menu_skip"))
                    DropdownMenuItem(text = { Text("Add a set") }, onClick = { menu = false; scope.launch { repo.addSet(e.ex.id) } }, modifier = Modifier.testTag("menu_addset"))
                    if (pos > 0) DropdownMenuItem(text = { Text("Move up") }, onClick = { menu = false; scope.launch { repo.moveExercise(e.ex.id, -1) } }, modifier = Modifier.testTag("menu_up"))
                    if (pos < count - 1) DropdownMenuItem(text = { Text("Move down") }, onClick = { menu = false; scope.launch { repo.moveExercise(e.ex.id, 1) } }, modifier = Modifier.testTag("menu_down"))
                    DropdownMenuItem(text = { Text("Remove from workout") }, onClick = { menu = false; scope.launch { repo.removeExercise(e.ex.id) } }, modifier = Modifier.testTag("menu_remove"))
                    DropdownMenuItem(text = { Text("Never suggest this") }, onClick = {
                        menu = false; scope.launch { repo.exclude(e.ex.exerciseId, true) }
                    }, modifier = Modifier.testTag("menu_never"))
                }
            }
        }
        if (e.ex.note.isNotBlank() && !e.ex.skipped) Text(e.ex.note, fontStyle = FontStyle.Italic, color = Tokens.Synergist, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp).testTag("note_$pos"))
        if (e.ex.skipped) {
            Row(Modifier.padding(top = 8.dp)) { Pill("Skipped today") }
            return@Column
        }
        Gap(6)
        Row(Modifier.fillMaxWidth().padding(bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Set", color = Tokens.TextDim, fontSize = 12.sp, modifier = Modifier.width(34.dp))
            when (tracking) {
                Tracking.WEIGHT_REPS -> { Text(s.units.label, color = Tokens.TextDim, fontSize = 12.sp, modifier = Modifier.weight(1.4f), textAlign = TextAlign.Center); Text("reps", color = Tokens.TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
                Tracking.REPS -> Text("reps", color = Tokens.TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Tracking.WEIGHT_TIME -> { Text(s.units.label, color = Tokens.TextDim, fontSize = 12.sp, modifier = Modifier.weight(1.4f), textAlign = TextAlign.Center); Text("seconds", color = Tokens.TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
                Tracking.TIME -> Text("seconds", color = Tokens.TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Tracking.CARDIO -> Text("minutes", color = Tokens.TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
            if (s.logEffort && tracking != Tracking.CARDIO) Text("RIR", color = Tokens.TextDim, fontSize = 12.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
            Text("", modifier = Modifier.width(48.dp))
        }
        for (set in sets) {
            SetRow(set, tracking, s.units, ladder, set.id == cursor, s.logEffort, pos,
                onEdit = { w, r, sec, rir -> scope.launch { repo.updateSet(set.id, w, r, sec, rir) } },
                onCheck = { w, r, sec, rir -> onCheck(set, w, r, sec, rir) },
                onRemove = { scope.launch { if (!repo.removeSet(set.id)) onError("SET-002: An exercise needs at least one set. Use ⋮ › Remove instead.") } },
            )
        }
        TextButton(onClick = { scope.launch { repo.addSet(e.ex.id) } }, modifier = Modifier.testTag("add_set_$pos")) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Text("Add set") }
    }
}

private fun targetText(e: SessionExerciseWithSets, t: Tracking): String {
    val x = e.ex
    val rest = if (x.restSec > 0) " · rest ${Fmt.secs(x.restSec)}" else ""
    return when (t) {
        Tracking.CARDIO -> "Target ${Fmt.secs(x.seconds)}"
        Tracking.TIME, Tracking.WEIGHT_TIME -> "${x.targetSets} × ${Fmt.secs(x.seconds)}$rest"
        else -> "${x.targetSets} × ${x.repMin}–${x.repMax} reps · ${x.rir} in reserve$rest"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SetRow(
    set: SetLogEntity, t: Tracking, units: Units, ladder: Ladder, isCursor: Boolean, logEffort: Boolean, pos: Int,
    onEdit: (Double?, Int?, Int?, Int?) -> Unit,
    onCheck: (Double?, Int?, Int?, Int?) -> Unit,
    onRemove: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var w by remember(set.id, set.weight) { mutableStateOf(Fmt.w(set.weight)) }
    var r by remember(set.id, set.reps) { mutableStateOf(set.reps?.toString() ?: "") }
    var sec by remember(set.id, set.seconds) {
        mutableStateOf(set.seconds?.let { if (t == Tracking.CARDIO) Fmt.w(it / 60.0) else it.toString() } ?: "")
    }
    var rir by remember(set.id, set.rir) { mutableStateOf(set.rir?.toString() ?: "") }
    fun seconds(): Int? = if (t == Tracking.CARDIO) sec.toDoubleOrNull()?.let { (it * 60).toInt() } else sec.toIntOrNull()
    fun push() = onEdit(w.toDoubleOrNull(), r.toIntOrNull(), seconds(), rir.toIntOrNull())
    val i = set.setIndex
    val bg = when { set.done -> Tokens.Mat.copy(alpha = 0.35f); isCursor -> Tokens.Prime.copy(alpha = 0.12f); else -> Color.Transparent }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(10.dp)).background(bg).padding(vertical = 2.dp).testTag("row_${pos}_$i"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Long-press the set number to delete an un-logged set.
        Text("${i + 1}", fontWeight = FontWeight.Bold, modifier = Modifier.width(34.dp)
            .combinedClickable(enabled = !set.done, onClick = {}, onLongClick = onRemove).testTag("setnum_${pos}_$i"), textAlign = TextAlign.Center)
        if (t == Tracking.WEIGHT_REPS || t == Tracking.WEIGHT_TIME) {
            Row(Modifier.weight(1.4f), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val cur = w.toDoubleOrNull()
                    val nv = if (cur == null) null else ladder.down(cur) ?: (cur - units.defaultStep).coerceAtLeast(0.0)
                    if (nv != null) { w = Fmt.w(nv); push() }
                }, enabled = !set.done, modifier = Modifier.size(32.dp).testTag("wminus_${pos}_$i")) { Icon(Icons.Filled.Remove, "Less weight", Modifier.size(18.dp)) }
                NumField(w, { v -> w = v.filter { c -> c.isDigit() || c == '.' }.take(6); push() }, !set.done, Modifier.weight(1f).testTag("w_${pos}_$i"), decimal = true)
                IconButton(onClick = {
                    val cur = w.toDoubleOrNull()
                    val nv = if (cur == null) (ladder.up(0.0) ?: units.defaultStep) else ladder.up(cur) ?: (cur + units.defaultStep)
                    w = Fmt.w(nv); push()
                }, enabled = !set.done, modifier = Modifier.size(32.dp).testTag("wplus_${pos}_$i")) { Icon(Icons.Filled.Add, "More weight", Modifier.size(18.dp)) }
            }
        }
        when (t) {
            Tracking.WEIGHT_REPS, Tracking.REPS -> NumField(r, { v -> r = v.filter { it.isDigit() }.take(3); push() }, !set.done, Modifier.weight(1f).padding(horizontal = 4.dp).testTag("r_${pos}_$i"))
            else -> NumField(sec, { v -> sec = v.filter { c -> c.isDigit() || (t == Tracking.CARDIO && c == '.') }.take(5); push() }, !set.done, Modifier.weight(1f).padding(horizontal = 4.dp).testTag("s_${pos}_$i"), decimal = t == Tracking.CARDIO)
        }
        if (logEffort && t != Tracking.CARDIO) NumField(rir, { v -> rir = v.filter { it.isDigit() }.take(1); push() }, !set.done, Modifier.width(52.dp).testTag("rir_${pos}_$i"))
        IconButton(onClick = {
            if (!set.done) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onCheck(w.toDoubleOrNull(), r.toIntOrNull(), seconds(), rir.toIntOrNull())
        }, modifier = Modifier.size(48.dp).testTag("check_${pos}_$i")) {
            Icon(
                if (set.done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                if (set.done) "Logged. Tap to undo" else "Log set",
                tint = if (set.done) Color(0xFF3CCB8A) else if (isCursor) Tokens.Prime else Tokens.TextDim,
                modifier = Modifier.size(34.dp),
            )
        }
    }
    if (set.pr != null) Row(Modifier.padding(start = 34.dp)) { Pill("PR · ${set.pr}", Tokens.Synergist, Color.Black) }
}

@Composable
private fun NumField(value: String, onChange: (String) -> Unit, enabled: Boolean, modifier: Modifier, decimal: Boolean = false) {
    OutlinedTextField(
        value, onChange, enabled = enabled, singleLine = true, modifier = modifier,
        textStyle = TextStyle(fontSize = 17.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, color = Tokens.Text),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
    )
}
