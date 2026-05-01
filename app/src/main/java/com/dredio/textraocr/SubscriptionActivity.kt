package com.dredio.textraocr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.ProductDetails
import com.dredio.textraocr.ui.theme.TextraOcrTheme

class SubscriptionActivity : ComponentActivity() {
    private var premiumState by mutableStateOf(PremiumUiState())
    private lateinit var billingManager: PremiumBillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TextraOcrTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SubscriptionPlansScreen(
                        availableProducts = premiumState.availableProducts,
                        subscriptionAvailable = premiumState.subscriptionAvailable,
                        onSubscribePlan = { plan ->
                            val productDetails = premiumState.availableProducts.firstOrNull { it.productId == plan.productId }
                            if (productDetails == null) {
                                Toast.makeText(
                                    this@SubscriptionActivity,
                                    getString(R.string.subscription_unavailable),
                                    Toast.LENGTH_LONG
                                ).show()
                            } else if (!billingManager.launchSubscription(this@SubscriptionActivity, productDetails)) {
                                Toast.makeText(
                                    this@SubscriptionActivity,
                                    getString(R.string.subscription_unavailable),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onRestorePurchases = {
                            billingManager.refreshPurchases()
                            Toast.makeText(
                                this@SubscriptionActivity,
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
            premiumState = state
        }
        billingManager.start()
    }

    override fun onResume() {
        super.onResume()
        billingManager.refreshPurchases()
    }

    override fun onDestroy() {
        billingManager.stop()
        super.onDestroy()
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, SubscriptionActivity::class.java)
    }
}

private data class SubscriptionPlanSpec(
    val productId: String,
    val titleRes: Int,
    val badgeRes: Int,
    val descriptionRes: Int,
    val fallbackPriceRes: Int,
    val featured: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionPlansScreen(
    availableProducts: List<ProductDetails>,
    subscriptionAvailable: Boolean,
    onSubscribePlan: (SubscriptionPlanSpec) -> Unit,
    onRestorePurchases: () -> Unit,
    onClose: () -> Unit,
) {
    val plans = listOf(
        SubscriptionPlanSpec(
            productId = PremiumBillingManager.PREMIUM_WEEKLY_PRODUCT_ID,
            titleRes = R.string.subscription_plan_weekly_title,
            badgeRes = R.string.subscription_plan_weekly_badge,
            descriptionRes = R.string.subscription_plan_weekly_description,
            fallbackPriceRes = R.string.subscription_weekly_label,
            featured = false
        ),
        SubscriptionPlanSpec(
            productId = PremiumBillingManager.PREMIUM_MONTHLY_PRODUCT_ID,
            titleRes = R.string.subscription_plan_monthly_title,
            badgeRes = R.string.subscription_plan_monthly_badge,
            descriptionRes = R.string.subscription_plan_monthly_description,
            fallbackPriceRes = R.string.subscription_monthly_label,
            featured = true
        ),
        SubscriptionPlanSpec(
            productId = PremiumBillingManager.PREMIUM_YEARLY_PRODUCT_ID,
            titleRes = R.string.subscription_plan_yearly_title,
            badgeRes = R.string.subscription_plan_yearly_badge,
            descriptionRes = R.string.subscription_plan_yearly_description,
            fallbackPriceRes = R.string.subscription_yearly_label,
            featured = false
        )
    )

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.subscription_page_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onClose) {
                        Text(text = stringResource(R.string.close_label))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 18.dp)
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 120.dp, start = 20.dp)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeroSubscriptionCard(
                    subscriptionAvailable = subscriptionAvailable,
                    onRestorePurchases = onRestorePurchases
                )

                if (!subscriptionAvailable) {
                    Text(
                        text = stringResource(R.string.subscription_page_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                plans.forEach { plan ->
                    val product = availableProducts.firstOrNull { it.productId == plan.productId }
                    PlanCard(
                        plan = plan,
                        product = product,
                        onSubscribe = { onSubscribePlan(plan) }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.subscription_page_footer),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.subscription_plan_feature_restore),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onRestorePurchases, modifier = Modifier.fillMaxWidth()) {
                            Text(text = stringResource(R.string.subscription_page_restore))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HeroSubscriptionCard(
    subscriptionAvailable: Boolean,
    onRestorePurchases: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(R.string.subscription_page_hero_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            Text(
                text = stringResource(R.string.subscription_page_hero_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeatureChip(text = stringResource(R.string.subscription_plan_feature_no_ads))
                FeatureChip(text = stringResource(R.string.subscription_plan_feature_unlimited_scans))
            }
            Text(
                text = if (subscriptionAvailable) stringResource(R.string.subscription_purchase_hint) else stringResource(R.string.subscription_page_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onRestorePurchases, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.subscription_page_restore))
            }
        }
    }
}

@Composable
private fun FeatureChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlanSpec,
    product: ProductDetails?,
    onSubscribe: () -> Unit,
) {
    val planTitle = stringResource(plan.titleRes)
    val priceText = product
        ?.subscriptionOfferDetails
        ?.firstOrNull()
        ?.pricingPhases
        ?.pricingPhaseList
        ?.firstOrNull()
        ?.formattedPrice
        .orEmpty()
        .ifBlank { stringResource(plan.fallbackPriceRes) }
    val descriptionText = product?.description?.ifBlank { stringResource(plan.descriptionRes) }
        ?: stringResource(plan.descriptionRes)
    val isFeatured = plan.featured

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFeatured) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            }
        ),
        border = BorderStroke(
            width = if (isFeatured) 1.5.dp else 1.dp,
            color = if (isFeatured) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFeatured) 8.dp else 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = planTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = priceText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = stringResource(plan.badgeRes),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = stringResource(R.string.subscription_plan_feature_no_ads), style = MaterialTheme.typography.bodyMedium)
                Text(text = stringResource(R.string.subscription_plan_feature_unlimited_scans), style = MaterialTheme.typography.bodyMedium)
                Text(text = stringResource(R.string.subscription_plan_feature_restore), style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = onSubscribe,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = stringResource(R.string.subscription_plan_cta_template, planTitle))
            }
        }
    }
}