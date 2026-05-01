package com.dredio.textraocr

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dredio.textraocr.ui.theme.TextraOcrTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.FileOutputStream

class DocumentScannerActivity : ComponentActivity() {

    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)

                val pages = scanResult?.pages?.mapIndexed { index, page ->
                    saveImageToFilesDir(page.imageUri, index)?.toString() ?: page.imageUri.toString()
                }.orEmpty()
                if (pages.isNotEmpty()) {
                    AppSettings.recordCompletedScan(this)
                    val intent = Intent(this, OcrResultActivity::class.java).apply {
                        putStringArrayListExtra(EXTRA_IMAGE_URIS, ArrayList(pages))
                    }
                    startActivity(intent)
                }
            }
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppDiagnostics.logBreadcrumb(this, "Document scanner screen created")

        setContent {
            TextraOcrTheme(darkTheme = AppSettings.isDarkThemeEnabled(this)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(R.string.scanner_loading_message),
                                modifier = Modifier.padding(top = 16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }

        launchScanner()
    }

    private fun launchScanner() {
        AppDiagnostics.logBreadcrumb(this, "Preparing ML Kit document scanner")

        if (!AppSettings.canStartScan(this, AppSettings.isPremiumCached(this))) {
            Toast.makeText(this, getString(R.string.daily_limit_reached), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(100)
            .setResultFormats(RESULT_FORMAT_JPEG)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()

        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                AppDiagnostics.logBreadcrumb(this, "Launching ML Kit scanner intent")
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { exception ->
                AppDiagnostics.logBreadcrumb(this, "Failed to start scanner", exception)
                Toast.makeText(
                    this,
                    getString(
                        R.string.scanner_start_failed,
                        exception.message ?: getString(R.string.error_unknown)
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
    }

    private fun saveImageToFilesDir(uri: Uri, pageIndex: Int): Uri? {
        return try {
            val file = createUserDocumentFile(
                context = this,
                prefix = "Scanned_Page_${pageIndex + 1}",
                extension = ".jpg"
            )
            val inputStream = contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Unable to open scanned page stream")
            inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            AppDiagnostics.logBreadcrumb(
                this,
                "Scanner image saved to ${AppDiagnostics.describeUri(Uri.fromFile(file).toString())}"
            )
            androidx.core.content.FileProvider.getUriForFile(
                this,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file
            )
        } catch (exception: Exception) {
            AppDiagnostics.logBreadcrumb(this, "Error saving scanned page", exception)
            null
        }
    }

    companion object {
        const val EXTRA_IMAGE_URIS = "imageUris"
    }
}
