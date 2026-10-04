package com.nunna.fitnessgym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunna.fitnessgym.anim.Anatomy
import com.nunna.fitnessgym.anim.Camera
import com.nunna.fitnessgym.anim.FigureScene
import com.nunna.fitnessgym.anim.Library
import com.nunna.fitnessgym.data.ContentRepo
import com.nunna.fitnessgym.ui.FigureView
import com.nunna.fitnessgym.ui.theme.Tokens
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseScreen(repo: ContentRepo, id: String, back: () -> Unit) {
    val e = repo.exercise(id)
    if (e == null) {
        Text("CNT-004: exercise '$id' not found. Fix: go back and pick it again.", Modifier.padding(16.dp))
        return
    }
    val motion = repo.motionFor(e)
    val scene = remember(e.id) { FigureScene(motion, e.activation()) }
    val views = listOf(
        "Coach" to scene.defaultCamera(),
        "Front" to Camera(0.0, 4.0),
        "Side" to Camera(-90.0, 4.0),
        "Back" to Camera(180.0, 4.0),
    )
    var view by rememberSaveable { mutableStateOf(0) }
    var playing by rememberSaveable { mutableStateOf(true) }
    var slow by rememberSaveable { mutableStateOf(false) }
    var tapped by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(tapped) { if (tapped != null) { delay(2500); tapped = null } }

    Column(Modifier.fillMaxSize().background(Tokens.Bg).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(e.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }

        Box(Modifier.fillMaxWidth().height(380.dp).padding(horizontal = 12.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF0C0F15))) {
            FigureView(
                scene, views[view].second, Modifier.fillMaxSize(),
                playing = playing, speed = if (slow) 0.5 else 1.0,
                onTapMuscle = { tapped = it },
            )
            tapped?.let { g ->
                val role = when (g) { in e.primary -> "working"; in e.secondary -> "helping"; else -> "not targeted" }
                Text(
                    "${Anatomy.LABELS[g] ?: g} · $role",
                    modifier = Modifier.align(Alignment.TopCenter).padding(10.dp)
                        .clip(RoundedCornerShape(10.dp)).background(Tokens.SurfaceHi).padding(horizontal = 10.dp, vertical = 5.dp),
                    fontSize = 14.sp,
                )
            }
            if (motion.template.id == "stand" && e.motion != "stand") {
                Text("Animation coming soon", color = Tokens.TextDim, fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp))
            }
        }

        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(onClick = { playing = !playing }) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pause" else "Play")
            }
            FilterChip(selected = slow, onClick = { slow = !slow }, label = { Text("½×") })
            for ((i, v) in views.withIndex()) FilterChip(selected = view == i, onClick = { view = i }, label = { Text(v.first) })
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            MuscleLegend("Working", e.primary, Tokens.Prime)
            if (e.secondary.isNotEmpty()) MuscleLegend("Helping", e.secondary, Tokens.Synergist)
            Text("Tap a muscle on the figure to name it.", color = Tokens.TextDim, fontSize = 12.sp)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                for (t in e.tags) AssistChip(onClick = {}, label = { Text(Library.TAG_LABELS[t] ?: t) })
                AssistChip(onClick = {}, label = { Text(e.level) })
                if (e.unilateral) AssistChip(onClick = {}, label = { Text("one side at a time") })
            }

            Section("Coaching cues", e.cues, bullet = "✓")
            Section("Common mistakes", e.mistakes, bullet = "✗")
            Section("How to", e.instructions, numbered = true)
            if (e.source == "free-exercise-db") {
                Text("Exercise data: free-exercise-db (public domain).", color = Tokens.TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MuscleLegend(label: String, groups: List<String>, color: Color) {
    FlowRow(verticalArrangement = Arrangement.Center, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 3.dp)) {
        Text(label, color = Tokens.TextDim, fontSize = 14.sp, modifier = Modifier.width(64.dp))
        for (g in groups) Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(4.dp))
            Text(Anatomy.LABELS[g] ?: g, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun Section(title: String, lines: List<String>, bullet: String = "•", numbered: Boolean = false) {
    if (lines.isEmpty()) return
    Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
    for ((i, l) in lines.withIndex()) Row(Modifier.padding(vertical = 2.dp)) {
        Text(if (numbered) "${i + 1}." else bullet, color = Tokens.TextDim, modifier = Modifier.width(24.dp))
        Text(l, fontSize = 15.sp)
    }
}
