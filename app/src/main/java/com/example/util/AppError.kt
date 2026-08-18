package com.example.util

import java.io.IOException
import java.sql.SQLException

sealed interface AppError {
    val message: String
    val cause: Throwable?

    data class Network(
        override val message: String = "Network connection issue. Please check your internet connection.",
        override val cause: Throwable? = null
    ) : AppError

    data class Parser(
        override val message: String = "Failed to parse the vocabulary document. Please check the document structure.",
        override val cause: Throwable? = null
    ) : AppError

    data class Database(
        override val message: String = "Database error. Failed to save or read data locally.",
        override val cause: Throwable? = null
    ) : AppError

    data class Synchronization(
        override val message: String = "Synchronization failed. Please check the document URL or your network.",
        override val cause: Throwable? = null
    ) : AppError

    data class Unknown(
        override val message: String = "An unexpected error occurred. Please try again.",
        override val cause: Throwable? = null
    ) : AppError

    companion object {
        fun from(throwable: Throwable): AppError {
            return when (throwable) {
                is IOException -> Network(cause = throwable)
                is SQLException -> Database(cause = throwable)
                is IllegalArgumentException, is IndexOutOfBoundsException -> Parser(cause = throwable)
                else -> Unknown(cause = throwable)
            }
        }
    }
}
