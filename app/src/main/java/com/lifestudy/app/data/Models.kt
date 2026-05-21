package com.lifestudy.app.data

data class Item(
    val id: String,
    val name: String,
    val englishName: String,
    val tags: List<String>,
    val description: String,
    val imageUrl: String,
    val extraImages: List<String> = emptyList(),
    val coverOverride: String = "",  // 用户上传的封面 (file:// URL), 非空时优先于内置 drawable / 远程图
)

data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val subtitle: String,
    val accent: Long,
    val items: List<Item>,
)
