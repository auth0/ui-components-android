package com.auth0.android.ui_components.data.mapper

import com.auth0.android.ui_components.domain.model.EnrollmentChallenge
import com.auth0.android.ui_components.domain.model.MfaEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.RecoveryCodeEnrollmentChallenge
import com.auth0.android.ui_components.domain.model.TotpEnrollmentChallenge
import com.auth0.android.result.EnrollmentChallenge as SdkEnrollmentChallenge
import com.auth0.android.result.MfaEnrollmentChallenge as SdkMfaEnrollmentChallenge
import com.auth0.android.result.RecoveryCodeEnrollmentChallenge as SdkRecoveryCodeEnrollmentChallenge
import com.auth0.android.result.TotpEnrollmentChallenge as SdkTotpEnrollmentChallenge

/**
 * Mapper functions to convert Auth0 SDK enrollment challenges to local domain models.
 * This decouples the UI components from SDK types for better flexibility.
 */

fun SdkMfaEnrollmentChallenge.toDomainModel(): MfaEnrollmentChallenge {
    return MfaEnrollmentChallenge(
        id = this.id,
        authSession = this.authSession
    )
}


fun SdkTotpEnrollmentChallenge.toDomainModel(): TotpEnrollmentChallenge {
    return TotpEnrollmentChallenge(
        id = this.id,
        authSession = this.authSession,
        barcodeUri = this.barcodeUri,
        manualInputCode = this.manualInputCode
    )
}

fun SdkRecoveryCodeEnrollmentChallenge.toDomainModel(): RecoveryCodeEnrollmentChallenge {
    return RecoveryCodeEnrollmentChallenge(
        id = this.id,
        authSession = this.authSession,
        recoveryCode = this.recoveryCode
    )
}

fun SdkEnrollmentChallenge.toDomainModel(): EnrollmentChallenge {
    return when (this) {
        is SdkTotpEnrollmentChallenge -> this.toDomainModel()
        is SdkRecoveryCodeEnrollmentChallenge -> this.toDomainModel()
        is SdkMfaEnrollmentChallenge -> this.toDomainModel()
    }
}
