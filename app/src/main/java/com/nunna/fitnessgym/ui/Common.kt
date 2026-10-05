package com.nunna.fitnessgym.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunna.fitnessgym.core.Progression
import com.nunna.fitnessgym.ui.theme.Tokens

/** A large tappable choice card (onboarding answers). */
@Composable
fun ChoiceCard(title: String, blurb: String?, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) Tokens.Prime.copy(alpha = 0.16f) else Tokens.Surface)
            .border(BorderStroke(2.dp, if (selected) Tokens.Prime else Tokens.SurfaceHi), RoundedCornerShape(16.dp))
            .semantics { this.selected = selected; role = Role.RadioButton }
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (blurb != null) Text(blurb, fontSize = 14.sp, color = Tokens.TextDim)
        }
        if (selected) Icon(Icons.Filled.CheckCircle, null, tint = Tokens.Prime)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, tag: String? = null) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp).let { if (tag != null) it.testTag(tag) else it },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Tokens.Prime, contentColor = Color.White),
    ) { Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = modifier.padding(top = 16.dp, bottom = 6.dp))

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}.clip(RoundedCornerShape(14.dp)).background(Tokens.Surface).padding(12.dp)) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, color = Tokens.TextDim)
    }
}

@Composable
fun Pill(text: String, color: Color = Tokens.SurfaceHi, textColor: Color = Tokens.Text) {
    Text(
        text, fontSize = 12.sp, color = textColor,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun Gap(h: Int = 12) = Spacer(Modifier.height(h.dp))

@Composable
fun HGap(w: Int = 8) = Spacer(Modifier.width(w.dp))

@Composable
fun Dot(color: Color, size: Int = 8) = Spacer(Modifier.size(size.dp).clip(RoundedCornerShape(50)).background(color))

@Composable
fun RowSpaced(content: @Composable () -> Unit) =
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { content() }

object Fmt {
    fun w(v: Double?) = v?.let { Progression.fmt(it) } ?: ""
    fun secs(s: Int) = Progression.secText(s)
    fun clock(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)
    fun volume(v: Double) = if (v >= 10000) "%.1fk".format(v / 1000) else v.toInt().toString()
}
