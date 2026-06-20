package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ParserHelpers
import com.example.viewmodel.ReaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReaderPdfScreen(
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeBook by viewModel.activeBook.collectAsState()
    val totalPages by viewModel.pdfPageCount.collectAsState()
    val activePdfPage by viewModel.activePdfPage.collectAsState()

    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(false) }
    var showOverlayControls by remember { mutableStateOf(true) }

    // Re-render PDF page into high density bitmap whenever page index is adjusted
    LaunchedEffect(activePdfPage, activeBook) {
        val book = activeBook ?: return@LaunchedEffect
        isLoadingPage = true
        withContext(Dispatchers.IO) {
            val bitmap = ParserHelpers.renderPdfPageToBitmap(context, book.filePath, activePdfPage)
            withContext(Dispatchers.Main) {
                pageBitmap = bitmap
                isLoadingPage = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)) // Dark cinematic backboard for PDF rendering and comic streams
    ) {
        // --- LIVE PDF RENDER SURFACE ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { showOverlayControls = !showOverlayControls },
            contentAlignment = Alignment.Center
        ) {
            val bitmap = pageBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "PDF Page ${activePdfPage + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Fit
                )
            } else if (isLoadingPage) {
                CircularProgressIndicator(color = Color(0xFFD97756))
            } else {
                Text(
                    text = "无法渲染该 PDF 页面",
                    color = Color.White.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Serif
                )
            }
        }

        // --- OVERLAY CONTROLS (Floating header and side controls) ---
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
                                text = "PDF Document",
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
                            text = "${activePdfPage + 1} / $totalPages",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- LOWER SLIDER TIMELINE ---
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
                    // Quick next page / prev page tactile buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (activePdfPage > 0) viewModel.updatePdfPage(activePdfPage - 1) },
                            enabled = activePdfPage > 0
                        ) {
                            Text("<", fontSize = 22.sp, color = if (activePdfPage > 0) Color.White else Color.White.copy(alpha = 0.2f), fontWeight = FontWeight.Bold)
                        }

                        // Slider channel
                        Slider(
                            value = activePdfPage.toFloat(),
                            onValueChange = { viewModel.updatePdfPage(it.toInt()) },
                            valueRange = 0f..(totalPages - 1).coerceAtLeast(1).toFloat(),
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFD97756),
                                activeTrackColor = Color(0xFFD97756),
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        IconButton(
                            onClick = { if (activePdfPage < totalPages - 1) viewModel.updatePdfPage(activePdfPage + 1) },
                            enabled = activePdfPage < totalPages - 1
                        ) {
                            Text(">", fontSize = 22.sp, color = if (activePdfPage < totalPages - 1) Color.White else Color.White.copy(alpha = 0.2f), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
