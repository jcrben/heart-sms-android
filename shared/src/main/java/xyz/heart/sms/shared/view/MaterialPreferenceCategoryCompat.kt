package xyz.heart.sms.shared.view

import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceCategory

import xyz.heart.sms.shared.R

class MaterialPreferenceCategoryCompat : PreferenceCategory {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    init {
        layoutResource = R.layout.preference_category_card
    }
}
