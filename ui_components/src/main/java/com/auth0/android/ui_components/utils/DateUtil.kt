package com.auth0.android.ui_components.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateUtil {
    
    /**
     * Formats ISO8601 date string to M/dd/yy format
     */
    fun formatIsoDate(isoDate: String): String {
        return try {
            val dateTime = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_DATE_TIME)
            dateTime.format(DateTimeFormatter.ofPattern("M/dd/yy"))
        } catch (e: Exception) {
            isoDate.take(10)
        }
    }
}