package nz.eloque.foss_wallet.persistence.backup

import android.location.Location
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import nz.eloque.foss_wallet.model.Attachment
import nz.eloque.foss_wallet.model.BarCode
import nz.eloque.foss_wallet.model.Pass
import nz.eloque.foss_wallet.model.PassColors
import nz.eloque.foss_wallet.model.PassGroup
import nz.eloque.foss_wallet.model.PassMetadata
import nz.eloque.foss_wallet.model.PassRelevantDate
import nz.eloque.foss_wallet.model.PassTagCrossRef
import nz.eloque.foss_wallet.model.SortOptionSerializer
import nz.eloque.foss_wallet.model.Tag
import nz.eloque.foss_wallet.model.field.PassField
import nz.eloque.foss_wallet.persistence.BarcodePosition
import nz.eloque.foss_wallet.persistence.SettingsStore
import nz.eloque.foss_wallet.persistence.TypeConverters
import nz.eloque.foss_wallet.utils.stringOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZonedDateTime
import java.util.LinkedHashSet
import java.util.UUID
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object BackupSerializer {
    private val typeConverters = TypeConverters()

    fun serializePass(pass: Pass): JSONObject =
        JSONObject().apply {
            put("id", pass.id)
            put("description", pass.description)
            put("formatVersion", pass.formatVersion)
            put("organization", pass.organization)
            put("serialNumber", pass.serialNumber)
            put("type", typeConverters.fromPassType(pass.type))
            put("barCodes", barcodesToJson(pass.barCodes))
            put("addedAt", pass.addedAt.toEpochMilli())
            put("hasLogo", pass.hasLogo)
            put("hasStrip", pass.hasStrip)
            put("hasThumbnail", pass.hasThumbnail)
            put("hasFooter", pass.hasFooter)
            put("deviceId", pass.deviceId.toString())
            pass.colors?.let { put("colors", colorsToJson(it)) }
            put("relevantDates", relevantDatesToJson(pass.relevantDates))
            pass.expirationDate?.let { put("expirationDate", it.toString()) }
            pass.maxDistance?.let { put("maxDistance", it) }
            pass.logoText?.let { put("logoText", it) }
            pass.authToken?.let { put("authToken", it) }
            pass.webServiceUrl?.let { put("webServiceUrl", it) }
            pass.passTypeIdentifier?.let { put("passTypeIdentifier", it) }
            put("locations", locationsToJson(pass.locations))
            put("headerFields", fieldsToJson(pass.headerFields))
            put("primaryFields", fieldsToJson(pass.primaryFields))
            put("secondaryFields", fieldsToJson(pass.secondaryFields))
            put("auxiliaryFields", fieldsToJson(pass.auxiliaryFields))
            put("backFields", fieldsToJson(pass.backFields))
        }

    fun deserializePass(json: JSONObject): Pass {
        val barcodesArray = json.getJSONArray("barCodes")
        val barcodes = (0 until barcodesArray.length()).map { BarCode.fromJson(barcodesArray.getJSONObject(it)) }

        val colorsJson = json.optJSONObject("colors")
        val colors =
            colorsJson?.let {
                PassColors(
                    Color(it.getInt("background")),
                    Color(it.getInt("foreground")),
                    Color(it.getInt("label")),
                )
            }

        val relevantDatesArray = json.getJSONArray("relevantDates")
        val relevantDates =
            (0 until relevantDatesArray.length()).map { i ->
                val dateJson = relevantDatesArray.getJSONObject(i)
                if (dateJson.has("date")) {
                    PassRelevantDate.Date(ZonedDateTime.parse(dateJson.getString("date")))
                } else {
                    PassRelevantDate.DateInterval(
                        ZonedDateTime.parse(dateJson.getString("startDate")),
                        ZonedDateTime.parse(dateJson.getString("endDate")),
                    )
                }
            }

        val locationsArray = json.getJSONArray("locations")
        val locations =
            (0 until locationsArray.length()).map { i ->
                val locJson = locationsArray.getJSONObject(i)
                Location("").also {
                    it.latitude = locJson.getDouble("latitude")
                    it.longitude = locJson.getDouble("longitude")
                }
            }

        fun jsonToFields(array: JSONArray): List<PassField> = (0 until array.length()).map { PassField.fromJson(array.getJSONObject(it)) }

        return Pass(
            id = json.getString("id"),
            description = json.getString("description"),
            formatVersion = json.getInt("formatVersion"),
            organization = json.getString("organization"),
            serialNumber = json.getString("serialNumber"),
            type = typeConverters.toPassType(json.getString("type")),
            barCodes = LinkedHashSet(barcodes),
            addedAt = Instant.ofEpochMilli(json.getLong("addedAt")),
            hasLogo = json.getBoolean("hasLogo"),
            hasStrip = json.getBoolean("hasStrip"),
            hasThumbnail = json.getBoolean("hasThumbnail"),
            hasFooter = json.getBoolean("hasFooter"),
            deviceId = UUID.fromString(json.getString("deviceId")),
            colors = colors,
            relevantDates = relevantDates,
            expirationDate = json.stringOrNull("expirationDate")?.let { ZonedDateTime.parse(it) },
            maxDistance = if (json.has("maxDistance")) json.getDouble("maxDistance") else null,
            logoText = json.stringOrNull("logoText"),
            authToken = json.stringOrNull("authToken"),
            webServiceUrl = json.stringOrNull("webServiceUrl"),
            passTypeIdentifier = json.stringOrNull("passTypeIdentifier"),
            locations = locations,
            headerFields = jsonToFields(json.getJSONArray("headerFields")),
            primaryFields = jsonToFields(json.getJSONArray("primaryFields")),
            secondaryFields = jsonToFields(json.getJSONArray("secondaryFields")),
            auxiliaryFields = jsonToFields(json.getJSONArray("auxiliaryFields")),
            backFields = jsonToFields(json.getJSONArray("backFields")),
        )
    }

    fun serializeMetadata(meta: PassMetadata): JSONObject =
        JSONObject().apply {
            put("passId", meta.passId)
            meta.groupId?.let { put("groupId", it) }
            put("archived", meta.archived)
            put("autoArchive", meta.autoArchive)
            put("renderLegacy", meta.renderLegacy)
        }

    fun deserializeMetadata(json: JSONObject): PassMetadata =
        PassMetadata(
            passId = json.getString("passId"),
            groupId = if (json.has("groupId")) json.getLong("groupId") else null,
            archived = json.getBoolean("archived"),
            autoArchive = json.getBoolean("autoArchive"),
            renderLegacy = json.getBoolean("renderLegacy"),
        )

    fun serializeTag(tag: Tag): JSONObject =
        JSONObject().apply {
            put("label", tag.label)
            put("color", tag.color.toArgb())
        }

    fun deserializeTag(json: JSONObject): Tag =
        Tag(
            label = json.getString("label"),
            color = Color(json.getInt("color")),
        )

    fun serializeCrossRef(ref: PassTagCrossRef): JSONObject =
        JSONObject().apply {
            put("passId", ref.passId)
            put("tagLabel", ref.tagLabel)
        }

    fun deserializeCrossRef(json: JSONObject): PassTagCrossRef =
        PassTagCrossRef(
            passId = json.getString("passId"),
            tagLabel = json.getString("tagLabel"),
        )

    fun serializeAttachment(attachment: Attachment): JSONObject =
        JSONObject().apply {
            put("fileName", attachment.fileName)
            put("passId", attachment.passId)
        }

    fun deserializeAttachment(json: JSONObject): Attachment =
        Attachment(
            fileName = json.getString("fileName"),
            passId = json.getString("passId"),
        )

    fun serializeSettings(settingsStore: SettingsStore): JSONObject =
        JSONObject().apply {
            put("syncEnabled", settingsStore.isSyncEnabled())
            put("syncIntervalMinutes", settingsStore.syncInterval().inWholeMinutes)
            put("barcodePosition", settingsStore.barcodePosition().key)
            put("passViewBrightness", settingsStore.increasePassViewBrightness())
            put("sortOption", SortOptionSerializer.serialize(settingsStore.sortOption()))
            put("deleteConfirmationEnabled", settingsStore.deleteConfirmationEnabled())
        }

    fun restoreSettings(
        json: JSONObject,
        settingsStore: SettingsStore,
    ) {
        settingsStore.enableSync(json.getBoolean("syncEnabled"))
        settingsStore.setSyncInterval(json.getLong("syncIntervalMinutes").toDuration(DurationUnit.MINUTES))
        settingsStore.setBarcodePosition(BarcodePosition.of(json.getString("barcodePosition")))
        settingsStore.enablePassViewBrightness(json.getBoolean("passViewBrightness"))
        SortOptionSerializer.deserialize(json.getString("sortOption"))?.let { settingsStore.setSortOption(it) }
        settingsStore.setDeleteConfirmationEnabled(json.getBoolean("deleteConfirmationEnabled"))
    }

    private fun barcodesToJson(barcodes: Set<BarCode>): JSONArray {
        val arr = JSONArray()
        barcodes.forEach { arr.put(it.toJson()) }
        return arr
    }

    private fun colorsToJson(colors: PassColors): JSONObject =
        JSONObject().apply {
            put("background", colors.background.toArgb())
            put("foreground", colors.foreground.toArgb())
            put("label", colors.label.toArgb())
        }

    private fun relevantDatesToJson(dates: List<PassRelevantDate>): JSONArray {
        val arr = JSONArray()
        dates.forEach { date ->
            arr.put(
                when (date) {
                    is PassRelevantDate.Date ->
                        JSONObject().apply { put("date", date.date.toString()) }
                    is PassRelevantDate.DateInterval ->
                        JSONObject().apply {
                            put("startDate", date.startDate.toString())
                            put("endDate", date.endDate.toString())
                        }
                },
            )
        }
        return arr
    }

    private fun locationsToJson(locations: List<Location>): JSONArray {
        val arr = JSONArray()
        locations.forEach { loc ->
            arr.put(
                JSONObject().apply {
                    put("latitude", loc.latitude)
                    put("longitude", loc.longitude)
                },
            )
        }
        return arr
    }

    private fun fieldsToJson(fields: List<PassField>): JSONArray {
        val arr = JSONArray()
        fields.forEach { arr.put(it.toJson()) }
        return arr
    }
}
