package nz.eloque.foss_wallet.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nz.eloque.foss_wallet.MainActivity
import nz.eloque.foss_wallet.R
import nz.eloque.foss_wallet.model.LocalizedPassWithTags
import nz.eloque.foss_wallet.shortcut.Shortcut

class NearbyPassesWidget : GlanceAppWidget() {
    private data class PassWithLogo(
        val localizedPass: LocalizedPassWithTags,
        val logo: Bitmap?,
    )

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val nearbyIds = entryPoint.nearbyPassesStore().nearbyPassIds.value
        val passes =
            withContext(Dispatchers.IO) {
                nearbyIds.mapNotNull { passId ->
                    val pass = entryPoint.passRepository().findById(passId) ?: return@mapNotNull null
                    val logo =
                        pass.pass
                            .logoFile(context)
                            ?.takeIf { it.exists() }
                            ?.let { BitmapFactory.decodeFile(it.absolutePath) }
                    PassWithLogo(pass, logo)
                }
            }

        provideContent {
            GlanceTheme {
                if (passes.isEmpty()) {
                    EmptyState(context)
                } else {
                    NearbyPassList(context, passes)
                }
            }
        }
    }

    @androidx.glance.GlanceComposable
    @androidx.compose.runtime.Composable
    private fun EmptyState(context: Context) {
        val openWalletIntent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        Box(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color.Transparent))
                    .padding(16.dp)
                    .clickable(actionStartActivity(openWalletIntent)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = context.getString(R.string.widget_view_all_passes),
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    ),
            )
        }
    }

    @androidx.glance.GlanceComposable
    @androidx.compose.runtime.Composable
    private fun NearbyPassList(
        context: Context,
        passes: List<PassWithLogo>,
    ) {
        LazyColumn(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color.Transparent)),
        ) {
            items(passes) { item ->
                PassRow(context, item)
            }
        }
    }

    @androidx.glance.GlanceComposable
    @androidx.compose.runtime.Composable
    private fun PassRow(
        context: Context,
        item: PassWithLogo,
    ) {
        val pass = item.localizedPass.pass
        val bgColor = pass.colors?.background ?: Color(0xFF1E88E5)
        val fgColor = pass.colors?.foreground ?: Color.White

        val detailIntent =
            Intent(
                Intent.ACTION_VIEW,
                "${Shortcut.BASE_URI}/${pass.id}".toUri(),
                context,
                MainActivity::class.java,
            )

        // Outer gap between cards
        Column(modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            // Shadow + border: 1dp border all around, extra 2dp right / 3dp bottom = drop shadow
            Column(
                modifier =
                    GlanceModifier
                        .fillMaxWidth()
                        .cornerRadius(16.dp)
                        .background(ColorProvider(Color(0x20000000)))
                        .padding(top = 1.dp, start = 1.dp, end = 3.dp, bottom = 4.dp),
            ) {
        Column(
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .cornerRadius(15.dp)
                    .background(ColorProvider(bgColor))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .clickable(actionStartActivity(detailIntent)),
        ) {
            Text(
                text = context.getString(R.string.widget_nearby_label),
                style =
                    TextStyle(
                        color = ColorProvider(fgColor.copy(alpha = 0.65f)),
                        fontSize = 11.sp,
                    ),
            )
            Spacer(GlanceModifier.height(6.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    val logoText = pass.logoText
                    if (!logoText.isNullOrBlank()) {
                        Text(
                            text = logoText,
                            style =
                                TextStyle(
                                    color = ColorProvider(fgColor.copy(alpha = 0.8f)),
                                    fontSize = 12.sp,
                                ),
                            maxLines = 1,
                        )
                        Spacer(GlanceModifier.height(2.dp))
                    }
                    Text(
                        text = pass.description,
                        style =
                            TextStyle(
                                color = ColorProvider(fgColor),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        maxLines = 2,
                    )
                }
                if (item.logo != null) {
                    Spacer(GlanceModifier.width(12.dp))
                    Image(
                        provider = ImageProvider(item.logo),
                        contentDescription = null,
                        modifier = GlanceModifier.size(48.dp).cornerRadius(8.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            }
            }
        }
    }
}
