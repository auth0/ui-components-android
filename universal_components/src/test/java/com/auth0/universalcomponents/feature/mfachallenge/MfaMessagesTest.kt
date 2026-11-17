package com.auth0.universalcomponents.feature.mfachallenge

import android.content.res.Resources
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.mfachallenge.presentation.endingDigits
import com.auth0.universalcomponents.feature.mfachallenge.presentation.resolve
import com.auth0.universalcomponents.feature.mfachallenge.presentation.screens.words
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class MfaMessagesTest {

    /** Stands in for the string resources: each default is its resource id and its argument. */
    private val resources = mockk<Resources> {
        every { getString(any<Int>()) } answers { default(firstArg()) }
        every { getString(any<Int>(), any()) } answers { default(firstArg(), *secondArg<Array<Any>>()) }
    }

    private fun default(id: Int, vararg args: Any) = "default($id${args.joinToString(prefix = ",", separator = ",")})"

    @Test
    fun `nothing set - every word is the string resource's`() {
        val words = MfaMessages().resolve(resources)

        assertThat(words.pushChallenge.title).isEqualTo(default(R.string.auth0_mfa_push_challenge_title))
        assertThat(words.pushChallenge.waiting).isEqualTo(default(R.string.auth0_mfa_push_challenge_waiting))
        assertThat(words.loading.accessibilityLabel)
            .isEqualTo(default(R.string.auth0_mfa_loading_accessibility_label))
        assertThat(words.errors.startOver).isEqualTo(default(R.string.auth0_mfa_errors_start_over))
        assertThat(words.authenticatorList.headers.challengeAdditional.title)
            .isEqualTo(default(R.string.auth0_mfa_authenticator_list_headers_challenge_additional_title))
    }

    @Test
    fun `one key set - replaces that key only, the others keep their defaults`() {
        val words = MfaMessages { pushChallenge { title = "Approve on your phone" } }.resolve(resources)

        assertThat(words.pushChallenge.title).isEqualTo("Approve on your phone")
        assertThat(words.pushChallenge.waiting).isEqualTo(default(R.string.auth0_mfa_push_challenge_waiting))
        assertThat(words.pushChallenge.verifyAnotherWay)
            .isEqualTo(default(R.string.auth0_mfa_push_challenge_verify_another_way))
    }

    @Test
    fun `nested keys - a header's title is changed without its description`() {
        val words = MfaMessages {
            authenticatorList { headers { challengeAdditional { title = "Almost there" } } }
        }.resolve(resources)

        val header = words.authenticatorList.headers.challengeAdditional
        assertThat(header.title).isEqualTo("Almost there")
        assertThat(header.description)
            .isEqualTo(default(R.string.auth0_mfa_authenticator_list_headers_challenge_additional_description))
        assertThat(words.authenticatorList.headers.challenge.title)
            .isEqualTo(default(R.string.auth0_mfa_authenticator_list_headers_challenge_title))
    }

    @Test
    fun `dynamic text - the device name and the seconds reach the app's function`() {
        val words = MfaMessages {
            pushChallenge {
                description = { device -> "Sent to $device." }
                resend { cooldown = { seconds -> "Again in ${seconds}s" } }
            }
        }.resolve(resources)

        assertThat(words.pushChallenge.description("Jane's phone")).isEqualTo("Sent to Jane's phone.")
        assertThat(words.pushChallenge.resend.cooldown(42)).isEqualTo("Again in 42s")
    }

    @Test
    fun `dynamic text - left unset - the string resource is formatted with the argument`() {
        val words = MfaMessages().resolve(resources)

        assertThat(words.pushChallenge.description("Jane's phone"))
            .isEqualTo(default(R.string.auth0_mfa_push_challenge_description, "Jane's phone"))
        assertThat(words.pushChallenge.resend.cooldown(7))
            .isEqualTo(default(R.string.auth0_mfa_push_challenge_resend_cooldown, 7))
    }

    @Test
    fun `error text - every case - has its own words, and an override changes one case only`() {
        val words = MfaMessages { errors { rejected { title = "Denied" } } }.resolve(resources)

        val cases = listOf(
            MfaError.Cancelled(),
            MfaError.TokenExpired(),
            MfaError.TokenInvalid(),
            MfaError.Rejected(),
            MfaError.TooManyAttempts(),
            MfaError.NoEligibleFactors(),
            MfaError.Network(),
            MfaError.Unexpected(),
        )
        val titles = cases.map { words.errors.messageFor(it).title }

        assertThat(titles).containsNoDuplicates()
        // Never shown, so it has no words of its own.
        assertThat(words.errors.messageFor(MfaError.PresentationUnavailable()))
            .isSameInstanceAs(words.errors.messageFor(MfaError.Unexpected()))
        assertThat(words.errors.messageFor(MfaError.Rejected()).title).isEqualTo("Denied")
        assertThat(words.errors.messageFor(MfaError.Rejected()).description)
            .isEqualTo(default(R.string.auth0_mfa_errors_rejected_description))
    }

    @Test
    fun `row - the factor in the list - has a title and a description, either of which can be changed`() {
        val words = MfaMessages { pushChallenge { row { description = "Tap to approve." } } }.resolve(resources)

        assertThat(words.pushChallenge.row.title).isEqualTo(default(R.string.auth0_mfa_push_challenge_row_title))
        assertThat(words.pushChallenge.row.description).isEqualTo("Tap to approve.")
    }

    @Test
    fun `error text - the way out - Start over and Close can be changed`() {
        val words = MfaMessages {
            errors {
                startOver = "Restart"
                close = "Done"
            }
        }.resolve(resources)

        assertThat(words.errors.startOver).isEqualTo("Restart")
        assertThat(words.errors.close).isEqualTo("Done")
    }

    @Test
    fun `phone challenge - nothing set - every word is the string resource's, naming the end of the number`() {
        val words = MfaMessages().resolve(resources).phoneChallenge

        assertThat(words.title).isEqualTo(default(R.string.auth0_mfa_phone_challenge_title))
        assertThat(words.codeLabel).isEqualTo(default(R.string.auth0_mfa_phone_challenge_code_label))
        // The server hides a number with X (or *); only the digits it shows are named, and as the end of the number.
        assertThat(words.description("XXXXXXXXX0128"))
            .isEqualTo(default(R.string.auth0_mfa_phone_challenge_description_ending_in, "0128"))
        assertThat(words.descriptionVoice("XXXXXXXXX0128"))
            .isEqualTo(default(R.string.auth0_mfa_phone_challenge_description_voice_ending_in, "0128"))
        // The server may show fewer than four: they are all named.
        assertThat(words.description("(4**)***-*900"))
            .isEqualTo(default(R.string.auth0_mfa_phone_challenge_description_ending_in, "900"))
        assertThat(words.description("+1 (XXX) XXX-0128"))
            .isEqualTo(default(R.string.auth0_mfa_phone_challenge_description_ending_in, "0128"))
        // Whatever the server uses to hide a digit counts as hidden.
        assertThat(words.description("∗＊X●0128"))
            .isEqualTo(default(R.string.auth0_mfa_phone_challenge_description_ending_in, "0128"))
        // A number typed in full is named by its last four.
        assertThat(words.descriptionVoice("+14155550123"))
            .isEqualTo(default(R.string.auth0_mfa_phone_challenge_description_voice_ending_in, "0123"))
        // The list row names the way the code comes, as the design does; an app can add the number itself.
        assertThat(words.rowText("***9239")).isEqualTo(default(R.string.auth0_mfa_phone_challenge_row_text))
        assertThat(words.rowVoice("***9239")).isEqualTo(default(R.string.auth0_mfa_phone_challenge_row_voice))
        assertThat(words.resend.cooldown(30)).isEqualTo(default(R.string.auth0_mfa_phone_challenge_resend_cooldown, 30))
    }

    @Test
    fun `phone challenge - a number the server hid completely - is not named in the default words`() {
        val words = MfaMessages().resolve(resources).phoneChallenge

        listOf("*********", "XXXXXXXXX", "").forEach { hidden ->
            assertThat(words.description(hidden))
                .isEqualTo(default(R.string.auth0_mfa_phone_challenge_description_hidden))
            assertThat(words.descriptionVoice(hidden))
                .isEqualTo(default(R.string.auth0_mfa_phone_challenge_description_voice_hidden))
        }
    }

    @Test
    fun `ending digits - the last digits the server shows - and nothing when it shows none`() {
        mapOf(
            "+14155550123" to "0123",
            "+1 (415) 555-0123" to "0123",
            "(4**)***-*900" to "900",
            "+1******900" to "900",
            "+1XXXXXX0123" to "0123",
            "••• 9239" to "9239",
            "***-**-12" to "12",
            "+4412" to "4412",
            // Only the digits after the last hidden one are the end of the number.
            "(4**)*9*0" to "0",
        ).forEach { (number, ending) -> assertThat(number.endingDigits()).isEqualTo(ending) }

        listOf("", "*****", "••••", "XXXX", "(***) ***-****", "no digits")
            .forEach { assertThat(it.endingDigits()).isNull() }
    }

    @Test
    fun `ending digits - a count is chosen - takes that many`() {
        assertThat("+14155550123".endingDigits(count = 2)).isEqualTo("23")
        assertThat("+14155550123".endingDigits(count = 20)).isEqualTo("14155550123")
    }

    @Test
    fun `phone challenge - some set - replaces those, the others keep their defaults`() {
        val words = MfaMessages {
            phoneChallenge {
                title = "Enter your code"
                description = { number -> "Texted $number" }
                resend { button = "Send again" }
            }
        }.resolve(resources).phoneChallenge

        assertThat(words.title).isEqualTo("Enter your code")
        assertThat(words.description("***9239")).isEqualTo("Texted ***9239")
        assertThat(words.resend.button).isEqualTo("Send again")
        assertThat(words.wrongCode).isEqualTo(default(R.string.auth0_mfa_phone_challenge_wrong_code))
        assertThat(words.verifyAnotherWay).isEqualTo(default(R.string.auth0_mfa_phone_challenge_verify_another_way))
    }

    @Test
    fun `email challenge - nothing set - every word is the string resource's, with the address filled in`() {
        val words = MfaMessages().resolve(resources).emailChallenge

        assertThat(words.title).isEqualTo(default(R.string.auth0_mfa_email_challenge_title))
        assertThat(words.description("j***@example.com"))
            .isEqualTo(default(R.string.auth0_mfa_email_challenge_description, "j***@example.com"))
        assertThat(words.descriptionUnnamed)
            .isEqualTo(default(R.string.auth0_mfa_email_challenge_description_unnamed))
        assertThat(words.codeLabel).isEqualTo(default(R.string.auth0_mfa_email_challenge_code_label))
        assertThat(words.verifyButton).isEqualTo(default(R.string.auth0_mfa_email_challenge_verify_button))
        assertThat(words.wrongCode).isEqualTo(default(R.string.auth0_mfa_email_challenge_wrong_code))
        assertThat(words.expiredCode).isEqualTo(default(R.string.auth0_mfa_email_challenge_expired_code))
        assertThat(words.noConnection).isEqualTo(default(R.string.auth0_mfa_email_challenge_no_connection))
        assertThat(words.tooManyRequests).isEqualTo(default(R.string.auth0_mfa_email_challenge_too_many_requests))
        assertThat(words.row.title).isEqualTo(default(R.string.auth0_mfa_email_challenge_row_title))
        assertThat(words.row.description).isEqualTo(default(R.string.auth0_mfa_email_challenge_row_description))
        assertThat(words.resend.button).isEqualTo(default(R.string.auth0_mfa_email_challenge_resend_button))
        assertThat(words.resend.cooldown(30)).isEqualTo(default(R.string.auth0_mfa_email_challenge_resend_cooldown, 30))
    }

    @Test
    fun `email challenge - some set - replaces those, the others keep their defaults`() {
        val words = MfaMessages {
            emailChallenge {
                title = "Check your inbox"
                description = { address -> "Mailed to $address" }
                row { title = "Email" }
                resend { cooldown = { seconds -> "Wait $seconds" } }
            }
        }.resolve(resources).emailChallenge

        assertThat(words.title).isEqualTo("Check your inbox")
        assertThat(words.description("j***@example.com")).isEqualTo("Mailed to j***@example.com")
        assertThat(words.row.title).isEqualTo("Email")
        assertThat(words.row.description).isEqualTo(default(R.string.auth0_mfa_email_challenge_row_description))
        assertThat(words.resend.cooldown(5)).isEqualTo("Wait 5")
        assertThat(words.resend.button).isEqualTo(default(R.string.auth0_mfa_email_challenge_resend_button))
        assertThat(words.verifyAnotherWay).isEqualTo(default(R.string.auth0_mfa_email_challenge_verify_another_way))
    }

    @Test
    fun `totp challenge - nothing set - every word is the string resource's`() {
        val words = MfaMessages().resolve(resources).totpChallenge

        assertThat(words.title).isEqualTo(default(R.string.auth0_mfa_totp_challenge_title))
        assertThat(words.description).isEqualTo(default(R.string.auth0_mfa_totp_challenge_description))
        assertThat(words.codeLabel).isEqualTo(default(R.string.auth0_mfa_totp_challenge_code_label))
        assertThat(words.verifyButton).isEqualTo(default(R.string.auth0_mfa_totp_challenge_verify_button))
        assertThat(words.noConnection).isEqualTo(default(R.string.auth0_mfa_totp_challenge_no_connection))
        assertThat(words.wrongCode).isEqualTo(default(R.string.auth0_mfa_totp_challenge_wrong_code))
        assertThat(words.verifyAnotherWay).isEqualTo(default(R.string.auth0_mfa_totp_challenge_verify_another_way))
        assertThat(words.row.title).isEqualTo(default(R.string.auth0_mfa_totp_challenge_row_title))
        assertThat(words.row.description).isEqualTo(default(R.string.auth0_mfa_totp_challenge_row_description))
    }

    @Test
    fun `totp challenge - some set - replaces those, the others keep their defaults`() {
        val words = MfaMessages {
            totpChallenge {
                title = "Type your code"
                description = "Open your authenticator"
                row { title = "Authenticator" }
            }
        }.resolve(resources).totpChallenge

        assertThat(words.title).isEqualTo("Type your code")
        assertThat(words.description).isEqualTo("Open your authenticator")
        assertThat(words.row.title).isEqualTo("Authenticator")
        assertThat(words.row.description).isEqualTo(default(R.string.auth0_mfa_totp_challenge_row_description))
        assertThat(words.verifyButton).isEqualTo(default(R.string.auth0_mfa_totp_challenge_verify_button))
        assertThat(words.wrongCode).isEqualTo(default(R.string.auth0_mfa_totp_challenge_wrong_code))
    }

    @Test
    fun `recovery code challenge - nothing set - every word is the string resource's`() {
        val words = MfaMessages().resolve(resources).recoveryCodeChallenge

        assertThat(words.title).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_title))
        assertThat(words.description).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_description))
        assertThat(words.codeLabel).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_code_label))
        assertThat(words.placeholder).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_placeholder))
        assertThat(words.continueButton).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_continue_button))
        assertThat(words.wrongCode).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_wrong_code))
        assertThat(words.noConnection).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_no_connection))
        assertThat(words.verifyAnotherWay)
            .isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_verify_another_way))
        assertThat(words.showCode).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_show_code))
        assertThat(words.hideCode).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_hide_code))
        assertThat(words.saveTitle).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_save_title))
        assertThat(words.saveDescription)
            .isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_save_description))
        assertThat(words.saveHelper).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_save_helper))
        assertThat(words.savedConfirmation)
            .isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_saved_confirmation))
        assertThat(words.copied).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_copied))
        assertThat(words.copyCode).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_copy_code))
        assertThat(words.row.title).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_row_title))
        assertThat(words.row.description).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_row_description))
    }

    @Test
    fun `recovery code challenge - some set - replaces those, the others keep their defaults`() {
        val words = MfaMessages {
            recoveryCodeChallenge {
                saveDescription = "Keep it for when you lose your device"
                savedConfirmation = "I have stored this code"
                row { title = "Backup code" }
            }
        }.resolve(resources).recoveryCodeChallenge

        assertThat(words.saveDescription).isEqualTo("Keep it for when you lose your device")
        assertThat(words.savedConfirmation).isEqualTo("I have stored this code")
        assertThat(words.row.title).isEqualTo("Backup code")
        assertThat(words.row.description).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_row_description))
        assertThat(words.saveTitle).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_save_title))
        assertThat(words.continueButton).isEqualTo(default(R.string.auth0_mfa_recovery_code_challenge_continue_button))
    }

    @Test
    fun `code screens - the expired, no-connection and too-many words - default to the resources, and each can be replaced`() {
        val defaults = MfaMessages().resolve(resources).phoneChallenge
        assertThat(defaults.expiredCode).isEqualTo(default(R.string.auth0_mfa_phone_challenge_expired_code))
        assertThat(defaults.noConnection).isEqualTo(default(R.string.auth0_mfa_phone_challenge_no_connection))
        assertThat(defaults.tooManyRequests).isEqualTo(default(R.string.auth0_mfa_phone_challenge_too_many_requests))

        val words = MfaMessages {
            phoneChallenge {
                expiredCode = "Old code"
                noConnection = "Offline"
                tooManyRequests = "Slow down"
            }
            totpChallenge { noConnection = "No network" }
        }.resolve(resources)

        assertThat(words.phoneChallenge.expiredCode).isEqualTo("Old code")
        assertThat(words.phoneChallenge.noConnection).isEqualTo("Offline")
        assertThat(words.phoneChallenge.tooManyRequests).isEqualTo("Slow down")
        assertThat(words.totpChallenge.noConnection).isEqualTo("No network")
        // Another factor's words are its own.
        assertThat(words.emailChallenge.noConnection)
            .isEqualTo(default(R.string.auth0_mfa_email_challenge_no_connection))
    }

    private fun recoveryRow() =
        AuthenticatorListModel.Row(id = "recovery_code", factor = MfaFactorId.RecoveryCode, title = "")

    @Test
    fun `recovery code row - has no name of its own - is the factor's name with its description under it`() {
        val resolved = MfaMessages().resolve(resources)

        val shown = recoveryRow().words(resolved)

        assertThat(shown.title).isEqualTo(resolved.recoveryCodeChallenge.row.title)
        assertThat(shown.description).isEqualTo(resolved.recoveryCodeChallenge.row.description)
        assertThat(shown.description).isNotEmpty()
    }

    @Test
    fun `recovery code row - the app's own words - replace both lines`() {
        val custom = MfaMessages {
            recoveryCodeChallenge {
                row {
                    title = "Backup code"
                    description = "Use one of your saved codes"
                }
            }
        }.resolve(resources)

        val shown = recoveryRow().words(custom)

        assertThat(shown.title).isEqualTo("Backup code")
        assertThat(shown.description).isEqualTo("Use one of your saved codes")
    }

    @Test
    fun `a word with a percent sign - is read as it is, never formatted`() {
        val sure = mockk<Resources>()
        every { sure.getString(any<Int>()) } returns "100% sure"

        val words = MfaMessages().resolve(sure)

        assertThat(words.pushChallenge.title).isEqualTo("100% sure")
        assertThat(words.errors.startOver).isEqualTo("100% sure")
    }

    @Test
    fun `from a context - the words are the ones the standard screens use`() {
        val context = mockk<android.content.Context> { every { resources } returns this@MfaMessagesTest.resources }

        val words = ResolvedMfaMessages.from(context, MfaMessages { pushChallenge { title = "Approve" } })

        assertThat(words.pushChallenge.title).isEqualTo("Approve")
        assertThat(words.pushChallenge.waiting).isEqualTo(default(R.string.auth0_mfa_push_challenge_waiting))
    }
}
