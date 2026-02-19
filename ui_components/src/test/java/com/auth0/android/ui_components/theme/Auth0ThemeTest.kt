package com.auth0.android.ui_components.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Test suite for Auth0Theme Phase 2 functionality.
 */
class Auth0ThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Auth0Theme provides all tokens`() {
        var capturedColor: Auth0Color? = null
        var capturedTypography: Auth0Typography? = null
        var capturedShapes: Auth0Shapes? = null
        var capturedDimensions: Auth0Dimensions? = null

        composeTestRule.setContent {
            Auth0Theme {
                capturedColor = Auth0TokenDefaults.color()
                capturedTypography = Auth0TokenDefaults.typography()
                capturedShapes = Auth0TokenDefaults.shapes()
                capturedDimensions = Auth0TokenDefaults.dimensions()
            }
        }

        composeTestRule.waitForIdle()

        // All accessors should work without error
        assertNotNull(capturedColor, "Auth0Color should be provided")
        assertNotNull(capturedTypography, "Auth0Typography should be provided")
        assertNotNull(capturedShapes, "Auth0Shapes should be provided")
        assertNotNull(capturedDimensions, "Auth0Dimensions should be provided")
    }

    @Test
    fun `Auth0Theme uses light mode by default`() {
        var capturedColor: Auth0Color? = null

        composeTestRule.setContent {
            Auth0Theme(darkTheme = false) {
                capturedColor = Auth0TokenDefaults.color()
            }
        }

        composeTestRule.waitForIdle()

        // Should use light mode colors
        assertEquals(
            Auth0Color.light().primary,
            capturedColor?.primary,
            "Should use light mode primary color"
        )
    }

    @Test
    fun `Auth0Theme uses dark mode when specified`() {
        var capturedColor: Auth0Color? = null

        composeTestRule.setContent {
            Auth0Theme(darkTheme = true) {
                capturedColor = Auth0TokenDefaults.color()
            }
        }

        composeTestRule.waitForIdle()

        // Should use dark mode colors
        assertEquals(
            Auth0Color.dark().primary,
            capturedColor?.primary,
            "Should use dark mode primary color"
        )
    }

    @Test
    fun `Auth0Theme uses custom configuration`() {
        val customPrimary = Color(0xFFFF6B00)
        var capturedColor: Auth0Color? = null

        composeTestRule.setContent {
            Auth0Theme(
                configuration = Auth0ThemeConfiguration(
                    color = Auth0Color.light().copy(primary = customPrimary)
                )
            ) {
                capturedColor = Auth0TokenDefaults.color()
            }
        }

        composeTestRule.waitForIdle()

        // Should use custom primary color
        assertEquals(
            customPrimary,
            capturedColor?.primary,
            "Should use custom primary color"
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `Auth0TokenDefaults throws error outside Auth0Theme`() {
        composeTestRule.setContent {
            // Not wrapped in Auth0Theme - should throw
            Auth0TokenDefaults.color()
        }

        composeTestRule.waitForIdle()
    }
}
