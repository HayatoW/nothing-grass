package com.hayatow.nothinggrass.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import com.hayatow.nothinggrass.data.SettingsStore
import com.hayatow.nothinggrass.data.WidgetState
import com.hayatow.nothinggrass.ui.MainActivity
import com.hayatow.nothinggrass.work.RefreshWorker

class GrassWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = SettingsStore(context)
        val initial = store.current()
        provideContent {
            val state by store.state.collectAsState(initial)
            Content(state)
        }
    }

    @androidx.compose.runtime.Composable
    private fun Content(state: WidgetState) {
        val context = LocalContext.current
        val size = LocalSize.current
        val density = context.resources.displayMetrics.density
        val bitmap = GrassRenderer(context).render(
            (size.width.value * density).toInt(),
            (size.height.value * density).toInt(),
            state.login,
            state.calendar,
            GrassPalette.forContext(context, state.colorMode),
        )
        val action = if (state.login.isBlank()) actionStartActivity<MainActivity>() else actionRunCallback<RefreshAction>()
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = state.calendar?.let { "${state.login}: ${it.total} contributions" } ?: "GitHub contributions",
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxSize().clickable(action),
        )
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        RefreshWorker.refreshNow(context)
    }
}

class GrassWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GrassWidget()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val manager = AppWidgetManager.getInstance(context)
            onUpdate(context, manager, manager.getAppWidgetIds(ComponentName(context, javaClass)))
            return
        }
        super.onReceive(context, intent)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RefreshWorker.schedule(context)
    }
}
