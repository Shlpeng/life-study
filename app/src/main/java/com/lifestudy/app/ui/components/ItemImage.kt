package com.lifestudy.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest

/**
 * 渲染优先级:
 * 1) 用户上传的 coverOverride (file:// URL, 通过 Coil 加载)
 * 2) 本地 drawable 资源 (按 itemId 在 res/drawable/ 查找同名 jpg/png)
 * 3) 远程图片 URL (Coil)
 * 4) accent 色块 + 分类 emoji 兜底
 */
@Composable
fun ItemImage(
    itemId: String,
    url: String,
    contentDescription: String,
    fallbackEmoji: String,
    accent: Color,
    modifier: Modifier = Modifier,
    fallbackFontSize: TextUnit = 36.sp,
    coverOverride: String = "",
) {
    val context = LocalContext.current
    val drawableId = remember(itemId) {
        if (itemId.isBlank()) 0
        else context.resources.getIdentifier(itemId, "drawable", context.packageName)
    }

    Box(
        modifier = modifier.background(accent.copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            coverOverride.isNotBlank() -> {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverOverride)
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                        else -> Text(text = fallbackEmoji, fontSize = fallbackFontSize)
                    }
                }
            }
            drawableId != 0 -> {
                Image(
                    painter = painterResource(drawableId),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            url.isNotBlank() -> {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                        else -> Text(text = fallbackEmoji, fontSize = fallbackFontSize)
                    }
                }
            }
            else -> {
                Text(text = fallbackEmoji, fontSize = fallbackFontSize)
            }
        }
    }
}
