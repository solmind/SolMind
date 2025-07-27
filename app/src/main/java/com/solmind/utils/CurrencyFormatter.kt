package com.solmind.utils

import com.solmind.data.manager.CurrencyDisplayMode
import com.solmind.data.model.AccountMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val solFormatter = DecimalFormat("#,##0.####").apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 4
    }
    
    private val usdFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    
    /**
     * Formats an amount based on the currency display mode and account mode
     * Offchain mode always uses USD regardless of user preference
     * @param amount The amount to format
     * @param mode The currency display mode (USD or SOL)
     * @param accountMode The account mode (ONCHAIN or OFFCHAIN)
     * @return Formatted string like "$1.23" or "1.2345 SOL"
     */
    fun formatAmount(amount: Double, mode: CurrencyDisplayMode, accountMode: AccountMode): String {
        return when {
            accountMode == AccountMode.OFFCHAIN -> usdFormatter.format(amount)
            mode == CurrencyDisplayMode.USD -> usdFormatter.format(amount)
            mode == CurrencyDisplayMode.SOL -> "${solFormatter.format(amount)} SOL"
            else -> usdFormatter.format(amount)
        }
    }
    
    /**
     * Formats an amount with a sign prefix for transactions
     * Offchain mode always uses USD regardless of user preference
     * @param amount The amount to format
     * @param isIncome Whether this is income (positive) or expense (negative)
     * @param mode The currency display mode (USD or SOL)
     * @param accountMode The account mode (ONCHAIN or OFFCHAIN)
     * @return Formatted string like "+$1.23" or "+1.2345 SOL"
     */
    fun formatTransactionAmount(amount: Double, isIncome: Boolean, mode: CurrencyDisplayMode, accountMode: AccountMode): String {
        val sign = if (isIncome) "+" else "-"
        return when {
            accountMode == AccountMode.OFFCHAIN -> "$sign${usdFormatter.format(amount).removePrefix("$")}"
            mode == CurrencyDisplayMode.USD -> "$sign${usdFormatter.format(amount).removePrefix("$")}"
            mode == CurrencyDisplayMode.SOL -> "$sign${solFormatter.format(amount)} SOL"
            else -> "$sign${usdFormatter.format(amount).removePrefix("$")}"
        }
    }
    
    /**
     * Formats an amount for input fields (without currency suffix)
     * @param amount The amount to format
     * @return Formatted string like "1.2345"
     */
    fun formatForInput(amount: Double): String {
        return solFormatter.format(amount)
    }
    
    // Legacy methods for backward compatibility
    @Deprecated("Use formatAmount with CurrencyDisplayMode and AccountMode instead")
    fun formatAmount(amount: Double, mode: CurrencyDisplayMode): String {
        return formatAmount(amount, mode, AccountMode.ONCHAIN)
    }
    
    @Deprecated("Use formatTransactionAmount with CurrencyDisplayMode and AccountMode instead")
    fun formatTransactionAmount(amount: Double, isIncome: Boolean, mode: CurrencyDisplayMode): String {
        return formatTransactionAmount(amount, isIncome, mode, AccountMode.ONCHAIN)
    }
    
    @Deprecated("Use formatAmount with CurrencyDisplayMode and AccountMode instead")
    fun formatAsSol(amount: Double): String {
        return formatAmount(amount, CurrencyDisplayMode.SOL, AccountMode.ONCHAIN)
    }
    
    @Deprecated("Use formatTransactionAmount with CurrencyDisplayMode and AccountMode instead")
    fun formatTransactionAmount(amount: Double, isIncome: Boolean): String {
        return formatTransactionAmount(amount, isIncome, CurrencyDisplayMode.SOL, AccountMode.ONCHAIN)
    }
}