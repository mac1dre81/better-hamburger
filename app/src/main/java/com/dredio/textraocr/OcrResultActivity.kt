package com.dredio.textraocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.dredio.textraocr.DocumentScannerActivity.Companion.EXTRA_IMAGE_URIS
import com.dredio.textraocr.ocr.LineResult
import com.dredio.textraocr.ocr.OcrPage
import com.dredio.textraocr.ocr.OcrPreprocessingOptions
import com.dredio.textraocr.ocr.OcrProcessor
import com.dredio.textraocr.ocr.OcrSessionResult
import com.dredio.textraocr.ocr.PreprocessingMetadata
import com.dredio.textraocr.ocr.ReviewLevel
import com.dredio.textraocr.ui.theme.TextraOcrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class OcrResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppDiagnostics.logBreadcrumb(this, "OCR result screen created")
        val imageUris = intent.getStringArrayListExtra(EXTRA_IMAGE_URIS) ?: emptyList()

        setContent {
            TextraOcrTheme(darkTheme = AppSettings.isDarkThemeEnabled(this)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OcrScreen(
                        imageUris = imageUris,
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}

private data class OcrUiPage(
    val pageNumber: Int,
    val imageUri: Uri,
    val editableText: String,
    val lines: List<LineResult>,
    val preprocessingMetadata: PreprocessingMetadata,
    val preprocessingOptions: OcrPreprocessingOptions,
    val errorMessage: String? = null,
    val selectedLineId: String? = null,
    val textSelectionStart: Int = 0,
    val textSelectionEnd: Int = 0
)

@Composable
private fun OcrScreen(
    imageUris: List<String>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val noImagesFoundText = stringResource(R.string.no_images_found)
    val scannedImageDescription = stringResource(R.string.scanned_image_content_description)
    val extractingText = stringResource(R.string.extracting_text)
    val loadingDetail = stringResource(R.string.preprocessing_details)
    val ocrErrorText = stringResource(R.string.ocr_error)
    val retryText = stringResource(R.string.retry)
    val ocrResultLabel = stringResource(R.string.ocr_result_label)
    val copiedText = stringResource(R.string.copied_to_clipboard)
    val copyText = stringResource(R.string.ocr_copy)
    val saveTxtText = stringResource(R.string.save_txt)
    val saveMdText = stringResource(R.string.save_md)
    val saveErrorText = stringResource(R.string.save_error)
    val savedAsTemplate = stringResource(R.string.saved_as, "%s")
    val preprocessingTemplate = stringResource(R.string.preprocessing_summary_template)
    val pageChipTemplate = stringResource(R.string.page_chip_template)
    val textFieldLabelTemplate = stringResource(R.string.extracted_text_page_label)
    val compareLinesText = stringResource(R.string.compare_lines_hint)
    val lineSelectedText = stringResource(R.string.line_selected)
    val pageHeaderTemplate = stringResource(R.string.page_header_template)
    val enabledText = stringResource(R.string.enabled)
    val disabledText = stringResource(R.string.disabled)
    val preprocessingAdviceText = stringResource(R.string.preprocessing_missing_letters_hint)
    val preprocessingDialogTitle = stringResource(R.string.preprocessing_dialog_title)
    val applySettingsText = stringResource(R.string.apply_settings)
    val cancelText = stringResource(R.string.cancel)
    val resetText = stringResource(R.string.reset_settings)
    val deskewAdjustmentTemplate = stringResource(R.string.deskew_adjustment_template)
    val denoiseLabel = stringResource(R.string.denoise_label)
    val contrastLabel = stringResource(R.string.contrast_label)
    val binarizeLabel = stringResource(R.string.binarize_label)
    val reviewConfidenceTemplate = stringResource(R.string.review_confidence_template)
    val reviewLevelLow = stringResource(R.string.review_level_low)
    val reviewLevelMedium = stringResource(R.string.review_level_medium)
    val reviewLevelHigh = stringResource(R.string.review_level_high)
    val reviewLevelUnknown = stringResource(R.string.review_level_unknown)
    val ocrCorrectionHint = stringResource(R.string.ocr_correction_hint)
    val blackWhitePreviewLabel = stringResource(R.string.black_white_preview)
    val ocrSettingsButtonText = stringResource(R.string.ocr_settings_button)

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pages by remember { mutableStateOf<List<OcrUiPage>>(emptyList()) }
    var selectedPageIndex by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var settingsDraft by remember { mutableStateOf(OcrPreprocessingOptions()) }
    var blackWhitePreviewEnabled by remember {
        mutableStateOf(AppSettings.isBlackWhitePreviewEnabled(context))
    }
    var hasAutoSaved by remember { mutableStateOf(false) }
    val textFocusRequester = remember { FocusRequester() }

    fun formatPreprocessingSummary(metadata: PreprocessingMetadata): String {
        return String.format(
            Locale.US,
            preprocessingTemplate,
            metadata.deskewAngleDegrees,
            if (metadata.denoised) enabledText else disabledText,
            if (metadata.normalizedContrast) enabledText else disabledText,
            if (metadata.adaptiveBinarization) enabledText else disabledText
        )
    }

    fun parsePage(page: OcrPage, options: OcrPreprocessingOptions): OcrUiPage {
        return OcrUiPage(
            pageNumber = page.pageNumber,
            imageUri = page.imageUri,
            editableText = page.recognizedText,
            lines = page.linesWithConfidence,
            preprocessingMetadata = page.preprocessingMetadata,
            preprocessingOptions = options,
            errorMessage = page.errorMessage
        )
    }

    fun parseSession(
        result: OcrSessionResult,
        preprocessingOptionsByPage: Map<Int, OcrPreprocessingOptions>
    ): List<OcrUiPage> {
        return result.session.pages.map { page ->
            parsePage(
                page = page,
                options = preprocessingOptionsByPage[page.pageNumber] ?: OcrPreprocessingOptions()
            )
        }
    }

    fun reviewConfidence(line: LineResult): String {
        val levelText = when (line.reviewLevel) {
            ReviewLevel.LOW -> reviewLevelLow
            ReviewLevel.MEDIUM -> reviewLevelMedium
            ReviewLevel.HIGH -> reviewLevelHigh
            ReviewLevel.UNKNOWN -> reviewLevelUnknown
        }
        val percentage = line.confidence?.times(100)?.toInt()
        return if (percentage != null) {
            String.format(reviewConfidenceTemplate, levelText, percentage)
        } else {
            levelText
        }
    }

    fun mergedRecognizedText(): String {
        return pages.joinToString(separator = "\n\n--- Page Break ---\n\n") { page ->
            String.format(pageHeaderTemplate, page.pageNumber) + "\n" + page.editableText
        }.trim()
    }

    fun saveOcrResult(
        extension: String,
        closeAfterSave: Boolean = true,
        showToast: Boolean = true
    ) {
        scope.launch {
            try {
                val mergedText = mergedRecognizedText()
                val file = createUserDocumentFile(
                    context = context,
                    prefix = "OCR_Result",
                    extension = extension
                )

                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { stream ->
                        stream.write(mergedText.toByteArray())
                    }
                }

                AppDiagnostics.logBreadcrumb(
                    context,
                    "OCR result saved to ${AppDiagnostics.describeUri(Uri.fromFile(file).toString())}"
                )
                if (showToast) {
                    Toast.makeText(context, savedAsTemplate.format(file.name), Toast.LENGTH_SHORT).show()
                }
                if (closeAfterSave) {
                    onClose()
                }
            } catch (exception: Exception) {
                AppDiagnostics.logBreadcrumb(context, "OCR result save failed", exception)
                Toast.makeText(context, saveErrorText, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun runOcr(preprocessingOptionsByPage: Map<Int, OcrPreprocessingOptions> = emptyMap()) {
        scope.launch {
            isLoading = true
            errorMessage = null
            AppDiagnostics.logBreadcrumb(context, "Starting OCR pipeline for ${imageUris.size} image(s)")

            try {
                val result = OcrProcessor(context).processSession(
                    imageUris = imageUris,
                    preprocessingOptionsByPage = preprocessingOptionsByPage
                )
                val parsedPages = parseSession(result, preprocessingOptionsByPage)

                pages = if (parsedPages.isEmpty()) {
                    listOf(
                        OcrUiPage(
                            pageNumber = 1,
                            imageUri = Uri.EMPTY,
                            editableText = noImagesFoundText,
                            lines = emptyList(),
                            preprocessingMetadata = PreprocessingMetadata(),
                            preprocessingOptions = OcrPreprocessingOptions()
                        )
                    )
                } else {
                    parsedPages
                }
                selectedPageIndex = 0

                if (AppSettings.isAutoSaveOcrEnabled(context) && !hasAutoSaved && parsedPages.any { it.imageUri != Uri.EMPTY }) {
                    hasAutoSaved = true
                    saveOcrResult(
                        AppSettings.getDefaultOutputFormat(context),
                        closeAfterSave = false,
                        showToast = true
                    )
                }

                AppDiagnostics.logBreadcrumb(
                    context,
                    "OCR pipeline complete: success=${result.successCount}, failed=${result.failureCount}"
                )
            } catch (exception: Exception) {
                errorMessage = exception.message ?: ocrErrorText
                AppDiagnostics.logBreadcrumb(context, "OCR pipeline failed", exception)
            } finally {
                isLoading = false
            }
        }
    }

    fun computeLineRanges(
        text: String,
        lines: List<LineResult>
    ): Map<String, TextRange> {
        var searchStart = 0
        return buildMap {
            lines.forEach { line ->
                if (line.text.isBlank()) return@forEach

                val index = text.indexOf(line.text, startIndex = searchStart)
                    .takeIf { it >= 0 }
                    ?: text.indexOf(line.text).takeIf { it >= 0 }
                    ?: return@forEach

                val end = index + line.text.length
                put(line.id, TextRange(index, end))
                searchStart = end
            }
        }
    }

    fun updateCurrentPageText(value: TextFieldValue) {
        pages = pages.mapIndexed { index, page ->
            if (index == selectedPageIndex) {
                page.copy(
                    editableText = value.text,
                    textSelectionStart = value.selection.start,
                    textSelectionEnd = value.selection.end
                )
            } else {
                page
            }
        }
    }

    fun highlightCurrentLine(line: LineResult) {
        val page = pages.getOrNull(selectedPageIndex) ?: return
        val selection = computeLineRanges(page.editableText, page.lines)[line.id] ?: return

        pages = pages.mapIndexed { index, current ->
            if (index == selectedPageIndex) {
                current.copy(
                    selectedLineId = line.id,
                    textSelectionStart = selection.start,
                    textSelectionEnd = selection.end
                )
            } else {
                current
            }
        }
    }

    fun reprocessCurrentPage(options: OcrPreprocessingOptions) {
        val page = pages.getOrNull(selectedPageIndex) ?: return
        if (page.imageUri == Uri.EMPTY) return

        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                val refreshedPage = OcrProcessor(context).processPage(
                    pageNumber = page.pageNumber,
                    imageUriString = page.imageUri.toString(),
                    preprocessingOptions = options
                )
                pages = pages.mapIndexed { index, existingPage ->
                    if (index == selectedPageIndex) {
                        parsePage(refreshedPage, options)
                    } else {
                        existingPage
                    }
                }
                AppDiagnostics.logBreadcrumb(
                    context,
                    "OCR page ${page.pageNumber} reprocessed with updated preprocessing"
                )
            } catch (exception: Exception) {
                AppDiagnostics.logBreadcrumb(context, "OCR page reprocess failed", exception)
                Toast.makeText(
                    context,
                    exception.message ?: ocrErrorText,
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(imageUris) {
        if (imageUris.isNotEmpty()) {
            runOcr()
        } else {
            isLoading = false
            errorMessage = noImagesFoundText
        }
    }

    val currentPage = pages.getOrNull(selectedPageIndex)
    val currentTextFieldValue = currentPage?.let { page ->
        val safeStart = page.textSelectionStart.coerceIn(0, page.editableText.length)
        val safeEnd = page.textSelectionEnd.coerceIn(0, page.editableText.length)
        TextFieldValue(
            text = page.editableText,
            selection = TextRange(safeStart, safeEnd)
        )
    } ?: TextFieldValue("")
    val previewColorFilter = if (blackWhitePreviewEnabled) {
        ColorFilter.colorMatrix(
            ColorMatrix().apply { setToSaturation(0f) }
        )
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = ocrResultLabel,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    settingsDraft = currentPage?.preprocessingOptions ?: OcrPreprocessingOptions()
                    showSettingsDialog = true
                }
            ) {
                Text(text = ocrSettingsButtonText)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite }
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = extractingText,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = loadingDetail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    ) {
                        Text(
                            text = errorMessage ?: ocrErrorText,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { runOcr() }) {
                            Text(text = retryText)
                        }
                    }
                }
            }

            else -> {
                if (pages.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(pages) { index, page ->
                            FilterChip(
                                selected = index == selectedPageIndex,
                                onClick = { selectedPageIndex = index },
                                label = {
                                    Text(text = String.format(pageChipTemplate, page.pageNumber))
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                currentPage?.let { page ->
                    val sortedReviewLines = page.lines.sortedBy { it.confidence ?: 1f }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            if (page.imageUri != Uri.EMPTY) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = page.imageUri),
                                    contentDescription = scannedImageDescription,
                                    colorFilter = previewColorFilter,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Text(
                                text = formatPreprocessingSummary(page.preprocessingMetadata),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = preprocessingAdviceText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = currentTextFieldValue,
                        onValueChange = ::updateCurrentPageText,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .focusRequester(textFocusRequester),
                        label = {
                            Text(text = String.format(textFieldLabelTemplate, page.pageNumber))
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (sortedReviewLines.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = compareLinesText,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = ocrCorrectionHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    sortedReviewLines.forEach { line ->
                                        val isSelected = page.selectedLineId == line.id
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    highlightCurrentLine(line)
                                                    textFocusRequester.requestFocus()
                                                    Toast.makeText(
                                                        context,
                                                        lineSelectedText,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) {
                                                    MaterialTheme.colorScheme.secondaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                                }
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp)
                                            ) {
                                                Text(
                                                    text = line.text,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    text = reviewConfidence(line),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }                                        
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(ocrResultLabel, mergedRecognizedText())
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(text = copyText)
                    }

                    Button(onClick = { saveOcrResult(".txt") }) {
                        Text(text = saveTxtText)
                    }

                    Button(onClick = { saveOcrResult(".md") }) {
                        Text(text = saveMdText)
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text(text = preprocessingDialogTitle) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = preprocessingAdviceText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            deskewAdjustmentTemplate,
                            settingsDraft.deskewAdjustmentDegrees
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = settingsDraft.deskewAdjustmentDegrees,
                        onValueChange = { value ->
                            settingsDraft = settingsDraft.copy(
                                deskewAdjustmentDegrees = (value * 2).toInt() / 2f
                            )
                        },
                        valueRange = -10f..10f
                    )
                    PreprocessingToggleRow(
                        label = blackWhitePreviewLabel,
                        checked = blackWhitePreviewEnabled,
                        onCheckedChange = { checked ->
                            blackWhitePreviewEnabled = checked
                            AppSettings.setBlackWhitePreviewEnabled(context, checked)
                        }
                    )
                    PreprocessingToggleRow(
                        label = denoiseLabel,
                        checked = settingsDraft.denoiseEnabled,
                        onCheckedChange = { checked ->
                            settingsDraft = settingsDraft.copy(denoiseEnabled = checked)
                        }
                    )
                    PreprocessingToggleRow(
                        label = contrastLabel,
                        checked = settingsDraft.contrastEnabled,
                        onCheckedChange = { checked ->
                            settingsDraft = settingsDraft.copy(contrastEnabled = checked)
                        }
                    )
                    if (settingsDraft.contrastEnabled) {
                        Text(
                            text = String.format(
                                Locale.US,
                                stringResource(R.string.contrast_level_template),
                                settingsDraft.contrastLevel
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = settingsDraft.contrastLevel,
                            onValueChange = { value ->
                                settingsDraft = settingsDraft.copy(contrastLevel = value)
                            },
                            valueRange = 0.5f..2.0f
                        )
                    }
                    PreprocessingToggleRow(
                        label = binarizeLabel,
                        checked = settingsDraft.binarizeEnabled,
                        onCheckedChange = { checked ->
                            settingsDraft = settingsDraft.copy(binarizeEnabled = checked)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSettingsDialog = false
                        reprocessCurrentPage(settingsDraft)
                    }
                ) {
                    Text(text = applySettingsText)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = { settingsDraft = OcrPreprocessingOptions() }
                    ) {
                        Text(text = resetText)
                    }
                    TextButton(
                        onClick = { showSettingsDialog = false }
                    ) {
                        Text(text = cancelText)
                    }
                }
            }
        )
    }
}

@Composable
private fun PreprocessingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
