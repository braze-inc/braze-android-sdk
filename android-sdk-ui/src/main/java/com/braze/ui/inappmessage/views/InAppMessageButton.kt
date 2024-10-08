package com.braze.ui.inappmessage.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.Button

@SuppressLint("AppCompatCustomView")
open class InAppMessageButton : Button {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )
}
