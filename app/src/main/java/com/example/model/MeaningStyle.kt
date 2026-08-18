package com.example.model

/**
 * Defines the detail level / format for AI-generated word meanings.
 */
enum class MeaningStyle(val label: String, val description: String) {
    SHORT(
        label = "Few Words",
        description = "2-4 words concise essence"
    ),
    ONE_LINER(
        label = "One Liner",
        description = "1 crisp sentence (8-15 words)"
    ),
    DESCRIPTIVE(
        label = "Descriptive",
        description = "Detailed definition with UPSC context (20-35 words)"
    )
}
