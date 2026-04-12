package com.nexus.platform.core.ui

import android.content.Context
import android.view.Gravity
import android.widget.Toast

fun showCenterToast(
    context: Context,
    message: String,
    duration: Int = Toast.LENGTH_SHORT
) {
    Toast.makeText(context, message, duration).apply {
        setGravity(Gravity.CENTER, 0, 0)
    }.show()
}
