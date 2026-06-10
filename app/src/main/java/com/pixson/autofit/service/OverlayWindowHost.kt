package com.pixson.autofit.service

interface OverlayWindowHost {
    fun canShow(): Boolean
    fun showOrUpdate(text: CharSequence)
    fun dismiss()
}
