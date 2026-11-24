package com.auth0.universalcomponents.feature.embedded.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.auth0.universalcomponents.feature.embedded.EmbeddedEntryParts
import com.auth0.universalcomponents.feature.embedded.LocalEmbeddedEntry
import com.auth0.universalcomponents.feature.embedded.resolve
import com.auth0.universalcomponents.theme.Auth0Theme
import com.auth0.universalcomponents.theme.tintOr

/**
 * The first screen of embedded login: the logo, the [title], the [form] (the field and Continue) and, when there are
 * other ways to sign in, [alternatives]. The app's [com.auth0.universalcomponents.feature.embedded.EmbeddedEntryLayout]
 * places these inside the screen's padding. The screen scrolls when it doesn't fit, as in landscape or with large text,
 * and keeps clear of the keyboard.
 */
@Composable
internal fun EmbeddedEntryScaffold(
    title: String,
    form: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    alternatives: (@Composable () -> Unit)? = null,
) {
    val entry = LocalEmbeddedEntry.current
    val parts = EmbeddedEntryParts(
        logo = entry.logo.resolve { BrandLogo() },
        title = { EntryTitle(title) },
        form = form,
        alternatives = alternatives,
    )
    BoxWithConstraints(modifier = modifier.fillMaxSize().imePadding()) {
        // At least the height of the screen, handed on to the layout, so a centered layout has room to center in.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(Auth0Theme.dimensions.spacingLg),
            propagateMinConstraints = true,
        ) {
            entry.layout.content(parts)
        }
    }
}

/** The theme's logo at the theme's height, however wide it is, tinted as [tintOr] says. */
@Composable
private fun BrandLogo() {
    val brand = Auth0Theme.brand
    val painter = painterResource(brand.logo)
    val intrinsic = painter.intrinsicSize
    // Scaled by its height, so a logo of another shape keeps it.
    val shape = if (intrinsic.isSpecified && intrinsic.height > 0f) {
        Modifier.aspectRatio(intrinsic.width / intrinsic.height, matchHeightConstraintsFirst = true)
    } else {
        Modifier
    }
    Icon(
        painter = painter,
        contentDescription = brand.logoDescription,
        modifier = Modifier.height(Auth0Theme.sizes.logoHeight).then(shape),
        tint = brand.tintOr(Auth0Theme.colors.textBold),
    )
}

@Composable
private fun EntryTitle(title: String) {
    Text(
        text = title,
        style = Auth0Theme.typography.display,
        color = Auth0Theme.colors.textBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().semantics { heading() },
    )
}

/**
 * The default layouts: the logo, the title, the form and the other ways, one below the other, in the middle of the
 * screen or at the top.
 */
@Composable
internal fun StackedEntry(parts: EmbeddedEntryParts, isCentered: Boolean) {
    val dimensions = Auth0Theme.dimensions
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isCentered) Arrangement.Center else Arrangement.Top,
    ) {
        parts.logo?.let { logo ->
            logo()
            Spacer(Modifier.height(dimensions.spacingLg))
        }
        parts.title()
        Spacer(Modifier.height(dimensions.spacingXl))
        parts.form()
        parts.alternatives?.let { alternatives ->
            Spacer(Modifier.height(dimensions.spacingLg))
            alternatives()
        }
    }
}
