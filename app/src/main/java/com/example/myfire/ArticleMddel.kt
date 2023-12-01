package com.example.myfire

data class ArticleMddel(
    val sellerId: String,
    val sellerName: String, // 이 속성 추가
    val title: String,
    val content: String,    // 이 속성 추가
    val isSell: Boolean,
    val createdAt: Long,
    val price: String,
    val imageUrl: String
) {
    constructor() : this("", "", "","", false, 0, "", "")
}