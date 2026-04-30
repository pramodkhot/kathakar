package com.kathakar.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * FileUtils — reads .docx and .txt files without Apache POI.
 *
 * A .docx file is just a ZIP archive. Inside it is word/document.xml
 * which contains the full text wrapped in <w:t> tags.
 * We unzip the file in memory, find document.xml, parse the XML,
 * and extract all <w:t> text nodes — no external library needed.
 * Works on Android API 21+.
 */
object FileUtils {

    const val MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024L   // 2 MB
    const val MAX_WORD_COUNT      = 10_000

    data class FileReadResult(
        val text: String = "",
        val wordCount: Int = 0,
        val fileName: String = "",
        val error: String? = null
    ) {
        val isSuccess get() = error == null && text.isNotBlank()
    }

    // ── Public entry point ────────────────────────────────────────────────────
    fun readFile(context: Context, uri: Uri): FileReadResult {
        return try {
            val fileName = getFileName(context, uri)
            val fileSize = getFileSize(context, uri)

            if (fileSize > MAX_FILE_SIZE_BYTES) {
                return FileReadResult(error =
                    "File too large. Maximum allowed size is 2 MB.\n" +
                    "Please split your chapter into smaller parts.")
            }

            val mimeType = context.contentResolver.getType(uri) ?: ""

            val rawText = when {
                mimeType == "text/plain" ||
                fileName.endsWith(".txt", ignoreCase = true) ->
                    readTxtFile(context, uri)

                mimeType.contains("wordprocessingml") ||
                fileName.endsWith(".docx", ignoreCase = true) ->
                    readDocxFile(context, uri)

                else -> return FileReadResult(
                    error = "Unsupported file type.\n" +
                        "Please use .docx (Word) or .txt files only.")
            }

            val cleaned   = cleanText(rawText)
            val wordCount = countWords(cleaned)

            if (cleaned.isBlank()) {
                return FileReadResult(
                    error = "File appears to be empty. Please check the file and try again.")
            }

            if (wordCount > MAX_WORD_COUNT) {
                return FileReadResult(
                    error = "Chapter is too long ($wordCount words).\n" +
                        "Maximum is $MAX_WORD_COUNT words per chapter.\n" +
                        "Please divide your story into smaller chapters.")
            }

            FileReadResult(text = cleaned, wordCount = wordCount, fileName = fileName)

        } catch (e: Exception) {
            FileReadResult(error = "Could not read file: " + (e.localizedMessage ?: "Unknown error"))
        }
    }

    // ── .txt reader ───────────────────────────────────────────────────────────
    private fun readTxtFile(context: Context, uri: Uri): String {
        val stream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open file")
        return stream.use { it.bufferedReader(Charsets.UTF_8).readText() }
    }

    // ── .docx reader — pure ZIP + XML, no Apache POI ─────────────────────────
    // A .docx is a ZIP file. The full story text lives in word/document.xml
    // Text is stored inside <w:t> elements, paragraphs are <w:p> elements.
    private fun readDocxFile(context: Context, uri: Uri): String {
        val stream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open file")

        return stream.use { inputStream ->
            val zipStream  = ZipInputStream(inputStream.buffered())
            val sb         = StringBuilder()
            var found      = false

            // Walk ZIP entries until we find word/document.xml
            var entry = zipStream.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    found = true
                    parseDocumentXml(zipStream, sb)
                    break
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
            zipStream.close()

            if (!found) throw Exception(
                "Could not find document content inside the .docx file. " +
                "Please re-save the file in Word and try again.")

            sb.toString()
        }
    }

    // ── XML parser for word/document.xml ──────────────────────────────────────
    // Walks the XML tree:
    //   <w:p>  = paragraph  → we add a newline after each paragraph
    //   <w:t>  = text run   → we collect the text content
    //   <w:br> = line break → we add \n
    private fun parseDocumentXml(stream: InputStream, sb: StringBuilder) {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser  = factory.newPullParser()
        // Wrap stream so the ZIP entry stays open while we parse
        parser.setInput(stream.buffered(), "UTF-8")

        var insideBody = false
        var event      = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name ?: ""
                    when {
                        // word/document.xml structure: w:document > w:body > ...
                        name == "body"             -> insideBody = true
                        insideBody && name == "t"  -> {
                            // <w:t> may have xml:space="preserve" — getText() handles it
                            val text = parser.nextText()
                            if (text.isNotEmpty()) sb.append(text)
                        }
                        insideBody && name == "br" -> sb.append("\n")
                        insideBody && name == "cr" -> sb.append("\n")
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name ?: ""
                    when {
                        // End of paragraph → add newline
                        insideBody && name == "p"    -> sb.append("\n")
                        insideBody && name == "body" -> insideBody = false
                    }
                }
            }
            event = parser.next()
        }
    }

    // ── Text cleanup ──────────────────────────────────────────────────────────
    private fun cleanText(raw: String): String {
        return raw
            .replace("\r\n", "\n")
            .replace("\r",   "\n")
            .replace("\u00A0", " ")     // Non-breaking space → normal space
            .replace("\u000B", "\n")    // Vertical tab → newline
            .replace("\u000C", "\n")    // Form feed → newline
            .lines()
            .joinToString("\n") { it.trimEnd() }   // Trim trailing spaces per line
            .replace(Regex("\n{3,}"), "\n\n")       // Max 2 consecutive blank lines
            .trim()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    fun countWords(text: String): Int =
        if (text.isBlank()) 0
        else text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
        }
        return name
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && idx >= 0) size = cursor.getLong(idx)
        }
        // If SIZE not available, read the stream to check
        if (size == 0L) {
            try {
                context.contentResolver.openInputStream(uri)?.use { s ->
                    size = s.available().toLong()
                }
            } catch (_: Exception) {}
        }
        return size
    }
}
