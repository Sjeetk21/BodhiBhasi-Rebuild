package com.example.entity

import androidx.room.ColumnInfo

data class LearningStatusCount(
    @ColumnInfo(name = "learningStatus") val status: String,
    @ColumnInfo(name = "count") val count: Int
)


data class SubjectCount(
    @ColumnInfo(name = "topic") val topic: String,
    @ColumnInfo(name = "count") val count: Int
)