package com.dredio.textraocr

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

data class UserDocumentFile(
    val file: File,
    val displayName: String,
    val modifiedAt: Long,
)

private const val USER_DOCUMENTS_DIR = "documents"

fun createUserDocumentFile(
    context: Context,
    prefix: String,
    extension: String
): File {
    val directory = File(context.filesDir, USER_DOCUMENTS_DIR).apply { mkdirs() }
    return File(directory, "${prefix}_${System.currentTimeMillis()}$extension")
}

fun listUserDocumentFiles(context: Context): List<UserDocumentFile> {
    val documentsDirectory = File(context.filesDir, USER_DOCUMENTS_DIR)
    val legacyFiles = context.filesDir.listFiles().orEmpty().filter(::isUserVisibleDocument)
    val documentFiles = documentsDirectory.listFiles().orEmpty().filter(::isUserVisibleDocument)

    return (legacyFiles + documentFiles)
        .distinctBy { it.absolutePath }
        .sortedByDescending { it.lastModified() }
        .map { file ->
            UserDocumentFile(
                file = file,
                displayName = file.name,
                modifiedAt = file.lastModified()
            )
        }
}

fun openUserDocument(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        file
    )
    openDocumentUri(
        context = context,
        uri = uri,
        mimeType = mimeTypeForFile(file.name),
        displayName = file.name
    )
}

fun openDocumentUri(
    context: Context,
    uri: Uri,
    mimeType: String? = null,
    displayName: String = displayNameForUri(context, uri)
) {
    if (canOpenInternally(mimeType, displayName)) {
        if (isImageDocument(mimeType, displayName) && AppSettings.isAutoOcrImageOpenEnabled(context)) {
            context.startActivity(
                Intent(context, OcrResultActivity::class.java).apply {
                    putStringArrayListExtra(
                        DocumentScannerActivity.EXTRA_IMAGE_URIS,
                        arrayListOf(uri.toString())
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
            return
        }

        context.startActivity(
            DocumentViewerActivity.createIntent(
                context = context,
                uri = uri,
                mimeType = mimeType,
                displayName = displayName
            ).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
        return
    }

    openExternalDocument(context, uri, mimeType)
}

private fun isImageDocument(mimeType: String?, displayName: String): Boolean {
    val normalizedMime = mimeType?.lowercase().orEmpty()
    val normalizedName = displayName.lowercase()
    return normalizedMime.startsWith("image/") ||
        normalizedName.endsWith(".jpg") ||
        normalizedName.endsWith(".jpeg") ||
        normalizedName.endsWith(".png")
}

fun openExternalDocument(
    context: Context,
    uri: Uri,
    mimeType: String? = null
) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_file)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.open_file_error, Toast.LENGTH_SHORT).show()
    }
}

private fun isUserVisibleDocument(file: File): Boolean {
    if (!file.isFile) return false
    return file.name.startsWith("OCR_Result_") ||
        file.name.startsWith("Scanned_Document_") ||
        file.name.startsWith("Scanned_Page_")
}

fun mimeTypeForFile(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "txt" -> "text/plain"
        "md" -> "text/markdown"
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    }
}

private fun canOpenInternally(mimeType: String?, displayName: String): Boolean {
    val normalizedMime = mimeType?.lowercase().orEmpty()
    val normalizedName = displayName.lowercase()

    return normalizedMime.startsWith("image/") ||
        normalizedMime.startsWith("text/") ||
        normalizedName.endsWith(".txt") ||
        normalizedName.endsWith(".md") ||
        normalizedName.endsWith(".jpg") ||
        normalizedName.endsWith(".jpeg") ||
        normalizedName.endsWith(".png")
}

private fun displayNameForUri(context: Context, uri: Uri): String {
    if (uri.scheme == "file") {
        return uri.lastPathSegment?.substringAfterLast('/') ?: "document"
    }

    val cursor: Cursor? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    }.getOrNull()

    cursor?.use {
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && it.moveToFirst()) {
            return it.getString(index)
        }
    }

    return uri.lastPathSegment?.substringAfterLast('/') ?: "document"
}
