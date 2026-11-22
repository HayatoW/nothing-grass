package com.hayatow.nothinggrass.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import com.hayatow.nothinggrass.data.ColorMode
import com.hayatow.nothinggrass.data.SettingsStore
import com.hayatow.nothinggrass.widget.GrassPalette
import com.hayatow.nothinggrass.widget.GrassRenderer
import com.hayatow.nothinggrass.widget.GrassWidget
import com.hayatow.nothinggrass.widget.GrassWidgetReceiver
import com.hayatow.nothinggrass.work.RefreshWorker
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFD71921))) {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF000000)) { SettingsScreen() }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    val state by store.state.collectAsState(initial = null)
    var login by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(ColorMode.MONO) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        val s = state ?: return@LaunchedEffect
        if (!loaded) { login = s.login; token = s.token; mode = s.colorMode; loaded = true }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("GitHub の草", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        val density = LocalDensity.current
        val s = state
        val preview = remember(s, mode) {
            with(density) {
                GrassRenderer(context).render(
                    340.dp.roundToPx(), 160.dp.roundToPx(), s?.login.orEmpty(), s?.calendar,
                    GrassPalette.of(mode, dark = true, accent = if (mode == ColorMode.WALLPAPER) 0xFFFFB4AB.toInt() else null),
                ).asImageBitmap()
            }
        }
        Image(preview, "プレビュー", Modifier.fillMaxWidth().height(160.dp).testTag("preview"))
        OutlinedTextField(login, { login = it.trim() }, Modifier.fillMaxWidth().testTag("login"), label = { Text("GitHub ユーザー名") }, singleLine = true)
        OutlinedTextField(
            token, { token = it.trim() }, Modifier.fillMaxWidth().testTag("token"),
            label = { Text("トークン（任意・非公開の草も含める）") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        Text("色", color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ColorMode.MONO to "モノクロ", ColorMode.GREEN to "GitHub 緑", ColorMode.WALLPAPER to "壁紙").forEach { (m, label) ->
                FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(label) }, modifier = Modifier.testTag("mode_$m"))
            }
        }
        Button(
            onClick = {
                scope.launch {
                    store.saveSettings(login, token, mode)
                    GrassWidget().updateAll(context)
                    RefreshWorker.schedule(context)
                    RefreshWorker.refreshNow(context)
                }
            },
            enabled = login.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("save"),
        ) { Text("保存して更新") }
        OutlinedButton(
            onClick = {
                val manager = AppWidgetManager.getInstance(context)
                if (manager.isRequestPinAppWidgetSupported) {
                    manager.requestPinAppWidget(ComponentName(context, GrassWidgetReceiver::class.java), null, null)
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("pin"),
        ) { Text("ホーム画面に追加") }
        s?.error?.let { Text("取得エラー: $it", color = Color(0xFFFF6B6B), modifier = Modifier.testTag("error")) }
        s?.calendar?.let {
            val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(s.updatedAt))
            Text("1 年で ${it.total} コントリビューション・連続 ${it.currentStreak} 日（$time 更新）", color = Color.Gray, modifier = Modifier.testTag("summary"))
        }
        Text("ウィジェットをタップすると再取得します。トークンは端末内にのみ保存されます。", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}
