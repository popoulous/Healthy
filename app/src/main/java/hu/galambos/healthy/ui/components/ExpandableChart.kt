package hu.galambos.healthy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import hu.galambos.healthy.R

/**
 * A chart with a way to see it bigger.
 *
 * A phone-width chart of a whole night is a few pixels per minute, which is
 * enough to read the shape and not enough to read a five-minute stage. Rather
 * than compromise the card by making it taller, the same chart is offered
 * full-screen — the drawing is identical, it simply gets the room.
 *
 * [chart] is passed whether it is being drawn expanded, so a chart that wants
 * to be taller when it has the space can be, without any of its callers
 * knowing there are two sizes.
 */
@Composable
fun ExpandableChart(
    title: String,
    modifier: Modifier = Modifier,
    chart: @Composable (expanded: Boolean) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier.fillMaxWidth()) {
        chart(false)
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_expand),
                contentDescription = stringResource(R.string.chart_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }

    if (expanded) {
        Dialog(
            onDismissRequest = { expanded = false },
            // The platform's default dialog width would defeat the point.
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = title, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { expanded = false }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.chart_collapse),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        chart(true)
                    }
                }
            }
        }
    }
}
