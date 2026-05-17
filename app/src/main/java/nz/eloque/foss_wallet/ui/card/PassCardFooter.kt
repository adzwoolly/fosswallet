package nz.eloque.foss_wallet.ui.card

import android.content.ActivityNotFoundException
import android.content.Intent
import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import nz.eloque.foss_wallet.R
import nz.eloque.foss_wallet.model.LocalizedPassWithTags
import nz.eloque.foss_wallet.model.PassRelevantDate
import nz.eloque.foss_wallet.model.Tag
import nz.eloque.foss_wallet.ui.components.CalendarButton
import nz.eloque.foss_wallet.ui.components.ChipRow
import nz.eloque.foss_wallet.ui.components.LocationButton
import nz.eloque.foss_wallet.ui.components.tag.TagChooser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassCardFooter(
    localizedPass: LocalizedPassWithTags,
    allTags: Set<Tag>,
    onTagClick: (Tag) -> Unit = {},
    onTagAdd: (Tag) -> Unit = {},
    onTagCreate: (Tag) -> Unit = {},
    readOnly: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val pass = localizedPass.pass
        val tags = localizedPass.tags

        var tagChooserShown by remember { mutableStateOf(false) }

        if (pass.relevantDates.any { it is PassRelevantDate.DateInterval }) {
            val interval: PassRelevantDate.DateInterval =
                pass.relevantDates.filter {
                    it is PassRelevantDate.DateInterval
                }[0] as PassRelevantDate.DateInterval
            CalendarButton(
                title = pass.description,
                start = interval.startDate,
                end = interval.endDate,
            )
        } else if (pass.relevantDates.any { it is PassRelevantDate.Date }) {
            val date: PassRelevantDate.Date =
                pass.relevantDates.filter {
                    it is PassRelevantDate.Date
                }[0] as PassRelevantDate.Date
            CalendarButton(
                title = pass.description,
                start = date.date,
                end = pass.expirationDate,
            )
        }
        MultiLocationButton(locations = pass.locations)

        Spacer(modifier = Modifier.width(8.dp))

        val chipColors = FilterChipDefaults.filterChipColors()
        ChipRow(
            options = tags,
            onOptionClick = {
                if (!readOnly) {
                    onTagClick(it)
                }
            },
            optionLabel = { it.label },
            optionColors = {
                val contentColor = it.contentColor()
                chipColors.copy(
                    containerColor = it.color,
                    labelColor = contentColor,
                    leadingIconColor = contentColor,
                )
            },
            trailingIcon = {
                if (!readOnly) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.remove_tag),
                        tint = it.contentColor(),
                    )
                }
            },
            modifier = Modifier.weight(1f),
        )

        if (!readOnly) {
            IconButton(onClick = {
                tagChooserShown = true
            }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_tag),
                )
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }

        if (tagChooserShown) {
            ModalBottomSheet(onDismissRequest = {
                tagChooserShown = false
            }) {
                TagChooser(
                    tags = allTags.minus(localizedPass.tags),
                    onSelected = {
                        onTagAdd(it)
                        tagChooserShown = false
                    },
                    onTagCreate = onTagCreate,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiLocationButton(locations: List<Location>) {
    if (locations.isEmpty()) return

    val context = LocalContext.current
    var pickerShown by remember { mutableStateOf(false) }

    fun openMap(location: Location) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).also {
                    it.data = "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}".toUri()
                },
            )
        } catch (e: ActivityNotFoundException) {
            Log.e("LocationButton", "No map app found!", e)
        }
    }

    IconButton(onClick = {
        if (locations.size == 1) openMap(locations.first()) else pickerShown = true
    }) {
        Icon(imageVector = Icons.Default.LocationOn, contentDescription = stringResource(R.string.pass_location))
    }

    if (pickerShown) {
        ModalBottomSheet(onDismissRequest = { pickerShown = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.pass_location),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                locations.forEach { location ->
                    val label = location.extras?.getString("relevantText")
                        ?: "%.6f, %.6f".format(location.latitude, location.longitude)
                    TextButton(
                        onClick = { openMap(location); pickerShown = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(text = label, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
