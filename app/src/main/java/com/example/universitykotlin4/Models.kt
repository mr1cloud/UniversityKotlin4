package com.example.universitykotlin4

import kotlinx.serialization.Serializable

@Serializable
data class SocialPost (
    val id: Int,
    val userId: Int,
    val title: String,
    val body: String,
    val avatarUrl: String
)

@Serializable
data class Comment(
    val postId: Int,
    val id: Int,
    val name: String,
    val body: String
)

@Serializable
data class PostState(
    val post: SocialPost,
    val comments: List<Comment> = emptyList(),
    val status: LoadState = LoadState.Loading
)

enum class LoadState {
    Loading,
    Success,
    Error
}