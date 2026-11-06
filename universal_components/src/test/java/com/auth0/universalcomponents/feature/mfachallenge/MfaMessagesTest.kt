package com.auth0.universalcomponents.feature.mfachallenge

import android.content.res.Resources
import com.auth0.universalcomponents.R
import com.auth0.universalcomponents.feature.mfachallenge.presentation.resolve
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
    fun `phone challenge - nothing set - every word is the string resource's, with the number filled in`() {
        val words = MfaMessages().resolve(resources).phoneChallenge

        assertThat(words.title).isEqualTo(default(R.string.auth0_mfa_phone_challenge_title))
        assertThat(words.codeLabel).isEqualTo(default(R.string.auth0_mfa_phone_challenge_code_label))
        // The server hides a number with X (or *); the screen draws each hidden digit as the same *, as the design does.
        assertThat(words.description("XXXXXXXXX0128"))
            .isEqualTo(default(R.string.auth0_mfa_phone_challenge_description, "*********0128"))
        assertThat(words.descriptionVoice("XXXXXXXXX0128"))
            .isEqualTo(default(R.string.auth0_mfa_phone_challenge_description_voice, "*********0128"))
        assertThat(words.description("*****0128"))
            .isEqualTo(default(R.string.auth0_mfa_phone_challenge_description, "*****0128"))
        // Whatever the server uses to hide a digit ends up as the same *, so the hidden part is one size.
        assertThat(words.description("∗＊X●0128"))
            .isEqualTo(default(R.string.auth0_mfa_phone_challenge_description, "****0128"))
        assertThat(words.description("+1 (XXX) XXX-0128"))
            .isEqualTo(default(R.string.auth0_mfa_phone_challenge_description, "+1 (***) ***-0128"))
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
