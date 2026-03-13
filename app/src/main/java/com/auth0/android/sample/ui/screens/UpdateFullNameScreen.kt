package com.auth0.android.sample.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.auth0.android.ui_components.presentation.ui.components.GradientButton
import com.auth0.android.ui_components.presentation.ui.components.TopBar
import com.auth0.android.ui_components.theme.Auth0Theme

/**
 * Screen for updating the user's full name.
 *
 * Shows first and last name text fields with an Update button.
 *
 * @param currentFirstName Pre-filled first name
 * @param currentLastName Pre-filled last name
 * @param onBack Navigate back
 * @param onUpdate Callback with updated first and last names
 */
@Composable
fun UpdateFullNameScreen(
    currentFirstName: String = "Sarah",
    currentLastName: String = "Doe",
    onBack: () -> Unit,
    onUpdate: (String, String) -> Unit = { _, _ -> }
) {
    val colors = Auth0Theme.colors
    val typography = Auth0Theme.typography
    val dimensions = Auth0Theme.dimensions
    val sizes = Auth0Theme.sizes
    val shapes = Auth0Theme.shapes

    var firstName by rememberSaveable { mutableStateOf(currentFirstName) }
    var lastName by rememberSaveable { mutableStateOf(currentLastName) }

    Scaffold(
        topBar = {
            TopBar(title = "", showBackNavigation = true, onBackClick = onBack)
        },
        containerColor = colors.backgroundLayerBase
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = dimensions.spacingLg)
        ) {
            Spacer(modifier = Modifier.height(dimensions.spacingMd))

            Text(
                text = "Update Your Full Name",
                style = typography.display,
                color = colors.textBold
            )

            Spacer(modifier = Modifier.height(dimensions.spacingLg))

            Text(
                text = "First Name",
                style = typography.label,
                color = colors.textBold
            )
            Spacer(modifier = Modifier.height(dimensions.spacingXs))
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = typography.body,
                shape = shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.backgroundPrimary,
                    unfocusedBorderColor = colors.borderDefault,
                    focusedContainerColor = colors.backgroundLayerTop,
                    unfocusedContainerColor = colors.backgroundLayerTop
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(dimensions.spacingLg))

            Text(
                text = "Last Name",
                style = typography.label,
                color = colors.textBold
            )
            Spacer(modifier = Modifier.height(dimensions.spacingXs))
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = typography.body,
                shape = shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.backgroundPrimary,
                    unfocusedBorderColor = colors.borderDefault,
                    focusedContainerColor = colors.backgroundLayerTop,
                    unfocusedContainerColor = colors.backgroundLayerTop
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(dimensions.spacingXl))

            GradientButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sizes.buttonHeight),
                onClick = {
                    onUpdate(firstName, lastName)
                    onBack()
                }
            ) {
                Text(
                    text = "Update",
                    style = typography.label,
                    color = colors.textOnPrimary
                )
            }
        }
    }
}
