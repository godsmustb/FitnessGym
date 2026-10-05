package com.nunna.fitnessgym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunna.fitnessgym.data.Summary
import com.nunna.fitnessgym.ui.Fmt
import com.nunna.fitnessgym.ui.Gap
import com.nunna.fitnessgym.ui.PrimaryButton
import com.nunna.fitnessgym.ui.SectionTitle
import com.nunna.fitnessgym.ui.StatTile
import com.nunna.fitnessgym.ui.theme.Tokens

@Composable
fun SummaryScreen(summary: Summary?, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Tokens.Bg).verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (summary == null) {
            Text("Workout closed. Nothing was logged, so nothing was saved.", fontSize = 18.sp, modifier = Modifier.testTag("summary_empty"))
            Gap(16)
            PrimaryButton("Back to Today", onDone, tag = "summary_done")
            return@Column
        }
        Text("Workout done! 💪", fontSize = 30.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("summary_title"))
        Text(summary.title, fontSize = 18.sp, color = Tokens.TextDim)
        Gap(14)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("minutes", "${summary.minutes}", Modifier.weight(1f))
            StatTile("sets", "${summary.sets}", Modifier.weight(1f).testTag("summary_sets"))
            StatTile("volume ${summary.unit.label}", Fmt.volume(summary.volume), Modifier.weight(1f))
        }
        Gap(8)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("XP earned", "+${summary.xp}", Modifier.weight(1f).testTag("summary_xp"))
            StatTile(if (summary.levelAfter > summary.levelBefore) "LEVEL UP!" else "level", "Lv ${summary.levelAfter}", Modifier.weight(1f))
        }
        if (summary.records.isNotEmpty()) {
            SectionTitle("Personal records 🏆")
            for ((ex, what) in summary.records) Text("$ex: $what", color = Tokens.Synergist, modifier = Modifier.padding(vertical = 3.dp).testTag("summary_pr"))
        }
        Gap(10)
        Text("Next time, the app suggests your weights from today's sets.", color = Tokens.TextDim)
        Gap(20)
        PrimaryButton("Done", onDone, tag = "summary_done")
    }
}
