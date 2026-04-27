package com.dredio.textraocr

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.dredio.textraocr.DocumentScannerActivity.Companion.EXTRA_IMAGE_URIS
import com.dredio.textraocr.ui.theme.TextraOcrTheme

class DocumentViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val documentUri = intent.getStringExtra(EXTRA_DOCUMENT_URI)?.let(Uri::parse)
        val mimeType = intent.getStringExtra(EXTRA_DOCUMENT_MIME)
        val displayName = intent.getStringExtra(EXTRA_DOCUMENT_NAME)
            ?: getString(R.string.open_file)

        if (documentUri == null) {
            Toast.makeText(this, R.string.open_file_error, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            TextraOcrTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DocumentViewerScreen(
                        documentUri = documentUri,
                        mimeType = mimeType,
                        displayName = displayName,
                        blackWhitePreviewEnabled = AppSettings.isBlackWhitePreviewEnabled(this),
                        onRunOcr = {
                            startActivity(
                                Intent(this, OcrResultActivity::class.java).apply {
                                    putStringArrayListExtra(
                                        EXTRA_IMAGE_URIS,
                                        arrayListOf(documentUri.toString())
                                    )
                                }
                            )
                        },
                        onOpenExternally = {
                            openExternalDocument(
                                context = this,
                                uri = documentUri,
                                mimeType = mimeType
                            )
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val EXTRA_DOCUMENT_URI = "document_uri"
        private const val EXTRA_DOCUMENT_MIME = "document_mime"
        private const val EXTRA_DOCUMENT_NAME = "document_name"

        fun createIntent(
            context: Context,
            uri: Uri,
            mimeType: String?,
            displayName: String
        ): Intent {
            return Intent(context, DocumentViewerActivity::class.java).apply {
                putExtra(EXTRA_DOCUMENT_URI, uri.toString())
                putExtra(EXTRA_DOCUMENT_MIME, mimeType)
                putExtra(EXTRA_DOCUMENT_NAME, displayName)
            }
        }
    }
}

@Composable
private fun DocumentViewerScreen(
    documentUri: Uri,
    mimeType: String?,
    displayName: String,
    blackWhitePreviewEnabled: Boolean,
    onRunOcr: () -> Unit,
    onOpenExternally: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val textContent = remember(documentUri, mimeType) {
        if (mimeType?.startsWith("text/") == true ||
            displayName.endsWith(".txt", ignoreCase = true) ||
            displayName.endsWith(".md", ignoreCase = true)
        ) {
            runCatching {
                context.contentResolver.openInputStream(documentUri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
        } else {
            null
        }
    }

    val isImage = mimeType?.startsWith("image/") == true ||
        displayName.endsWith(".jpg", ignoreCase = true) ||
        displayName.endsWith(".jpeg", ignoreCase = true) ||
        displayName.endsWith(".png", ignoreCase = true)

    val colorFilter = if (blackWhitePreviewEnabled && isImage) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            isImage -> {
                Image(
                    painter = rememberAsyncImagePainter(model = documentUri),
                    contentDescription = stringResource(R.string.scanned_image_content_description),
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRunOcr,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.run_ocr_on_image))
                }
            }

            textContent != null -> {
                Text(
                    text = textContent,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            else -> {
                Text(
                    text = stringResource(R.string.open_external_message),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onOpenExternally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.open_with_other_app))
                }
            }
        }
    }
}
