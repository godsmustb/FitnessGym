package com.nunna.fitnessgym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunna.fitnessgym.anim.Anatomy
import com.nunna.fitnessgym.anim.Exercise
import com.nunna.fitnessgym.anim.Library
import com.nunna.fitnessgym.core.Classify
import com.nunna.fitnessgym.core.Generator
import com.nunna.fitnessgym.core.UserSettings
import com.nunna.fitnessgym.data.GymRepo
import com.nunna.fitnessgym.ui.theme.Tokens
import kotlinx.coroutines.launch

/** Adds an exercise to the current workout ([swapSeId] = 0) or swaps one (equipment-aware suggestions first). */
@Composable
fun PickerScreen(repo: GymRepo, swapSeId: Long, onDone: () -> Unit) {
    val active by repo.activeFlow.collectAsState(null)
    val settings by repo.settingsFlow.collectAsState(null)
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf<String?>(null) }
    var mineOnly by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    val s = settings ?: UserSettings()
    val swapping = active?.exercises?.firstOrNull { it.ex.id == swapSeId }
    val swapEx = swapping?.let { repo.exercise(it.ex.exerciseId) }
    val inWorkout = active?.exercises?.map { it.ex.exerciseId }?.toSet() ?: emptySet()

    val suggested: List<Exercise> = remember(swapEx?.id, s) { swapEx?.let { Generator.substitutes(it, repo.exercises, s) } ?: emptyList() }
    val all = remember(query, tag, mineOnly, s) {
        repo.exercises.filter { e ->
            (tag == null || tag in e.tags) && (!mineOnly || Classify.available(e, s.profile)) && e.id !in s.excluded &&
                (query.isBlank() || e.name.contains(query.trim(), true) || (e.primary).any { (Anatomy.LABELS[it] ?: it).contains(query.trim(), true) })
        }
    }

    fun choose(ex: Exercise) {
        if (busy) return
        val a = active ?: return
        busy = true
        scope.launch {
            if (swapSeId != 0L) repo.swapExercise(swapSeId, ex.id) else repo.addExercise(a.session.id, ex.id)
            onDone()
        }
    }

    // Wait for the workout + settings before drawing the list; otherwise the "Best swaps" rows arrive
    // later and get inserted above the list's scroll anchor, out of sight.
    if (settings == null || active == null || (swapSeId != 0L && swapEx == null)) {
        Column(Modifier.fillMaxSize().background(Tokens.Bg)) {}
        return
    }

    Column(Modifier.fillMaxSize().background(Tokens.Bg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(if (swapEx != null) "Swap ${swapEx.name}" else "Add exercise", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(query, { query = it }, singleLine = true, placeholder = { Text("Search name or muscle") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("pick_search"))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = mineOnly, onClick = { mineOnly = !mineOnly }, label = { Text("My equipment") }, modifier = Modifier.testTag("pick_mine"))
            for (t in Library.TAGS) FilterChip(selected = tag == t, onClick = { tag = if (tag == t) null else t }, label = { Text(Library.TAG_LABELS[t]!!) })
        }
        LazyColumn(Modifier.fillMaxSize().testTag("pick_list")) {
            if (suggested.isNotEmpty() && query.isBlank()) {
                item { Text("Best swaps", fontWeight = FontWeight.Bold, color = Tokens.Prime, modifier = Modifier.padding(16.dp, 8.dp)) }
                items(suggested, key = { "s_" + it.id }) { e -> PickRow(e, e.id in inWorkout) { choose(e) } }
                item { Text("All exercises", fontWeight = FontWeight.Bold, color = Tokens.TextDim, modifier = Modifier.padding(16.dp, 8.dp)) }
            }
            items(all, key = { it.id }) { e -> PickRow(e, e.id in inWorkout) { choose(e) } }
            if (all.isEmpty()) item { Text("No matches. Try turning off \"My equipment\".", color = Tokens.TextDim, modifier = Modifier.padding(16.dp).testTag("pick_empty")) }
        }
    }
}

@Composable
private fun PickRow(e: Exercise, already: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp).testTag("pick_${e.id}")) {
        Text(e.name + if (already) "  (in workout)" else "", fontWeight = FontWeight.SemiBold)
        Text(e.primary.joinToString { Anatomy.LABELS[it] ?: it } + " · " + e.equipment.replace('_', ' '), color = Tokens.TextDim, fontSize = 12.sp)
    }
}
