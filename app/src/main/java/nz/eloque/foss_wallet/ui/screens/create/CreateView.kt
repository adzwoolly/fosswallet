package nz.eloque.foss_wallet.ui.screens.create

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.intl.Locale
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
import nz.eloque.foss_wallet.ui.components.Section
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
fun CreateView(
    navController: NavHostController,
    createViewModel: CreateViewModel,
    initialBarcode: BarCode? = null,
    existingPass: LocalizedPassWithTags? = null,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()

    val isEditing = existingPass != null

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
    var transitType by remember {
        mutableStateOf((existingPass?.pass?.type as? PassType.Boarding)?.transitType ?: TransitType.GENERIC)
    }
    var type by remember { mutableStateOf(existingPass?.pass?.type ?: PassType.Generic) }

    var location by remember {
        mutableStateOf<Location?>(
            existingPass?.pass?.locations?.firstOrNull()?.let { loc ->
                Location("").also { it.latitude = loc.latitude; it.longitude = loc.longitude }
            },
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

    var backgroundColor by remember { mutableStateOf(existingPass?.pass?.colors?.background) }
    var foregroundColor by remember { mutableStateOf(existingPass?.pass?.colors?.foreground) }
    var labelColor by remember { mutableStateOf(existingPass?.pass?.colors?.label) }

    var fields by remember {
        mutableStateOf<List<FieldDraft>>(
            existingPass?.pass?.run {
                val all = mutableListOf<FieldDraft>()
                headerFields.forEach { all.add(it.toFieldDraft(FieldCategory.Header)) }
                primaryFields.forEach { all.add(it.toFieldDraft(FieldCategory.Primary)) }
                secondaryFields.forEach { all.add(it.toFieldDraft(FieldCategory.Secondary)) }
                auxiliaryFields.forEach { all.add(it.toFieldDraft(FieldCategory.Auxiliary)) }
                backFields.forEach { all.add(it.toFieldDraft(FieldCategory.Back)) }
                all.toList()
            } ?: emptyList(),
        )
    }

    var showLocationPicker by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf<ColorTarget?>(null) }

    var isSaving by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(isEditing) }
    var detailsExpanded by remember { mutableStateOf(isEditing) }

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
    val showNameError = nameTouched && !nameValid
    val barcodesValid =
        barcodes.isNotEmpty() &&
            barcodes.indices.all { i ->
                barcodes[i].message.isNotEmpty() && barCodeValidity[i]
            }
    val datesValid = relevantEnd == null || relevantStart != null
    val createValid = nameValid && barcodesValid && datesValid && pass != null && !isSaving

    val allColorsBlank = backgroundColor == null && foregroundColor == null && labelColor == null

    val scanLauncher =
        ScanLauncher.launch {
            if (activeBarcodeIndex !in barcodes.indices) return@launch
            barcodes =
                barcodes.mapIndexed { index, barcode ->
                    if (index != activeBarcodeIndex) {
                        barcode
                    } else {
                        barcode.copy(
                            message = it.message,
                            altText = it.altText ?: it.message,
                            format = it.format,
                        )
                    }
                }
            detailsExpanded = true
        }

    if (showLocationPicker) {
        LocationPickerDialog(
            createViewModel = createViewModel,
            initial = location,
            onDismiss = { showLocationPicker = false },
            onConfirm = {
                location = it
                showLocationPicker = false
            },
        )
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
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        barcodes.forEachIndexed { index, barcode ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        label = { Text(stringResource(R.string.barcode_value)) },
                        value = barcode.message,
                        onValueChange = { value ->
                            barcodes =
                                barcodes.mapIndexed { i, item ->
                                    if (i == index) item.copy(message = value) else item
                                }
                        },
                        modifier = Modifier.fillMaxWidth(fraction = 0.72f),
                        isError = barcode.message.isNotEmpty() && !barCodeValidity[index],
                        supportingText = {
                            if (barcode.message.isNotEmpty() && !barCodeValidity[index]) {
                                Text(stringResource(R.string.barcode_value_invalid, barcode.format.toString()))
                            }
                        },
                    )

                    IconButton(onClick = {
                        activeBarcodeIndex = index
                        scanLauncher.launch(
                            Intent(context, ScanActivity::class.java),
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.scan_barcode),
                        )
                    }
                    IconButton(
                        enabled = barcodes.size > 1,
                        onClick = {
                            barcodes = barcodes.filterIndexed { i, _ -> i != index }
                            activeBarcodeIndex = activeBarcodeIndex.coerceAtMost(barcodes.lastIndex.coerceAtLeast(0))
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.delete),
                        )
                    }
                }

                ComboBox(
                    title = stringResource(R.string.barcode_format),
                    options = BarcodeFormat.entries,
                    selectedOption = barcode.format,
                    onOptionSelected = { selected ->
                        barcodes =
                            barcodes.mapIndexed { i, item ->
                                if (i == index) item.copy(format = selected) else item
                            }
                    },
                    optionLabel = { it.name },
                    onInfo = {
                        val intent =
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://en.wikipedia.org/wiki/Barcode#Types_of_barcodes".toUri(),
                            )
                        context.startActivity(intent)
                    },
                )
            }
        }

        ElevatedButton(
            onClick = {
                barcodes = barcodes +
                    BarcodeDraft(
                        message = "",
                        altText = "",
                        format = BarcodeFormat.QR_CODE,
                    )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.add_another_barcode))
        }

        if (!detailsExpanded) {
            Button(
                enabled = barcodesValid,
                onClick = { detailsExpanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.continue_to_details))
            }
        }

        AnimatedVisibility(
            visible = detailsExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    label = { Text(stringResource(R.string.pass_name)) },
                    value = name,
                    onValueChange = {
                        nameTouched = true
                        name = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showNameError,
                )

                ComboBox(
                    title = stringResource(R.string.pass_type),
                    options =
                        listOf(
                            PassType.Generic,
                            PassType.StoreCard,
                            PassType.Coupon,
                            PassType.Event,
                            PassType.Boarding(transitType),
                        ),
                    selectedOption = type,
                    onOptionSelected = {
                        type = if (it is PassType.Boarding) PassType.Boarding(transitType) else it
                    },
                    optionLabel = { resources.getString(it.label) },
                )

                if (type is PassType.Boarding) {
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

                Section(heading = stringResource(R.string.pass_fields)) {
                    fields.forEachIndexed { index, field ->
                        FieldRow(
                            field = field,
                            onLabelChange = { fields = fields.mapIndexed { i, f -> if (i == index) f.copy(label = it) else f } },
                            onValueChange = { fields = fields.mapIndexed { i, f -> if (i == index) f.copy(value = it) else f } },
                            onCategoryChange = { fields = fields.mapIndexed { i, f -> if (i == index) f.copy(category = it) else f } },
                            onDelete = { fields = fields.filterIndexed { i, _ -> i != index } },
                        )
                    }
                    TextButton(
                        onClick = {
                            fields = fields + FieldDraft(
                                key = UUID.randomUUID().toString().take(8),
                                label = "",
                                value = "",
                                category = FieldCategory.Primary,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(stringResource(R.string.add_field))
                    }
                }

                ElevatedButton(
                    onClick = { advancedExpanded = !advancedExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.additional_fields))
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = if (advancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription =
                            if (advancedExpanded) {
                                stringResource(R.string.collapse)
                            } else {
                                stringResource(R.string.expand)
                            },
                    )
                }

                AnimatedVisibility(
                    visible = advancedExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PickableOutlinedField(
                            label = stringResource(R.string.pass_relevant_start),
                            value = relevantStart?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")) ?: "",
                            leadingIcon = Icons.Default.CalendarToday,
                            onPick = { openDateTimePicker(context, relevantStart) { relevantStart = it } },
                            onClear = { relevantStart = null },
                            clearEnabled = relevantStart != null,
                        )
                        PickableOutlinedField(
                            label = stringResource(R.string.pass_relevant_end),
                            value = relevantEnd?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")) ?: "",
                            leadingIcon = Icons.Default.CalendarToday,
                            onPick = { openDateTimePicker(context, relevantEnd) { relevantEnd = it } },
                            onClear = { relevantEnd = null },
                            clearEnabled = relevantEnd != null,
                        )
                        PickableOutlinedField(
                            label = stringResource(R.string.pass_expiration_date),
                            value = expirationDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")) ?: "",
                            leadingIcon = Icons.Default.CalendarToday,
                            onPick = { openDateTimePicker(context, expirationDate) { expirationDate = it } },
                            onClear = { expirationDate = null },
                            clearEnabled = expirationDate != null,
                        )
                        PickableOutlinedField(
                            label = stringResource(R.string.pass_location),
                            value = location?.let { "${it.latitude.formatCoord()}, ${it.longitude.formatCoord()}" } ?: "",
                            leadingIcon = Icons.Default.LocationOn,
                            onPick = { showLocationPicker = true },
                            onClear = { location = null },
                            clearEnabled = location != null,
                        )
                        ColorPickerRow(
                            label = stringResource(R.string.pass_background_color),
                            icon = Icons.Default.Palette,
                            color = backgroundColor,
                            onPick = { colorPickerTarget = ColorTarget.Background },
                            onClear = { backgroundColor = null },
                            onColorChange = { backgroundColor = it },
                        )
                        ColorPickerRow(
                            label = stringResource(R.string.pass_foreground_color),
                            icon = Icons.Default.Palette,
                            color = foregroundColor,
                            onPick = { colorPickerTarget = ColorTarget.Foreground },
                            onClear = { foregroundColor = null },
                            onColorChange = { foregroundColor = it },
                        )
                        ColorPickerRow(
                            label = stringResource(R.string.pass_label_color),
                            icon = Icons.Default.Palette,
                            color = labelColor,
                            onPick = { colorPickerTarget = ColorTarget.Label },
                            onClear = { labelColor = null },
                            onColorChange = { labelColor = it },
                        )
                        OutlinedTextField(
                            label = { Text(stringResource(R.string.organization)) },
                            value = organization,
                            onValueChange = { organization = it },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = stringResource(R.string.organization),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            label = { Text(stringResource(R.string.serial_number)) },
                            value = serialNumber,
                            onValueChange = { serialNumber = it },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = stringResource(R.string.serial_number),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ImagePicker(
                            imageUrl = logoUrl,
                            onClear = { logoUrl = null },
                            onChoose = { logoUrl = it },
                            label = stringResource(R.string.logo),
                            labelIcon = Icons.Default.Image,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ImagePicker(
                            imageUrl = iconUrl,
                            onClear = { iconUrl = null },
                            onChoose = { iconUrl = it },
                            label = stringResource(R.string.icon),
                            labelIcon = Icons.Default.Image,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (type is PassType.Coupon || type is PassType.Event || type is PassType.StoreCard) {
                            ImagePicker(
                                imageUrl = stripUrl,
                                onClear = { stripUrl = null },
                                onChoose = { stripUrl = it },
                                label = stringResource(R.string.strip),
                                labelIcon = Icons.Default.Image,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (type is PassType.Generic || type is PassType.Event) {
                            ImagePicker(
                                imageUrl = thumbnailUrl,
                                onClear = { thumbnailUrl = null },
                                onChoose = { thumbnailUrl = it },
                                label = stringResource(R.string.thumbnail),
                                labelIcon = Icons.Default.Image,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (type is PassType.Boarding) {
                            ImagePicker(
                                imageUrl = footerUrl,
                                onClear = { footerUrl = null },
                                onChoose = { footerUrl = it },
                                label = stringResource(R.string.footer),
                                labelIcon = Icons.Default.Image,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                Button(
                    enabled = createValid,
                    onClick = {
                        isSaving = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val relevantDates =
                                when {
                                    relevantStart != null && relevantEnd != null -> {
                                        listOf(
                                            PassRelevantDate.DateInterval(relevantStart!!, relevantEnd!!),
                                        )
                                    }

                                    relevantStart != null -> {
                                        listOf(PassRelevantDate.Date(relevantStart!!))
                                    }

                                    else -> {
                                        emptyList()
                                    }
                                }

                            val colors =
                                if (allColorsBlank) {
                                    null
                                } else {
                                    val fallbackColor = requireNotNull(backgroundColor ?: foregroundColor ?: labelColor)
                                    PassColors(
                                        background = backgroundColor ?: fallbackColor,
                                        foreground = foregroundColor ?: fallbackColor,
                                        label = labelColor ?: fallbackColor,
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
                                    location = location,
                                    relevantDates = relevantDates,
                                    expirationDate = expirationDate,
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
                                    existingPassId = existingPass?.pass?.id,
                                    existingAddedAt = existingPass?.pass?.addedAt,
                                )
                            withContext(Dispatchers.Main) {
                                isSaving = false
                                if (isEditing) {
                                    navController.navigate("pass/$savedPassId") {
                                        popUpTo("edit/$savedPassId") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("pass/$savedPassId") {
                                        popUpTo(Screen.Wallet.route)
                                    }
                                }
                            }
                        }
                    },
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
        }
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
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(
                modifier =
                    Modifier
                        .matchParentSize()
                        .clickable { onPick() },
            )
        }
        IconButton(onClick = onClear, enabled = clearEnabled) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = stringResource(R.string.clear_selection),
            )
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
                    } catch (_: IllegalArgumentException) { }
                }
            },
            label = { Text(label) },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace),
            isError = hexInput.isNotEmpty() && color == null,
            leadingIcon = {
                Row(
                    modifier =
                        Modifier
                            .padding(start = 10.dp)
                            .clickable { onPick() },
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
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = stringResource(R.string.clear_selection),
            )
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
                    modifier =
                        Modifier
                            .width(220.dp)
                            .size(220.dp),
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
private fun LocationPickerDialog(
    createViewModel: CreateViewModel,
    initial: Location?,
    onDismiss: () -> Unit,
    onConfirm: (Location) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<CreateViewModel.GeocodeResult>>(emptyList()) }
    var selected by remember {
        mutableStateOf(
            initial?.let {
                CreateViewModel.GeocodeResult(
                    displayName = "${it.latitude.formatCoord()}, ${it.longitude.formatCoord()}",
                    latitude = it.latitude,
                    longitude = it.longitude,
                )
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pass_location)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    label = { Text(stringResource(R.string.location_search_query)) },
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                isSearching = true
                                error = null
                                try {
                                    results = createViewModel.geocode(query)
                                    if (results.isEmpty()) {
                                        error = resources.getString(R.string.no_search_results)
                                    }
                                } catch (e: Exception) {
                                    error = e.message ?: resources.getString(R.string.exception)
                                } finally {
                                    isSearching = false
                                }
                            }
                        },
                        enabled = query.isNotBlank() && !isSearching,
                    ) {
                        Text(if (isSearching) stringResource(R.string.searching) else stringResource(R.string.search))
                    }
                    TextButton(onClick = { selected = null }) {
                        Text(stringResource(R.string.clear_selection))
                    }
                }

                error?.let { Text(it, color = Color.Red) }

                selected?.let {
                    Text(stringResource(R.string.selected_location, it.displayName))
                }

                results.forEach { result ->
                    ElevatedButton(
                        onClick = { selected = result },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(result.displayName)
                    }
                }

                TextButton(
                    onClick = {
                        val geoUri =
                            if (selected != null) {
                                "geo:${selected!!.latitude},${selected!!.longitude}?q=${selected!!.latitude},${selected!!.longitude}"
                                    .toUri()
                            } else {
                                "geo:0,0?q=${Uri.encode(query)}".toUri()
                            }
                        val intent = Intent(Intent.ACTION_VIEW, geoUri)
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, resources.getString(R.string.no_map_app_found), Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = query.isNotBlank() || selected != null,
                ) {
                    Text(stringResource(R.string.open_in_map_app))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selected?.let {
                        val location =
                            Location("").apply {
                                this.latitude = it.latitude
                                this.longitude = it.longitude
                            }
                        onConfirm(location)
                    }
                },
                enabled = selected != null,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.back)) }
        },
    )
}

private fun openDateTimePicker(
    context: Context,
    initial: ZonedDateTime?,
    onPicked: (ZonedDateTime) -> Unit,
) {
    val seed = initial ?: ZonedDateTime.now()
    val calendar =
        Calendar.getInstance().apply {
            timeInMillis = seed.toInstant().toEpochMilli()
        }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    onPicked(
                        ZonedDateTime.of(
                            year,
                            month + 1,
                            dayOfMonth,
                            hourOfDay,
                            minute,
                            0,
                            0,
                            ZoneId.systemDefault(),
                        ),
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

private enum class ColorTarget {
    Background,
    Foreground,
    Label,
}

private enum class FieldCategory {
    Header,
    Primary,
    Secondary,
    Auxiliary,
    Back;

    fun displayName(): String = name
}

private data class BarcodeDraft(
    val message: String,
    val altText: String,
    val format: BarcodeFormat,
)

private data class FieldDraft(
    val key: String,
    val label: String,
    val value: String,
    val category: FieldCategory,
)

private fun barcodeValid(barCode: BarCode): Boolean = barCode.encodeAsBitmap(100, 100, false) != null

private fun PassField.toFieldDraft(category: FieldCategory): FieldDraft =
    FieldDraft(key = key, label = label ?: "", value = content.prettyPrint(), category = category)

@Composable
private fun FieldRow(
    field: FieldDraft,
    onLabelChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onCategoryChange: (FieldCategory) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                ComboBox(
                    title = stringResource(R.string.field_category),
                    options = FieldCategory.entries,
                    selectedOption = field.category,
                    onOptionSelected = onCategoryChange,
                    optionLabel = { it.displayName() },
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.delete),
                )
            }
        }
        HorizontalDivider()
    }
}

private fun Double.formatCoord(): String = String.format(Locale.current.platformLocale, "%.6f", this)

private fun Color.toHexColor(): String = String.format("#%06X", this.toArgb() and 0x00FFFFFF)

private fun Color.opaque(): Color = this.copy(alpha = 1f)
