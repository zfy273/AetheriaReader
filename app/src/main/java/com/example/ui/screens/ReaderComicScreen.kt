package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.viewmodel.ReaderViewModel
import java.io.File

@Composable
fun ReaderComicScreen(
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier
) {
    val activeBook by viewModel.activeBook.collectAsState()
    val pages by viewModel.comicPages.collectAsState()
    val activeComicPage by viewModel.activeComicPage.collectAsState()

    var showOverlayControls by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)) // Deep absolute backboard for comics
    ) {
        // --- STREAMING IMAGE CANVAS ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { showOverlayControls = !showOverlayControls },
            contentAlignment = Alignment.Center
        ) {
            if (pages.isNotEmpty() && activeComicPage in pages.indices) {
                val imageFile = File(pages[activeComicPage])
                if (imageFile.exists()) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = "Comic Page ${activeComicPage + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    CircularProgressIndicator(color = Color(0xFFD97756))
                }
            } else {
                Text(
                    text = "正在准备画册 / Preparing pages...",
                    color = Color.White.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Serif
                )
            }
        }

        // --- SIDEBAR NAVIGATION TRIGGERS (Enables rapid reading clicks) ---
        Row(modifier = Modifier.fillMaxSize()) {
            // Left trigger (Previous page)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        if (activeComicPage > 0) {
                            viewModel.updateComicPage(activeComicPage - 1)
                        }
                    }
            )
            
            // Middle screen sensor (Toggles toolbar overlay)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1.5f)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        showOverlayControls = !showOverlayControls
                    }
            )

            // Right trigger (Next page)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        if (activeComicPage < pages.size - 1) {
                            viewModel.updateComicPage(activeComicPage + 1)
                        }
                    }
            )
        }

        // --- UPPER SCREEN OVERLAY TOOLBARS ---
        AnimatedVisibility(
            visible = showOverlayControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                color = Color.Black.copy(alpha = 0.85f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = activeBook?.title ?: stringResource(R.string.untitled_book),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = stringResource(R.string.book_format_comic),
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${activeComicPage + 1} / ${pages.size}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- COMIC PROGRESS TIMELINES ---
        AnimatedVisibility(
            visible = showOverlayControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = Color.Black.copy(alpha = 0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (activeComicPage > 0) viewModel.updateComicPage(activeComicPage - 1) },
                            enabled = activeComicPage > 0
                        ) {
                            Text("<", fontSize = 22.sp, color = if (activeComicPage > 0) Color.White else Color.White.copy(alpha = 0.2f), fontWeight = FontWeight.Bold)
                        }

                        Slider(
                            value = activeComicPage.toFloat(),
                            onValueChange = { viewModel.updateComicPage(it.toInt()) },
                            valueRange = 0f..(pages.size - 1).coerceAtLeast(1).toFloat(),
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFD97756),
                                activeTrackColor = Color(0xFFD97756),
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        IconButton(
                            onClick = { if (activeComicPage < pages.size - 1) viewModel.updateComicPage(activeComicPage + 1) },
                            enabled = activeComicPage < pages.size - 1
                        ) {
                            Text(">", fontSize = 22.sp, color = if (activeComicPage < pages.size - 1) Color.White else Color.White.copy(alpha = 0.2f), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
