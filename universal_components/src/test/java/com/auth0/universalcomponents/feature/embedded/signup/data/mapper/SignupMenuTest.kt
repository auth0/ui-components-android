package com.auth0.universalcomponents.feature.embedded.signup.data.mapper

import com.auth0.android.embedded.authorize.EmbeddedCapability
import com.auth0.android.embedded.authorize.IdentifierType
import com.auth0.android.embedded.authorize.NextAction
import com.auth0.android.embedded.authorize.SignupIdentifier
import com.auth0.universalcomponents.data.embeddedauth.sdkInstance
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupChallenge
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupKind
import com.auth0.universalcomponents.feature.embedded.signup.domain.model.SignupNext
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SignupMenuTest {

    // region the offer

    @Test
    fun `offer - no sign-up in the menu - there is none`() {
        assertThat(listOf(NextAction.IdentifyEmail).toSignupOffer()).isNull()
        assertThat(emptyList<NextAction>().toSignupOffer()).isNull()
    }

    @Test
    fun `offer - an email required - it can be finished here`() {
        val offer = listOf(signup(EMAIL to true)).toSignupOffer()

        assertThat(offer?.required).containsExactly(SignupKind.Email)
        assertThat(offer?.isSupported).isTrue()
    }

    @Test
    fun `offer - an optional kind besides the required email - is left out`() {
        val offer = listOf(signup(EMAIL to true, PHONE to false, USERNAME to false)).toSignupOffer()

        assertThat(offer?.required).containsExactly(SignupKind.Email)
        assertThat(offer?.isSupported).isTrue()
    }

    @Test
    fun `offer - a phone required - it can't be finished here, since there is no screen for it`() {
        val offer = listOf(signup(PHONE to true)).toSignupOffer()

        assertThat(offer?.requiresUnsupportedKind).isTrue()
        assertThat(offer?.isSupported).isFalse()
    }

    @Test
    fun `offer - a username required - it can't be finished here`() {
        assertThat(listOf(signup(USERNAME to true)).toSignupOffer()?.isSupported).isFalse()
    }

    @Test
    fun `offer - an email and a phone both required - it can't be finished here`() {
        assertThat(listOf(signup(EMAIL to true, PHONE to true)).toSignupOffer()?.isSupported).isFalse()
    }

    @Test
    fun `offer - nothing required - it can't be finished here`() {
        assertThat(listOf(signup(EMAIL to false)).toSignupOffer()?.isSupported).isFalse()
    }

    // endregion

    // region the menu

    @Test
    fun `menu - the email asked for - is what comes next`() {
        val menu = listOf<NextAction>(NextAction.IdentifyEmail).toSignupMenu()

        assertThat(menu.askedFor).containsExactly(SignupKind.Email)
        assertThat(menu.next).isEqualTo(SignupNext.AskFor(SignupKind.Email))
    }

    @Test
    fun `menu - a code the server will send - is found`() {
        val menu = listOf<NextAction>(challengeEmail(at = 3)).toSignupMenu()

        assertThat(menu.challenge).isEqualTo(SignupChallenge(index = 3))
    }

    @Test
    fun `menu - the first code on offer - is the one`() {
        val menu = listOf<NextAction>(challengeEmail(at = 1), challengeEmail(at = 2)).toSignupMenu()

        assertThat(menu.challenge).isEqualTo(SignupChallenge(index = 1))
    }

    @Test
    fun `menu - the account can be created - that comes before asking for anything`() {
        val menu = listOf(NextAction.IdentifyEmail, NextAction.ConfirmSignup).toSignupMenu()

        assertThat(menu.canConfirm).isTrue()
        assertThat(menu.next).isEqualTo(SignupNext.ReadyToCreate)
    }

    @Test
    fun `menu - nothing this flow can use - there is nothing to do`() {
        val menu = listOf(NextAction.IdentifyPhone, NextAction.IdentifyUsername).toSignupMenu()

        assertThat(menu.askedFor).isEmpty()
        assertThat(menu.challenge).isNull()
        assertThat(menu.next).isEqualTo(SignupNext.NothingToDo)
    }

    @Test
    fun `menu - empty - there is nothing to do`() {
        assertThat(emptyList<NextAction>().toSignupMenu().next).isEqualTo(SignupNext.NothingToDo)
    }

    // endregion

    @Test
    fun `an email - is identified as one, and advertises its three steps`() {
        assertThat(SignupKind.Email.identifierType).isEqualTo(IdentifierType.EMAIL)
        assertThat(SignupKind.Email.capabilities).containsExactly(
            EmbeddedCapability.IDENTIFY_EMAIL,
            EmbeddedCapability.CHALLENGE_EMAIL,
            EmbeddedCapability.VERIFY_OTP,
        )
    }

    private fun signup(vararg identifiers: Pair<IdentifierType, Boolean>): NextAction.Signup =
        sdkInstance(
            List::class.java to identifiers.map { (type, required) ->
                sdkInstance<SignupIdentifier>(IdentifierType::class.java to type, Boolean::class.java to required)
            },
        )

    private fun challengeEmail(at: Int): NextAction.ChallengeEmail =
        sdkInstance(Int::class.java to at, String::class.java to "j***@example.com")

    private companion object {
        val EMAIL = IdentifierType.EMAIL
        val PHONE = IdentifierType.PHONE
        val USERNAME = IdentifierType.USERNAME
    }
}
