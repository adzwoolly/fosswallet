package nz.eloque.foss_wallet.ui.card

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nz.eloque.foss_wallet.R
import nz.eloque.foss_wallet.model.LocalizedPassWithTags
import nz.eloque.foss_wallet.model.PassColors
import nz.eloque.foss_wallet.model.Tag
import nz.eloque.foss_wallet.ui.components.SelectionIndicator

@Composable
fun ShortPassCard(
    pass: LocalizedPassWithTags,
    allTags: Set<Tag>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit,
    selected: Boolean = false,
    isNearby: Boolean = false,
) {
    val cardColors = passCardColors(pass.pass.colors)
    val scale by animateFloatAsState(if (selected) 0.95f else 1f)

    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        ElevatedCard(
            colors = cardColors,
            modifier =
                modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    ),
        ) {
            if (isNearby) {
                NearbyBadgeStrip()
            }

            ShortPassContent(
                localizedPass = pass,
            )

            PassCardFooter(
                localizedPass = pass,
                allTags = allTags,
                readOnly = true,
            )
        }
        if (selected) {
            SelectionIndicator(Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun NearbyBadgeStrip() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.pass_nearby),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
fun PassCard(
    localizedPass: LocalizedPassWithTags,
    allTags: Set<Tag>,
    onTagClick: (Tag) -> Unit,
    onTagAdd: (Tag) -> Unit,
    onTagCreate: (Tag) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    selected: Boolean = false,
    content: @Composable () -> Unit = {},
) {
    val pass = localizedPass.pass

    val cardColors = passCardColors(pass.colors)
    val scale by animateFloatAsState(if (selected) 0.95f else 1f)

    ElevatedCard(
        colors = cardColors,
        modifier =
            modifier
                .fillMaxWidth()
                .scale(scale)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
    ) {
        PassContent(
            localizedPass = localizedPass,
            content = content,
        )

        PassCardFooter(
            localizedPass = localizedPass,
            allTags = allTags,
            onTagClick = onTagClick,
            onTagAdd = onTagAdd,
            onTagCreate = onTagCreate,
        )
    }
}

@Composable
fun passCardColors(passColors: PassColors?): CardColors =
    passColors?.toCardColors()
        ?: CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )

@Preview
@Composable
private fun PasscardPreview() {
    PassCard(
        localizedPass = LocalizedPassWithTags.placeholder(),
        allTags = setOf(Tag("Tag 1", Color(0, 0, 0)), Tag("Tag 2", Color(100, 100, 100))),
        onTagClick = {},
        onTagAdd = {},
        onTagCreate = {},
    ) {}
}
