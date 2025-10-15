package com.auth0.android.ui_components.presentation.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.auth0.android.ui_components.presentation.ui.mfa.MFAEnrolledScreen
import com.auth0.android.ui_components.presentation.ui.mfa.MFAMethodsScreen


@Composable
internal fun MFANavigationHost(
    modifier: Modifier,
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = MFAMethodList
    ) {
        composable<MFAMethodList> {
            MFAMethodsScreen(
                modifier = modifier,
                onAuthenticatorClick = { mfaUiModel ->
                    if (mfaUiModel.confirmed) {
                        navController.navigate(MFAEnrolledItem(mfaUiModel.type))
                    } else {

                    }
                },
                onBackPress = {
                    Log.d("TAG", "onBackPress")
                })
        }

        composable<MFAEnrolledItem> {
            val args = it.toRoute<MFAEnrolledItem>()
            MFAEnrolledScreen(
                args.authenticatorType,
            )
        }
    }
}