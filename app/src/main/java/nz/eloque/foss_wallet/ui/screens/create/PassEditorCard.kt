package nz.eloque.foss_wallet.ui.screens.create

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import nz.eloque.foss_wallet.R
import nz.eloque.foss_wallet.model.PassColors
import nz.eloque.foss_wallet.model.PassType
import nz.eloque.foss_wallet.model.TransitType
import nz.eloque.foss_wallet.ui.card.passCardColors

@Composable
internal fun PassEditorCard(
    type: PassType,
    logoUri: Uri?,
    logoText: String?,
    headerFields: List<FieldDraft>,
    primaryFields: List<FieldDraft>,
    secondaryFields: List<FieldDraft>,
    auxiliaryFields: List<FieldDraft>,
    stripUri: Uri?,
    thumbnailUri: Uri?,
    colors: PassColors?,
    modifier: Modifier = Modifier,
    onLogoClick: () -> Unit,
    onLogoTextClick: () -> Unit,
    onHeaderFieldsClick: () -> Unit,
    onPrimaryFieldsClick: () -> Unit,
    onSecondaryFieldsClick: () -> Unit,
    onAuxiliaryFieldsClick: () -> Unit,
    onStripClick: () -> Unit,
    onThumbnailClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onBackFieldsClick: () -> Unit,
) {
    val cardColors = passCardColors(colors)

    ElevatedCard(
        colors = cardColors,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditorHeaderRow(
                logoUri = logoUri,
                logoText = logoText,
                headerFields = headerFields,
                onLogoClick = onLogoClick,
                onLogoTextClick = onLogoTextClick,
                onHeaderFieldsClick = onHeaderFieldsClick,
            )

            when (type) {
                is PassType.Boarding ->
                    EditorBoardingPrimary(
                        transitType = type.transitType,
                        primaryFields = primaryFields,
                        onPrimaryFieldsClick = onPrimaryFieldsClick,
                    )
                else ->
                    EditorGenericPrimary(
                        primaryFields = primaryFields,
                        thumbnailUri = thumbnailUri,
                        showThumbnail = type is PassType.Generic || type is PassType.Event,
                        onPrimaryFieldsClick = onPrimaryFieldsClick,
                        onThumbnailClick = onThumbnailClick,
                    )
            }

            if (type is PassType.Coupon || type is PassType.Event || type is PassType.StoreCard) {
                EditorZone(
                    label = stringResource(R.string.strip),
                    isEmpty = stripUri == null,
                    onClick = onStripClick,
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                ) {
                    AsyncImage(
                        model = stripUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            EditorFieldsRow(
                label = stringResource(R.string.secondary_fields),
                fields = secondaryFields,
                onClick = onSecondaryFieldsClick,
            )

            EditorFieldsRow(
                label = stringResource(R.string.auxiliary_fields),
                fields = auxiliaryFields,
                onClick = onAuxiliaryFieldsClick,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onBackFieldsClick) {
                    Text(stringResource(R.string.back_fields))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(start = 4.dp),
                    )
                }
                TextButton(onClick = onAppearanceClick) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(end = 4.dp),
                    )
                    Text(stringResource(R.string.appearance))
                }
            }
        }
    }
}

@Composable
private fun EditorHeaderRow(
    logoUri: Uri?,
    logoText: String?,
    headerFields: List<FieldDraft>,
    onLogoClick: () -> Unit,
    onLogoTextClick: () -> Unit,
    onHeaderFieldsClick: () -> Unit,
) {
    BoxWithConstraints {
        Row(
            modifier = Modifier.height(38.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditorZone(
                label = stringResource(R.string.logo),
                isEmpty = logoUri == null,
                onClick = onLogoClick,
                modifier = Modifier.width(72.dp).fillMaxHeight(),
            ) {
                AsyncImage(
                    model = logoUri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier =
                        Modifier
                            .widthIn(max = this@BoxWithConstraints.maxWidth * 0.4f)
                            .fillMaxHeight(),
                )
            }

            EditorZone(
                label = stringResource(R.string.logo_text),
                isEmpty = logoText.isNullOrBlank(),
                onClick = onLogoTextClick,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                Text(
                    text = logoText.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            EditorZone(
                label = stringResource(R.string.header_fields),
                isEmpty = headerFields.isEmpty(),
                onClick = onHeaderFieldsClick,
                modifier =
                    Modifier
                        .widthIn(max = this@BoxWithConstraints.maxWidth * 0.45f)
                        .fillMaxHeight(),
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    headerFields.take(3).forEach { field ->
                        Column(horizontalAlignment = Alignment.End) {
                            if (field.label.isNotBlank()) {
                                Text(
                                    text = field.label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                )
                            }
                            Text(
                                text = field.value,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorGenericPrimary(
    primaryFields: List<FieldDraft>,
    thumbnailUri: Uri?,
    showThumbnail: Boolean,
    onPrimaryFieldsClick: () -> Unit,
    onThumbnailClick: () -> Unit,
) {
    Row(
        modifier = Modifier.height(90.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EditorZone(
            label = stringResource(R.string.primary_fields),
            isEmpty = primaryFields.isEmpty(),
            onClick = onPrimaryFieldsClick,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            primaryFields.firstOrNull()?.let { field ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (field.label.isNotBlank()) {
                        Text(
                            text = field.label.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = field.value,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                    )
                }
            }
        }

        if (showThumbnail) {
            EditorZone(
                label = stringResource(R.string.thumbnail),
                isEmpty = thumbnailUri == null,
                onClick = onThumbnailClick,
                modifier = Modifier.width(90.dp).fillMaxHeight(),
            ) {
                AsyncImage(
                    model = thumbnailUri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun EditorBoardingPrimary(
    transitType: TransitType,
    primaryFields: List<FieldDraft>,
    onPrimaryFieldsClick: () -> Unit,
) {
    val departure = primaryFields.getOrNull(0)
    val destination = primaryFields.getOrNull(1)

    EditorZone(
        label = stringResource(R.string.primary_fields),
        isEmpty = primaryFields.isEmpty(),
        onClick = onPrimaryFieldsClick,
        modifier = Modifier.fillMaxWidth().height(70.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = (departure?.label ?: stringResource(R.string.departure)).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Text(
                    text = departure?.value ?: "—",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )
            }

            Icon(
                imageVector = transitType.icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = (destination?.label ?: stringResource(R.string.destination)).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                )
                Text(
                    text = destination?.value ?: "—",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun EditorFieldsRow(
    label: String,
    fields: List<FieldDraft>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EditorZone(
        label = label,
        isEmpty = fields.isEmpty(),
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(38.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            fields.forEach { field ->
                Column(modifier = Modifier.weight(1f)) {
                    if (field.label.isNotBlank()) {
                        Text(
                            text = field.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = field.value,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
internal fun EditorZone(
    label: String,
    isEmpty: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val borderColor = LocalContentColor.current.copy(alpha = 0.35f)

    Box(
        modifier =
            modifier
                .then(if (isEmpty) Modifier.dashedBorder(borderColor) else Modifier)
                .clickable(onClick = onClick)
                .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isEmpty) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        } else {
            content()
        }
    }
}

private fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp = 6.dp,
    dashLength: Dp = 5.dp,
    gapLength: Dp = 4.dp,
    strokeWidth: Dp = 1.dp,
): Modifier =
    this.drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            style =
                Stroke(
                    width = strokeWidth.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength.toPx(), gapLength.toPx())),
                ),
        )
    }
