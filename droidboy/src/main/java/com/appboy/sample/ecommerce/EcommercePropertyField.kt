package com.appboy.sample.ecommerce

import android.text.InputType

/**
 * Describes a single editable field shown in the eCommerce tester form.
 */
data class EcommercePropertyField(
    val key: String,
    val label: String,
    val defaultValue: String,
    val inputType: Int = InputType.TYPE_CLASS_TEXT,
    val dropdownOptions: List<String>? = null,
)
