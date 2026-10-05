package com.nunna.fitnessgym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunna.fitnessgym.core.AvoidArea
import com.nunna.fitnessgym.core.Equip
import com.nunna.fitnessgym.core.EquipmentProfile
import com.nunna.fitnessgym.core.Experience
import com.nunna.fitnessgym.core.Goal
import com.nunna.fitnessgym.core.Interest
import com.nunna.fitnessgym.core.Units
import com.nunna.fitnessgym.core.UserSettings
import com.nunna.fitnessgym.data.GymRepo
import com.nunna.fitnessgym.ui.ChoiceCard
import com.nunna.fitnessgym.ui.Fmt
import com.nunna.fitnessgym.ui.Gap
import com.nunna.fitnessgym.ui.PrimaryButton
import com.nunna.fitnessgym.ui.theme.Tokens
import kotlinx.coroutines.launch

enum class Where(val label: String, val blurb: String) {
    GYM("A gym", "GoodLife / LA Fitness style: machines, cables, dumbbells, cardio"),
    HOME("At home", "Bodyweight and whatever you own"),
    BOTH("Both", "Gym and home. Switch any time from the Today screen"),
}

/** PAR-Q+ (2023) general health questions, paraphrased. Any "yes" shows advice; it never blocks the app. */
val PARQ = listOf(
    "Has a doctor ever said you have a heart condition or high blood pressure?",
    "Do you feel chest pain at rest, in daily activities, or when you exercise?",
    "Do you lose balance from dizziness, or have you lost consciousness in the last 12 months?",
    "Have you been diagnosed with another chronic medical condition?",
    "Do you take prescribed medication for a chronic medical condition?",
    "Do you have a bone, joint or soft-tissue problem that activity could make worse?",
    "Has a doctor said you should only exercise under medical supervision?",
)

private val KG_WEIGHTS = listOf(2.0, 4.0, 5.0, 6.0, 8.0, 10.0, 12.0, 12.5, 15.0, 17.5, 20.0, 22.5, 25.0, 30.0)
private val LB_WEIGHTS = listOf(5.0, 8.0, 10.0, 12.0, 15.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0, 50.0)

/** Answers being edited. Kept separate from [UserSettings] so "where you train" can be rebuilt. */
data class Draft(
    val name: String = "",
    val goal: Goal? = null,
    val experience: Experience? = null,
    val days: Int = 3,
    val minutes: Int = 45,
    val where: Where? = null,
    val homeItems: Set<Equip> = setOf(Equip.MAT),
    val dumbbells: Set<Double> = emptySet(),
    val interests: Set<Interest> = emptySet(),
    val avoid: Set<AvoidArea> = emptySet(),
    val parq: Int = 0,
    val units: Units = Units.KG,
    val excluded: Set<String> = emptySet(),
    val logEffort: Boolean = false,
) {
    fun homeProfile() = EquipmentProfile(
        "Home", homeItems.filter { it != Equip.DUMBBELLS || dumbbells.isNotEmpty() }.toSet(),
        if (Equip.DUMBBELLS in homeItems) dumbbells.sorted() else emptyList(), units.machineStep,
    )

    fun toSettings() = UserSettings(
        name = name.trim(), goal = goal ?: Goal.GENERAL_FITNESS, experience = experience ?: Experience.BEGINNER,
        daysPerWeek = days, sessionMinutes = minutes,
        profiles = when (where ?: Where.GYM) {
            Where.GYM -> listOf(EquipmentProfile.gym(units))
            Where.HOME -> listOf(homeProfile())
            Where.BOTH -> listOf(EquipmentProfile.gym(units), homeProfile())
        },
        activeProfile = 0, interests = interests, avoid = avoid, excluded = excluded, units = units, parqFlags = parq,
        logEffort = logEffort,
    ).validated()

    companion object {
        fun from(s: UserSettings): Draft {
            val gym = s.profiles.any { Equip.GYM in it.items }
            val home = s.profiles.firstOrNull { Equip.GYM !in it.items }
            return Draft(
                s.name, s.goal, s.experience, s.daysPerWeek, s.sessionMinutes,
                when { gym && home != null -> Where.BOTH; gym -> Where.GYM; else -> Where.HOME },
                (home?.items ?: setOf(Equip.MAT)) + (if (home?.dumbbells?.isNotEmpty() == true) setOf(Equip.DUMBBELLS) else emptySet()),
                home?.dumbbells?.toSet() ?: emptySet(), s.interests, s.avoid, s.parqFlags, s.units, s.excluded, s.logEffort,
            )
        }
    }
}

private const val STEPS = 9

/**
 * First-run questions (and "Edit goals" later). Every step has Back; Next stays disabled until the
 * step is answered. Finishing builds the plan.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(repo: GymRepo, initial: UserSettings?, editing: Boolean, onDone: () -> Unit, onCancel: (() -> Unit)? = null) {
    var d by remember { mutableStateOf(initial?.let { Draft.from(it) } ?: Draft()) }
    var step by rememberSaveable { mutableIntStateOf(if (editing) 1 else 0) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val first = if (editing) 1 else 0

    val canNext = when (step) {
        1 -> d.goal != null
        2 -> d.experience != null
        4 -> d.where != null && (d.where == Where.GYM || Equip.DUMBBELLS !in d.homeItems || d.dumbbells.isNotEmpty())
        else -> true
    }

    Column(Modifier.fillMaxSize().background(Tokens.Bg).imePadding()) {
        LinearProgressIndicator(progress = { (step + 1f) / STEPS }, modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 0.dp), color = Tokens.Prime)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            when (step) {
                0 -> {
                    Text("Welcome to Fitness Gym", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Gap(8)
                    Text(
                        "A few quick questions and you'll get a workout plan built around your goal, your schedule and the equipment you have. You can change any answer later.",
                        fontSize = 16.sp, color = Tokens.TextDim,
                    )
                    Gap(20)
                    OutlinedTextField(
                        d.name, { d = d.copy(name = it.take(30)) }, label = { Text("Your first name (optional)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth().testTag("name_field"),
                    )
                }
                1 -> {
                    Title("What's your main goal?")
                    for (g in Goal.entries) ChoiceCard(g.label, g.blurb, d.goal == g, { d = d.copy(goal = g) })
                }
                2 -> {
                    Title("How much lifting experience do you have?")
                    for (x in Experience.entries) ChoiceCard(x.label, x.blurb, d.experience == x, { d = d.copy(experience = x) })
                }
                3 -> {
                    Title("How many days a week will you train?")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (n in 1..7) FilterChip(selected = d.days == n, onClick = { d = d.copy(days = n) }, label = { Text("$n") }, modifier = Modifier.testTag("days_$n"))
                    }
                    Text(splitText(d.days), color = Tokens.TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    Gap(20)
                    Title("How long is each workout?")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (m in listOf(20, 30, 45, 60, 75, 90)) FilterChip(selected = d.minutes == m, onClick = { d = d.copy(minutes = m) }, label = { Text("$m min") }, modifier = Modifier.testTag("mins_$m"))
                    }
                }
                4 -> {
                    Title("Where will you train?")
                    for (w in Where.entries) ChoiceCard(w.label, w.blurb, d.where == w, { d = d.copy(where = w) })
                    Gap(8)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Weights in", modifier = Modifier.weight(1f))
                        for (u in Units.entries) FilterChip(selected = d.units == u, onClick = { d = d.copy(units = u, dumbbells = emptySet()) }, label = { Text(u.label) }, modifier = Modifier.padding(start = 6.dp))
                    }
                    if (d.where == Where.HOME || d.where == Where.BOTH) {
                        Text("At home I have", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (e in listOf(Equip.MAT, Equip.DUMBBELLS, Equip.BENCH, Equip.PULLUP_BAR)) FilterChip(
                                selected = e in d.homeItems,
                                onClick = { d = d.copy(homeItems = if (e in d.homeItems) d.homeItems - e else d.homeItems + e) },
                                label = { Text(e.label) },
                            )
                        }
                        if (Equip.DUMBBELLS in d.homeItems) DumbbellPicker(d) { d = it }
                    }
                }
                5 -> {
                    Title("Add any of these?")
                    Text("They're added as short finishers at the end of your workouts. Fat-loss plans always get cardio.", color = Tokens.TextDim)
                    Gap(8)
                    for (i in Interest.entries) ChoiceCard(i.label, interestBlurb(i), i in d.interests, {
                        d = d.copy(interests = if (i in d.interests) d.interests - i else d.interests + i)
                    })
                }
                6 -> {
                    Title("Anything to protect?")
                    Text("Moves that load these areas are left out of your plan. Skip this if you're pain-free.", color = Tokens.TextDim)
                    Gap(8)
                    for (a in AvoidArea.entries) ChoiceCard(a.label, null, a in d.avoid, {
                        d = d.copy(avoid = if (a in d.avoid) d.avoid - a else d.avoid + a)
                    })
                }
                7 -> {
                    Title("Quick safety check")
                    Text("Based on the PAR-Q+ questionnaire. Your answers stay on your phone.", color = Tokens.TextDim)
                    for ((i, q) in PARQ.withIndex()) {
                        val yes = d.parq and (1 shl i) != 0
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(q, modifier = Modifier.weight(1f), fontSize = 15.sp)
                            FilterChip(selected = !yes, onClick = { d = d.copy(parq = d.parq and (1 shl i).inv()) }, label = { Text("No") }, modifier = Modifier.padding(start = 6.dp))
                            FilterChip(selected = yes, onClick = { d = d.copy(parq = d.parq or (1 shl i)) }, label = { Text("Yes") }, modifier = Modifier.padding(start = 6.dp).testTag("parq_yes_$i"))
                        }
                    }
                    if (d.parq != 0) Text(
                        "You answered yes to at least one question. Please check with your doctor or a qualified exercise professional before you increase your activity. You can still use the app, so start light.",
                        color = Tokens.Synergist, modifier = Modifier.padding(top = 10.dp).clip(RoundedCornerShape(12.dp)).background(Tokens.Surface).padding(12.dp).testTag("parq_warning"),
                    )
                }
                else -> {
                    Title(if (editing) "Ready to update your plan" else "Your plan is ready to build")
                    val s = d.toSettings()
                    SummaryLine("Goal", s.goal.label)
                    SummaryLine("Experience", s.experience.label)
                    SummaryLine("Schedule", "${s.daysPerWeek} days a week, ${s.sessionMinutes} min")
                    SummaryLine("Equipment", s.profiles.joinToString { p -> p.name + if (p.dumbbells.isNotEmpty()) " (dumbbells ${p.dumbbells.joinToString { Fmt.w(it) }} ${s.units.label})" else "" })
                    SummaryLine("Extras", s.interests.joinToString { it.label }.ifEmpty { "None" })
                    SummaryLine("Protecting", s.avoid.joinToString { it.label }.ifEmpty { "Nothing" })
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Log effort (reps in reserve)", fontWeight = FontWeight.SemiBold)
                            Text("Optional box on each set: how many more reps you could have done.", color = Tokens.TextDim, fontSize = 13.sp)
                        }
                        Switch(d.logEffort, { d = d.copy(logEffort = it) })
                    }
                    if (editing) Text("Your workout history is kept. Only the plan changes.", color = Tokens.TextDim, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > first) OutlinedButton(onClick = { step-- }, modifier = Modifier.testTag("onb_back")) { Text("Back") }
            else if (onCancel != null) TextButton(onClick = onCancel) { Text("Cancel") }
            Row(Modifier.weight(1f)) {}
            if (step < STEPS - 1) {
                PrimaryButton(if (step == 0) "Get started" else "Next", { step++ }, Modifier.width(170.dp), enabled = canNext, tag = "onb_next")
            } else {
                PrimaryButton(if (editing) "Save & rebuild" else "Build my plan", {
                    if (busy) return@PrimaryButton
                    busy = true
                    scope.launch {
                        if (editing) repo.updateSettings(d.toSettings()) else repo.completeOnboarding(d.toSettings())
                        onDone()
                    }
                }, Modifier.width(200.dp), enabled = !busy, tag = "build_plan")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DumbbellPicker(d: Draft, set: (Draft) -> Unit) {
    var custom by remember { mutableStateOf("") }
    Text("Which dumbbells do you own? (${d.units.label}, one pair each)", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val common = if (d.units == Units.KG) KG_WEIGHTS else LB_WEIGHTS
        for (w in (common + d.dumbbells).distinct().sorted()) FilterChip(
            selected = w in d.dumbbells,
            onClick = { set(d.copy(dumbbells = if (w in d.dumbbells) d.dumbbells - w else d.dumbbells + w)) },
            label = { Text(Fmt.w(w)) }, modifier = Modifier.testTag("db_${Fmt.w(w)}"),
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            custom, { custom = it.filter { c -> c.isDigit() || c == '.' }.take(5) }, label = { Text("Other weight") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.width(160.dp).testTag("db_custom"),
        )
        val v = custom.toDoubleOrNull()
        TextButton(onClick = { if (v != null && v > 0 && v <= 200) { set(d.copy(dumbbells = d.dumbbells + v)); custom = "" } }, enabled = v != null && v > 0 && v <= 200, modifier = Modifier.testTag("db_add")) { Text("Add") }
    }
    if (d.dumbbells.isEmpty()) Text("Pick at least one weight, or untick Dumbbells.", color = Tokens.Synergist, fontSize = 13.sp, modifier = Modifier.testTag("db_hint"))
}

@Composable
private fun Title(t: String) = Text(t, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))

@Composable
private fun SummaryLine(k: String, v: String) = Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    Text(k, color = Tokens.TextDim, modifier = Modifier.width(110.dp))
    Text(v, fontWeight = FontWeight.Medium)
}

private fun splitText(days: Int) = when (days) {
    1 -> "One full-body workout a week."
    2 -> "Two full-body workouts (A and B)."
    3 -> "Three full-body workouts (A, B, C)."
    4 -> "Upper / lower split, each twice a week."
    5 -> "Upper, lower, push, pull, legs."
    6 -> "Push / pull / legs, twice through."
    else -> "Push / pull / legs twice, plus an active-recovery day."
}

private fun interestBlurb(i: Interest) = when (i) {
    Interest.CARDIO -> "Intervals or a cardio machine to finish"
    Interest.PILATES -> "Mat Pilates for core and control"
    Interest.KICKBOXING -> "Punch and kick rounds for conditioning"
}
