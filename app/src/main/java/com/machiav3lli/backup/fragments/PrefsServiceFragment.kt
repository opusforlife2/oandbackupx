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
package com.machiav3lli.backup.fragments

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.machiav3lli.backup.Constants
import com.machiav3lli.backup.Constants.classTag
import com.machiav3lli.backup.R

class PrefsServiceFragment : PreferenceFragmentCompat() {
    var encryptPref: CheckBoxPreference? = null
    var passwordPref: EditTextPreference? = null
    var passwordConfirmationPref: EditTextPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_service, rootKey)
        encryptPref = findPreference(Constants.PREFS_ENCRYPTION)
        passwordPref = findPreference(Constants.PREFS_PASSWORD)
        passwordConfirmationPref = findPreference(Constants.PREFS_PASSWORD_CONFIRMATION)
        passwordPref!!.isVisible = encryptPref!!.isChecked
        passwordConfirmationPref!!.isVisible = encryptPref!!.isChecked
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        passwordConfirmationPref!!.summary = if (passwordPref!!.text == passwordConfirmationPref!!.text) getString(R.string.prefs_password_match_true) else getString(R.string.prefs_password_match_false)
        passwordPref!!.setOnBindEditTextListener { editText: EditText -> editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        passwordConfirmationPref!!.setOnBindEditTextListener { editText: EditText -> editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        encryptPref!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, _: Any? -> onPrefChangeEncryption(encryptPref, passwordPref, passwordConfirmationPref) }
        passwordPref!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any -> onPrefChangePassword(passwordConfirmationPref, newValue as String, passwordConfirmationPref!!.text) }
        passwordConfirmationPref!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any -> onPrefChangePassword(passwordConfirmationPref, passwordPref!!.text, newValue as String) }
    }

    private fun onPrefChangeEncryption(encryption: CheckBoxPreference?, password: EditTextPreference?, passwordConfirmation: EditTextPreference?): Boolean {
        if (encryption!!.isChecked) {
            password!!.text = ""
            passwordConfirmation!!.text = ""
        }
        password!!.isVisible = !encryption.isChecked
        passwordConfirmation!!.isVisible = !encryption.isChecked
        return true
    }

    private fun onPrefChangePassword(passwordConfirmation: EditTextPreference?, password: String, passwordCheck: String): Boolean {
        passwordConfirmation!!.summary = if (password == passwordCheck) getString(R.string.prefs_password_match_true) else getString(R.string.prefs_password_match_false)
        return true
    }

    companion object {
        private val TAG = classTag(".PrefsServiceFragment")
    }
}