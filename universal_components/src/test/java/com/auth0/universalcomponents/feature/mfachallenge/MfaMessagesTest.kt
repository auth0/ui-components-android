package com.auth0.universalcomponents.feature.mfachallenge

import com.auth0.universalcomponents.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MfaMessagesTest {

    /** Stands in for the string resources: each default is its resource id and its arguments. */
    private val defaults = MfaStrings { id, args -> "default($id${args.joinToString(prefix = ",", separator = ",")})" }

    private fun default(id: Int, vararg args: Any) = "default($id${args.joinToString(prefix = ",", separator = ",")})"

    @Test
    fun `nothing set - every word is the string resource's`() {
        val words = MfaMessages().resolve(defaults)

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
        val words = MfaMessages { pushChallenge { title = "Approve on your phone" } }.resolve(defaults)

        assertThat(words.pushChallenge.title).isEqualTo("Approve on your phone")
        assertThat(words.pushChallenge.waiting).isEqualTo(default(R.string.auth0_mfa_push_challenge_waiting))
        assertThat(words.pushChallenge.verifyAnotherWay)
            .isEqualTo(default(R.string.auth0_mfa_push_challenge_verify_another_way))
    }

    @Test
    fun `nested keys - a header's title is changed without its description`() {
        val words = MfaMessages {
            authenticatorList { headers { challengeAdditional { title = "Almost there" } } }
        }.resolve(defaults)

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
        }.resolve(defaults)

        assertThat(words.pushChallenge.description("Jane's phone")).isEqualTo("Sent to Jane's phone.")
        assertThat(words.pushChallenge.resend.cooldown(42)).isEqualTo("Again in 42s")
    }

    @Test
    fun `dynamic text - left unset - the string resource is formatted with the argument`() {
        val words = MfaMessages().resolve(defaults)

        assertThat(words.pushChallenge.description("Jane's phone"))
            .isEqualTo(default(R.string.auth0_mfa_push_challenge_description, "Jane's phone"))
        assertThat(words.pushChallenge.resend.cooldown(7))
            .isEqualTo(default(R.string.auth0_mfa_push_challenge_resend_cooldown, 7))
    }

    @Test
    fun `error text - every case - has its own words, and an override changes one case only`() {
        val words = MfaMessages { errors { rejected { title = "Denied" } } }.resolve(defaults)

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
        val titles = cases.map { words.errors.textFor(it).title }

        assertThat(titles).containsNoDuplicates()
        // Never shown, so it has no words of its own.
        assertThat(words.errors.textFor(MfaError.PresentationUnavailable()))
            .isSameInstanceAs(words.errors.textFor(MfaError.Unexpected()))
        assertThat(words.errors.textFor(MfaError.Rejected()).title).isEqualTo("Denied")
        assertThat(words.errors.textFor(MfaError.Rejected()).description)
            .isEqualTo(default(R.string.auth0_mfa_errors_rejected_description))
    }

    @Test
    fun `row - the factor in the list - has a title and a description, either of which can be changed`() {
        val words = MfaMessages { pushChallenge { row { description = "Tap to approve." } } }.resolve(defaults)

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
        }.resolve(defaults)

        assertThat(words.errors.startOver).isEqualTo("Restart")
        assertThat(words.errors.close).isEqualTo("Done")
    }
}
