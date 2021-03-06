/*
 * OAndBackupX: open-source apps backup and restore app.
 * Copyright (C) 2020  Antonios Hazim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.machiav3lli.backup.handler

import android.util.Log
import com.machiav3lli.backup.Constants.classTag
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Crypto. The class to handle encryption and decryption of streams.
 * Call `encryptStream` or `decryptStream` with a password and a salt or a better a secret key
 * (for performance reasons) and the class will wrap the given stream in return.
 *
 *
 * Android Keystore API is not used on purpose, because the key material needs to be portable for
 * uses cases when the device has been wiped or when backups are restored on another device.
 *
 *
 * The IV is static as it may be public.
 */
object Crypto {

    /**
     * Default salt, if no user specified salt is available to improve security.
     * Better a constant salt for the app that using no salt.
     */
    val FALLBACK_SALT = "oandbackupx".toByteArray(StandardCharsets.UTF_8)
    private val TAG = classTag(".Crypto")
    private const val ENCRYPTION_SETUP_FAILED = "Could not setup encryption"

    /**
     * TODO migrate to one of the newer algorithms
     * Taken from here. Chosen because of API Level 24+ compatibility. Newer algorithms are available
     * with API Level 26+.
     * https://developer.android.com/guide/topics/security/cryptography#SupportedSecretKeyFactory
     *
     *
     * The actual choice was inspired by this blog post:
     * https://www.raywenderlich.com/778533-encryption-tutorial-for-android-getting-started
     */
    private const val DEFAULT_SECRET_KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA1"
    const val CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val DEFAULT_IV_BLOCK_SIZE = 16 // 128 bit
    private const val ITERATION_COUNT = 1000
    private const val KEY_LENGTH = 128

    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun generateKeyFromPassword(password: String, salt: ByteArray?): SecretKey = generateKeyFromPassword(password, salt, DEFAULT_SECRET_KEY_FACTORY_ALGORITHM, CIPHER_ALGORITHM)

    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun generateKeyFromPassword(password: String, salt: ByteArray?, keyFactoryAlgorithm: String?, cipherAlgorithm: String): SecretKey {
        val factory = SecretKeyFactory.getInstance(keyFactoryAlgorithm)
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, cipherAlgorithm.split(File.separator).toTypedArray()[0])
    }

    @Throws(CryptoSetupException::class)
    fun encryptStream(os: OutputStream?, password: String, salt: ByteArray?): CipherOutputStream {
        return try {
            val secret = generateKeyFromPassword(password, salt)
            encryptStream(os, secret)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Could not setup encryption: ${e.message}")
            throw CryptoSetupException(ENCRYPTION_SETUP_FAILED, e)
        } catch (e: InvalidKeySpecException) {
            Log.e(TAG, "Could not setup encryption: ${e.message}")
            throw CryptoSetupException(ENCRYPTION_SETUP_FAILED, e)
        }
    }

    @Throws(CryptoSetupException::class)
    fun encryptStream(os: OutputStream?, secret: SecretKey?): CipherOutputStream = encryptStream(os, secret, CIPHER_ALGORITHM)

    @Throws(CryptoSetupException::class)
    fun encryptStream(os: OutputStream?, secret: SecretKey?, cipherAlgorithm: String): CipherOutputStream {
        return try {
            val cipher = Cipher.getInstance(cipherAlgorithm)
            val iv = IvParameterSpec(initIv(cipherAlgorithm))
            cipher.init(Cipher.ENCRYPT_MODE, secret, iv)
            CipherOutputStream(os, cipher)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Could not setup encryption: ${e.message}")
            throw CryptoSetupException(ENCRYPTION_SETUP_FAILED, e)
        } catch (e: InvalidKeyException) {
            Log.e(TAG, "Could not setup encryption: ${e.message}")
            throw CryptoSetupException(ENCRYPTION_SETUP_FAILED, e)
        } catch (e: InvalidAlgorithmParameterException) {
            Log.e(TAG, "Could not setup encryption: ${e.message}")
            throw CryptoSetupException(ENCRYPTION_SETUP_FAILED, e)
        } catch (e: NoSuchPaddingException) {
            Log.e(TAG, "Could not setup encryption: ${e.message}")
            throw CryptoSetupException(ENCRYPTION_SETUP_FAILED, e)
        }
    }

    @Throws(CryptoSetupException::class)
    fun decryptStream(stream: InputStream?, password: String, salt: ByteArray?): CipherInputStream {
        return try {
            val secret = generateKeyFromPassword(password, salt)
            decryptStream(stream, secret)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Could not setup encryption: ${e.message}")
            throw CryptoSetupException(ENCRYPTION_SETUP_FAILED, e)
        } catch (e: InvalidKeySpecException) {
            Log.e(TAG, "Could not setup encryption: ${e.message}")
            throw CryptoSetupException(ENCRYPTION_SETUP_FAILED, e)
        }
    }

    @Throws(CryptoSetupException::class)
    fun decryptStream(stream: InputStream?, secret: SecretKey?): CipherInputStream = decryptStream(stream, secret, CIPHER_ALGORITHM)

    @Throws(CryptoSetupException::class)
    fun decryptStream(stream: InputStream?, secret: SecretKey?, cipherAlgorithm: String): CipherInputStream {
        return try {
            val cipher = Cipher.getInstance(cipherAlgorithm)
            val iv = IvParameterSpec(initIv(cipherAlgorithm))
            cipher.init(Cipher.DECRYPT_MODE, secret, iv)
            CipherInputStream(stream, cipher)
        } catch (e: NoSuchPaddingException) {
            Log.e(TAG, "Could not setup encryption: ${e.message}")
            throw CryptoSetupException(ENCRYPTION_SETUP_FAILED, e)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Could not setup encryption: ${e.message}")
            throw CryptoSetupException(ENCRYPTION_SETUP_FAILED, e)
        } catch (e: InvalidAlgorithmParameterException) {
            Log.e(TAG, "Could not setup encryption: ${e.message}")
            throw CryptoSetupException(ENCRYPTION_SETUP_FAILED, e)
        } catch (e: InvalidKeyException) {
            Log.e(TAG, "Could not setup encryption: ${e.message}")
            throw CryptoSetupException(ENCRYPTION_SETUP_FAILED, e)
        }
    }

    private fun initIv(cipherAlgorithm: String): ByteArray {
        val blockSize: Int
        blockSize = try {
            val cipher = Cipher.getInstance(cipherAlgorithm)
            cipher.blockSize
        } catch (e: NoSuchAlgorithmException) {
            // Fallback if the cipher has issues. Might lead to another exception later, but saves
            // the situation here. The use cipher might not match or will cause other exceptions
            // when used like this.
            DEFAULT_IV_BLOCK_SIZE
        } catch (e: NoSuchPaddingException) {
            DEFAULT_IV_BLOCK_SIZE
        }
        // IV is nothing secret. Could also be constant, but why not spend a few cpu cycles to have
        // it dynamic, if the algorithm changes?
        val iv = ByteArray(blockSize)
        for (i in 0 until blockSize) {
            iv[i] = 0
        }
        return iv
    }

    class CryptoSetupException(message: String?, cause: Throwable?) : Exception(message, cause)
}