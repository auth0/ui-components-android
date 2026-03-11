package com.auth0.android.ui_components.data.network

import com.auth0.android.ui_components.domain.error.Auth0Error
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class WithErrorMappingTest {

    @Test
    fun `withErrorMapping - successful execution - returns result`() = runTest {
        val result = withErrorMapping<String>(scope = "test_scope") {
            "success"
        }
        assertThat(result).isEqualTo("success")
    }

    @Test
    fun `withErrorMapping - CancellationException thrown - rethrows without mapping`() {
        val cancellationException = CancellationException("coroutine cancelled")

        val thrown = assertThrows(CancellationException::class.java) {
            runTest {
                withErrorMapping<String>(scope = "test_scope") {
                    throw cancellationException
                }
            }
        }
        assertThat(thrown.message).isEqualTo("coroutine cancelled")
    }

    @Test
    fun `withErrorMapping - non-cancellation exception thrown - maps to Auth0Error`() {
        val exception = RuntimeException("something went wrong")

        val thrown = assertThrows(Auth0Error::class.java) {
            runTest {
                withErrorMapping<String>(scope = "test_scope") {
                    throw exception
                }
            }
        }
        assertThat(thrown).isInstanceOf(Auth0Error.Unknown::class.java)
        assertThat(thrown.message).isEqualTo("something went wrong")
    }
}
