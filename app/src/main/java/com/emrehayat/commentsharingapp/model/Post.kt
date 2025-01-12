package com.emrehayat.commentsharingapp.model

import com.google.firebase.Timestamp

data class Post(
    val userName: String = "",
    val comment: String = "",
    val downloadUrl: String = "",
    val date: Timestamp = Timestamp.now(),
    val userId: String? = null,
    val isDeleted: Boolean = false
)