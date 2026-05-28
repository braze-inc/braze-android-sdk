package com.appboy.sample.ecommerce

import android.text.InputType
import com.braze.models.recommended.ecommerce.CartUpdatedAction
import com.braze.models.recommended.ecommerce.CartUpdatedEvent
import com.braze.models.recommended.ecommerce.CheckoutStartedEvent
import com.braze.models.recommended.ecommerce.EcommerceEvent
import com.braze.models.recommended.ecommerce.EcommerceProduct
import com.braze.models.recommended.ecommerce.OrderPlacedEvent
import com.braze.models.recommended.ecommerce.ProductViewedEvent

private const val KEY_CHECKOUT_ID = "checkout_id"
private const val KEY_CART_ID = "cart_id"
private const val KEY_ORDER_ID = "order_id"
private const val KEY_ACTION = "action"
private const val KEY_CURRENCY = "currency"
private const val KEY_SOURCE = "source"
private const val KEY_TOTAL_VALUE = "total_value"
private const val KEY_TOTAL_DISCOUNTS = "total_discounts"
private const val KEY_PRODUCT_ID = "product_id"
private const val KEY_PRODUCT_NAME = "product_name"
private const val KEY_VARIANT_ID = "variant_id"
private const val KEY_PRICE = "price"
private const val KEY_QUANTITY = "quantity"
private const val KEY_IMAGE_URL = "image_url"
private const val KEY_PRODUCT_URL = "product_url"

/**
 * Supported eCommerce events in the Droidboy tester, with their editable properties.
 */
enum class EcommerceEventType(
    val displayName: String,
    val propertyFields: List<EcommercePropertyField>,
) {
    CHECKOUT_STARTED(
        displayName = "Checkout Started",
        propertyFields = checkoutStartedFields(),
    ),
    CART_UPDATED(
        displayName = "Cart Updated",
        propertyFields = cartUpdatedFields(),
    ),
    ORDER_PLACED(
        displayName = "Order Placed",
        propertyFields = orderPlacedFields(),
    ),
    PRODUCT_VIEWED(
        displayName = "Product Viewed",
        propertyFields = productViewedFields(),
    ),
    CUSTOM(
        displayName = "Custom",
        propertyFields = customFields(),
    ),
    ;

    val isCustom: Boolean
        get() = this == CUSTOM

    /** Builds the SDK [EcommerceEvent] for this type from form field values keyed by wire property name. */
    fun buildEvent(inputs: Map<String, String>): EcommerceEvent =
        when (this) {
            CUSTOM -> throw IllegalArgumentException("Custom events use logCustomEvent.")
            CHECKOUT_STARTED ->
                CheckoutStartedEvent(
                    checkoutId = required(inputs, KEY_CHECKOUT_ID),
                    currency = required(inputs, KEY_CURRENCY),
                    source = required(inputs, KEY_SOURCE),
                    totalValue = parseDouble(inputs, KEY_TOTAL_VALUE),
                    products = listOf(buildProduct(inputs)),
                    cartId = optional(inputs, KEY_CART_ID),
                )
            CART_UPDATED -> {
                val action = parseCartAction(required(inputs, KEY_ACTION))
                val totalValue =
                    when (action) {
                        CartUpdatedAction.ADD, CartUpdatedAction.REMOVE ->
                            optionalDouble(inputs, KEY_TOTAL_VALUE)
                        CartUpdatedAction.REPLACE ->
                            parseDouble(inputs, KEY_TOTAL_VALUE)
                    }
                CartUpdatedEvent(
                    cartId = required(inputs, KEY_CART_ID),
                    currency = required(inputs, KEY_CURRENCY),
                    source = required(inputs, KEY_SOURCE),
                    totalValue = totalValue,
                    products = listOf(buildProduct(inputs)),
                    action = action,
                )
            }
            ORDER_PLACED ->
                OrderPlacedEvent(
                    orderId = required(inputs, KEY_ORDER_ID),
                    currency = required(inputs, KEY_CURRENCY),
                    source = required(inputs, KEY_SOURCE),
                    totalValue = parseDouble(inputs, KEY_TOTAL_VALUE),
                    products = listOf(buildProduct(inputs)),
                    cartId = optional(inputs, KEY_CART_ID),
                    totalDiscounts = optionalDouble(inputs, KEY_TOTAL_DISCOUNTS),
                )
            PRODUCT_VIEWED ->
                ProductViewedEvent(
                    productId = required(inputs, KEY_PRODUCT_ID),
                    productName = required(inputs, KEY_PRODUCT_NAME),
                    variantId = required(inputs, KEY_VARIANT_ID),
                    price = parseDouble(inputs, KEY_PRICE),
                    currency = required(inputs, KEY_CURRENCY),
                    source = required(inputs, KEY_SOURCE),
                    imageUrl = optional(inputs, KEY_IMAGE_URL),
                    productUrl = optional(inputs, KEY_PRODUCT_URL),
                )
        }

    companion object {
        const val KEY_EVENT_NAME = "event_name"
        const val KEY_PROPERTIES_JSON = "properties_json"
    }
}

/** Form fields for custom events (event name + JSON properties). */
private fun customFields(): List<EcommercePropertyField> =
    listOf(
        EcommercePropertyField(
            EcommerceEventType.KEY_EVENT_NAME,
            "Event name",
            "my_custom_event",
        ),
        EcommercePropertyField(
            EcommerceEventType.KEY_PROPERTIES_JSON,
            "Properties",
            "{}",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
        ),
    )

/** Shared currency, source, and total value fields used by cart-based events. */
private fun commonEventFields(): List<EcommercePropertyField> =
    listOf(
        EcommercePropertyField(KEY_CURRENCY, "Currency", "USD"),
        EcommercePropertyField(KEY_SOURCE, "Source", "droidboy"),
        EcommercePropertyField(
            KEY_TOTAL_VALUE,
            "Total Value",
            "49.99",
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
        ),
    )

/** Product line-item fields included in events that carry a [EcommerceProduct] list. */
private fun productFields(): List<EcommercePropertyField> =
    listOf(
        EcommercePropertyField(KEY_PRODUCT_ID, "Product ID", "SKU-001"),
        EcommercePropertyField(KEY_PRODUCT_NAME, "Product Name", "Test Widget"),
        EcommercePropertyField(KEY_VARIANT_ID, "Variant ID", "widget_blue_lg"),
        EcommercePropertyField(
            KEY_PRICE,
            "Price",
            "49.99",
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
        ),
        EcommercePropertyField(
            KEY_QUANTITY,
            "Quantity",
            "1",
            InputType.TYPE_CLASS_NUMBER,
        ),
    )

/** Form fields for [CheckoutStartedEvent]. */
private fun checkoutStartedFields(): List<EcommercePropertyField> =
    listOf(
        EcommercePropertyField(KEY_CHECKOUT_ID, "Checkout ID", "checkout-123"),
        EcommercePropertyField(KEY_CART_ID, "Cart ID", "cart-456"),
    ) + commonEventFields() + productFields()

/** Form fields for [CartUpdatedEvent], including cart action dropdown options. */
private fun cartUpdatedFields(): List<EcommercePropertyField> =
    listOf(
        EcommercePropertyField(KEY_CART_ID, "Cart ID", "cart-456"),
        EcommercePropertyField(
            KEY_ACTION,
            "Action",
            CartUpdatedAction.REPLACE.wireValue,
            dropdownOptions = CartUpdatedAction.entries.map { it.wireValue },
        ),
    ) + commonEventFields() + productFields()

/** Form fields for [OrderPlacedEvent]. */
private fun orderPlacedFields(): List<EcommercePropertyField> =
    listOf(
        EcommercePropertyField(KEY_ORDER_ID, "Order ID", "order-789"),
        EcommercePropertyField(KEY_CART_ID, "Cart ID", "cart-456"),
        EcommercePropertyField(
            KEY_TOTAL_DISCOUNTS,
            "Total Discounts",
            "5.0",
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
        ),
    ) + commonEventFields() + productFields()

/** Form fields for [ProductViewedEvent] flat product properties. */
private fun productViewedFields(): List<EcommercePropertyField> =
    listOf(
        EcommercePropertyField(KEY_PRODUCT_ID, "Product ID", "SKU-001"),
        EcommercePropertyField(KEY_PRODUCT_NAME, "Product Name", "Test Widget"),
        EcommercePropertyField(KEY_VARIANT_ID, "Variant ID", "widget_blue_lg"),
        EcommercePropertyField(
            KEY_PRICE,
            "Price",
            "49.99",
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
        ),
        EcommercePropertyField(KEY_CURRENCY, "Currency", "USD"),
        EcommercePropertyField(KEY_SOURCE, "Source", "droidboy"),
        EcommercePropertyField(KEY_IMAGE_URL, "Image URL", "https://example.com/widget.jpg"),
        EcommercePropertyField(
            KEY_PRODUCT_URL,
            "Product URL",
            "https://example.com/products/widget",
        ),
    )

/** Returns a non-blank trimmed value or throws if [key] is missing from [inputs]. */
private fun required(
    inputs: Map<String, String>,
    key: String,
): String {
    val value = inputs[key]?.trim().orEmpty()
    require(value.isNotEmpty()) { "$key is required." }
    return value
}

/** Returns a trimmed value, or null when [key] is absent or blank. */
private fun optional(
    inputs: Map<String, String>,
    key: String,
): String? {
    val value = inputs[key]?.trim().orEmpty()
    return value.ifEmpty { null }
}

/** Parses a required decimal field from [inputs]. */
private fun parseDouble(
    inputs: Map<String, String>,
    key: String,
): Double =
    required(inputs, key).toDoubleOrNull()
        ?: throw IllegalArgumentException("$key must be a valid number.")

/** Parses an optional decimal field from [inputs], or returns null when blank. */
private fun optionalDouble(
    inputs: Map<String, String>,
    key: String,
): Double? {
    val value = optional(inputs, key) ?: return null
    return value.toDoubleOrNull()
        ?: throw IllegalArgumentException("$key must be a valid number.")
}

/** Maps a wire-format action string to [CartUpdatedAction]. */
private fun parseCartAction(value: String): CartUpdatedAction =
    CartUpdatedAction.entries.firstOrNull {
        it.wireValue.equals(value, ignoreCase = true)
    } ?: throw IllegalArgumentException(
        "action must be one of: ${CartUpdatedAction.entries.joinToString { it.wireValue }}",
    )

/** Builds a single [EcommerceProduct] from shared product form fields in [inputs]. */
private fun buildProduct(inputs: Map<String, String>): EcommerceProduct {
    val quantity =
        required(inputs, KEY_QUANTITY).toLongOrNull()
            ?: throw IllegalArgumentException("$KEY_QUANTITY must be a valid integer.")
    return EcommerceProduct(
        productId = required(inputs, KEY_PRODUCT_ID),
        productName = required(inputs, KEY_PRODUCT_NAME),
        variantId = required(inputs, KEY_VARIANT_ID),
        price = parseDouble(inputs, KEY_PRICE),
        quantity = quantity,
    )
}
