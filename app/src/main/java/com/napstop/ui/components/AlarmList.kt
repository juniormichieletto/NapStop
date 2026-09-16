package com.napstop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.napstop.ui.model.LocationAlarmUiModel

/**
 * List container displaying saved location alarms according to Phase 1:
 * - Replaces "Saved Stops" with "Location alarms" header and count indicator
 * - Displays elevated Material 3 alarm cards with spaced margins
 */
@Composable
fun AlarmList(
    alarms: List<LocationAlarmUiModel>,
    onAlarmSelected: (LocationAlarmUiModel) -> Unit,
    onAlarmEdit: (LocationAlarmUiModel) -> Unit,
    onAlarmTogglePause: (LocationAlarmUiModel) -> Unit,
    onAlarmDeleted: (LocationAlarmUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = alarms.isNotEmpty(), modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            // Header: "Location alarms" + count pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Location alarms",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${alarms.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmListItem(
                        alarm = alarm,
                        onClick = { onAlarmSelected(alarm) },
                        onEdit = { onAlarmEdit(alarm) },
                        onTogglePause = { onAlarmTogglePause(alarm) },
                        onDelete = { onAlarmDeleted(alarm) }
                    )
                }
            }
        }
    }
}
