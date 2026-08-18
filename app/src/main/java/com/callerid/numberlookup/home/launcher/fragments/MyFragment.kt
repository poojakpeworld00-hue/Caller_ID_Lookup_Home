package com.callerid.numberlookup.home.launcher.fragments

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.viewbinding.ViewBinding
import com.callerid.numberlookup.home.launcher.activities.HomeDeckActivity

abstract class MyFragment<BINDING : ViewBinding>(
    context: Context,
    attributeSet: AttributeSet
) : RelativeLayout(context, attributeSet) {
    protected var activity: HomeDeckActivity? = null
    protected lateinit var binding: BINDING

    abstract fun setupFragment(activity: HomeDeckActivity)
}
