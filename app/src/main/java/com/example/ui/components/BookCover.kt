package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.BookEntity
import java.io.File

@Composable
fun BookCover(
    book: BookEntity,
    modifier: Modifier = Modifier
) {
    if (!book.coverPath.isNullOrEmpty()) {
        val coverFile = File(book.coverPath)
        if (coverFile.exists()) {
            AsyncImage(
                model = coverFile,
                contentDescription = book.title,
                modifier = modifier
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(3.dp, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            )
            return
        }
    }

    // Elegant fallback mock covers (styled Claude-style based on standard formats)
    val colorAccent = when (book.format.uppercase()) {
        "TXT" -> Color(0xFFC05638) // Terracotta Rust
        "EPUB" -> Color(0xFFD97756) // Muted orange
        "PDF" -> Color(0xFF4A6B82) // Muted slate blue
        else -> Color(0xFF5A8E72) // Sage green
    }

    // Light Theme vs Dark Theme card back
    val containerBg = if (book.format.uppercase() == "TXT") {
        Color(0xFF191919) // Elegant ink cover
    } else {
        Color(0xFFFFFFFF) // Clean elegant paper cover
    }

    val textCol = if (containerBg == Color(0xFF191919)) Color(0xFFE5DFD4) else Color(0xFF191919)

    Box(
        modifier = modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(containerBg)
            .border(
                1.dp, 
                if (containerBg == Color(0xFFFFFFFF)) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.05f), 
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        // Spine shadow overlay
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(8.dp)
                .align(Alignment.CenterStart)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.12f),
                            Color.Black.copy(alpha = 0.02f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Corner tag indicator
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(colorAccent, RoundedCornerShape(50))
            )
        }

        // Inner metadata
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = book.format.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorAccent,
                    letterSpacing = 1.2.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.title,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 19.sp,
                    maxLines = 3,
                    color = textCol,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Cover author representation
            Text(
                text = if (book.format.uppercase() == "EPUB") "E-BOOK" else "LITERARY CORE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Light,
                color = textCol.copy(alpha = 0.5f),
                letterSpacing = 0.7.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}
