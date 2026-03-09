package com.example.universitykotlin4

import kotlinx.serialization.Serializable

@Serializable
data class Repo(
    val id: Int,
    val full_name: String,
    val description: String? = null,
    val stargazers_count: Int,
    val language: String,
)
