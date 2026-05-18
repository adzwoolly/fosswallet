package nz.eloque.foss_wallet.persistence.backup

import android.content.Context
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import nz.eloque.foss_wallet.model.PassGroup
import nz.eloque.foss_wallet.persistence.SettingsStore
import nz.eloque.foss_wallet.persistence.WalletDb
import nz.eloque.foss_wallet.persistence.pass.PassDao
import nz.eloque.foss_wallet.persistence.tag.TagDao
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val BACKUP_VERSION = 1
private const val BACKUP_JSON_ENTRY = "backup.json"

class BackupStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val passDao: PassDao,
        private val tagDao: TagDao,
        private val settingsStore: SettingsStore,
        private val walletDb: WalletDb,
    ) {
        suspend fun exportBackup(outputStream: OutputStream) {
            val passes = passDao.allPassesSnapshot()
            val metadata = passDao.allMetadata()
            val groups = passDao.allGroups()
            val tags = tagDao.allTagsSnapshot()
            val crossRefs = tagDao.crossRef()
            val attachments = passDao.allAttachments()

            val attachmentsByPass = attachments.groupBy { it.passId }

            val backupJson =
                JSONObject().apply {
                    put("version", BACKUP_VERSION)
                    put("createdAt", Instant.now().toEpochMilli())

                    val passesArr = JSONArray()
                    passes.forEach { passesArr.put(BackupSerializer.serializePass(it)) }
                    put("passes", passesArr)

                    val metadataArr = JSONArray()
                    metadata.forEach { metadataArr.put(BackupSerializer.serializeMetadata(it)) }
                    put("metadata", metadataArr)

                    val groupsArr = JSONArray()
                    groups.forEach { groupsArr.put(JSONObject().apply { put("id", it.id) }) }
                    put("groups", groupsArr)

                    val groupMembershipsArr = JSONArray()
                    metadata.filter { it.groupId != null }.forEach { meta ->
                        groupMembershipsArr.put(
                            JSONObject().apply {
                                put("passId", meta.passId)
                                put("groupId", meta.groupId!!)
                            },
                        )
                    }
                    put("groupMemberships", groupMembershipsArr)

                    val tagsArr = JSONArray()
                    tags.forEach { tagsArr.put(BackupSerializer.serializeTag(it)) }
                    put("tags", tagsArr)

                    val crossRefsArr = JSONArray()
                    crossRefs.forEach { crossRefsArr.put(BackupSerializer.serializeCrossRef(it)) }
                    put("passTagRefs", crossRefsArr)

                    val attachmentsArr = JSONArray()
                    attachments.forEach { attachmentsArr.put(BackupSerializer.serializeAttachment(it)) }
                    put("attachments", attachmentsArr)

                    put("settings", BackupSerializer.serializeSettings(settingsStore))
                }

            ZipOutputStream(outputStream).use { zip ->
                zip.putNextEntry(ZipEntry(BACKUP_JSON_ENTRY))
                zip.write(backupJson.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                for (pass in passes) {
                    val passDir = File(context.filesDir, pass.id)

                    fun addFile(
                        name: String,
                        file: File?,
                    ) {
                        val f = file ?: return
                        if (!f.exists()) return
                        zip.putNextEntry(ZipEntry("passes/${pass.id}/$name"))
                        f.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }

                    addFile("icon.png", File(passDir, "icon.png"))
                    if (pass.hasLogo) addFile("logo.png", File(passDir, "logo.png"))
                    if (pass.hasStrip) addFile("strip.png", File(passDir, "strip.png"))
                    if (pass.hasThumbnail) addFile("thumbnail.png", File(passDir, "thumbnail.png"))
                    if (pass.hasFooter) addFile("footer.png", File(passDir, "footer.png"))
                    addFile("original.pkpass", File(passDir, "original.pkpass").takeIf { it.exists() })

                    attachmentsByPass[pass.id]?.forEach { attachment ->
                        addFile("attachments/${attachment.fileName}", attachment.getFile(context))
                    }
                }
            }
        }

        suspend fun importBackup(inputStream: InputStream) {
            val fileEntries = mutableMapOf<String, ByteArray>()
            var backupJsonBytes: ByteArray? = null

            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val bytes = zip.readBytes()
                        if (entry.name == BACKUP_JSON_ENTRY) {
                            backupJsonBytes = bytes
                        } else {
                            fileEntries[entry.name] = bytes
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val backupJson = JSONObject(backupJsonBytes!!.toString(Charsets.UTF_8))

            val passesArray = backupJson.getJSONArray("passes")
            val metadataArray = backupJson.getJSONArray("metadata")
            val groupsArray = backupJson.getJSONArray("groups")
            val tagsArray = backupJson.getJSONArray("tags")
            val crossRefsArray = backupJson.getJSONArray("passTagRefs")
            val attachmentsArray = backupJson.getJSONArray("attachments")
            val settingsJson = backupJson.optJSONObject("settings")

            walletDb.withTransaction {
                // Groups: insert fresh, map old ID → new ID
                val groupIdMap = mutableMapOf<Long, Long>()
                for (i in 0 until groupsArray.length()) {
                    val oldId = groupsArray.getJSONObject(i).getLong("id")
                    val newId = passDao.insert(PassGroup())
                    groupIdMap[oldId] = newId
                }

                // Tags
                for (i in 0 until tagsArray.length()) {
                    tagDao.insert(BackupSerializer.deserializeTag(tagsArray.getJSONObject(i)))
                }

                // Build metadata map for quick lookup by passId
                val metaByPassId =
                    (0 until metadataArray.length())
                        .map { BackupSerializer.deserializeMetadata(metadataArray.getJSONObject(it)) }
                        .associateBy { it.passId }

                // Passes + metadata + image files
                for (i in 0 until passesArray.length()) {
                    val pass = BackupSerializer.deserializePass(passesArray.getJSONObject(i))
                    passDao.insert(pass)

                    val meta = metaByPassId[pass.id] ?: nz.eloque.foss_wallet.model.PassMetadata(pass.id)
                    val remappedMeta = meta.copy(groupId = meta.groupId?.let { groupIdMap[it] })
                    passDao.insert(remappedMeta)

                    val passDir = File(context.filesDir, pass.id).also { it.mkdirs() }
                    fileEntries.entries
                        .filter { it.key.startsWith("passes/${pass.id}/") && !it.key.contains("/attachments/") }
                        .forEach { (entryName, bytes) ->
                            val fileName = entryName.removePrefix("passes/${pass.id}/")
                            File(passDir, fileName).writeBytes(bytes)
                        }
                }

                // PassTagCrossRefs
                for (i in 0 until crossRefsArray.length()) {
                    passDao.tag(BackupSerializer.deserializeCrossRef(crossRefsArray.getJSONObject(i)))
                }

                // Attachments
                for (i in 0 until attachmentsArray.length()) {
                    val attachment = BackupSerializer.deserializeAttachment(attachmentsArray.getJSONObject(i))
                    passDao.insert(attachment)
                    fileEntries["passes/${attachment.passId}/attachments/${attachment.fileName}"]?.let { bytes ->
                        val attDir = File(context.filesDir, "${attachment.passId}/attachments").also { it.mkdirs() }
                        File(attDir, attachment.fileName).writeBytes(bytes)
                    }
                }
            }

            // Settings (outside transaction — SharedPreferences)
            settingsJson?.let { BackupSerializer.restoreSettings(it, settingsStore) }
        }
    }
