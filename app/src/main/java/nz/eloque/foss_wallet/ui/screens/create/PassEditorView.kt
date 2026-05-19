package nz.eloque.foss_wallet.ui.screens.create

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.google.zxing.BarcodeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nz.eloque.foss_wallet.R
import nz.eloque.foss_wallet.model.BarCode
import nz.eloque.foss_wallet.model.LocalizedPassWithTags
import nz.eloque.foss_wallet.model.PassColors
import nz.eloque.foss_wallet.model.PassCreator
import nz.eloque.foss_wallet.model.PassRelevantDate
import nz.eloque.foss_wallet.model.PassType
import nz.eloque.foss_wallet.model.TransitType
import nz.eloque.foss_wallet.model.field.PassContent
import nz.eloque.foss_wallet.model.field.PassField
import nz.eloque.foss_wallet.ui.Screen
import nz.eloque.foss_wallet.ui.components.ImagePicker
import nz.eloque.foss_wallet.ui.screens.scan.ScanLauncher
import nz.eloque.foss_wallet.ui.screens.settings.ComboBox
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.UUID

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassEditorView(
    navController: NavHostController,
    createViewModel: CreateViewModel,
    initialBarcode: BarCode? = null,
    existingPass: LocalizedPassWithTags? = null,
    isTemplate: Boolean = false,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    val isEditing = existingPass != null && !isTemplate

    fun existingImageUri(fileName: String): Uri? =
        existingPass?.pass?.id?.let { id ->
            val file = java.io.File(context.filesDir, "$id/$fileName")
            if (file.exists()) file.toUri() else null
        }

    var iconUrl by remember { mutableStateOf(existingImageUri("icon.png")) }
    var logoUrl by remember { mutableStateOf(existingImageUri("logo.png")) }
    var stripUrl by remember { mutableStateOf(existingImageUri("strip.png")) }
    var thumbnailUrl by remember { mutableStateOf(existingImageUri("thumbnail.png")) }
    var footerUrl by remember { mutableStateOf(existingImageUri("footer.png")) }

    var name by remember { mutableStateOf(existingPass?.pass?.description ?: "") }
    var nameTouched by remember { mutableStateOf(isEditing) }
    var organization by remember {
        mutableStateOf(
            existingPass?.pass?.organization?.let {
                if (it == PassCreator.ORGANIZATION) "" else it
            } ?: "",
        )
    }
    var serialNumber by remember {
        mutableStateOf(
            existingPass?.pass?.serialNumber?.let {
                if (it == existingPass.pass.id) "" else it
            } ?: "",
        )
    }
    var logoText by remember { mutableStateOf(existingPass?.pass?.logoText ?: "") }

    var transitType by remember {
        mutableStateOf((existingPass?.pass?.type as? PassType.Boarding)?.transitType ?: TransitType.GENERIC)
    }
    var type by remember { mutableStateOf(existingPass?.pass?.type ?: PassType.Generic) }

    var locationDrafts by remember {
        mutableStateOf<List<LocationDraft>>(
            existingPass?.pass?.locations?.map { loc ->
                LocationDraft(
                    coords = "${loc.latitude.formatCoord()}, ${loc.longitude.formatCoord()}",
                    relevantText = loc.extras?.getString("relevantText") ?: "",
                )
            } ?: emptyList(),
        )
    }
    var relevantStart by remember {
        mutableStateOf<ZonedDateTime?>(
            existingPass?.pass?.relevantDates?.firstOrNull()?.let {
                when (it) {
                    is PassRelevantDate.Date -> it.date
                    is PassRelevantDate.DateInterval -> it.startDate
                }
            },
        )
    }
    var relevantEnd by remember {
        mutableStateOf<ZonedDateTime?>(
            existingPass?.pass?.relevantDates?.firstOrNull()?.let {
                if (it is PassRelevantDate.DateInterval) it.endDate else null
            },
        )
    }
    var expirationDate by remember { mutableStateOf(existingPass?.pass?.expirationDate) }
    var maxDistance by remember { mutableStateOf(existingPass?.pass?.maxDistance?.toString() ?: "") }

    var backgroundColor by remember { mutableStateOf(existingPass?.pass?.colors?.background) }
    var foregroundColor by remember { mutableStateOf(existingPass?.pass?.colors?.foreground) }
    var labelColor by remember { mutableStateOf(existingPass?.pass?.colors?.label) }

    var fields by remember {
        mutableStateOf<List<FieldDraft>>(
            existingPass?.pass?.run {
                val all = mutableListOf<FieldDraft>()
                headerFields.forEach { all.add(it.toEditorFieldDraft(FieldCategory.Header)) }
                primaryFields.forEach { all.add(it.toEditorFieldDraft(FieldCategory.Primary)) }
                secondaryFields.forEach { all.add(it.toEditorFieldDraft(FieldCategory.Secondary)) }
                auxiliaryFields.forEach { all.add(it.toEditorFieldDraft(FieldCategory.Auxiliary)) }
                backFields.forEach { all.add(it.toEditorFieldDraft(FieldCategory.Back)) }
                all.toList()
            } ?: emptyList(),
        )
    }

    var barcodes by remember {
        mutableStateOf(
            existingPass?.pass?.barCodes?.map { bc ->
                BarcodeDraft(message = bc.message, altText = bc.altText ?: bc.message, format = bc.format)
            } ?: initialBarcode?.let {
                listOf(BarcodeDraft(message = it.message, altText = it.altText ?: it.message, format = it.format))
            } ?: emptyList(),
        )
    }
    var activeBarcodeIndex by remember { mutableIntStateOf(0) }

    var activeSheet by remember { mutableStateOf<EditorSheet?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf<ColorTarget?>(null) }

    val barCodeModels =
        remember(barcodes) {
            barcodes.map {
                BarCode(
                    format = it.format,
                    message = it.message,
                    encoding = Charsets.UTF_8,
                    altText = it.altText.ifBlank { it.message },
                )
            }
        }
    val barCodeValidity = remember(barCodeModels) { barCodeModels.map { barcodeValid(it) } }
    val pass = PassCreator.create(name, type, barCodeModels)

    val nameValid = name.length in 1..<30
    val barcodesValid =
        barcodes.isNotEmpty() &&
            barcodes.indices.all { i -> barcodes[i].message.isNotEmpty() && barCodeValidity[i] }
    val datesValid = relevantEnd == null || relevantStart != null
    val createValid = nameValid && barcodesValid && datesValid && pass != null && !isSaving

    val allColorsBlank = backgroundColor == null && foregroundColor == null && labelColor == null
    val previewColors =
        if (allColorsBlank) {
            null
        } else {
            val fallback = requireNotNull(backgroundColor ?: foregroundColor ?: labelColor)
            PassColors(
                background = backgroundColor ?: fallback,
                foreground = foregroundColor ?: fallback,
                label = labelColor ?: fallback,
            )
        }

    val scanLauncher =
        ScanLauncher.launch { scanned ->
            if (activeBarcodeIndex !in barcodes.indices) return@launch
            barcodes =
                barcodes.mapIndexed { index, barcode ->
                    if (index != activeBarcodeIndex) barcode
                    else barcode.copy(message = scanned.message, altText = scanned.altText ?: scanned.message, format = scanned.format)
                }
        }

    fun fieldsOf(cat: FieldCategory) = fields.filter { it.category == cat }

    val doSave = {
        isSaving = true
        coroutineScope.launch(Dispatchers.IO) {
            val relevantDates =
                when {
                    relevantStart != null && relevantEnd != null ->
                        listOf(PassRelevantDate.DateInterval(relevantStart!!, relevantEnd!!))
                    relevantStart != null ->
                        listOf(PassRelevantDate.Date(relevantStart!!))
                    else -> emptyList()
                }

            val colors =
                if (allColorsBlank) {
                    null
                } else {
                    val fallback = requireNotNull(backgroundColor ?: foregroundColor ?: labelColor)
                    PassColors(
                        background = backgroundColor ?: fallback,
                        foreground = foregroundColor ?: fallback,
                        label = labelColor ?: fallback,
                    )
                }

            fun List<FieldDraft>.toPassFields(cat: FieldCategory) =
                filter { it.category == cat }.map {
                    PassField(key = it.key, label = it.label.ifBlank { null }, content = PassContent.Plain(it.value))
                }

            val savedPassId =
                createViewModel.savePass(
                    name = name,
                    organization = organization,
                    serialNumber = serialNumber,
                    type = type,
                    barcodes = barCodeModels,
                    colors = colors,
                    logoText = logoText.ifBlank { null },
                    locations = locationDrafts.mapNotNull { draft ->
                        parseLatLon(draft.coords)?.also { loc ->
                            if (draft.relevantText.isNotBlank()) {
                                loc.extras = android.os.Bundle().apply { putString("relevantText", draft.relevantText) }
                            }
                        }
                    },
                    relevantDates = relevantDates,
                    expirationDate = expirationDate,
                    maxDistance = maxDistance.toDoubleOrNull(),
                    iconUrl = iconUrl,
                    logoUrl = logoUrl,
                    stripUrl = stripUrl,
                    thumbnailUrl = thumbnailUrl,
                    footerUrl = footerUrl,
                    headerFields = fields.toPassFields(FieldCategory.Header),
                    primaryFields = fields.toPassFields(FieldCategory.Primary),
                    secondaryFields = fields.toPassFields(FieldCategory.Secondary),
                    auxiliaryFields = fields.toPassFields(FieldCategory.Auxiliary),
                    backFields = fields.toPassFields(FieldCategory.Back),
                    existingPassId = if (isTemplate) null else existingPass?.pass?.id,
                    existingAddedAt = if (isTemplate) null else existingPass?.pass?.addedAt,
                )
            withContext(Dispatchers.Main) {
                isSaving = false
                if (isEditing) {
                    navController.popBackStack("pass/$savedPassId", inclusive = false)
                } else {
                    navController.navigate("pass/$savedPassId") {
                        popUpTo(Screen.Wallet.route)
                    }
                }
            }
        }
    }

    colorPickerTarget?.let { target ->
        val initial =
            when (target) {
                ColorTarget.Background -> backgroundColor ?: Color.White
                ColorTarget.Foreground -> foregroundColor ?: Color.White
                ColorTarget.Label -> labelColor ?: Color.White
            }
        ColorPickerDialog(
            title =
                when (target) {
                    ColorTarget.Background -> stringResource(R.string.pass_background_color)
                    ColorTarget.Foreground -> stringResource(R.string.pass_foreground_color)
                    ColorTarget.Label -> stringResource(R.string.pass_label_color)
                },
            initialColor = initial,
            onDismiss = { colorPickerTarget = null },
            onConfirm = { selected ->
                when (target) {
                    ColorTarget.Background -> backgroundColor = selected.opaque()
                    ColorTarget.Foreground -> foregroundColor = selected.opaque()
                    ColorTarget.Label -> labelColor = selected.opaque()
                }
                colorPickerTarget = null
            },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            label = { Text(stringResource(R.string.pass_name)) },
            value = name,
            onValueChange = {
                nameTouched = true
                name = it
            },
            modifier = Modifier.fillMaxWidth(),
            isError = nameTouched && !nameValid,
            singleLine = true,
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PassType.all()) { passType ->
                FilterChip(
                    selected = type.isSameType(passType),
                    onClick = {
                        type = if (passType is PassType.Boarding) PassType.Boarding(transitType) else passType
                    },
                    label = { Text(resources.getString(passType.label)) },
                )
            }
        }

        AnimatedVisibility(
            visible = type is PassType.Boarding,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            ComboBox(
                title = stringResource(R.string.transit_type),
                options = TransitType.entries,
                selectedOption = transitType,
                onOptionSelected = {
                    transitType = it
                    type = PassType.Boarding(it)
                },
                optionLabel = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
            )
        }

        PassEditorCard(
            type = type,
            logoUri = logoUrl,
            logoText = logoText.ifBlank { null },
            headerFields = fieldsOf(FieldCategory.Header),
            primaryFields = fieldsOf(FieldCategory.Primary),
            secondaryFields = fieldsOf(FieldCategory.Secondary),
            auxiliaryFields = fieldsOf(FieldCategory.Auxiliary),
            stripUri = stripUrl,
            thumbnailUri = thumbnailUrl,
            colors = previewColors,
            onLogoClick = { activeSheet = EditorSheet.Logo },
            onLogoTextClick = { activeSheet = EditorSheet.LogoText },
            onHeaderFieldsClick = { activeSheet = EditorSheet.Fields(FieldCategory.Header) },
            onPrimaryFieldsClick = { activeSheet = EditorSheet.Fields(FieldCategory.Primary) },
            onSecondaryFieldsClick = { activeSheet = EditorSheet.Fields(FieldCategory.Secondary) },
            onAuxiliaryFieldsClick = { activeSheet = EditorSheet.Fields(FieldCategory.Auxiliary) },
            onStripClick = { activeSheet = EditorSheet.Strip },
            onThumbnailClick = { activeSheet = EditorSheet.Thumbnail },
            onAppearanceClick = { activeSheet = EditorSheet.Appearance },
            onBackFieldsClick = { activeSheet = EditorSheet.Fields(FieldCategory.Back) },
        )

        EditorZone(
            label = stringResource(R.string.barcode),
            isEmpty = barcodes.isEmpty(),
            onClick = { activeSheet = EditorSheet.Barcodes },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null)
                Text(
                    text =
                        barcodes.take(2).joinToString(", ") { it.format.name }
                            .let { if (barcodes.size > 2) "$it…" else it },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
            }
        }

        OutlinedButton(
            onClick = { activeSheet = EditorSheet.Metadata },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = Icons.Default.Info, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.metadata))
        }

        Button(
            enabled = createValid,
            onClick = { doSave() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(if (isEditing) R.string.save_changes else R.string.create_pass))
        }

        if (!isEditing) {
            Text(
                text = stringResource(R.string.created_pass_export_warning),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    activeSheet?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            when (sheet) {
                EditorSheet.Logo ->
                    ImagePickerSheetContent(
                        title = stringResource(R.string.logo),
                        description = stringResource(R.string.zone_logo_desc),
                        imageUri = logoUrl,
                        onChoose = { logoUrl = it },
                        onClear = { logoUrl = null },
                        extra = {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = stringResource(R.string.icon),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            Text(
                                text = "Small square image required for all passes (used in system shortcuts and notifications).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            ImagePicker(
                                imageUrl = iconUrl,
                                onClear = { iconUrl = null },
                                onChoose = { iconUrl = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                    )

                EditorSheet.LogoText ->
                    LogoTextSheetContent(
                        logoText = logoText,
                        onLogoTextChange = { logoText = it },
                    )

                is EditorSheet.Fields ->
                    FieldsSheetContent(
                        category = sheet.category,
                        passType = type,
                        fields = fieldsOf(sheet.category),
                        onFieldUpdate = { key, label, value ->
                            fields =
                                fields.map { if (it.key == key) it.copy(label = label, value = value) else it }
                        },
                        onFieldDelete = { key -> fields = fields.filter { it.key != key } },
                        onFieldAdd = {
                            fields =
                                fields +
                                    FieldDraft(
                                        key = UUID.randomUUID().toString().take(8),
                                        label = "",
                                        value = "",
                                        category = sheet.category,
                                    )
                        },
                    )

                EditorSheet.Strip ->
                    ImagePickerSheetContent(
                        title = stringResource(R.string.strip),
                        description = stringResource(R.string.zone_strip_desc),
                        imageUri = stripUrl,
                        onChoose = { stripUrl = it },
                        onClear = { stripUrl = null },
                    )

                EditorSheet.Thumbnail ->
                    ImagePickerSheetContent(
                        title = stringResource(R.string.thumbnail),
                        description = stringResource(R.string.zone_thumbnail_desc),
                        imageUri = thumbnailUrl,
                        onChoose = { thumbnailUrl = it },
                        onClear = { thumbnailUrl = null },
                    )

                EditorSheet.Footer ->
                    ImagePickerSheetContent(
                        title = stringResource(R.string.footer),
                        description = stringResource(R.string.zone_footer_desc),
                        imageUri = footerUrl,
                        onChoose = { footerUrl = it },
                        onClear = { footerUrl = null },
                    )

                EditorSheet.Barcodes ->
                    BarcodeSheetContent(
                        barcodes = barcodes,
                        barCodeValidity = barCodeValidity,
                        activeBarcodeIndex = activeBarcodeIndex,
                        scanLauncher = scanLauncher,
                        context = context,
                        onBarcodeUpdate = { index, updated ->
                            barcodes = barcodes.mapIndexed { i, b -> if (i == index) updated else b }
                        },
                        onBarcodeDelete = { index ->
                            if (barcodes.size > 1) {
                                barcodes = barcodes.filterIndexed { i, _ -> i != index }
                                activeBarcodeIndex =
                                    activeBarcodeIndex.coerceAtMost(barcodes.lastIndex.coerceAtLeast(0))
                            }
                        },
                        onBarcodeAdd = {
                            barcodes = barcodes + BarcodeDraft("", "", BarcodeFormat.QR_CODE)
                        },
                        onActiveBarcodeIndexChange = { activeBarcodeIndex = it },
                    )

                EditorSheet.Appearance ->
                    AppearanceSheetContent(
                        backgroundColor = backgroundColor,
                        foregroundColor = foregroundColor,
                        labelColor = labelColor,
                        footerUri = footerUrl,
                        boardingPassActive = type is PassType.Boarding,
                        onColorPick = { colorPickerTarget = it },
                        onColorClear = { target ->
                            when (target) {
                                ColorTarget.Background -> backgroundColor = null
                                ColorTarget.Foreground -> foregroundColor = null
                                ColorTarget.Label -> labelColor = null
                            }
                        },
                        onColorChange = { target, color ->
                            when (target) {
                                ColorTarget.Background -> backgroundColor = color
                                ColorTarget.Foreground -> foregroundColor = color
                                ColorTarget.Label -> labelColor = color
                            }
                        },
                        onFooterChoose = { footerUrl = it },
                        onFooterClear = { footerUrl = null },
                    )

                EditorSheet.Metadata ->
                    MetadataSheetContent(
                        organization = organization,
                        serialNumber = serialNumber,
                        relevantStart = relevantStart,
                        relevantEnd = relevantEnd,
                        expirationDate = expirationDate,
                        maxDistance = maxDistance,
                        locationDrafts = locationDrafts,
                        createViewModel = createViewModel,
                        onOrganizationChange = { organization = it },
                        onSerialNumberChange = { serialNumber = it },
                        onRelevantStartPick = { openDateTimePicker(context, relevantStart) { relevantStart = it } },
                        onRelevantStartClear = { relevantStart = null },
                        onRelevantEndPick = { openDateTimePicker(context, relevantEnd) { relevantEnd = it } },
                        onRelevantEndClear = { relevantEnd = null },
                        onExpirationPick = { openDateTimePicker(context, expirationDate) { expirationDate = it } },
                        onExpirationClear = { expirationDate = null },
                        onMaxDistanceChange = { maxDistance = it },
                        onLocationAdd = { locationDrafts = locationDrafts + LocationDraft() },
                        onLocationDelete = { index -> locationDrafts = locationDrafts.filterIndexed { i, _ -> i != index } },
                        onLocationChange = { index, coords -> locationDrafts = locationDrafts.mapIndexed { i, d -> if (i == index) d.copy(coords = coords) else d } },
                        onLocationRelevantTextChange = { index, text -> locationDrafts = locationDrafts.mapIndexed { i, d -> if (i == index) d.copy(relevantText = text) else d } },
                    )
            }
        }
    }
}

@Composable
private fun ImagePickerSheetContent(
    title: String,
    description: String,
    imageUri: Uri?,
    onChoose: (Uri?) -> Unit,
    onClear: () -> Unit,
    extra: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ImagePicker(
            imageUrl = imageUri,
            onClear = onClear,
            onChoose = onChoose,
            modifier = Modifier.fillMaxWidth(),
        )
        extra?.invoke()
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun LogoTextSheetContent(
    logoText: String,
    onLogoTextChange: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.logo_text), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.zone_logo_text_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = logoText,
            onValueChange = onLogoTextChange,
            label = { Text(stringResource(R.string.logo_text)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Composable
private fun FieldsSheetContent(
    category: FieldCategory,
    passType: PassType,
    fields: List<FieldDraft>,
    onFieldUpdate: (key: String, label: String, value: String) -> Unit,
    onFieldDelete: (key: String) -> Unit,
    onFieldAdd: () -> Unit,
) {
    val title =
        when (category) {
            FieldCategory.Header -> stringResource(R.string.header_fields)
            FieldCategory.Primary -> stringResource(R.string.primary_fields)
            FieldCategory.Secondary -> stringResource(R.string.secondary_fields)
            FieldCategory.Auxiliary -> stringResource(R.string.auxiliary_fields)
            FieldCategory.Back -> stringResource(R.string.back_fields)
        }
    val description =
        when (category) {
            FieldCategory.Header -> stringResource(R.string.zone_header_desc)
            FieldCategory.Primary -> stringResource(R.string.zone_primary_desc)
            FieldCategory.Secondary -> stringResource(R.string.zone_secondary_desc)
            FieldCategory.Auxiliary -> stringResource(R.string.zone_auxiliary_desc)
            FieldCategory.Back -> stringResource(R.string.zone_back_desc)
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(fieldHintRes(passType, category)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )

        fields.forEach { field ->
            SheetFieldRow(
                field = field,
                onLabelChange = { onFieldUpdate(field.key, it, field.value) },
                onValueChange = { onFieldUpdate(field.key, field.label, it) },
                onDelete = { onFieldDelete(field.key) },
            )
            HorizontalDivider()
        }

        TextButton(
            onClick = onFieldAdd,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.add_field))
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun SheetFieldRow(
    field: FieldDraft,
    onLabelChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = field.label,
                onValueChange = onLabelChange,
                label = { Text(stringResource(R.string.field_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = field.value,
                onValueChange = onValueChange,
                label = { Text(stringResource(R.string.field_value)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Default.Clear, contentDescription = stringResource(R.string.delete))
        }
    }
}

@Composable
private fun BarcodeSheetContent(
    barcodes: List<BarcodeDraft>,
    barCodeValidity: List<Boolean>,
    activeBarcodeIndex: Int,
    scanLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context,
    onBarcodeUpdate: (Int, BarcodeDraft) -> Unit,
    onBarcodeDelete: (Int) -> Unit,
    onBarcodeAdd: () -> Unit,
    onActiveBarcodeIndexChange: (Int) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.barcode), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.zone_barcode_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        barcodes.forEachIndexed { index, barcode ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        label = { Text(stringResource(R.string.barcode_value)) },
                        value = barcode.message,
                        onValueChange = { onBarcodeUpdate(index, barcode.copy(message = it)) },
                        modifier = Modifier.fillMaxWidth(fraction = 0.72f),
                        isError = barcode.message.isNotEmpty() && !barCodeValidity[index],
                        supportingText = {
                            if (barcode.message.isNotEmpty() && !barCodeValidity[index]) {
                                Text(stringResource(R.string.barcode_value_invalid, barcode.format.toString()))
                            }
                        },
                        singleLine = true,
                    )
                    IconButton(onClick = {
                        onActiveBarcodeIndexChange(index)
                        scanLauncher.launch(Intent(context, ScanActivity::class.java))
                    }) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.scan_barcode))
                    }
                    IconButton(
                        enabled = barcodes.size > 1,
                        onClick = { onBarcodeDelete(index) },
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = stringResource(R.string.delete))
                    }
                }
                OutlinedTextField(
                    label = { Text(stringResource(R.string.barcode_alt_text)) },
                    value = barcode.altText,
                    onValueChange = { onBarcodeUpdate(index, barcode.copy(altText = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text(stringResource(R.string.barcode_alt_text_desc), style = MaterialTheme.typography.bodySmall) },
                )
                ComboBox(
                    title = stringResource(R.string.barcode_format),
                    options = BarcodeFormat.entries,
                    selectedOption = barcode.format,
                    onOptionSelected = { onBarcodeUpdate(index, barcode.copy(format = it)) },
                    optionLabel = { it.name },
                )
                HorizontalDivider()
            }
        }

        ElevatedButton(
            onClick = onBarcodeAdd,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.add_another_barcode))
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun AppearanceSheetContent(
    backgroundColor: Color?,
    foregroundColor: Color?,
    labelColor: Color?,
    footerUri: Uri?,
    boardingPassActive: Boolean,
    onColorPick: (ColorTarget) -> Unit,
    onColorClear: (ColorTarget) -> Unit,
    onColorChange: (ColorTarget, Color) -> Unit,
    onFooterChoose: (Uri?) -> Unit,
    onFooterClear: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.appearance), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.zone_appearance_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ColorPickerRow(
            label = stringResource(R.string.pass_background_color),
            icon = Icons.Default.Palette,
            color = backgroundColor,
            onPick = { onColorPick(ColorTarget.Background) },
            onClear = { onColorClear(ColorTarget.Background) },
            onColorChange = { onColorChange(ColorTarget.Background, it) },
        )
        ColorPickerRow(
            label = stringResource(R.string.pass_foreground_color),
            icon = Icons.Default.Palette,
            color = foregroundColor,
            onPick = { onColorPick(ColorTarget.Foreground) },
            onClear = { onColorClear(ColorTarget.Foreground) },
            onColorChange = { onColorChange(ColorTarget.Foreground, it) },
        )
        ColorPickerRow(
            label = stringResource(R.string.pass_label_color),
            icon = Icons.Default.Palette,
            color = labelColor,
            onPick = { onColorPick(ColorTarget.Label) },
            onClear = { onColorClear(ColorTarget.Label) },
            onColorChange = { onColorChange(ColorTarget.Label, it) },
        )

        if (boardingPassActive) {
            HorizontalDivider()
            Text(
                text = stringResource(R.string.footer),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.zone_footer_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ImagePicker(
                imageUrl = footerUri,
                onClear = onFooterClear,
                onChoose = onFooterChoose,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun MetadataSheetContent(
    organization: String,
    serialNumber: String,
    relevantStart: ZonedDateTime?,
    relevantEnd: ZonedDateTime?,
    expirationDate: ZonedDateTime?,
    maxDistance: String,
    locationDrafts: List<LocationDraft>,
    createViewModel: CreateViewModel,
    onOrganizationChange: (String) -> Unit,
    onSerialNumberChange: (String) -> Unit,
    onRelevantStartPick: () -> Unit,
    onRelevantStartClear: () -> Unit,
    onRelevantEndPick: () -> Unit,
    onRelevantEndClear: () -> Unit,
    onExpirationPick: () -> Unit,
    onExpirationClear: () -> Unit,
    onMaxDistanceChange: (String) -> Unit,
    onLocationAdd: () -> Unit,
    onLocationDelete: (Int) -> Unit,
    onLocationChange: (Int, String) -> Unit,
    onLocationRelevantTextChange: (Int, String) -> Unit,
) {
    val dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.metadata), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.zone_metadata_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            label = { Text(stringResource(R.string.organization)) },
            value = organization,
            onValueChange = onOrganizationChange,
            leadingIcon = { Icon(imageVector = Icons.Default.Business, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            label = { Text(stringResource(R.string.serial_number)) },
            value = serialNumber,
            onValueChange = onSerialNumberChange,
            leadingIcon = { Icon(imageVector = Icons.Default.Badge, contentDescription = null) },
            supportingText = { Text(stringResource(R.string.serial_number_desc), style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        PickableOutlinedField(
            label = stringResource(R.string.pass_relevant_start),
            value = relevantStart?.format(dtFormatter) ?: "",
            leadingIcon = Icons.Default.CalendarToday,
            onPick = onRelevantStartPick,
            onClear = onRelevantStartClear,
            clearEnabled = relevantStart != null,
            supportingText = stringResource(R.string.pass_relevant_start_desc),
        )
        PickableOutlinedField(
            label = stringResource(R.string.pass_relevant_end),
            value = relevantEnd?.format(dtFormatter) ?: "",
            leadingIcon = Icons.Default.CalendarToday,
            onPick = onRelevantEndPick,
            onClear = onRelevantEndClear,
            clearEnabled = relevantEnd != null,
            supportingText = stringResource(R.string.pass_relevant_end_desc),
        )
        PickableOutlinedField(
            label = stringResource(R.string.pass_expiration_date),
            value = expirationDate?.format(dtFormatter) ?: "",
            leadingIcon = Icons.Default.CalendarToday,
            onPick = onExpirationPick,
            onClear = onExpirationClear,
            clearEnabled = expirationDate != null,
            supportingText = stringResource(R.string.pass_expiration_date_desc),
        )
        OutlinedTextField(
            label = { Text(stringResource(R.string.pass_max_distance)) },
            value = maxDistance,
            onValueChange = onMaxDistanceChange,
            leadingIcon = { Icon(imageVector = Icons.Default.LocationOn, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )

        locationDrafts.forEachIndexed { index, draft ->
            LocationRow(
                draft = draft,
                createViewModel = createViewModel,
                onTextChange = { onLocationChange(index, it) },
                onRelevantTextChange = { onLocationRelevantTextChange(index, it) },
                onDelete = { onLocationDelete(index) },
            )
        }

        TextButton(
            onClick = onLocationAdd,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.add_location))
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun LocationRow(
    draft: LocationDraft,
    createViewModel: CreateViewModel,
    onTextChange: (String) -> Unit,
    onRelevantTextChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var searchShown by remember { mutableStateOf(false) }
    val coordValid = draft.coords.isBlank() || parseLatLon(draft.coords) != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = draft.coords,
            onValueChange = onTextChange,
            label = { Text(stringResource(R.string.latlon_input)) },
            isError = !coordValid,
            leadingIcon = {
                IconButton(onClick = { searchShown = true }) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = stringResource(R.string.location_search_query))
                }
            },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Default.Clear, contentDescription = stringResource(R.string.delete))
        }
    }
    OutlinedTextField(
        value = draft.relevantText,
        onValueChange = onRelevantTextChange,
        label = { Text(stringResource(R.string.location_relevant_text)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    if (searchShown) {
        LocationSearchDialog(
            createViewModel = createViewModel,
            onDismiss = { searchShown = false },
            onConfirm = { coords ->
                onTextChange(coords)
                searchShown = false
            },
        )
    }
}

@Composable
private fun PickableOutlinedField(
    label: String,
    value: String,
    leadingIcon: ImageVector,
    onPick: () -> Unit,
    onClear: () -> Unit,
    clearEnabled: Boolean,
    supportingText: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                leadingIcon = { Icon(imageVector = leadingIcon, contentDescription = label) },
                supportingText = supportingText?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.matchParentSize().clickable { onPick() })
        }
        IconButton(onClick = onClear, enabled = clearEnabled) {
            Icon(imageVector = Icons.Default.Clear, contentDescription = stringResource(R.string.clear_selection))
        }
    }
}

@Composable
private fun ColorPickerRow(
    label: String,
    icon: ImageVector,
    color: Color?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    onColorChange: (Color) -> Unit,
) {
    var hexInput by remember { mutableStateOf(color?.toHexColor() ?: "") }

    LaunchedEffect(color) {
        if (color != null) {
            val canonical = color.toHexColor()
            if (!hexInput.trimStart('#').equals(canonical.drop(1), ignoreCase = true)) {
                hexInput = canonical
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = hexInput,
            onValueChange = { raw ->
                hexInput = raw
                val stripped = raw.trimStart('#').uppercase()
                val expanded =
                    when (stripped.length) {
                        3 -> stripped.flatMap { listOf(it, it) }.joinToString("")
                        6 -> stripped
                        else -> null
                    }
                expanded?.let {
                    try {
                        val parsed = Color(android.graphics.Color.parseColor("#$it"))
                        onColorChange(parsed.opaque())
                    } catch (_: IllegalArgumentException) {
                    }
                }
            },
            label = { Text(label) },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace),
            isError = hexInput.isNotEmpty() && color == null,
            leadingIcon = {
                Row(
                    modifier = Modifier.padding(start = 10.dp).clickable { onPick() },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = icon, contentDescription = label)
                    Spacer(
                        modifier =
                            Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color ?: Color.Transparent),
                    )
                }
            },
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = {
                hexInput = ""
                onClear()
            },
            enabled = color != null || hexInput.isNotEmpty(),
        ) {
            Icon(imageVector = Icons.Default.Clear, contentDescription = stringResource(R.string.clear_selection))
        }
    }
}

@Composable
private fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    var envelope by remember {
        mutableStateOf(ColorEnvelope(initialColor.opaque(), initialColor.opaque().toHexColor().drop(1), false))
    }
    val controller = rememberColorPickerController()

    LaunchedEffect(initialColor) {
        controller.selectByColor(initialColor.opaque(), fromUser = false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HsvColorPicker(
                    controller = controller,
                    initialColor = initialColor,
                    onColorChanged = { envelope = it },
                    modifier = Modifier.width(220.dp).size(220.dp),
                )
                Text(stringResource(R.string.brightness))
                BrightnessSlider(
                    controller = controller,
                    modifier =
                        Modifier
                            .width(220.dp)
                            .size(width = 220.dp, height = 28.dp)
                            .padding(horizontal = 4.dp),
                )
                Text("#${envelope.hexCode}", fontFamily = FontFamily.Monospace)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(envelope.color) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.back)) }
        },
    )
}

@Composable
private fun LocationSearchDialog(
    createViewModel: CreateViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<CreateViewModel.GeocodeResult>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.location_search_query)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    label = { Text(stringResource(R.string.location_search_query)) },
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            isSearching = true
                            error = null
                            try {
                                results = createViewModel.geocode(query)
                                if (results.isEmpty()) error = resources.getString(R.string.no_search_results)
                            } catch (e: Exception) {
                                error = e.message ?: resources.getString(R.string.exception)
                            } finally {
                                isSearching = false
                            }
                        }
                    },
                    enabled = query.isNotBlank() && !isSearching,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isSearching) stringResource(R.string.searching) else stringResource(R.string.search))
                }
                error?.let { Text(it, color = Color.Red) }
                results.forEach { result ->
                    ElevatedButton(
                        onClick = { onConfirm("${result.latitude.formatCoord()}, ${result.longitude.formatCoord()}") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(result.displayName)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.back)) }
        },
    )
}

private fun parseLatLon(input: String): Location? {
    val parts = input.split(",").map { it.trim() }
    if (parts.size != 2) return null
    val lat = parts[0].toDoubleOrNull() ?: return null
    val lon = parts[1].toDoubleOrNull() ?: return null
    if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) return null
    return Location("").apply { latitude = lat; longitude = lon }
}

private fun openDateTimePicker(
    context: Context,
    initial: ZonedDateTime?,
    onPicked: (ZonedDateTime) -> Unit,
) {
    val seed = initial ?: ZonedDateTime.now()
    val calendar = Calendar.getInstance().apply { timeInMillis = seed.toInstant().toEpochMilli() }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    onPicked(
                        ZonedDateTime.of(year, month + 1, dayOfMonth, hourOfDay, minute, 0, 0, ZoneId.systemDefault()),
                    )
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true,
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH),
    ).show()
}

@StringRes
private fun fieldHintRes(
    passType: PassType,
    category: FieldCategory,
): Int =
    when (passType) {
        is PassType.Boarding ->
            when (category) {
                FieldCategory.Header -> R.string.hint_field_boarding_header
                FieldCategory.Primary -> R.string.hint_field_boarding_primary
                FieldCategory.Secondary -> R.string.hint_field_boarding_secondary
                FieldCategory.Auxiliary -> R.string.hint_field_boarding_auxiliary
                FieldCategory.Back -> R.string.hint_field_boarding_back
            }
        is PassType.Event ->
            when (category) {
                FieldCategory.Header -> R.string.hint_field_event_header
                FieldCategory.Primary -> R.string.hint_field_event_primary
                FieldCategory.Secondary -> R.string.hint_field_event_secondary
                FieldCategory.Auxiliary -> R.string.hint_field_event_auxiliary
                FieldCategory.Back -> R.string.hint_field_event_back
            }
        is PassType.Coupon ->
            when (category) {
                FieldCategory.Header -> R.string.hint_field_coupon_header
                FieldCategory.Primary -> R.string.hint_field_coupon_primary
                FieldCategory.Secondary -> R.string.hint_field_coupon_secondary
                FieldCategory.Auxiliary -> R.string.hint_field_coupon_auxiliary
                FieldCategory.Back -> R.string.hint_field_coupon_back
            }
        is PassType.StoreCard ->
            when (category) {
                FieldCategory.Header -> R.string.hint_field_store_header
                FieldCategory.Primary -> R.string.hint_field_store_primary
                FieldCategory.Secondary -> R.string.hint_field_store_secondary
                FieldCategory.Auxiliary -> R.string.hint_field_store_auxiliary
                FieldCategory.Back -> R.string.hint_field_store_back
            }
        else ->
            when (category) {
                FieldCategory.Header -> R.string.hint_field_generic_header
                FieldCategory.Primary -> R.string.hint_field_generic_primary
                FieldCategory.Secondary -> R.string.hint_field_generic_secondary
                FieldCategory.Auxiliary -> R.string.hint_field_generic_auxiliary
                FieldCategory.Back -> R.string.hint_field_generic_back
            }
    }

private fun barcodeValid(barCode: BarCode): Boolean = barCode.encodeAsBitmap(100, 100, false) != null

internal fun PassField.toEditorFieldDraft(category: FieldCategory): FieldDraft =
    FieldDraft(key = key, label = label ?: "", value = content.prettyPrint(), category = category)

private enum class ColorTarget { Background, Foreground, Label }

private fun Double.formatCoord(): String = String.format(Locale.current.platformLocale, "%.6f", this)

private fun Color.toHexColor(): String = String.format("#%06X", this.toArgb() and 0x00FFFFFF)

private fun Color.opaque(): Color = this.copy(alpha = 1f)
