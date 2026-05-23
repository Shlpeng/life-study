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
    /**
     * 可选: 本地 drawable 资源 ID, 优先于 emoji 用作分类卡片图标.
     * 0 表示未设置, UI 会 fallback 到 [emoji].
     *
     * 注意: Android 资源 ID 是构建期生成的, 跨 build 可能变化,
     * 因此本字段**不持久化到 JSON**, 仅由内置 [DataSource] 提供;
     * 用户自定义分类始终为 0, 走 emoji 路径.
     */
    val iconRes: Int = 0,
)
