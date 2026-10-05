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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunna.fitnessgym.anim.Activation
import com.nunna.fitnessgym.anim.Anatomy
import com.nunna.fitnessgym.anim.Camera
import com.nunna.fitnessgym.anim.FigureScene
import com.nunna.fitnessgym.anim.Library
import com.nunna.fitnessgym.data.ContentRepo
import com.nunna.fitnessgym.ui.FigureView
import com.nunna.fitnessgym.ui.theme.Tokens

/** Front + back body map. Tap a muscle (or a chip) to list the exercises that train it. */
@Composable
fun MusclesScreen(repo: ContentRepo, open: (String) -> Unit) {
    var group by rememberSaveable { mutableStateOf("chest") }
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    val stand = repo.motions.getValue("stand")
    val scene = remember(group) { FigureScene(stand, Activation(setOf(group), emptySet())) }
    val list = remember(group, tag) {
        repo.exercises.filter { group in it.primary && (tag == null || tag in it.tags) } +
            repo.exercises.filter { group in it.secondary && group !in it.primary && (tag == null || tag in it.tags) }
    }

    Column(Modifier.fillMaxSize().background(Tokens.Bg)) {
        Text("Muscles", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 0.dp))
        Row(Modifier.fillMaxWidth().height(300.dp)) {
            FigureView(scene, Camera(0.0, 4.0), Modifier.weight(1f).fillMaxSize(), playing = false, onTapMuscle = { group = it })
            FigureView(scene, Camera(180.0, 4.0), Modifier.weight(1f).fillMaxSize(), playing = false, onTapMuscle = { group = it })
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (g in Anatomy.GROUPS) FilterChip(selected = g == group, onClick = { group = g }, label = { Text(Anatomy.LABELS[g] ?: g) }, modifier = Modifier.testTag("muscle_$g"))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = tag == null, onClick = { tag = null }, label = { Text("All") })
            for (t in Library.TAGS) FilterChip(selected = tag == t, onClick = { tag = if (tag == t) null else t }, label = { Text(Library.TAG_LABELS[t]!!) })
        }
        Text("${list.size} exercises for ${Anatomy.LABELS[group]}", color = Tokens.TextDim, fontSize = 13.sp, modifier = Modifier.padding(16.dp, 6.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(list, key = { it.id }) { e ->
                Column(Modifier.fillMaxWidth().clickable { open(e.id) }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(e.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        (if (group in e.primary) "Main target" else "Helper") + " · " + e.tags.joinToString { Library.TAG_LABELS[it] ?: it },
                        color = if (group in e.primary) Tokens.Prime else Tokens.Synergist, fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
