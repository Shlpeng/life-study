package com.lifestudy.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifestudy.app.data.AppRepository
import com.lifestudy.app.data.Category
import com.lifestudy.app.ui.components.AddCategoryDialog
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onCategoryClick: (String) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    val haptics = LocalHapticFeedback.current

    val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyGridState
        val toKey = to.key as? String ?: return@rememberReorderableLazyGridState
        if (fromKey == HEADER_KEY || fromKey == FOOTER_KEY) return@rememberReorderableLazyGridState
        if (toKey == HEADER_KEY || toKey == FOOTER_KEY) return@rememberReorderableLazyGridState
        AppRepository.moveCategoryByKey(fromKey, toKey)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (editMode) {
                TopAppBar(
                    title = {
                        Text(
                            "拖动卡片调整顺序",
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    actions = {
                        TextButton(onClick = { editMode = false }) { Text("完成") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    ),
                )
            }
        },
        floatingActionButton = {
            if (!editMode) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = "新增分类") },
                    text = { Text("新增分类") },
                )
            }
        }
    ) { innerPad ->
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = if (editMode) 16.dp else 16.dp,
                bottom = 96.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPad)
                .then(if (editMode) Modifier else Modifier.statusBarsPadding()),
        ) {
            if (!editMode) {
                item(span = { GridItemSpan(2) }, key = HEADER_KEY) { HeaderBlock() }
            }
            items(AppRepository.categories, key = { it.id }) { cat ->
                ReorderableItem(reorderState, key = cat.id) { isDragging ->
                    LaunchedEffect(isDragging) {
                        if (isDragging) {
                            editMode = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                    CategoryCard(
                        category = cat,
                        wiggle = editMode && !isDragging,
                        lifted = isDragging,
                        onClick = {
                            if (!editMode) onCategoryClick(cat.id)
                        },
                        modifier = Modifier.longPressDraggableHandle(),
                    )
                }
            }
            if (!editMode) {
                item(span = { GridItemSpan(2) }, key = FOOTER_KEY) { FooterTip() }
            }
        }
    }

    if (showAdd) {
        AddCategoryDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, emoji, subtitle ->
                AppRepository.addCategory(name, emoji, subtitle)
                showAdd = false
            },
        )
    }
}

private const val HEADER_KEY = "__header__"
private const val FOOTER_KEY = "__footer__"

@Composable
private fun HeaderBlock() {
    Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
        Text(
            text = "生活常识百科",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "认识身边的动物、车、果蔬与花",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = "长按任意分类卡可进入排序",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun FooterTip() {
    Text(
        text = "右下角「新增分类」可自定义",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
    )
}

@Composable
private fun CategoryCard(
    category: Category,
    wiggle: Boolean,
    lifted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = Color(category.accent)

    val transition = rememberInfiniteTransition(label = "wiggle")
    val wiggleAngle by transition.animateFloat(
        initialValue = -1.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(140, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "angle",
    )
    val rotation = if (wiggle) wiggleAngle else 0f
    val scale by animateFloatAsState(
        targetValue = if (lifted) 1.06f else 1f,
        animationSpec = tween(150),
        label = "scale",
    )

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (lifted) 10.dp else 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.95f)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.5f)),
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = category.emoji, fontSize = 44.sp)
            }
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = category.subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                val total = category.items.size
                val learned = AppRepository.learnedCount(category)
                val done = total > 0 && learned == total
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (done) MaterialTheme.colorScheme.primary
                            else accent.copy(alpha = 0.3f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (done) "已全部认识 ✓"
                        else "已认识 $learned / $total",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (done) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
