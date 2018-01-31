package com.sierrawireless.avphone.message

import android.text.Spanned

/**
 * Generic interface for anything that can display success / error messages.
 */
interface IMessageDisplayer {

    fun showError(id: Int, vararg params: Any)

    fun showSuccess(id: Int, vararg params: Any)

    fun showErrorMessage(spanned: Spanned)
}
