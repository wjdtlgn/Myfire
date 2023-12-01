package com.example.myfire

data class ChatItem(
    val senderId: String,
    val message: String
) {
    constructor(): this("", "")
}