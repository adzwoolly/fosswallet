package nz.eloque.foss_wallet.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import nz.eloque.foss_wallet.MainActivity
import nz.eloque.foss_wallet.R
import nz.eloque.foss_wallet.model.Pass
import nz.eloque.foss_wallet.shortcut.Shortcut
import kotlin.random.Random

const val CHANNEL_ID: String = "pass_updates"
const val NEARBY_CHANNEL_ID: String = "nearby_passes"

class NotificationService
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        fun createNotificationChannel() {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.pass_updates_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.pass_updates_channel)
                }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        fun post(message: String?) {
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) &&
                message != null
            ) {
                val builder =
                    NotificationCompat
                        .Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.icon) // Ensure this exists
                        .setContentTitle(context.getString(R.string.pass_updated))
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                with(NotificationManagerCompat.from(context)) {
                    notify(Random.nextInt(), builder.build())
                }
            }
        }

        fun createNearbyNotificationChannel() {
            val channel = NotificationChannel(
                NEARBY_CHANNEL_ID,
                context.getString(R.string.nearby_passes_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.nearby_passes_channel_description)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        fun updateNearbyNotifications(passes: List<Pass>) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) return

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationManagerCompat = NotificationManagerCompat.from(context)

            val currentIds = passes.map { it.id.hashCode() }.toSet()
            notificationManager.activeNotifications
                .filter { it.notification.channelId == NEARBY_CHANNEL_ID }
                .filter { it.id !in currentIds }
                .forEach { notificationManagerCompat.cancel(it.id) }

            passes.forEach { pass ->
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    pass.id.hashCode(),
                    Intent(Intent.ACTION_VIEW, "${Shortcut.BASE_URI}/${pass.id}".toUri(), context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val publicVersion = NotificationCompat.Builder(context, NEARBY_CHANNEL_ID)
                    .setSmallIcon(R.drawable.icon)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(R.string.nearby_pass))
                    .build()

                val notification = NotificationCompat.Builder(context, NEARBY_CHANNEL_ID)
                    .setSmallIcon(R.drawable.icon)
                    .setContentTitle(context.getString(R.string.pass_nearby))
                    .setContentText(pass.description)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPublicVersion(publicVersion)
                    .setContentIntent(pendingIntent)
                    .setOngoing(false)
                    .build()

                notificationManagerCompat.notify(pass.id.hashCode(), notification)
            }
        }
    }
