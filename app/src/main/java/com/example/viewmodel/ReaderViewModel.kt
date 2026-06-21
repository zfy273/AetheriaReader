package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ReaderViewModel"
    private val bookDao = BookDatabase.getDatabase(application).bookDao()
    private val context = application.applicationContext

    // App Preferences Keys in Private SharedPreferences
    private val sharedPrefs = context.getSharedPreferences("aetheria_prefs", Context.MODE_PRIVATE)

    // Library state
    private val _books = MutableStateFlow<List<BookEntity>>(emptyList())
    val books: StateFlow<List<BookEntity>> = _books.asStateFlow()

    // Screen states: "LIBRARY", "READER_TXT", "READER_COMIC", "READER_PDF", "SETTINGS"
    private val _currentScreen = MutableStateFlow("LIBRARY")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Navigation Back Stack Helper
    private var screenHistory = mutableListOf<String>()

    // Reading session states
    private val _activeBook = MutableStateFlow<BookEntity?>(null)
    val activeBook: StateFlow<BookEntity?> = _activeBook.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterEntity>>(emptyList())
    val chapters: StateFlow<List<ChapterEntity>> = _chapters.asStateFlow()

    private val _activeChapter = MutableStateFlow<ChapterEntity?>(null)
    val activeChapter: StateFlow<ChapterEntity?> = _activeChapter.asStateFlow()

    private val _chapterTextContent = MutableStateFlow("")
    val chapterTextContent: StateFlow<String> = _chapterTextContent.asStateFlow()

    // Comic Book State
    private val _comicPages = MutableStateFlow<List<String>>(emptyList())
    val comicPages: StateFlow<List<String>> = _comicPages.asStateFlow()

    private val _activeComicPage = MutableStateFlow(0)
    val activeComicPage: StateFlow<Int> = _activeComicPage.asStateFlow()

    // PDF Book State
    private val _pdfPageCount = MutableStateFlow(0)
    val pdfPageCount: StateFlow<Int> = _pdfPageCount.asStateFlow()

    private val _activePdfPage = MutableStateFlow(0)
    val activePdfPage: StateFlow<Int> = _activePdfPage.asStateFlow()

    // Reader Configuration customization values
    private val _fontSize = MutableStateFlow(sharedPrefs.getFloat("reader_font_size", 18f))
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _lineHeightMultiplier = MutableStateFlow(sharedPrefs.getFloat("reader_line_height", 1.5f))
    val lineHeightMultiplier: StateFlow<Float> = _lineHeightMultiplier.asStateFlow()

    private val _marginPaddingType = MutableStateFlow(sharedPrefs.getString("reader_margin", "NORMAL") ?: "NORMAL")
    val marginPaddingType: StateFlow<String> = _marginPaddingType.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(sharedPrefs.getBoolean("reader_is_dark", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Localization State: "zh", "en", "ja"
    private val _localeCode = MutableStateFlow(sharedPrefs.getString("app_locale", "zh") ?: "zh")
    val localeCode: StateFlow<String> = _localeCode.asStateFlow()

    // Import status
    private val _importStatus = MutableStateFlow<String?>(null)
    val importStatus: StateFlow<String?> = _importStatus.asStateFlow()

    init {
        // Collect Books from database flow
        viewModelScope.launch {
            bookDao.getAllBooksFlow().collect { list ->
                _books.value = list
            }
        }
    }

    fun navigateTo(screen: String) {
        if (_currentScreen.value != screen) {
            screenHistory.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack() {
        if (screenHistory.isNotEmpty()) {
            _currentScreen.value = screenHistory.removeAt(screenHistory.size - 1)
        } else {
            _currentScreen.value = "LIBRARY"
        }
    }

    // --- Dynamic Locale Controller ---
    fun updateLocale(locale: String) {
        _localeCode.value = locale
        sharedPrefs.edit().putString("app_locale", locale).apply()
        
        val javaLocale = java.util.Locale(locale)
        java.util.Locale.setDefault(javaLocale)
        val config = context.resources.configuration
        config.setLocale(javaLocale)
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    // --- Reader Configuration controllers ---
    fun setFontSize(size: Float) {
        val clamped = size.coerceIn(12f, 30f)
        _fontSize.value = clamped
        sharedPrefs.edit().putFloat("reader_font_size", clamped).apply()
    }

    fun setLineHeightMultiplier(mult: Float) {
        val clamped = mult.coerceIn(1.0f, 2.2f)
        _lineHeightMultiplier.value = clamped
        sharedPrefs.edit().putFloat("reader_line_height", clamped).apply()
    }

    fun setMarginPaddingType(type: String) {
        _marginPaddingType.value = type
        sharedPrefs.edit().putString("reader_margin", type).apply()
    }

    fun setReaderDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        sharedPrefs.edit().putBoolean("reader_is_dark", isDark).apply()
    }

    // --- Load Book into Active Session ---
    fun openBook(book: BookEntity) {
        _activeBook.value = book
        _currentScreen.value = "LOADING" // Intermediary beautiful step
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure Uri permissions are preserved
                val uri = Uri.parse(book.filePath)
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Persistable permission already added or not required for this Uri: ${e.message}")
                }

                // Check and load Chapters
                var chapterList = bookDao.getChaptersList(book.id)
                if (chapterList.isEmpty()) {
                    // Populate based on Format on demand
                    when (book.format.uppercase()) {
                        "TXT" -> {
                            val parsed = ParserHelpers.parseTxtChapters(context, book.filePath)
                            val entities = parsed.mapIndexed { idx, ch ->
                                ChapterEntity(
                                    bookId = book.id,
                                    chapterIndex = idx,
                                    chapterTitle = ch.title,
                                    startByte = ch.startByte,
                                    endByte = ch.endByte
                                )
                            }
                            bookDao.insertChapters(entities)
                            chapterList = entities
                        }
                        "EPUB" -> {
                            val epubMeta = ParserHelpers.parseEpubMetadata(context, book.filePath)
                            val entities = epubMeta.spineItems.mapIndexed { idx, htmlPath ->
                                ChapterEntity(
                                    bookId = book.id,
                                    chapterIndex = idx,
                                    chapterTitle = htmlPath.substringAfterLast("/").substringBeforeLast("."),
                                    startByte = 0, // Spine HTML indices do not need bytes offsets mapping
                                    endByte = 0
                                )
                            }
                            bookDao.insertChapters(entities)
                            chapterList = entities
                        }
                        "PDF" -> {
                            val pages = ParserHelpers.getPdfPageCount(context, book.filePath)
                            val entities = (0 until pages).map { idx ->
                                ChapterEntity(
                                    bookId = book.id,
                                    chapterIndex = idx,
                                    chapterTitle = "第 ${idx + 1} 页 / Page ${idx + 1}",
                                    startByte = idx.toLong(),
                                    endByte = idx.toLong()
                                )
                            }
                            bookDao.insertChapters(entities)
                            chapterList = entities
                        }
                        "RAR", "ZIP", "CBZ" -> {
                            // Comic pages cached on load
                            val pages = ParserHelpers.parseComicBookPages(context, book.filePath)
                            val entities = pages.mapIndexed { idx, path ->
                                ChapterEntity(
                                    bookId = book.id,
                                    chapterIndex = idx,
                                    chapterTitle = "第 ${idx + 1} 页",
                                    startByte = idx.toLong(),
                                    endByte = idx.toLong()
                                )
                            }
                            bookDao.insertChapters(entities)
                            chapterList = entities
                        }
                    }
                }

                _chapters.value = chapterList
                
                // Update Book Database values of total chapters
                if (book.totalChapters != chapterList.size) {
                    bookDao.updateBook(book.copy(totalChapters = chapterList.size))
                }

                // Restore last read position or default to first index
                val lastReadIdx = chapterList.indexOfFirst { it.id == book.lastReadChapterId }.coerceAtLeast(0)
                val targetChapter = if (chapterList.isNotEmpty()) chapterList[lastReadIdx] else null
                
                withContext(Dispatchers.Main) {
                    _activeChapter.value = targetChapter
                    
                    when (book.format.uppercase()) {
                        "TXT" -> {
                            if (targetChapter != null) {
                                loadTxtChapterText(targetChapter)
                            }
                            _currentScreen.value = "READER_TXT"
                        }
                        "EPUB" -> {
                            if (targetChapter != null) {
                                loadEpubChapterText(targetChapter)
                            }
                            _currentScreen.value = "READER_TXT"
                        }
                        "PDF" -> {
                            _pdfPageCount.value = chapterList.size
                            _activePdfPage.value = book.lastReadPosition.toInt().coerceIn(0, (chapterList.size - 1).coerceAtLeast(0))
                            _currentScreen.value = "READER_PDF"
                        }
                        "RAR", "ZIP", "CBZ" -> {
                            val pages = ParserHelpers.parseComicBookPages(context, book.filePath)
                            _comicPages.value = pages
                            _activeComicPage.value = book.lastReadPosition.toInt().coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                            _currentScreen.value = "READER_COMIC"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error opening book session: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _currentScreen.value = "LIBRARY"
                }
            }
        }
    }

    // Load active chapters
    fun switchChapter(chapter: ChapterEntity) {
        val currentBook = _activeBook.value ?: return
        _activeChapter.value = chapter
        
        viewModelScope.launch(Dispatchers.IO) {
            when (currentBook.format.uppercase()) {
                "TXT" -> loadTxtChapterText(chapter)
                "EPUB" -> loadEpubChapterText(chapter)
            }
            
            // Persist location
            val progress = (chapter.chapterIndex.toFloat() / _chapters.value.size.coerceAtLeast(1).toFloat()) * 100f
            val updated = currentBook.copy(
                lastReadChapterId = chapter.id,
                lastReadPosition = chapter.startByte,
                progressPercentage = progress
            )
            bookDao.updateBook(updated)
            _activeBook.value = updated
        }
    }

    private suspend fun loadTxtChapterText(chapter: ChapterEntity) {
        val currentBook = _activeBook.value ?: return
        val text = withContext(Dispatchers.IO) {
            ParserHelpers.readTxtChapterChunk(
                context, 
                currentBook.filePath, 
                chapter.startByte, 
                chapter.endByte
            )
        }
        _chapterTextContent.value = text
    }

    private suspend fun loadEpubChapterText(chapter: ChapterEntity) {
        val currentBook = _activeBook.value ?: return
        val text = withContext(Dispatchers.IO) {
            // Chapter title contains path, we locate inside spineItems matching chapterIndex
            val index = chapter.chapterIndex
            val list = ParserHelpers.parseEpubMetadata(context, currentBook.filePath).spineItems
            if (index in list.indices) {
                ParserHelpers.readEpubChapter(context, currentBook.filePath, list[index])
            } else {
                ""
            }
        }
        _chapterTextContent.value = text
    }

    // --- Comic Page Turning controller ---
    fun updateComicPage(pageIndex: Int) {
        val book = _activeBook.value ?: return
        val pages = _comicPages.value
        if (pageIndex in pages.indices) {
            _activeComicPage.value = pageIndex
            viewModelScope.launch(Dispatchers.IO) {
                val progress = (pageIndex.toFloat() / pages.size.coerceAtLeast(1).toFloat()) * 100f
                bookDao.updateBook(book.copy(
                    lastReadPosition = pageIndex.toLong(),
                    progressPercentage = progress
                ))
            }
        }
    }

    // --- PDF Page Turning controller ---
    fun updatePdfPage(pageIndex: Int) {
        val book = _activeBook.value ?: return
        val count = _pdfPageCount.value
        if (pageIndex in 0 until count) {
            _activePdfPage.value = pageIndex
            viewModelScope.launch(Dispatchers.IO) {
                val progress = (pageIndex.toFloat() / count.coerceAtLeast(1).toFloat()) * 100f
                bookDao.updateBook(book.copy(
                    lastReadPosition = pageIndex.toLong(),
                    progressPercentage = progress
                ))
            }
        }
    }

    // --- Scan SAF Selected Directory and Import Books ---
    fun scanAndImportDirectory(treeUri: Uri) {
        _importStatus.value = "SCANNING"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Persistent permissions so user doesn't need to select directory on restarts
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

                val resolver = context.contentResolver
                val cursor = resolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_SIZE
                    ),
                    null, null, null
                )

                var importCount = 0
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val childId = cursor.getString(0)
                        val name = cursor.getString(1)
                        val mimeType = cursor.getString(2)
                        val size = cursor.getLong(3)

                        val extension = name.substringAfterLast(".").lowercase()
                        val isValidFormat = extension in listOf("txt", "epub", "pdf", "cbz", "zip")

                        if (isValidFormat) {
                            val contentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                            
                            // Check if already in DB
                            val format = extension.uppercase()
                            
                            // Pre-parse book profile representation
                            var title = name.substringBeforeLast(".")
                            var creator = "本地图书"
                            var coverPath: String? = null

                            if (format == "EPUB") {
                                try {
                                    val epubMeta = ParserHelpers.parseEpubMetadata(context, contentUri.toString())
                                    title = epubMeta.title
                                    creator = epubMeta.creator.ifEmpty { "本地图书" }
                                    coverPath = epubMeta.coverPath
                                } catch (e: Exception) {
                                    Log.w(TAG, "Error pre-parsing cover for EPUB: ${e.message}")
                                }
                            }

                            val bookEntity = BookEntity(
                                title = title,
                                filePath = contentUri.toString(),
                                fileSize = size,
                                format = format,
                                coverPath = coverPath,
                                totalChapters = 0,
                                progressPercentage = 0f
                            )
                            bookDao.insertBook(bookEntity)
                            importCount++
                        }
                    }
                    cursor.close()
                }

                withContext(Dispatchers.Main) {
                    _importStatus.value = "SUCCESS:$importCount"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed scanning document tree: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _importStatus.value = "FAILED"
                }
            }
        }
    }

    fun clearImportStatus() {
        _importStatus.value = null
    }

    // Delete book safely (clearing dependencies cache)
    fun removeBook(book: BookEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete localized comic cache folder
            if (book.format.uppercase() in listOf("RAR", "ZIP", "CBZ")) {
                val comicCacheDir = File(context.cacheDir, "comics_${book.filePath.hashCode()}")
                try {
                    comicCacheDir.deleteRecursively()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not delete cached comic files: ${e.message}")
                }
            }
            
            // Delete DB records
            bookDao.deleteChaptersForBook(book.id)
            bookDao.deleteBook(book)
        }
    }
}
