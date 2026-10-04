package com.nunna.fitnessgym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunna.fitnessgym.anim.Anatomy
import com.nunna.fitnessgym.anim.Exercise
import com.nunna.fitnessgym.anim.FigureScene
import com.nunna.fitnessgym.anim.Library
import com.nunna.fitnessgym.data.ContentRepo
import com.nunna.fitnessgym.ui.FigureView
import com.nunna.fitnessgym.ui.theme.Tokens

@Composable
fun LibraryScreen(repo: ContentRepo, open: (String) -> Unit) {
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(tag, query) {
        repo.exercises.filter { e ->
            (tag == null || tag in e.tags) &&
                (query.isBlank() || e.name.contains(query.trim(), true) ||
                    (e.primary + e.secondary).any { (Anatomy.LABELS[it] ?: it).contains(query.trim(), true) })
        }
    }

    Column(Modifier.fillMaxSize().background(Tokens.Bg)) {
        Text("Exercises", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            placeholder = { Text("Search name or muscle") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tag == null, onClick = { tag = null }, label = { Text("All ${repo.exercises.size}") })
            for (t in Library.TAGS) {
                val n = repo.exercises.count { t in it.tags }
                FilterChip(selected = tag == t, onClick = { tag = if (tag == t) null else t }, label = { Text("${Library.TAG_LABELS[t]} $n") })
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(shown, key = { it.id }) { e -> ExerciseRow(repo, e) { open(e.id) } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ExerciseRow(repo: ContentRepo, e: Exercise, onClick: () -> Unit) {
    val scene = remember(e.id) { FigureScene(repo.motionFor(e), e.activation()) }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FigureView(
            scene, scene.defaultCamera(), playing = false, staticPhase = 0.5,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Tokens.Surface),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                e.primary.joinToString { Anatomy.LABELS[it] ?: it },
                color = Tokens.Prime, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(e.equipment.replace('_', ' '), e.level, if ("mat" in e.tags) "mat" else null).joinToString(" · "),
                color = Tokens.TextDim, fontSize = 12.sp,
            )
        }
    }
}
