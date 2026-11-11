package com.example.personalassistant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.personalassistant.ui.theme.*

// One consistent outline icon family, with no additional icon dependency.
enum class Glyph(val path: String) {
    Home("M3 10 L12 3 L21 10 M5 9 L5 21 L10 21 L10 15 L14 15 L14 21 L19 21 L19 9"),
    Mail("M3 5 L21 5 L21 19 L3 19 Z M3 6 L12 13 L21 6"),
    Food("M5 3 L5 10 M2 3 L2 7 Q2 10 5 10 Q8 10 8 7 L8 3 M5 10 L5 21 M17 3 Q12 8 17 12 L20 12 M20 3 L20 21"),
    Spark("M12 3 L14.5 9.5 L21 12 L14.5 14.5 L12 21 L9.5 14.5 L3 12 L9.5 9.5 Z M20 2 L20 6 M18 4 L22 4"),
    Back("M15 5 L8 12 L15 19"),
    Next("M9 5 L16 12 L9 19"),
    Search("M17 10 A7 7 0 1 1 3 10 A7 7 0 1 1 17 10 M15 15 L21 21"),
    Plus("M12 5 L12 19 M5 12 L19 12"),
    Arrow("M12 19 L12 5 M6 11 L12 5 L18 11"),
    Send("M3 10 L21 3 L14 21 L10 14 Z M10 14 L21 3"),
    Check("M5 12 L10 17 L20 7"),
    Clock("M21 12 A9 9 0 1 1 3 12 A9 9 0 1 1 21 12 M12 7 L12 12 L16 14"),
    Archive("M3 4 L21 4 L21 9 L3 9 Z M5 9 L5 21 L19 21 L19 9 M9 13 L15 13"),
    Refresh("M20 7 A9 9 0 1 0 21 15 M20 2 L20 8 L14 8"),
    Save("M4 3 L17 3 L21 7 L21 21 L3 21 L3 3 Z M8 3 L8 9 L16 9 L16 3 M7 21 L7 14 L17 14 L17 21"),
    Sun("M16 12 A4 4 0 1 1 8 12 A4 4 0 1 1 16 12 M12 2 L12 4 M12 20 L12 22 M2 12 L4 12 M20 12 L22 12 M5 5 L6 6 M18 18 L19 19 M5 19 L6 18 M18 6 L19 5"),
    Moon("M20 15 A9 9 0 1 1 9 3 A8 8 0 0 0 20 15"),
    Cup("M5 7 L19 7 L18 21 L6 21 Z M8 3 L9 5 M13 2 L14 5 M19 10 Q24 10 21 15 L19 15"),
    Eye("M2 12 C5 7.5 8.5 5 12 5 C15.5 5 19 7.5 22 12 C19 16.5 15.5 19 12 19 C8.5 19 5 16.5 2 12 Z M15 12 A3 3 0 1 1 9 12 A3 3 0 1 1 15 12"),
    EyeOff("M2 12 C5 7.5 8.5 5 12 5 C15.5 5 19 7.5 22 12 C19 16.5 15.5 19 12 19 C8.5 19 5 16.5 2 12 Z M4 4 L20 20"),
    Close("M6 6 L18 18 M18 6 L6 18")
}

@Composable
fun AppIcon(glyph: Glyph, modifier: Modifier = Modifier, tint: Color = Ink, description: String? = null) {
    val vector = remember(glyph) {
        ImageVector.Builder(glyph.name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(PathParser().parsePathString(glyph.path).toNodes(), stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.65f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }.build()
    }
    Icon(vector, description, modifier.size(24.dp), tint)
}

fun mealGlyph(meal: String) = when (meal) {
    "早餐" -> Glyph.Sun; "午餐" -> Glyph.Food; "晚餐" -> Glyph.Moon; else -> Glyph.Cup
}

@Composable
fun PageColumn(modifier: Modifier = Modifier, spacing: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        .padding(top = 12.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
}

@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = SoftGray, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = color, contentColor = Ink) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun PageHeading(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        action?.invoke()
    }
}

@Composable
fun SectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        if (action != null) TextButton(onClick = onAction) { Text(action, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
fun Badge(text: String, green: Boolean = false) {
    Surface(shape = RoundedCornerShape(7.dp), color = if (green) SoftGreen else Color.White) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = if (green) Green else Muted,
            style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun SegmentedControl(options: List<String>, selected: String, onSelect: (String) -> Unit,
    modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().background(SoftGray, RoundedCornerShape(9.dp)).padding(2.dp)) {
        options.forEach { option ->
            val active = option == selected
            Surface(onClick = { onSelect(option) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(7.dp),
                color = if (active) Color.White else Color.Transparent,
                contentColor = if (active) Ink else Muted) {
                Text(option, Modifier.fillMaxWidth().padding(vertical = 5.dp), textAlign = TextAlign.Center,
                    maxLines = 1, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun IconTile(glyph: Glyph, green: Boolean = false, size: Int = 44) {
    Surface(Modifier.size(size.dp), shape = RoundedCornerShape(14.dp), color = if (green) SoftGreen else SoftGray) {
        Box(contentAlignment = Alignment.Center) { AppIcon(glyph, tint = if (green) Green else Ink) }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, glyph: Glyph? = null, enabled: Boolean = true) {
    Button(onClick, modifier.heightIn(min = 52.dp), enabled = enabled, shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
        if (glyph != null) { AppIcon(glyph, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(8.dp)) }
        Text(text)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, glyph: Glyph? = null) {
    OutlinedButton(onClick, modifier.heightIn(min = 50.dp), shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Line), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)) {
        if (glyph != null) { AppIcon(glyph, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)) }
        Text(text, color = Ink, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AiNote(title: String, body: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = SoftGreen, contentColor = Ink, border = BorderStroke(1.dp, Green.copy(alpha = .12f))) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIcon(Glyph.Spark, tint = Green)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = Green, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun EntryField(value: String, onChange: (String) -> Unit, label: String, placeholder: String,
    modifier: Modifier = Modifier, minLines: Int = 1, singleLine: Boolean = false, error: Boolean = false,
    supporting: String? = null) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(value, onChange, Modifier.fillMaxWidth().testTag("field_$label"), placeholder = { Text(placeholder) },
            minLines = minLines, singleLine = singleLine, isError = error,
            shape = RoundedCornerShape(16.dp), textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = SoftGray, focusedContainerColor = SoftGray,
                unfocusedBorderColor = Color.Transparent, focusedBorderColor = Green
            ))
        if (supporting != null) Text(supporting, style = MaterialTheme.typography.bodySmall,
            color = if (error) MaterialTheme.colorScheme.error else Muted)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun EmptyState(title: String, description: String? = null, action: String? = null,
    onClick: () -> Unit = {}, glyph: Glyph = Glyph.Mail) {
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        IconTile(glyph, size = 56)
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (description != null) Text(description, color = Muted, style = MaterialTheme.typography.bodyMedium)
        if (action != null) SecondaryButton(action, onClick)
    }
}

@Composable
fun Avatar(text: String) {
    Surface(Modifier.size(42.dp), CircleShape, color = SoftGreen) {
        Box(contentAlignment = Alignment.Center) {
            Text(text.take(1), color = Green, fontWeight = FontWeight.SemiBold)
        }
    }
}
