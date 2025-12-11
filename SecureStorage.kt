/*
 * Copyright (c) 2025 BABA Wallet / [Your Legal Name or Company Name]
 * All Rights Reserved.
 *
 * This code is part of the BABA Wallet project.
 *
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 * The content is proprietary and confidential.
 *
 * Written by [Your Name], December 2025.
 */

package com.molaanaa.babawallet

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SecureStorage {
    private const val FILE_NAME = "credits_wallet_prefs"
    private const val KEY_ACCOUNTS = "accounts"
    private const val KEY_ACTIVE_ACCOUNT = "active_account_public_key"
    private const val KEY_BIOMETRIC_AUTH_ENABLED = "biometric_auth_enabled"
    private const val KEY_DATA_MIGRATED = "data_migrated_to_byte_array"
    private const val KEY_APP_PIN = "app_pin" // New constant for PIN

    // DTO for safe serialization
    private data class AccountDto(
        val privateKey: String, // Base64 encoded for new, Base58 for old
        val publicKey: String,
        val name: String?,
        val order: Int
    )

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        val masterKeyBuilder = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            masterKeyBuilder.setRequestStrongBoxBacked(true)
        }

        val masterKey = try {
            masterKeyBuilder.build()
        } catch (e: Exception) {
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        }

        prefs = EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun setBiometricAuthEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BIOMETRIC_AUTH_ENABLED, enabled) }
    }

    fun isBiometricAuthEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_AUTH_ENABLED, false)
    }

    // --- New PIN Logic ---
    fun saveAppPin(pin: String) {
        prefs.edit { putString(KEY_APP_PIN, pin) }
    }

    fun getAppPin(): String? {
        return prefs.getString(KEY_APP_PIN, null)
    }

    fun hasAppPin(): Boolean {
        return prefs.contains(KEY_APP_PIN)
    }
    // ---------------------

    fun saveAccount(account: Account) {
        val allAccounts = getAccounts().toMutableList()
        val newAccount = account.copy(order = (allAccounts.maxOfOrNull { it.order } ?: -1) + 1)
        allAccounts.add(newAccount)
        saveAccounts(allAccounts)
        setActiveAccount(newAccount)
    }

    fun saveAccounts(accounts: List<Account>) {
        val dtoList = accounts.map {
            AccountDto(
                privateKey = Base64.encodeToString(it.privateKey, Base64.NO_WRAP),
                publicKey = it.publicKey,
                name = it.name,
                order = it.order
            )
        }
        prefs.edit {
            putString(KEY_ACCOUNTS, gson.toJson(dtoList))
            putBoolean(KEY_DATA_MIGRATED, true) // Mark data as migrated
        }
    }

    fun updateAccount(account: Account) {
        val allAccounts = getAccounts().toMutableList()
        val index = allAccounts.indexOfFirst { it.publicKey == account.publicKey }
        if (index != -1) {
            val originalAccount = allAccounts[index]
            allAccounts[index] = account.copy(order = originalAccount.order)
            saveAccounts(allAccounts)
        }
    }

    fun getActiveAccount(): Account? {
        val publicKey = prefs.getString(KEY_ACTIVE_ACCOUNT, null) ?: return null
        return getAccounts().find { it.publicKey == publicKey }
    }

    fun getAccounts(): List<Account> {
        val json = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        val dataMigrated = prefs.getBoolean(KEY_DATA_MIGRATED, false)

        val type = object : TypeToken<List<AccountDto>>() {}.type
        val dtoList = gson.fromJson<List<AccountDto>>(json, type) ?: emptyList()

        val accounts = dtoList.map { dto ->
            val privateKeyBytes = if (dataMigrated) {
                // New format: Decode from Base64
                Base64.decode(dto.privateKey, Base64.NO_WRAP)
            } else {
                // Old format: Decode from Base58
                Base58.decode(dto.privateKey) ?: ByteArray(0)
            }
            Account(
                privateKey = privateKeyBytes,
                publicKey = dto.publicKey,
                name = dto.name,
                order = dto.order
            )
        }

        // If data was not migrated and we successfully read accounts, save them in the new format.
        if (!dataMigrated && accounts.isNotEmpty()) {
            saveAccounts(accounts)
        }

        return accounts.sortedBy { it.order }
    }


    fun setActiveAccount(account: Account) {
        prefs.edit { putString(KEY_ACTIVE_ACCOUNT, account.publicKey) }
    }

    fun removeAccount(account: Account) {
        val allAccounts = getAccounts().toMutableList()
        if (allAccounts.size > 1) {
            val wasActive = getActiveAccount()?.publicKey == account.publicKey
            val removedIndex = allAccounts.indexOfFirst { it.publicKey == account.publicKey }
            allAccounts.removeAt(removedIndex)
            val reorderedAccounts = allAccounts.mapIndexed { index, acc -> acc.copy(order = index) }
            saveAccounts(reorderedAccounts)

            if (wasActive) {
                val newActiveIndex = if (removedIndex >= reorderedAccounts.size) reorderedAccounts.size - 1 else removedIndex
                setActiveAccount(reorderedAccounts[newActiveIndex])
            }
        } else {
            clear()
        }
    }

    fun clear() = prefs.edit { clear() }
}
