package io.github.anshireminder.app.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.anshireminder.app.R

@Composable
fun PillGuideScreen(modifier: Modifier = Modifier) {
    var isOver48Hours by remember { mutableStateOf(false) }
    var selectedWeek by remember { mutableIntStateOf(1) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScopeBanner()
        }

        item {
            Text(
                text = stringResource(R.string.guide_quick_help),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.guide_how_late),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TimeChoiceButton(
                        text = stringResource(R.string.guide_under_48),
                        selected = !isOver48Hours,
                        onClick = { isOver48Hours = false },
                        modifier = Modifier.weight(1f),
                    )
                    TimeChoiceButton(
                        text = stringResource(R.string.guide_over_48),
                        selected = isOver48Hours,
                        onClick = { isOver48Hours = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (isOver48Hours) {
            item {
                WeekSelector(selectedWeek = selectedWeek, onWeekSelected = { selectedWeek = it })
            }
        }

        item {
            ActionPanel(isOver48Hours = isOver48Hours, selectedWeek = selectedWeek)
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = stringResource(R.string.guide_more_answers),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            GuideAnswer(
                question = stringResource(R.string.guide_start_question),
                answer = stringResource(R.string.guide_start_answer),
            )
        }
        item {
            GuideAnswer(
                question = stringResource(R.string.guide_late_pack_question),
                answer = stringResource(R.string.guide_late_pack_answer),
            )
        }
        item {
            GuideAnswer(
                question = stringResource(R.string.guide_sickness_question),
                answer = stringResource(R.string.guide_sickness_answer),
            )
        }
        item {
            MedicalNotice()
        }
    }
}

@Composable
private fun ScopeBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.guide_scope_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.guide_scope_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TimeChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
    val buttonModifier = modifier.heightIn(min = 48.dp)
    if (selected) {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            contentPadding = contentPadding,
        ) { Text(text) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            contentPadding = contentPadding,
        ) { Text(text) }
    }
}

@Composable
private fun WeekSelector(selectedWeek: Int, onWeekSelected: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.guide_which_week),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (1..3).forEach { week ->
                val label = when (week) {
                    1 -> stringResource(R.string.guide_week_1)
                    2 -> stringResource(R.string.guide_week_2)
                    else -> stringResource(R.string.guide_week_3)
                }
                TimeChoiceButton(
                    text = label,
                    selected = selectedWeek == week,
                    onClick = { onWeekSelected(week) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ActionPanel(isOver48Hours: Boolean, selectedWeek: Int) {
    val title: String
    val body: String
    val urgent: Boolean

    if (!isOver48Hours) {
        title = stringResource(R.string.guide_under_48_title)
        body = stringResource(R.string.guide_under_48_action)
        urgent = false
    } else {
        title = stringResource(R.string.guide_over_48_title)
        body = when (selectedWeek) {
            1 -> stringResource(R.string.guide_week_1_action)
            2 -> stringResource(R.string.guide_week_2_action)
            else -> stringResource(R.string.guide_week_3_action)
        }
        urgent = selectedWeek == 1 || selectedWeek == 3
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (urgent) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (urgent) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun GuideAnswer(question: String, answer: String) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MedicalNotice() {
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.guide_notice_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.guide_notice_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.guide_sources),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
