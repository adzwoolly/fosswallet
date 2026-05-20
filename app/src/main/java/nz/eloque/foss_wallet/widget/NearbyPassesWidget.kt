package nz.eloque.foss_wallet.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.android.EntryPointAccessors
import nz.eloque.foss_wallet.MainActivity
import nz.eloque.foss_wallet.model.LocalizedPassWithTags
import nz.eloque.foss_wallet.shortcut.Shortcut

class NearbyPassesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val nearbyIds = entryPoint.nearbyPassesStore().nearbyPassIds.value
        val passes = withContext(Dispatchers.IO) {
            nearbyIds.mapNotNull { entryPoint.passRepository().findById(it) }
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
                    .background(GlanceTheme.colors.widgetBackground)
                    .padding(16.dp)
                    .clickable(actionStartActivity(openWalletIntent)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = context.getString(nz.eloque.foss_wallet.R.string.widget_view_all_passes),
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
            )
        }
    }

    @androidx.glance.GlanceComposable
    @androidx.compose.runtime.Composable
    private fun NearbyPassList(
        context: Context,
        passes: List<LocalizedPassWithTags>,
    ) {
        LazyColumn(modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground)) {
            items(passes) { localizedPass ->
                PassRow(context, localizedPass)
            }
        }
    }

    @androidx.glance.GlanceComposable
    @androidx.compose.runtime.Composable
    private fun PassRow(
        context: Context,
        localizedPass: LocalizedPassWithTags,
    ) {
        val pass = localizedPass.pass
        val bgColor = pass.colors?.background ?: Color(0xFF1E88E5)
        val fgColor = pass.colors?.foreground ?: Color.White

        val detailIntent =
            Intent(
                Intent.ACTION_VIEW,
                "${Shortcut.BASE_URI}/${pass.id}".toUri(),
                context,
                MainActivity::class.java,
            )

        Column(
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .clickable(actionStartActivity(detailIntent)),
        ) {
            // Nearby badge strip
            Box(
                modifier =
                    GlanceModifier
                        .fillMaxWidth()
                        .background(GlanceTheme.colors.tertiaryContainer)
                        .padding(horizontal = 12.dp, vertical = 2.dp),
            ) {
                Text(
                    text = context.getString(nz.eloque.foss_wallet.R.string.widget_nearby_label),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onTertiaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                )
            }
            // Pass card body
            Column(
                modifier =
                    GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(bgColor))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                val logoText = pass.logoText
                if (!logoText.isNullOrBlank()) {
                    Text(
                        text = logoText,
                        style =
                            TextStyle(
                                color = ColorProvider(fgColor.copy(alpha = 0.7f)),
                                fontSize = 10.sp,
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    maxLines = 2,
                )
            }
        }
    }
}
