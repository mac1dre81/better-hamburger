package com.dredio.textraocr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.ProductDetails
import com.dredio.textraocr.ui.theme.TextraOcrTheme

class SettingsActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_OPEN_SUBSCRIPTION = "extra_open_subscription"

        fun createIntent(context: Context, openSubscription: Boolean = false): Intent {
            return Intent(context, SettingsActivity::class.java).apply {
                putExtra(EXTRA_OPEN_SUBSCRIPTION, openSubscription)
            }
        }
    }

    private lateinit var billingManager: PremiumBillingManager
    private var isPremium by mutableStateOf(false)
    private var subscriptionAvailable by mutableStateOf(false)
    private var availableProducts by mutableStateOf<List<ProductDetails>>(emptyList())
    private var darkThemeEnabled by mutableStateOf(false)
    private var autoOcrImageOpenEnabled by mutableStateOf(true)
    private var autoSaveOcrEnabled by mutableStateOf(false)
    private var defaultOutputFormat by mutableStateOf(AppSettings.DEFAULT_OCR_OUTPUT_FORMAT)
    private var saveDestination by mutableStateOf(AppSettings.DEFAULT_SAVE_DESTINATION)
    private var defaultPreprocessingProfile by mutableStateOf(AppSettings.DEFAULT_PREPROCESSING_PROFILE)
    private var showSubscriptionSection by mutableStateOf(false)
    private var isLoadingProducts by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TextraOcrTheme(darkTheme = darkThemeEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        isPremium = isPremium,
                        subscriptionAvailable = subscriptionAvailable,
                        availableProducts = availableProducts,
                        darkThemeEnabled = darkThemeEnabled,
                        autoOcrImageOpenEnabled = autoOcrImageOpenEnabled,
                        autoSaveOcrEnabled = autoSaveOcrEnabled,
                        defaultOutputFormat = defaultOutputFormat,
                        saveDestination = saveDestination,
                        defaultPreprocessingProfile = defaultPreprocessingProfile,
                        showSubscriptionSection = showSubscriptionSection,
                        isLoadingProducts = isLoadingProducts,
                        onDarkThemeChanged = { enabled ->
                            darkThemeEnabled = enabled
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
                        onDefaultOutputFormatChanged = { outputFormat ->
                            defaultOutputFormat = outputFormat
                            AppSettings.setDefaultOutputFormat(this, outputFormat)
                        },
                        onSaveDestinationChanged = { destination ->
                            saveDestination = destination
                            AppSettings.setSaveDestination(this, destination)
                        },
                        onDefaultPreprocessingProfileChanged = { profile ->
                            defaultPreprocessingProfile = profile
                            AppSettings.setDefaultPreprocessingProfile(this, profile)
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
                                getString(R.string.restore_purchases),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onClose = { finish() }
                    )
                }
            }
        }

        billingManager = PremiumBillingManager(this) { state ->
            isPremium = state.isPremium
            subscriptionAvailable = state.subscriptionAvailable
            availableProducts = state.availableProducts
            refreshUiState()
        }
        billingManager.start()
        refreshUiState()

        showSubscriptionSection = intent.getBooleanExtra(EXTRA_OPEN_SUBSCRIPTION, false)
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
        darkThemeEnabled = AppSettings.isDarkThemeEnabled(this)
        autoOcrImageOpenEnabled = AppSettings.isAutoOcrImageOpenEnabled(this)
        autoSaveOcrEnabled = AppSettings.isAutoSaveOcrEnabled(this)
        defaultOutputFormat = AppSettings.getDefaultOutputFormat(this)
        saveDestination = AppSettings.getSaveDestination(this)
        defaultPreprocessingProfile = AppSettings.getDefaultPreprocessingProfile(this)
        isLoadingProducts = !subscriptionAvailable && availableProducts.isEmpty()
    }
}

@Composable
fun SettingsScreen(
    isPremium: Boolean,
    subscriptionAvailable: Boolean,
    availableProducts: List<ProductDetails>,
    darkThemeEnabled: Boolean,
    autoOcrImageOpenEnabled: Boolean,
    autoSaveOcrEnabled: Boolean,
    defaultOutputFormat: String,
    saveDestination: String,
    defaultPreprocessingProfile: String,
    showSubscriptionSection: Boolean,
    isLoadingProducts: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    onAutoOcrImageOpenChanged: (Boolean) -> Unit,
    onAutoSaveOcrChanged: (Boolean) -> Unit,
    onDefaultOutputFormatChanged: (String) -> Unit,
    onSaveDestinationChanged: (String) -> Unit,
    onDefaultPreprocessingProfileChanged: (String) -> Unit,
    onSubscribeProduct: (ProductDetails) -> Unit,
    onRestorePurchases: () -> Unit,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_screen_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_general_section_title),
                    style = MaterialTheme.typography.titleMedium
                )
                SettingsToggleRow(
                    label = stringResource(R.string.dark_theme_label),
                    checked = darkThemeEnabled,
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
                    OutlinedButton(
                        onClick = { onDefaultOutputFormatChanged(".txt") },
                        modifier = Modifier.weight(1f),
                        colors = if (defaultOutputFormat == ".txt") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(text = stringResource(R.string.format_txt_label))
                    }
                    OutlinedButton(
                        onClick = { onDefaultOutputFormatChanged(".md") },
                        modifier = Modifier.weight(1f),
                        colors = if (defaultOutputFormat == ".md") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(text = stringResource(R.string.format_md_label))
                    }
                    OutlinedButton(
                        onClick = { onDefaultOutputFormatChanged(".pdf") },
                        modifier = Modifier.weight(1f),
                        colors = if (defaultOutputFormat == ".pdf") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(text = stringResource(R.string.format_pdf_label))
                    }
                }
                Text(
                    text = stringResource(
                        R.string.save_destination_label
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSaveDestinationChanged(AppSettings.SAVE_DESTINATION_LOCAL) },
                        modifier = Modifier.weight(1f),
                        colors = if (saveDestination == AppSettings.SAVE_DESTINATION_LOCAL) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(text = stringResource(R.string.save_destination_local_label))
                    }
                    OutlinedButton(
                        onClick = { onSaveDestinationChanged(AppSettings.SAVE_DESTINATION_SHARE) },
                        modifier = Modifier.weight(1f),
                        colors = if (saveDestination == AppSettings.SAVE_DESTINATION_SHARE) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(text = stringResource(R.string.save_destination_share_label))
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.ocr_tuning_section_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.ocr_tuning_section_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.default_profile_label),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onDefaultPreprocessingProfileChanged(AppSettings.PROFILE_BALANCED) },
                        modifier = Modifier.weight(1f),
                        colors = if (defaultPreprocessingProfile == AppSettings.PROFILE_BALANCED) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(text = stringResource(R.string.profile_balanced_label))
                    }
                    OutlinedButton(
                        onClick = { onDefaultPreprocessingProfileChanged(AppSettings.PROFILE_CLEAN) },
                        modifier = Modifier.weight(1f),
                        colors = if (defaultPreprocessingProfile == AppSettings.PROFILE_CLEAN) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(text = stringResource(R.string.profile_clean_label))
                    }
                    OutlinedButton(
                        onClick = { onDefaultPreprocessingProfileChanged(AppSettings.PROFILE_FAST) },
                        modifier = Modifier.weight(1f),
                        colors = if (defaultPreprocessingProfile == AppSettings.PROFILE_FAST) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(text = stringResource(R.string.profile_fast_label))
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.subscription_section_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.subscription_section_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isPremium) {
                    Text(
                        text = stringResource(R.string.premium_status_premium),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    if (subscriptionAvailable && availableProducts.isNotEmpty()) {
                        availableProducts.forEach { product ->
                            SubscriptionProductRow(
                                product = product,
                                onSubscribe = { onSubscribeProduct(product) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else if (isLoadingProducts) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        Text(
                            text = stringResource(R.string.subscription_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = onRestorePurchases,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.restore_purchases))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.close_label))
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
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SubscriptionProductRow(
    product: ProductDetails,
    onSubscribe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = product.title, style = MaterialTheme.typography.bodyMedium)
            Text(text = product.description, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = onSubscribe,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.subscribe_premium))
            }
        }
    }
}
