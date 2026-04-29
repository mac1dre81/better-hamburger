package com.dredio.textraocr

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dredio.textraocr.ui.theme.TextraOcrTheme
import java.text.DateFormat

class MainActivity : ComponentActivity() {
    private var savedFiles by mutableStateOf<List<UserDocumentFile>>(emptyList())
    private var blackWhitePreviewEnabled by mutableStateOf(false)
    private var isDarkThemeEnabled by mutableStateOf(false)
    private var autoOcrImageOpenEnabled by mutableStateOf(true)
    private var autoSaveOcrEnabled by mutableStateOf(false)
    private var defaultOutputFormat by mutableStateOf(AppSettings.DEFAULT_OCR_OUTPUT_FORMAT)
    private var showSettingsDialog by mutableStateOf(false)
    private var premiumState by mutableStateOf(PremiumUiState())
    private var remainingFreeScans by mutableStateOf(0)

    private lateinit var billingManager: PremiumBillingManager

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            openDocumentUri(
                context = this,
                uri = uri,
                mimeType = contentResolver.getType(uri)
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        billingManager = PremiumBillingManager(this) { state ->
            premiumState = state
            refreshUiState()
        }
        billingManager.start()
        refreshUiState()

        AppDiagnostics.logBreadcrumb(this, "MainActivity created")
        AppDiagnostics.consumeLastCrashReport(this)?.let {
            Toast.makeText(this, getString(R.string.crash_recovery_message), Toast.LENGTH_LONG).show()
        }

        setContent {
            TextraOcrTheme(darkTheme = isDarkThemeEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            HomeScreen(
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .windowInsetsPadding(WindowInsets.safeDrawing),
                                savedFiles = savedFiles,
                                remainingFreeScans = remainingFreeScans,
                                isPremium = premiumState.isPremium,
                                blackWhitePreviewEnabled = blackWhitePreviewEnabled,
                                showAds = !premiumState.isPremium,
                                onScanClick = {
                                    if (!premiumState.isPremium && remainingFreeScans <= 0) {
                                        Toast.makeText(
                                            this,
                                            getString(R.string.daily_limit_reached),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        AppDiagnostics.logBreadcrumb(this, "Launching document scanner")
                                        startActivity(Intent(this, DocumentScannerActivity::class.java))
                                    }
                                },
                                onOpenFileClick = {
                                    openDocumentLauncher.launch(arrayOf("text/*", "application/pdf", "image/*"))
                                },
                                onOpenSavedFile = { file ->
                                    openUserDocument(this, file.file)
                                },
                                onSettingsClick = { showSettingsDialog = true }
                            )

                        if (showSettingsDialog) {
                            HomeSettingsDialog(
                                blackWhitePreviewEnabled = blackWhitePreviewEnabled,
                                isDarkThemeEnabled = isDarkThemeEnabled,
                                autoOcrImageOpenEnabled = autoOcrImageOpenEnabled,
                                autoSaveOcrEnabled = autoSaveOcrEnabled,
                                defaultOutputFormat = defaultOutputFormat,
                                isPremium = premiumState.isPremium,
                                remainingFreeScans = remainingFreeScans,
                                availableProducts = premiumState.availableProducts,
                                onBlackWhitePreviewChanged = { enabled ->
                                    blackWhitePreviewEnabled = enabled
                                    AppSettings.setBlackWhitePreviewEnabled(this, enabled)
                                },
                                onDarkThemeChanged = { enabled ->
                                    isDarkThemeEnabled = enabled
                                    AppSettings.setDarkThemeEnabled(this, enabled)
                                },
                                onAutoOcrImageOpenChanged = { enabled ->
                                    autoOcrImageOpenEnabled = enabled
                                    AppSettings.setAutoOcrImageOpenEnabled(this, enabled)
                                },
                                onAutoSaveOcrChanged = { enabled ->
                                    autoSaveOcrEnabled = enabled
                                    AppSettings.setAutoSaveOcrEnabled(this, enabled)
                                },
                                onDefaultOutputFormatChanged = { format ->
                                    defaultOutputFormat = format
                                    AppSettings.setDefaultOutputFormat(this, format)
                                },
                                onSubscribeProduct = { productDetails ->
                                    if (!billingManager.launchSubscription(this, productDetails)) {
                                        Toast.makeText(
                                            this,
                                            getString(R.string.subscription_unavailable),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onRestorePurchases = {
                                    billingManager.refreshPurchases()
                                    Toast.makeText(
                                        this,
                                        getString(R.string.subscription_purchase_hint),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onDismiss = { showSettingsDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUiState()
    }

    override fun onDestroy() {
        billingManager.stop()
        super.onDestroy()
    }

    private fun refreshUiState() {
        savedFiles = listUserDocumentFiles(this)
        blackWhitePreviewEnabled = AppSettings.isBlackWhitePreviewEnabled(this)
        isDarkThemeEnabled = AppSettings.isDarkThemeEnabled(this)
        autoOcrImageOpenEnabled = AppSettings.isAutoOcrImageOpenEnabled(this)
        autoSaveOcrEnabled = AppSettings.isAutoSaveOcrEnabled(this)
        defaultOutputFormat = AppSettings.getDefaultOutputFormat(this)
        remainingFreeScans = AppSettings.remainingFreeScans(this)
        if (!premiumState.isPremium) {
            premiumState = premiumState.copy(isPremium = AppSettings.isPremiumCached(this))
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    savedFiles: List<UserDocumentFile>,
    remainingFreeScans: Int,
    isPremium: Boolean,
    blackWhitePreviewEnabled: Boolean,
    showAds: Boolean,
    onScanClick: () -> Unit,
    onOpenFileClick: () -> Unit,
    onOpenSavedFile: (UserDocumentFile) -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onSettingsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.settings_button_label))
        }
        Spacer(modifier = Modifier.height(12.dp))
        SettingsSummaryCard(
            remainingFreeScans = remainingFreeScans,
            isPremium = isPremium,
            blackWhitePreviewEnabled = blackWhitePreviewEnabled,
            isDarkThemeEnabled = isDarkThemeEnabled,
            autoOcrImageOpenEnabled = autoOcrImageOpenEnabled
        )
        if (!isPremium) {
            Spacer(modifier = Modifier.height(16.dp))
            PremiumPricingCard(
                onOpenSettings = onSettingsClick
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onScanClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.start_scan))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onOpenFileClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.open_file))
        }
        if (showAds) {
            Spacer(modifier = Modifier.height(16.dp))
            BannerAdView(
                adUnitId = stringResource(R.string.admob_banner_test_unit_id),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.saved_files_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (savedFiles.isEmpty()) {
            Text(
                text = stringResource(R.string.saved_files_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            savedFiles.forEach { file ->
                SavedFileCard(
                    file = file,
                    onOpenFile = { onOpenSavedFile(file) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PremiumPricingCard(
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.pricing_section_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.pricing_section_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.view_pricing_settings))
            }
        }
    }
}

@Composable
private fun SettingsSummaryCard(
    remainingFreeScans: Int,
    isPremium: Boolean,
    blackWhitePreviewEnabled: Boolean,
    isDarkThemeEnabled: Boolean,
    autoOcrImageOpenEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.premium_status_template,
                    stringResource(
                        if (isPremium) R.string.premium_status_premium else R.string.premium_status_free
                    )
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.remaining_scans_template,
                    remainingFreeScans,
                    AppSettings.FREE_DAILY_SCAN_LIMIT
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.black_white_preview_status,
                    stringResource(
                        if (blackWhitePreviewEnabled) R.string.enabled else R.string.disabled
                    )
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.dark_theme_label
                ) + ": " + stringResource(
                    if (isDarkThemeEnabled) R.string.enabled else R.string.disabled
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.auto_ocr_image_open_label
                ) + ": " + stringResource(
                    if (autoOcrImageOpenEnabled) R.string.enabled else R.string.disabled
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.auto_save_ocr_label
                ) + ": " + stringResource(
                    if (autoSaveOcrEnabled) R.string.enabled else R.string.disabled
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.default_output_format_label) + ": " + defaultOutputFormat,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SavedFileCard(
    file: UserDocumentFile,
    onOpenFile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = file.displayName,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(
                    R.string.saved_file_modified_template,
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(file.modifiedAt)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onOpenFile) {
                    Text(text = stringResource(R.string.open_saved_file))
                }
            }
        }
    }
}

@Composable
private fun HomeSettingsDialog(
    blackWhitePreviewEnabled: Boolean,
    isDarkThemeEnabled: Boolean,
    autoOcrImageOpenEnabled: Boolean,
    autoSaveOcrEnabled: Boolean,
    defaultOutputFormat: String,
    isPremium: Boolean,
    remainingFreeScans: Int,
    availableProducts: List<com.android.billingclient.api.ProductDetails>,
    onBlackWhitePreviewChanged: (Boolean) -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit,
    onAutoOcrImageOpenChanged: (Boolean) -> Unit,
    onAutoSaveOcrChanged: (Boolean) -> Unit,
    onDefaultOutputFormatChanged: (String) -> Unit,
    onSubscribeProduct: (com.android.billingclient.api.ProductDetails) -> Unit,
    onRestorePurchases: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = stringResource(R.string.daily_limit_message),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = stringResource(
                            R.string.remaining_scans_template,
                            remainingFreeScans,
                            AppSettings.FREE_DAILY_SCAN_LIMIT
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    SettingsToggleRow(
                        label = stringResource(R.string.black_white_preview),
                        checked = blackWhitePreviewEnabled,
                        onCheckedChange = onBlackWhitePreviewChanged
                    )

                    SettingsToggleRow(
                        label = stringResource(R.string.dark_theme_label),
                        checked = isDarkThemeEnabled,
                        onCheckedChange = onDarkThemeChanged
                    )

                    SettingsToggleRow(
                        label = stringResource(R.string.auto_ocr_image_open_label),
                        checked = autoOcrImageOpenEnabled,
                        onCheckedChange = onAutoOcrImageOpenChanged
                    )

                    SettingsToggleRow(
                        label = stringResource(R.string.auto_save_ocr_label),
                        checked = autoSaveOcrEnabled,
                        onCheckedChange = onAutoSaveOcrChanged
                    )

                    Text(
                        text = stringResource(R.string.default_output_format_label),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onDefaultOutputFormatChanged(".txt") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stringResource(R.string.format_txt_label))
                        }
                        Button(
                            onClick = { onDefaultOutputFormatChanged(".md") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stringResource(R.string.format_md_label))
                        }
                    }

                    Text(
                        text = stringResource(R.string.premium_benefits),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = stringResource(
                            R.string.premium_status_template,
                            stringResource(
                                if (isPremium) R.string.premium_status_premium else R.string.premium_status_free
                            )
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (!isPremium) {
                        Text(
                            text = stringResource(R.string.subscription_plans_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.subscription_purchase_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onRestorePurchases,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.restore_purchases))
                        }
                        if (availableProducts.isEmpty()) {
                            Text(
                                text = stringResource(R.string.subscription_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            val weeklyProduct = availableProducts.firstOrNull { it.productId == "textraocr_premium_weekly" }
                            val monthlyProduct = availableProducts.firstOrNull { it.productId == "textraocr_premium_monthly" }
                            val yearlyProduct = availableProducts.firstOrNull { it.productId == "textraocr_premium_yearly" }

                            listOf(
                                weeklyProduct to stringResource(R.string.subscription_weekly_label),
                                monthlyProduct to stringResource(R.string.subscription_monthly_label),
                                yearlyProduct to stringResource(R.string.subscription_yearly_label)
                            ).forEach { (product, label) ->
                                product?.let {
                                    Button(
                                        onClick = { onSubscribeProduct(it) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = label)
                                    }
                                }
                            }

                            availableProducts.filter { product ->
                                product.productId !in listOf(
                                    "textraocr_premium_weekly",
                                    "textraocr_premium_monthly",
                                    "textraocr_premium_yearly"
                                )
                            }.forEach { product ->
                                Button(
                                    onClick = { onSubscribeProduct(product) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = product.title)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }

                if (!isPremium) {
                    BannerAdView(
                        adUnitId = stringResource(R.string.admob_banner_test_unit_id),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
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
