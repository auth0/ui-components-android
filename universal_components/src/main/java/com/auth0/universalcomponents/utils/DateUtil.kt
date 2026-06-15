package com.auth0.universalcomponents.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateUtil {

    private const val ISO_DATE_PREFIX_LENGTH = 10

    /**
     * Formats ISO8601 date string to M/dd/yy format
     */
    fun formatIsoDate(isoDate: String): String {
        return try {
            val dateTime = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_DATE_TIME)
            dateTime.format(DateTimeFormatter.ofPattern("M/dd/yy"))
        } catch (e: DateTimeParseException) {
            isoDate.take(ISO_DATE_PREFIX_LENGTH)
        }
    }
}
