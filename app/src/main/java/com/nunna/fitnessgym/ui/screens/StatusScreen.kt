package com.nunna.fitnessgym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import com.nunna.fitnessgym.BuildConfigInfo
import com.nunna.fitnessgym.anim.Library
import com.nunna.fitnessgym.data.ContentRepo
import com.nunna.fitnessgym.ui.theme.Tokens

/** Health screen: version, content counts, and any content problems with their error codes. */
@Composable
fun StatusScreen(repo: ContentRepo) {
    val animated = repo.exercises.count { it.motion in repo.motions }
    Column(Modifier.fillMaxSize().background(Tokens.Bg).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Status", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Line("Version", BuildConfigInfo.versionName)
        Line("Exercises", "${repo.exercises.size}")
        for (t in Library.TAGS) Line("  ${Library.TAG_LABELS[t]}", "${repo.exercises.count { t in it.tags }}")
        Line("On the mat", "${repo.exercises.count { "mat" in it.tags }}")
        Line("Animations", "${repo.motions.size} templates")
        Line("Exercises animated", "$animated / ${repo.exercises.size}")
        Text(
            if (repo.errors.isEmpty()) "✓ All content loaded" else "Problems",
            color = if (repo.errors.isEmpty()) Tokens.Mat else Tokens.Synergist,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp),
        )
        for (e in repo.errors) Text(e, fontSize = 13.sp, color = Tokens.TextDim, modifier = Modifier.padding(vertical = 4.dp))
        Text("Credits", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp))
        Text(
            "Exercise metadata and instructions: free-exercise-db (Unlicense / public domain). " +
                "Figure, animations, cues and Pilates/kickboxing/cardio content: Fitness Gym (AGPL-3.0).",
            fontSize = 13.sp, color = Tokens.TextDim,
        )
    }
}

@Composable
private fun Line(k: String, v: String) {
    androidx.compose.foundation.layout.Row(Modifier.padding(vertical = 3.dp)) {
        Text(k, modifier = Modifier.weight(1f), color = Tokens.TextDim)
        Text(v, fontWeight = FontWeight.SemiBold)
    }
}
