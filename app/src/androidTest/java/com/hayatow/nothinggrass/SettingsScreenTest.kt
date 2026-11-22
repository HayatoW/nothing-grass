package com.hayatow.nothinggrass

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hayatow.nothinggrass.data.HtmlContributionParser
import com.hayatow.nothinggrass.widget.GrassPalette
import com.hayatow.nothinggrass.widget.GrassRenderer
import com.hayatow.nothinggrass.data.ColorMode
import com.hayatow.nothinggrass.ui.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun saveFetchesContributions() {
        rule.onNodeWithTag("login").performTextReplacement("")
        rule.onNodeWithTag("save").assertIsNotEnabled()
        rule.onNodeWithTag("login").performTextReplacement("HayatoW")
        rule.onNodeWithTag("mode_GREEN").performClick()
        rule.onNodeWithTag("save").assertIsEnabled().performClick()
        rule.waitUntil(30_000) { rule.onAllNodes(androidx.compose.ui.test.hasTestTag("summary")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun rendererDrawsTranslucentCard() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val calendar = HtmlContributionParser.parse("<td data-date=\"2026-01-01\" data-level=\"3\" class=\"ContributionCalendar-day\"></td>")
        val bitmap = GrassRenderer(context).render(400, 200, "HayatoW", calendar, GrassPalette.of(ColorMode.MONO, dark = true))
        assertEquals(400, bitmap.width)
        val alpha = android.graphics.Color.alpha(bitmap.getPixel(200, 100))
        assertTrue("card should be translucent, alpha=$alpha", alpha in 1..254)
        assertEquals(0, android.graphics.Color.alpha(bitmap.getPixel(0, 0)))
    }
}
