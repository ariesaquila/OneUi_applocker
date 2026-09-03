package com.oneui.applocker.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Manages secure storage and verification of PIN codes and Patterns
 * using Android KeyStore and EncryptedSharedPreferences.
 */
class SecurityManager(context: Context) {

    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback if Keystore fails on some custom ROMs
        context.getSharedPreferences(FALLBACK_PREFS_FILENAME, Context.MODE_PRIVATE)
    }

    fun isConfigured(): Boolean {
        return sharedPreferences.contains(KEY_PIN_HASH) || sharedPreferences.contains(KEY_PATTERN_HASH)
    }

    fun setPin(pin: String) {
        val salt = generateSalt()
        val hash = hashWithSalt(pin, salt)
        sharedPreferences.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = sharedPreferences.getString(KEY_PIN_HASH, null) ?: return false
        val storedSalt = sharedPreferences.getString(KEY_PIN_SALT, null) ?: return false
        val computedHash = hashWithSalt(pin, storedSalt)
        return storedHash == computedHash
    }

    fun setPattern(pattern: List<Int>) {
        val patternString = pattern.joinToString(separator = "-")
        val salt = generateSalt()
        val hash = hashWithSalt(patternString, salt)
        sharedPreferences.edit()
            .putString(KEY_PATTERN_HASH, hash)
            .putString(KEY_PATTERN_SALT, salt)
            .apply()
    }

    fun verifyPattern(pattern: List<Int>): Boolean {
        val storedHash = sharedPreferences.getString(KEY_PATTERN_HASH, null) ?: return false
        val storedSalt = sharedPreferences.getString(KEY_PATTERN_SALT, null) ?: return false
        val patternString = pattern.joinToString(separator = "-")
        val computedHash = hashWithSalt(patternString, storedSalt)
        return storedHash == computedHash
    }

    fun clearSecurity() {
        sharedPreferences.edit().clear().apply()
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    private fun hashWithSalt(input: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = "$salt:$input"
        val bytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_FILENAME = "encrypted_lock_prefs"
        private const val FALLBACK_PREFS_FILENAME = "secure_lock_prefs_fallback"
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_PIN_SALT = "key_pin_salt"
        private const val KEY_PATTERN_HASH = "key_pattern_hash"
        private const val KEY_PATTERN_SALT = "key_pattern_salt"
    }
}
