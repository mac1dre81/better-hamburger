package com.dredio.textraocr

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.android.billingclient.api.ProductDetails
import com.dredio.textraocr.ui.theme.TextraOcrTheme

class MainActivity : ComponentActivity() {
    private var savedFiles by mutableStateOf<List<UserDocumentFile>>(emptyList())
    private var remainingFreeScans by mutableStateOf(0)
    private var premiumState by mutableStateOf(PremiumUiState())
    private var darkThemeEnabled by mutableStateOf(false)
    private var autoOcrImageOpenEnabled by mutableStateOf(false)
    private var autoSaveOcrEnabled by mutableStateOf(false)
    private var defaultOutputFormat by mutableStateOf(AppSettings.DEFAULT_OCR_OUTPUT_FORMAT)
    private var showSettingsDialog by mutableStateOf(false)
    private var showSubscriptionDialog by mutableStateOf(false)

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

        setContent {
            TextraOcrTheme(darkTheme = darkThemeEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            HomeScreen(
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing),
                                savedFiles = savedFiles,
                                remainingFreeScans = remainingFreeScans,
                                isPremium = premiumState.isPremium,
                                onScanClick = {
                                    if (!premiumState.isPremium && remainingFreeScans <= 0) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            getString(R.string.daily_limit_reached),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        startActivity(Intent(this@MainActivity, DocumentScannerActivity::class.java))
                                    }
                                },
                                onOpenFileClick = {
                                    openDocumentLauncher.launch(arrayOf("text/*", "application/pdf", "image/*"))
                                },
                                onOpenSavedFile = { file ->
                                    openUserDocument(this@MainActivity, file.file)
                                },
                                onSettingsClick = { showSettingsDialog = true },
                                onOpenSubscriptionScreen = { showSubscriptionDialog = true },
                                premiumProducts = premiumState.availableProducts,
                                premiumSubscriptionAvailable = premiumState.subscriptionAvailable,
                                onSubscribeProduct = { productDetails ->
                                    if (!billingManager.launchSubscription(this@MainActivity, productDetails)) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            getString(R.string.subscription_unavailable),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                onRestorePurchases = {
                                    billingManager.refreshPurchases()
                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(R.string.restore_purchases),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )

                            if (showSettingsDialog) {
                                HomeSettingsDialog(
                                    isPremium = premiumState.isPremium,
                                    availableProducts = premiumState.availableProducts,
                                    subscriptionAvailable = premiumState.subscriptionAvailable,
                                    darkThemeEnabled = darkThemeEnabled,
                                    autoOcrImageOpenEnabled = autoOcrImageOpenEnabled,
                                    autoSaveOcrEnabled = autoSaveOcrEnabled,
                                    defaultOutputFormat = defaultOutputFormat,
                                    onSubscribeProduct = { productDetails ->
                                        if (!billingManager.launchSubscription(this@MainActivity, productDetails)) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                getString(R.string.subscription_unavailable),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    onRestorePurchases = {
                                        billingManager.refreshPurchases()
                                        Toast.makeText(
                                            this@MainActivity,
                                            getString(R.string.restore_purchases),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onDarkThemeChanged = {
                                        darkThemeEnabled = it
                                        AppSettings.setDarkThemeEnabled(this@MainActivity, it)
                                    },
                                    onAutoOcrImageOpenChanged = {
                                        autoOcrImageOpenEnabled = it
                                        AppSettings.setAutoOcrImageOpenEnabled(this@MainActivity, it)
                                    },
                                    onAutoSaveOcrEnabledChanged = {
                                        autoSaveOcrEnabled = it
                                        AppSettings.setAutoSaveOcrEnabled(this@MainActivity, it)
                                    },
                                    onDefaultOutputFormatChanged = {
                                        defaultOutputFormat = it
                                        AppSettings.setDefaultOutputFormat(this@MainActivity, it)
                                    },
                                            onDismiss = { showSettingsDialog = false }
                                )
                            }

                            if (showSubscriptionDialog) {
                                SubscriptionDialog(
                                    availableProducts = premiumState.availableProducts,
                                    subscriptionAvailable = premiumState.subscriptionAvailable,
                                    onSubscribeProduct = { productDetails ->
                                        if (!billingManager.launchSubscription(this@MainActivity, productDetails)) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                getString(R.string.subscription_unavailable),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    onRestorePurchases = {
                                        billingManager.refreshPurchases()
                                        Toast.makeText(
                                            this@MainActivity,
                                            getString(R.string.restore_purchases),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onDismiss = { showSubscriptionDialog = false }
                                )
                            }
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
        remainingFreeScans = AppSettings.remainingFreeScans(this)
        darkThemeEnabled = AppSettings.isDarkThemeEnabled(this)
        autoOcrImageOpenEnabled = AppSettings.isAutoOcrImageOpenEnabled(this)
        autoSaveOcrEnabled = AppSettings.isAutoSaveOcrEnabled(this)
        defaultOutputFormat = AppSettings.getDefaultOutputFormat(this)
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
    onScanClick: () -> Unit,
    onOpenFileClick: () -> Unit,
    onOpenSavedFile: (UserDocumentFile) -> Unit,
    onSettingsClick: () -> Unit,
    onOpenSubscriptionScreen: () -> Unit,
    premiumProducts: List<ProductDetails>,
    premiumSubscriptionAvailable: Boolean,
    onSubscribeProduct: (ProductDetails) -> Unit,
    onRestorePurchases: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() }
            )
            Button(onClick = onSettingsClick) {
                Text(text = stringResource(R.string.settings_button_label))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        SettingsSummaryCard(
            isPremium = isPremium,
            remainingFreeScans = remainingFreeScans,
            onOpenSettings = onSettingsClick
        )

        if (!isPremium) {
            Spacer(modifier = Modifier.height(16.dp))
            PremiumPricingCard(
                products = premiumProducts,
                onOpenSubscriptionScreen = onOpenSubscriptionScreen
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
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
private fun HomeSettingsDialog(
    isPremium: Boolean,
    availableProducts: List<ProductDetails>,
    subscriptionAvailable: Boolean,
    darkThemeEnabled: Boolean,
    autoOcrImageOpenEnabled: Boolean,
    autoSaveOcrEnabled: Boolean,
    defaultOutputFormat: String,
    onSubscribeProduct: (ProductDetails) -> Unit,
    onRestorePurchases: () -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit,
    onAutoOcrImageOpenChanged: (Boolean) -> Unit,
    onAutoSaveOcrEnabledChanged: (Boolean) -> Unit,
    onDefaultOutputFormatChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var localDarkTheme by remember { mutableStateOf(darkThemeEnabled) }
    var localAutoOcrImageOpen by remember { mutableStateOf(autoOcrImageOpenEnabled) }
    var localAutoSaveOcr by remember { mutableStateOf(autoSaveOcrEnabled) }
    var localDefaultFormat by remember { mutableStateOf(defaultOutputFormat) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SettingsSummaryCard(
                    isPremium = isPremium,
                    remainingFreeScans = AppSettings.remainingFreeScans(context),
                    showSettingsButton = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!isPremium) {
                    Text(
                        text = stringResource(R.string.premium_benefits),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.subscription_purchase_hint),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (subscriptionAvailable && availableProducts.isNotEmpty()) {
                        availableProducts.forEach { product ->
                            SubscriptionProductRow(
                                product = product,
                                onSubscribe = { onSubscribeProduct(product) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.subscription_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onRestorePurchases) {
                        Text(text = stringResource(R.string.restore_purchases))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                SettingsToggleRow(
                    label = stringResource(R.string.dark_theme_label),
                    checked = localDarkTheme,
                    onCheckedChange = {
                        localDarkTheme = it
                        onDarkThemeChanged(it)
                    }
                )
                SettingsToggleRow(
                    label = stringResource(R.string.auto_ocr_image_open_label),
                    checked = localAutoOcrImageOpen,
                    onCheckedChange = {
                        localAutoOcrImageOpen = it
                        onAutoOcrImageOpenChanged(it)
                    }
                )
                SettingsToggleRow(
                    label = stringResource(R.string.auto_save_ocr_label),
                    checked = localAutoSaveOcr,
                    onCheckedChange = {
                        localAutoSaveOcr = it
                        onAutoSaveOcrEnabledChanged(it)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.default_output_format_label), fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            localDefaultFormat = AppSettings.DEFAULT_OCR_OUTPUT_FORMAT
                            onDefaultOutputFormatChanged(localDefaultFormat)
                        },
                        modifier = Modifier.fillMaxWidth(0.5f)
                    ) {
                        Text(text = stringResource(R.string.format_txt_label))
                    }
                    OutlinedButton(
                        onClick = {
                            localDefaultFormat = ".md"
                            onDefaultOutputFormatChanged(localDefaultFormat)
                        },
                        modifier = Modifier.fillMaxWidth(0.5f)
                    ) {
                        Text(text = stringResource(R.string.format_md_label))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.current_output_format, localDefaultFormat),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SettingsSummaryCard(
    isPremium: Boolean,
    remainingFreeScans: Int,
    showSettingsButton: Boolean = true,
    onOpenSettings: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.premium_status_template, if (isPremium) stringResource(R.string.premium_status_premium) else stringResource(R.string.premium_status_free)),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.remaining_scans_template, remainingFreeScans, AppSettings.FREE_DAILY_SCAN_LIMIT),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (showSettingsButton) {
                Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.settings_button_label))
                }
            }
        }
    }
}

@Composable
private fun PremiumPricingCard(
    products: List<ProductDetails>,
    onOpenSubscriptionScreen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.pricing_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.pricing_section_description),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (products.isNotEmpty()) {
                products.take(2).forEach { product ->
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = product.subscriptionOfferDetails
                            ?.firstOrNull()
                            ?.pricingPhases
                            ?.pricingPhaseList
                            ?.firstOrNull()
                            ?.formattedPrice
                            .orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                listOf(
                    stringResource(R.string.subscription_weekly_label),
                    stringResource(R.string.subscription_monthly_label),
                    stringResource(R.string.subscription_yearly_label)
                ).forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.subscription_purchase_hint),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(onClick = onOpenSubscriptionScreen, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.subscribe_premium_button))
            }
        }
    }
}

@Composable
private fun SubscriptionDialog(
    availableProducts: List<ProductDetails>,
    subscriptionAvailable: Boolean,
    onSubscribeProduct: (ProductDetails) -> Unit,
    onRestorePurchases: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.subscription_plans_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.subscription_purchase_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (subscriptionAvailable && availableProducts.isNotEmpty()) {
                    availableProducts.forEach { product ->
                        SubscriptionProductRow(
                            product = product,
                            onSubscribe = { onSubscribeProduct(product) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.subscription_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onRestorePurchases) {
                    Text(text = stringResource(R.string.restore_purchases))
                }
            }
        }
    }
}

@Composable
private fun SubscriptionProductRow(
    product: ProductDetails,
    onSubscribe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onSubscribe, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.subscribe_premium))
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SavedFileCard(
    file: UserDocumentFile,
    onOpenFile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = file.displayName,
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = onOpenFile, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.open_file))
            }
        }
    }
}
