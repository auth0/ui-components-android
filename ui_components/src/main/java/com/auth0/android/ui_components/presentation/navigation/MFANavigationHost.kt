package com.auth0.android.ui_components.presentation.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.auth0.android.ui_components.presentation.ui.mfa.AuthenticatorEnrollmentScreen
import com.auth0.android.ui_components.presentation.ui.mfa.EnrolledAuthenticatorListScreen
import com.auth0.android.ui_components.presentation.ui.mfa.MFAMethodsScreen
import com.auth0.android.ui_components.presentation.ui.mfa.OTPVerificationScreen


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
                        navController.navigate(EnrolledAuthenticator(mfaUiModel.type))
                    } else {
                        navController.navigate(EnrollAuthenticator(mfaUiModel.type))
                    }
                },
                onBackPress = {
                    Log.d("TAG", "onBackPress")
                })
        }

        composable<EnrolledAuthenticator> {
            val args = it.toRoute<EnrolledAuthenticator>()
            EnrolledAuthenticatorListScreen(
                args.authenticatorType,
            )
        }

        composable<EnrollAuthenticator> {
            val args = it.toRoute<EnrolledAuthenticator>()
            AuthenticatorEnrollmentScreen(authenticatorType = args.authenticatorType) { authenticationId, authSession ->
                navController.navigate(
                    OTPVerification(
                        args.authenticatorType,
                        authenticationId,
                        authSession
                    )
                )
            }
        }

        composable<OTPVerification> {
            val args = it.toRoute<OTPVerification>()
            OTPVerificationScreen(
                args.authenticatorType, args.authenticationId, args.authSession,
                onBackClick = {
                    navController.navigateUp()
                },
                onVerificationSuccess = {
                    navController.navigate(EnrolledAuthenticator(args.authenticatorType))
                })
        }
    }
}