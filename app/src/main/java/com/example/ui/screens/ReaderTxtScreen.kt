package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.viewmodel.ReaderViewModel
import com.example.ui.components.ReaderSettingsSheet
import kotlinx.coroutines.launch

@Composable
fun ReaderTxtScreen(
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier
) {
    val activeBook by viewModel.activeBook.collectAsState()
    val activeChapter by viewModel.activeChapter.collectAsState()
    val textContent by viewModel.chapterTextContent.collectAsState()
    val chapters by viewModel.chapters.collectAsState()

    val fontSize by viewModel.fontSize.collectAsState()
    val lineHeightMultiplier by viewModel.lineHeightMultiplier.collectAsState()
    val marginType by viewModel.marginPaddingType.collectAsState()
    val isCustomDarkTheme by viewModel.isDarkTheme.collectAsState()

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var showBottomControls by remember { mutableStateOf(false) }
    var showChaptersDrawer by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Margins logic mapping: narrow (8dp), normal (18dp), wide (28dp)
    val sidePaddingDp = when (marginType) {
        "NARROW" -> 10.dp
        "WIDE" -> 30.dp
        else -> 18.dp
    }

    // Auto-scroll to top whenever chapter content undergoes updates
    LaunchedEffect(activeChapter) {
        scrollState.scrollTo(0)
    }

    // Capture standard background color configuration matching the reader theme chosen by user
    val backgroundSurfaceColor = if (isCustomDarkTheme) Color(0xFF121212) else Color(0xFFF9F6F0)
    val textForegroundColor = if (isCustomDarkTheme) Color(0xFFE0E0E0) else Color(0xFF191919)

    // Override local MaterialTheme for the reader screen so it responds immediately to in-sheet theme switching
    val readerColorScheme = if (isCustomDarkTheme) {
        darkColorScheme(
            primary = Color(0xFFD97756),
            onPrimary = Color(0xFF121212),
            background = Color(0xFF121212),
            onBackground = Color(0xFFE0E0E0),
            surface = Color(0xFF1E1E1E),
            onSurface = Color(0xFFE0E0E0),
            outline = Color(0xFF2E2E2E),
            surfaceVariant = Color(0xFF1E1E1E)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFC05638),
            onPrimary = Color(0xFFFFFFFF),
            background = Color(0xFFF9F6F0),
            onBackground = Color(0xFF191919),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF191919),
            outline = Color(0xFFE5DFD4),
            surfaceVariant = Color(0xFFF9F6F0)
        )
    }

    MaterialTheme(colorScheme = readerColorScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundSurfaceColor)
        ) {
            // --- MAIN TEXT RENDERING (Serif configuration) ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .clickable { showBottomControls = !showBottomControls }
                    .padding(horizontal = sidePaddingDp, vertical = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(50.dp)) // edge-to-edge escape

                // Chapter Header tag
                Text(
                    text = activeChapter?.chapterTitle ?: stringResource(R.string.untitled_chapter),
                    fontSize = (fontSize - 2).sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = textForegroundColor.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
                )

                if (textContent.isEmpty() && activeBook != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    // Actual Serif parsed content body
                    Text(
                        text = textContent,
                        fontFamily = FontFamily.Serif,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * lineHeightMultiplier).sp,
                        color = textForegroundColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Chapter Pagination click triggers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val currentIndex = activeChapter?.chapterIndex ?: 0
                    
                    TextButton(
                        onClick = {
                            if (currentIndex > 0) {
                                viewModel.switchChapter(chapters[currentIndex - 1])
                            }
                        },
                        enabled = currentIndex > 0
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("<", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (currentIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "上一章", fontFamily = FontFamily.Serif)
                        }
                    }

                    TextButton(
                        onClick = {
                            if (currentIndex < chapters.size - 1) {
                                viewModel.switchChapter(chapters[currentIndex + 1])
                            }
                        },
                        enabled = currentIndex < chapters.size - 1
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "下一章", fontFamily = FontFamily.Serif)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(">", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (currentIndex < chapters.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                        }
                    }
                }
            }

            // --- FULLSCREEN HEADER CONTROLS OVERLAY ---
            AnimatedVisibility(
                visible = showBottomControls,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp
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
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = activeBook?.title ?: stringResource(R.string.untitled_book),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.widthIn(max = 200.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Chapters drawer toggle button
                            IconButton(onClick = { 
                                showChaptersDrawer = true 
                                showBottomControls = false
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Chapter Table of contents",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Reader styling settings trigger
                            IconButton(onClick = { 
                                showSettingsSheet = true
                                showBottomControls = false
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Style settings menu",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // --- CHAPTERS TABLE OF CONTENTS DRAWER PANELS ---
            AnimatedVisibility(
                visible = showChaptersDrawer,
                enter = slideInHorizontally(initialOffsetX = { -it }),
                exit = slideOutHorizontally(targetOffsetX = { -it }),
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.85f)
                    .align(Alignment.CenterStart)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(28.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.menu_chapters),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = { showChaptersDrawer = false }) {
                                Text(text = "关闭")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Chapters scroll list
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            chapters.forEach { chapter ->
                                val isSelected = chapter.id == activeChapter?.id
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.switchChapter(chapter)
                                            showChaptersDrawer = false
                                        }
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) 
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 24.dp, vertical = 14.dp)
                                ) {
                                    Text(
                                        text = chapter.chapterTitle.ifEmpty { "第 ${chapter.chapterIndex + 1} 章" },
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- SLIDE UP STYLE SHEET OVERLAYS ---
            ReaderSettingsSheet(
                viewModel = viewModel,
                isVisible = showSettingsSheet,
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}

fun Modifier.fillMaxWidth(fraction: Float): Modifier {
    return this.fillMaxWidth(fraction = fraction)
}
