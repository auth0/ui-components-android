package com.auth0.android.ui_components.presentation.navigation

import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.auth0.android.ui_components.domain.model.AuthenticatorType
import com.auth0.android.ui_components.presentation.ui.mfa.AuthenticatorEnrollmentScreen
import com.auth0.android.ui_components.presentation.ui.mfa.EnrolledAuthenticatorListScreen
import com.auth0.android.ui_components.presentation.ui.mfa.AuthenticatorMethodsScreen
import com.auth0.android.ui_components.presentation.ui.mfa.OTPVerificationScreen


@Composable
internal fun AuthenticatorSettingsNavigationHost(
    modifier: Modifier,
    navController: NavHostController,
) {

    if (0 != (LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)) {
        LaunchedEffect(Unit) {
            navController.currentBackStack
                .collect { entries ->
                    entries.map { it.destination.route }
                        .joinToString("\n")
                        .let {
                            Log.d("Navigation", "NavigationBackStack: \n$it")
                        }
                }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AuthenticatorRoute.AuthenticatorMethodList
    ) {
        composable<AuthenticatorRoute.AuthenticatorMethodList> {
            AuthenticatorMethodsScreen(
                modifier = modifier,
                onAuthenticatorItemClick = { mfaUiModel ->
                    if (mfaUiModel.confirmed) {
                        navController.navigate(
                            AuthenticatorRoute.EnrolledAuthenticatorMethod(
                                mfaUiModel.type
                            )
                        )
                    } else {
                        navController.navigate(
                            AuthenticatorRoute.EnrollAuthenticatorMethod(
                                mfaUiModel.type
                            )
                        )
                    }
                },
                onBackPress = {
                    navController.navigateUp()
                })
        }

        composable<AuthenticatorRoute.EnrolledAuthenticatorMethod> {
            val args = it.toRoute<AuthenticatorRoute.EnrolledAuthenticatorMethod>()
            EnrolledAuthenticatorListScreen(
                args.authenticatorType,
                onBackClick = { navController.navigateUp() },
                onAddClick = {
                    navController.navigate(AuthenticatorRoute.EnrollAuthenticatorMethod(args.authenticatorType))
                }
            )
        }

        composable<AuthenticatorRoute.EnrollAuthenticatorMethod> {
            val args = it.toRoute<AuthenticatorRoute.EnrollAuthenticatorMethod>()
            AuthenticatorEnrollmentScreen(
                authenticatorType = args.authenticatorType,
                onBackClick = {
                    navController.navigateUp()
                },
                onContinue = { authenticationId, authSession, phoneNumberOrEmail ->
                    when (args.authenticatorType) {
                        AuthenticatorType.RECOVERY_CODE, AuthenticatorType.PUSH -> {
                            navController.navigate(
                                AuthenticatorRoute.EnrolledAuthenticatorMethod(
                                    args.authenticatorType
                                )
                            ) {
                                popUpTo<AuthenticatorRoute.AuthenticatorMethodList> {
                                    inclusive = false
                                }
                            }
                        }

                        else -> {
                            navController.navigate(
                                AuthenticatorRoute.OTPVerification(
                                    authenticatorType = args.authenticatorType,
                                    authenticationId = authenticationId,
                                    authSession = authSession,
                                    phoneNumberOrEmail = phoneNumberOrEmail
                                )
                            )
                        }

                    }
                }
            )
        }

        composable<AuthenticatorRoute.OTPVerification> {
            val args = it.toRoute<AuthenticatorRoute.OTPVerification>()
            OTPVerificationScreen(
                authenticatorType = args.authenticatorType,
                authenticationId = args.authenticationId,
                authSession = args.authSession,
                phoneNumberOrEmail = args.phoneNumberOrEmail,
                onBackClick = {
                    navController.navigateUp()
                },
                onVerificationSuccess = {
                    navController.navigate(AuthenticatorRoute.EnrolledAuthenticatorMethod(args.authenticatorType)) {
                        popUpTo<AuthenticatorRoute.AuthenticatorMethodList> { inclusive = false }
                    }
                })
        }
    }
}