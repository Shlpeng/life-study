package com.lifestudy.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

object AppRepository {

    private const val PREFS_NAME = "life_study_v1"
    private const val KEY_CATEGORIES = "categories"
    private const val KEY_LEARNED = "learned_ids"

    // 历史上曾内置过、现在已下线的分类 id; 升级时会从用户数据中清理掉.
    private val DEPRECATED_BUILTIN_IDS = setOf("fruits", "veggies", "fishes", "birds", "planes")

    private lateinit var prefs: SharedPreferences

    val categories = mutableStateListOf<Category>()

    // "认识了" 标记集合; 用 mutableStateOf<Set<String>>, 任何读取它的 Composable 都会在变化时重组.
    var learnedIds: Set<String> by mutableStateOf(emptySet())
        private set

    private val accentPalette = listOf(
        0xFFFFB088L, 0xFFB8C7FFL, 0xFF9CD3FFL, 0xFFFFA1A1L,
        0xFFB5E2A1L, 0xFFFFB5DDL, 0xFFC5A6FFL, 0xFFFFD86BL,
        0xFFA8E4D9L, 0xFFFFCAB0L,
    )

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 加载 learned 集合
        learnedIds = prefs.getStringSet(KEY_LEARNED, emptySet()).orEmpty().toSet()

        val saved = prefs.getString(KEY_CATEGORIES, null)
        categories.clear()
        if (saved.isNullOrBlank()) {
            categories.addAll(DataSource.categories)
            save()
            return
        }
        runCatching { categories.addAll(decode(saved)) }
            .onFailure {
                categories.clear()
                categories.addAll(DataSource.categories)
                save()
                return
            }

        var dirty = false
        // 1) 清理已下线的内置分类（用户自定义 id 以 u_cat_ 开头，不会受影响）
        if (categories.removeAll { it.id in DEPRECATED_BUILTIN_IDS }) {
            dirty = true
        }
        // 2) 对每个仍在内置数据里的分类: 用 DataSource 最新内容刷新, 但保留:
        //    - 用户自定义条目 (u_item_ 开头)
        //    - 内置条目上用户已上传的 extraImages 与 coverOverride
        val builtinById = DataSource.categories.associateBy { it.id }
        for (i in categories.indices) {
            val current = categories[i]
            val builtin = builtinById[current.id] ?: continue
            val savedById = current.items.associateBy { it.id }
            val refreshedBuiltin = builtin.items.map { newItem ->
                val saved = savedById[newItem.id]
                if (saved == null) newItem
                else newItem.copy(
                    extraImages = saved.extraImages,
                    coverOverride = saved.coverOverride,
                )
            }
            val userItems = current.items.filter { it.id.startsWith("u_item_") }
            val target = builtin.copy(items = refreshedBuiltin + userItems)
            if (current != target) {
                categories[i] = target
                dirty = true
            }
        }
        // 3) 追加新的内置分类
        val existingIds = categories.map { it.id }.toSet()
        val toAdd = DataSource.categories.filter { it.id !in existingIds }
        if (toAdd.isNotEmpty()) {
            categories.addAll(toAdd)
            dirty = true
        }
        if (dirty) save()
    }

    fun categoryById(id: String): Category? = categories.firstOrNull { it.id == id }

    fun itemBy(categoryId: String, itemId: String): Pair<Category, Item>? {
        val c = categoryById(categoryId) ?: return null
        val it = c.items.firstOrNull { it.id == itemId } ?: return null
        return c to it
    }

    fun addCategory(name: String, emoji: String, subtitle: String) {
        val cleanEmoji = emoji.ifBlank { "✨" }.take(2)
        val id = "u_cat_${System.currentTimeMillis()}"
        val accent = accentPalette[categories.size % accentPalette.size]
        categories.add(
            Category(
                id = id,
                name = name.trim(),
                emoji = cleanEmoji,
                subtitle = subtitle.ifBlank { "我自己添加的分类" }.trim(),
                accent = accent,
                items = emptyList(),
            )
        )
        save()
    }

    fun addItem(
        categoryId: String,
        name: String,
        englishName: String,
        tagsCsv: String,
        description: String,
        imageUrl: String,
    ) {
        val idx = categories.indexOfFirst { it.id == categoryId }
        if (idx < 0) return
        val tags = tagsCsv.split(",", "，")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(5)
        val finalImage = imageUrl.ifBlank {
            val seed = name.trim().ifBlank { "object" }.replace(" ", "-")
            "https://picsum.photos/seed/$seed/480/360"
        }
        val item = Item(
            id = "u_item_${System.currentTimeMillis()}",
            name = name.trim(),
            englishName = englishName.trim(),
            tags = tags.ifEmpty { listOf("自定义") },
            description = description.trim().ifBlank { "（暂无简介）" },
            imageUrl = finalImage,
        )
        val old = categories[idx]
        categories[idx] = old.copy(items = old.items + item)
        save()
    }

    // --- 认识了 ---

    fun isLearned(itemId: String): Boolean = itemId in learnedIds

    fun toggleLearned(itemId: String) {
        learnedIds = if (itemId in learnedIds) learnedIds - itemId else learnedIds + itemId
        prefs.edit().putStringSet(KEY_LEARNED, learnedIds).apply()
    }

    fun learnedCount(category: Category): Int =
        category.items.count { it.id in learnedIds }

    // --- 分类排序 ---

    fun moveCategoryByKey(fromKey: String, toKey: String) {
        val fromIdx = categories.indexOfFirst { it.id == fromKey }
        val toIdx = categories.indexOfFirst { it.id == toKey }
        if (fromIdx < 0 || toIdx < 0 || fromIdx == toIdx) return
        val item = categories.removeAt(fromIdx)
        categories.add(toIdx, item)
        save()
    }

    // --- 详情页额外图片 (最多 3 张) ---

    fun addExtraImage(categoryId: String, itemId: String, fileUrl: String) {
        updateItem(categoryId, itemId) { it.copy(extraImages = (it.extraImages + fileUrl).take(3)) }
    }

    fun removeExtraImage(categoryId: String, itemId: String, index: Int) {
        updateItem(categoryId, itemId) {
            val newList = it.extraImages.toMutableList()
            if (index in newList.indices) newList.removeAt(index)
            it.copy(extraImages = newList)
        }
    }

    /** 替换指定槽位; 越界时追加 (前提是没超过 3 张). */
    fun setExtraImageAt(categoryId: String, itemId: String, index: Int, fileUrl: String) {
        updateItem(categoryId, itemId) {
            val list = it.extraImages.toMutableList()
            when {
                index in list.indices -> list[index] = fileUrl
                index == list.size && list.size < 3 -> list.add(fileUrl)
            }
            it.copy(extraImages = list)
        }
    }

    // --- 封面图覆盖 ---

    fun setCover(categoryId: String, itemId: String, fileUrl: String) {
        updateItem(categoryId, itemId) { it.copy(coverOverride = fileUrl) }
    }

    fun clearCover(categoryId: String, itemId: String) {
        updateItem(categoryId, itemId) { it.copy(coverOverride = "") }
    }

    private fun updateItem(categoryId: String, itemId: String, transform: (Item) -> Item) {
        val cIdx = categories.indexOfFirst { it.id == categoryId }
        if (cIdx < 0) return
        val cat = categories[cIdx]
        val iIdx = cat.items.indexOfFirst { it.id == itemId }
        if (iIdx < 0) return
        val newItems = cat.items.toMutableList()
        newItems[iIdx] = transform(newItems[iIdx])
        categories[cIdx] = cat.copy(items = newItems)
        save()
    }

    fun resetToDefaults() {
        categories.clear()
        categories.addAll(DataSource.categories)
        save()
    }

    // --- JSON ---

    private fun save() {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(KEY_CATEGORIES, encode(categories.toList())).apply()
    }

    private fun encode(list: List<Category>): String {
        val arr = JSONArray()
        list.forEach { c ->
            val items = JSONArray()
            c.items.forEach { it ->
                items.put(JSONObject().apply {
                    put("id", it.id)
                    put("name", it.name)
                    put("en", it.englishName)
                    put("tags", JSONArray(it.tags))
                    put("desc", it.description)
                    put("img", it.imageUrl)
                    put("extras", JSONArray(it.extraImages))
                    put("cover", it.coverOverride)
                })
            }
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("emoji", c.emoji)
                put("subtitle", c.subtitle)
                put("accent", c.accent)
                put("items", items)
            })
        }
        return arr.toString()
    }

    private fun decode(json: String): List<Category> {
        val arr = JSONArray(json)
        val out = ArrayList<Category>(arr.length())
        for (i in 0 until arr.length()) {
            val co = arr.getJSONObject(i)
            val itemsArr = co.getJSONArray("items")
            val items = ArrayList<Item>(itemsArr.length())
            for (j in 0 until itemsArr.length()) {
                val io = itemsArr.getJSONObject(j)
                val tagsArr = io.getJSONArray("tags")
                val tags = ArrayList<String>(tagsArr.length())
                for (k in 0 until tagsArr.length()) tags.add(tagsArr.getString(k))
                val extras = ArrayList<String>()
                io.optJSONArray("extras")?.let { ea ->
                    for (k in 0 until ea.length()) extras.add(ea.getString(k))
                }
                items.add(
                    Item(
                        id = io.getString("id"),
                        name = io.getString("name"),
                        englishName = io.optString("en"),
                        tags = tags,
                        description = io.optString("desc"),
                        imageUrl = io.optString("img"),
                        extraImages = extras,
                        coverOverride = io.optString("cover"),
                    )
                )
            }
            out.add(
                Category(
                    id = co.getString("id"),
                    name = co.getString("name"),
                    emoji = co.optString("emoji", "📚"),
                    subtitle = co.optString("subtitle"),
                    accent = co.optLong("accent", 0xFFB8C7FFL),
                    items = items,
                )
            )
        }
        return out
    }
}
