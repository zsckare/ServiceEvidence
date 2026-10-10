package com.serviceevidence.backend.subscriptions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * EN: An unknown tenant must be rejected before checking subscription quotas.
 * ES: Un tenant inexistente debe rechazarse antes de consultar las cuotas.
 *
 * Mockito's default answer returns an empty list for methods returning List.
 * This models the SELECT ... FOR UPDATE finding no tenant, without brittle
 * RowMapper/vararg matchers on overloaded JdbcTemplate.query() methods.
 *
 * Mockito devuelve una lista vacía para métodos que retornan List.
 * Así simulamos un tenant inexistente sin matchers ambiguos de JdbcTemplate.
 */
class SubscriptionQuotaServiceTest {
    private val jdbc = mock(JdbcTemplate::class.java)
    private val quotas = SubscriptionQuotaService(jdbc)

    @Test
    fun `user quota rejects unknown tenant`() {
        val error = assertThrows(ResponseStatusException::class.java) {
            quotas.requireUserSlot(UUID.randomUUID())
        }
        assertEquals(HttpStatus.NOT_FOUND, error.statusCode)
    }

    @Test
    fun `work order quota rejects unknown tenant`() {
        val error = assertThrows(ResponseStatusException::class.java) {
            quotas.requireWorkOrderSlot(UUID.randomUUID())
        }
        assertEquals(HttpStatus.NOT_FOUND, error.statusCode)
    }
}
