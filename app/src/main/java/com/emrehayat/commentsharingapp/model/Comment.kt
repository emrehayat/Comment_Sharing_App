package com.emrehayat.commentsharingapp.model

import com.google.firebase.Timestamp

data class Comment(
    val commentId: String = "",
    val userId: String = "",
    val username: String = "",
    val commentText: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val postId: String = ""
) 