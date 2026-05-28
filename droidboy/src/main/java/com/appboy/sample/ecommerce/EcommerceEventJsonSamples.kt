package com.appboy.sample.ecommerce

/**
 * Sample event names and property JSON payloads for the Droidboy eCommerce tester.
 */
object EcommerceEventJsonSamples {
    private val samplesByEventType =
        mapOf(
            EcommerceEventType.CHECKOUT_STARTED to
                Sample(
                    eventName = "ecommerce.checkout_started",
                    propertiesJson =
                        """
                {
                  "checkout_id": "checkout-123",
                  "cart_id": "cart-456",
                  "currency": "USD",
                  "source": "droidboy",
                  "total_value": 49.99,
                  "products": [
                    {
                      "product_id": "SKU-001",
                      "product_name": "Test Widget",
                      "variant_id": "widget_blue_lg",
                      "price": 49.99,
                      "quantity": 1
                    }
                  ]
                }
                        """.trimIndent(),
                ),
            EcommerceEventType.CART_UPDATED to
                Sample(
                    eventName = "ecommerce.cart_updated",
                    propertiesJson =
                        """
                {
                  "cart_id": "cart-456",
                  "action": "replace",
                  "currency": "USD",
                  "source": "droidboy",
                  "total_value": 49.99,
                  "products": [
                    {
                      "product_id": "SKU-001",
                      "product_name": "Test Widget",
                      "variant_id": "widget_blue_lg",
                      "price": 49.99,
                      "quantity": 1
                    }
                  ]
                }
                        """.trimIndent(),
                ),
            EcommerceEventType.ORDER_PLACED to
                Sample(
                    eventName = "ecommerce.order_placed",
                    propertiesJson =
                        """
                {
                  "order_id": "order-789",
                  "cart_id": "cart-456",
                  "total_discounts": 5.0,
                  "currency": "USD",
                  "source": "droidboy",
                  "total_value": 49.99,
                  "products": [
                    {
                      "product_id": "SKU-001",
                      "product_name": "Test Widget",
                      "variant_id": "widget_blue_lg",
                      "price": 49.99,
                      "quantity": 1
                    }
                  ]
                }
                        """.trimIndent(),
                ),
            EcommerceEventType.PRODUCT_VIEWED to
                Sample(
                    eventName = "ecommerce.product_viewed",
                    propertiesJson =
                        """
                {
                  "product_id": "SKU-001",
                  "product_name": "Test Widget",
                  "variant_id": "widget_blue_lg",
                  "price": 49.99,
                  "currency": "USD",
                  "source": "droidboy",
                  "image_url": "https://example.com/widget.jpg",
                  "product_url": "https://example.com/products/widget"
                }
                        """.trimIndent(),
                ),
        )

    val sampleEventTypes: List<EcommerceEventType> = samplesByEventType.keys.toList()

    fun sampleFor(eventType: EcommerceEventType): Sample? = samplesByEventType[eventType]

    data class Sample(
        val eventName: String,
        val propertiesJson: String,
    )
}
