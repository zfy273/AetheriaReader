package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry

object ParserHelpers {
    private const val TAG = "ParserHelpers"

    // --- TXT Parser with Byte-Offset Scanning ---
    data class ParsedChapter(
        val title: String,
        val startByte: Long,
        val endByte: Long
    )

    fun parseTxtChapters(context: Context, uriString: String): List<ParsedChapter> {
        val chapters = mutableListOf<ParsedChapter>()
        val uri = Uri.parse(uriString)
        
        // Regex supporting Chinese ("第...卷" or "第...章") and English ("Chapter ...")
        val chapterRegex = Regex(
            "^(第[零一二三四五六七八九十百千万\\d\\s]+[章节卷集部篇].*|Chapter\\s+\\d+.*)",
            RegexOption.IGNORE_CASE
        )

        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            
            var byteOffset = 0L
            var currentChapterTitleStr = "开始 / Introduction"
            var currentStartByte = 0L
            
            // Temporary collection to measure line bytes exactly
            var line: String? = reader.readLine()
            while (line != null) {
                // Approximate line bytes in UTF-8
                // Encoding line break (\n) is typically 1 or 2 bytes depending on system, 
                // we treat as UTF-8 line bytes + 1 byte for system newline
                val lineBytes = line.toByteArray(Charsets.UTF_8).size + 1L
                
                val trimmed = line.trim()
                if (chapterRegex.matches(trimmed)) {
                    // Save previous chapter before starting new one
                    if (byteOffset > currentStartByte) {
                        chapters.add(
                            ParsedChapter(
                                title = currentChapterTitleStr,
                                startByte = currentStartByte,
                                endByte = byteOffset
                            )
                        )
                    }
                    currentChapterTitleStr = trimmed
                    currentStartByte = byteOffset
                }
                
                byteOffset += lineBytes
                line = reader.readLine()
            }
            
            // Add remaining final chapter
            chapters.add(
                ParsedChapter(
                    title = currentChapterTitleStr,
                    startByte = currentStartByte,
                    endByte = byteOffset
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning TXT chapters: ${e.message}", e)
        } finally {
            inputStream?.close()
        }

        // If no chapters detected, create a single large chapter representing the entire file
        if (chapters.size <= 1) {
            chapters.clear()
            var fileSize = 0L
            try {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                    fileSize = it.length
                }
            } catch (e: Exception) {
                // fallback
                fileSize = 1000 * 1024L
            }
            chapters.add(
                ParsedChapter(
                    title = "全卷 / Full Volume",
                    startByte = 0L,
                    endByte = if (fileSize > 0) fileSize else 10 * 1024 * 1024L
                )
            )
        }
        return chapters
    }

    // High performance localized read: reads only specified chunk from TXT file
    fun readTxtChapterChunk(context: Context, uriString: String, startByte: Long, endByte: Long): String {
        val uri = Uri.parse(uriString)
        val stringBuilder = StringBuilder()
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return ""
            val skipped = inputStream.skip(startByte)
            if (skipped < startByte) {
                Log.w(TAG, "Could only skip $skipped bytes instead of requested $startByte")
            }
            
            val bytesToRead = (endByte - startByte).toInt().coerceIn(1, 2 * 1024 * 1024) // Cap at 2MB chunk safety
            val buffer = ByteArray(bytesToRead)
            
            var totalRead = 0
            while (totalRead < bytesToRead) {
                val read = inputStream.read(buffer, totalRead, bytesToRead - totalRead)
                if (read == -1) break
                totalRead += read
            }
            
            return String(buffer, 0, totalRead, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error chunk-loading TXT: ${e.message}")
        } finally {
            inputStream?.close()
        }
        return ""
    }


    // --- EPUB Parser ---
    data class EpubMetadata(
        val title: String,
        val creator: String = "",
        val coverPath: String? = null,
        val spineItems: List<String> = emptyList() // List of local HTML paths
    )

    fun parseEpubMetadata(context: Context, uriString: String): EpubMetadata {
        val uri = Uri.parse(uriString)
        var title = ""
        var creator = ""
        val spineItems = mutableListOf<String>()
        var coverPathInZip: String? = null

        var zipInputStream: ZipInputStream? = null
        try {
            val fileInputStream = context.contentResolver.openInputStream(uri) ?: return EpubMetadata("未命名 / Untitled")
            zipInputStream = ZipInputStream(fileInputStream)
            
            var entry: ZipEntry? = zipInputStream.nextEntry
            while (entry != null) {
                val name = entry.name
                
                // Parse OPF content file inside EPUB to locate spine & metadata
                if (name.endsWith(".opf", ignoreCase = true)) {
                    val bytes = zipInputStream.readBytes()
                    val opfContent = String(bytes, Charsets.UTF_8)
                    
                    // Simple Regex patterns are faster and highly robust for namespace-free search
                    title = Regex("<dc:title[^>]*>(.*?)</dc:title>", RegexOption.DOT_MATCHES_ALL)
                        .find(opfContent)?.groupValues?.get(1)?.trim() ?: ""
                    
                    creator = Regex("<dc:creator[^>]*>(.*?)</dc:creator>", RegexOption.DOT_MATCHES_ALL)
                        .find(opfContent)?.groupValues?.get(1)?.trim() ?: ""

                    // Populate spine order
                    // Spine items represent ID references, manifest defines the actual file paths. We correlate them!
                    val manifestMap = mutableMapOf<String, String>() // id to href
                    Regex("<item[^>]+id=\"([^\"]+)\"[^>]+href=\"([^\"]+)\"")
                        .findAll(opfContent).forEach { match ->
                            val id = match.groupValues[1]
                            val href = match.groupValues[2]
                            manifestMap[id] = href
                        }

                    // Identify cover image from manifest
                    // EPUB 2/3 covers generally look like <meta name="cover" content="some_id"> or manifest entry with id "cover"
                    val coverId = Regex("<meta[^>]+name=\"cover\"[^>]+content=\"([^\"]+)\"")
                        .find(opfContent)?.groupValues?.get(1) 
                        ?: Regex("<meta[^>]+content=\"([^\"]+)\"[^>]+name=\"cover\"")
                        .find(opfContent)?.groupValues?.get(1)

                    val matchedCoverHref = if (coverId != null) manifestMap[coverId] else null
                    coverPathInZip = matchedCoverHref ?: manifestMap.values.firstOrNull { 
                        it.contains("cover", ignoreCase = true) && 
                        (it.endsWith(".jpg", true) || it.endsWith(".png", true) || it.endsWith(".jpeg", true))
                    }

                    Regex("<itemref[^>]+idref=\"([^\"]+)\"")
                        .findAll(opfContent).forEach { match ->
                            val idref = match.groupValues[1]
                            manifestMap[idref]?.let { path ->
                                spineItems.add(path)
                            }
                        }
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing EPUB: ${e.message}", e)
        } finally {
            zipInputStream?.close()
        }

        // Clean titles if there are HTML entities
        title = title.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        if (title.isEmpty()) {
            // fallback to file name
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "EPUB 电子书"
            title = fileName.substringBeforeLast(".")
        }

        // Cache the cover image from zip if found
        var cachedCoverAbsolutePath: String? = null
        if (coverPathInZip != null) {
            cachedCoverAbsolutePath = extractAndCacheEpubCover(context, uri, coverPathInZip)
        }

        return EpubMetadata(
            title = title,
            creator = creator,
            coverPath = cachedCoverAbsolutePath,
            spineItems = spineItems
        )
    }

    private fun extractAndCacheEpubCover(context: Context, bookUri: Uri, coverZipPath: String): String? {
        var zipInputStream: ZipInputStream? = null
        try {
            val fileInputStream = context.contentResolver.openInputStream(bookUri) ?: return null
            zipInputStream = ZipInputStream(fileInputStream)
            
            var entry: ZipEntry? = zipInputStream.nextEntry
            while (entry != null) {
                // Cover file match inside archive
                if (entry.name.endsWith(coverZipPath, ignoreCase = true) || 
                    (coverZipPath.contains("/") && entry.name.endsWith(coverZipPath.substringAfterLast("/")))) {
                    
                    val cacheFile = File(context.cacheDir, "cover_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(cacheFile).use { fos ->
                        zipInputStream.copyTo(fos)
                    }
                    return cacheFile.absolutePath
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed extracting EPUB cover: ${e.message}")
        } finally {
            zipInputStream?.close()
        }
        return null
    }

    // Read full chapter file inside EPUB
    fun readEpubChapter(context: Context, uriString: String, chapterFileName: String): String {
        val uri = Uri.parse(uriString)
        var zipInputStream: ZipInputStream? = null
        try {
            val fileInputStream = context.contentResolver.openInputStream(uri) ?: return ""
            zipInputStream = ZipInputStream(fileInputStream)
            
            var entry: ZipEntry? = zipInputStream.nextEntry
            while (entry != null) {
                // Loose matching chapter file path
                if (entry.name.endsWith(chapterFileName, ignoreCase = true) || 
                    (chapterFileName.contains("/") && entry.name.endsWith(chapterFileName.substringAfterLast("/")))) {
                    
                    val bytes = zipInputStream.readBytes()
                    val htmlContent = String(bytes, Charsets.UTF_8)
                    
                    // Simple regex to strips HTML tags and leaves clean body content
                    val bodyMatches = Regex("<body[^>]*>(.*?)</body>", RegexOption.DOT_MATCHES_ALL)
                        .find(htmlContent)
                    val bodyText = bodyMatches?.groupValues?.get(1) ?: htmlContent
                    
                    // Standard entity decoding and formatting
                    var cleaned = bodyText
                        .replace("<p[^>]*>", "\n\n   ")
                        .replace("<div[^>]*>", "\n")
                        .replace("<br[^>]*/?>", "\n")
                        .replace(Regex("<[^>]+>"), "") // strip remaining tag tags
                        .replace("&nbsp;", " ")
                        .replace("&quot;", "\"")
                        .replace("&apos;", "'")
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .trim()
                    
                    return cleaned
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading EPUB chapter: ${e.message}")
        } finally {
            zipInputStream?.close()
        }
        return ""
    }


    // --- PDF Page Native Renderer ---
    fun renderPdfPageToBitmap(context: Context, uriString: String, pageIndex: Int): Bitmap? {
        val uri = Uri.parse(uriString)
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null
        try {
            parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            pdfRenderer = PdfRenderer(parcelFileDescriptor)
            
            if (pageIndex < 0 || pageIndex >= pdfRenderer.pageCount) {
                return null
            }
            
            val page = pdfRenderer.openPage(pageIndex)
            // Sharply render PDF page locally into a responsive scale
            // Size: standard width 1080p scale matching typical smartphone screen densities
            val width = 1080
            val height = (width * (page.height.toFloat() / page.width.toFloat())).toInt()
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering PDF page $pageIndex: ${e.message}")
        } finally {
            pdfRenderer?.close()
            parcelFileDescriptor?.close()
        }
        return null
    }

    fun getPdfPageCount(context: Context, uriString: String): Int {
        val uri = Uri.parse(uriString)
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null
        try {
            parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return 0
            pdfRenderer = PdfRenderer(parcelFileDescriptor)
            return pdfRenderer.pageCount
        } catch (e: Exception) {
            Log.e(TAG, "Error checking PDF page count: ${e.message}")
        } finally {
            pdfRenderer?.close()
            parcelFileDescriptor?.close()
        }
        return 0
    }


    // --- Comic Book ZIP/CBZ/RAR Images Parser ---
    fun parseComicBookPages(context: Context, uriString: String): List<String> {
        val pagePaths = mutableListOf<String>()
        val uri = Uri.parse(uriString)
        
        // Output directory specifically for this comic book in application's local cache
        val comicCacheDir = File(context.cacheDir, "comics_${uriString.hashCode()}")
        if (comicCacheDir.exists()) {
            // Already parsed & cached! Return alphabetical files
            val cachedFiles = comicCacheDir.listFiles { _, name ->
                name.endsWith(".jpg", true) || name.endsWith(".png", true) || name.endsWith(".jpeg", true) || name.endsWith(".webp", true)
            }
            if (!cachedFiles.isNullOrEmpty()) {
                return cachedFiles.sortedBy { it.name }.map { it.absolutePath }
            }
        } else {
            comicCacheDir.mkdirs()
        }

        var zipInputStream: ZipInputStream? = null
        try {
            val fileInputStream = context.contentResolver.openInputStream(uri)
            if (fileInputStream != null) {
                zipInputStream = ZipInputStream(fileInputStream)
                var entry: ZipEntry? = zipInputStream.nextEntry
                var index = 0
                while (entry != null) {
                    val name = entry.name
                    // Filter standard image assets
                    if (!entry.isDirectory && (name.endsWith(".jpg", true) || 
                                               name.endsWith(".png", true) || 
                                               name.endsWith(".jpeg", true) || 
                                               name.endsWith(".webp", true)) &&
                                               !name.contains("__MACOSX") && 
                                               !name.contains(".DS_Store")) {
                        
                        // We format file names with padded index sizes so alphabetical sorting works seamlessly
                        val formattedName = "page_${String.format("%04d", index)}.${name.substringAfterLast(".")}"
                        val cacheFile = File(comicCacheDir, formattedName)
                        
                        FileOutputStream(cacheFile).use { fos ->
                            zipInputStream.copyTo(fos)
                        }
                        pagePaths.add(cacheFile.absolutePath)
                        index++
                    }
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing comic book ZIP/CBZ: ${e.message}", e)
        } finally {
            zipInputStream?.close()
        }

        // Sort them alphabetically to provide continuous pages
        return pagePaths.sorted()
    }
}
