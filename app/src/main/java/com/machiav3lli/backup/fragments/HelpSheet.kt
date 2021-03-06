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

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.text.HtmlCompat
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.machiav3lli.backup.Constants
import com.machiav3lli.backup.Constants.classTag
import com.machiav3lli.backup.databinding.SheetHelpBinding
import java.io.IOException
import java.io.InputStream
import java.util.*

class HelpSheet : BottomSheetDialogFragment() {
    private lateinit var binding: SheetHelpBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val sheet = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        sheet.setOnShowListener { d: DialogInterface ->
            val bottomSheetDialog = d as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(R.id.design_bottom_sheet)
            if (bottomSheet != null) BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
        }
        return sheet
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = SheetHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupOnClicks()
        setupViews()
    }

    private fun setupOnClicks() {
        binding.dismiss.setOnClickListener { dismissAllowingStateLoss() }
        binding.changelog.setOnClickListener { requireContext().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.HELP_CHANGELOG))) }
        binding.telegram.setOnClickListener { requireContext().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.HELP_TELEGRAM))) }
        binding.element.setOnClickListener { requireContext().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.HELP_ELEMENT))) }
        binding.license.setOnClickListener { requireContext().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.HELP_LICENSE))) }
    }

    private fun setupViews() {
        try {
            binding.helpVersionName.text = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0).versionName
            val stream = resources.openRawResource(com.machiav3lli.backup.R.raw.help)
            val htmlString = convertStreamToString(stream)
            stream.close()
            binding.helpHtml.text = HtmlCompat.fromHtml(htmlString, HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.helpHtml.movementMethod = LinkMovementMethod.getInstance()
        } catch (e: IOException) {
            binding.helpHtml.text = e.toString()
        } catch (ignored: PackageManager.NameNotFoundException) {
        }
    }

    companion object {
        private val TAG = classTag(".HelpSheet")
        fun convertStreamToString(stream: InputStream?): String {
            val s = Scanner(stream, "utf-8").useDelimiter("\\A")
            return if (s.hasNext()) s.next() else ""
        }
    }
}