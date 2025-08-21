package com.example.onfit.HomeRegister.model

import android.net.Uri
import androidx.annotation.DrawableRes

data class DisplayImage(
    val uri: Uri? = null,
    @androidx.annotation.DrawableRes val resId: Int? = null
)
