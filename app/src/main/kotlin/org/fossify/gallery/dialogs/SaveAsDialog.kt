package org.fossify.gallery.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.isRPlus
import org.fossify.gallery.databinding.DialogSaveAsBinding
import java.io.File

class SaveAsDialog(
    val activity: BaseSimpleActivity, val path: String, val appendFilename: Boolean, val cancelCallback: (() -> Unit)? = null,
    val callback: (savePath: String) -> Unit
) {
    init {
        var realPath = path.getParentPath()
        if (activity.isRestrictedWithSAFSdk30(realPath) && !activity.isInDownloadDir(realPath)) {
            realPath = activity.getPicturesDirectoryPath(realPath)
        }

        val binding = DialogSaveAsBinding.inflate(activity.layoutInflater).apply {
            val folderPath = "${activity.humanizePath(realPath).trimEnd('/')}/"
            val fullName = path.getFilenameFromPath()
            val dotAt = fullName.lastIndexOf(".")
            var name = fullName
            var extension = ""

            if (dotAt > 0) {
                name = fullName.substring(0, dotAt)
                extension = fullName.substring(dotAt + 1)
                extensionValue.setText(extension)
            }

            var preAppendName = name

            if (appendFilename) {
                name += "_1"
            }

            overwriteFile.setOnClickListener {
                var shouldOverwrite = overwriteFile.isChecked
                folderValue.isEnabled = !shouldOverwrite
                filenameValue.isEnabled = !shouldOverwrite
                extensionValue.isEnabled = !shouldOverwrite

                if (shouldOverwrite) {
                    filenameValue.setText(preAppendName)
                    extensionValue.setText(extension)
                    folderValue.setText(folderPath)
                }
            }

            filenameValue.setText(name)
            folderValue.setText(folderPath)
            folderValue.setOnClickListener {
                activity.hideKeyboard(folderValue)
                FilePickerDialog(activity, realPath, false, false, true, true) {
                    folderValue.setText(activity.humanizePath(it))
                    realPath = it
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel) { dialog, which -> cancelCallback?.invoke() }
            .setOnCancelListener { cancelCallback?.invoke() }
            .apply {
                activity.setupDialogStuff(binding.root, this, org.fossify.commons.R.string.save_as) { alertDialog ->
                    alertDialog.showKeyboard(binding.filenameValue)

                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = binding.filenameValue.value
                        val extension = binding.extensionValue.value

                        if (filename.isEmpty()) {
                            activity.toast(org.fossify.commons.R.string.filename_cannot_be_empty)
                            return@setOnClickListener
                        }

                        if (extension.isEmpty()) {
                            activity.toast(org.fossify.commons.R.string.extension_cannot_be_empty)
                            return@setOnClickListener
                        }

                        val newFilename = "$filename.$extension"
                        val newPath = "${realPath.trimEnd('/')}/$newFilename"
                        if (!newFilename.isAValidFilename()) {
                            activity.toast(org.fossify.commons.R.string.filename_invalid_characters)
                            return@setOnClickListener
                        }

                        if (activity.getDoesFilePathExist(newPath) && !binding.overwriteFile.isChecked) {
                            val title = String.format(activity.getString(org.fossify.commons.R.string.file_already_exists_overwrite), newFilename)
                            ConfirmationDialog(activity, title) {
                                if ((isRPlus() && !isExternalStorageManager())) {
                                    val fileDirItem = arrayListOf(File(newPath).toFileDirItem(activity))
                                    val fileUris = activity.getFileUrisFromFileDirItems(fileDirItem)
                                    activity.updateSDK30Uris(fileUris) { success ->
                                        if (success) {
                                            selectPath(alertDialog, newPath)
                                        }
                                    }
                                } else {
                                    selectPath(alertDialog, newPath)
                                }
                            }
                        } else {
                            selectPath(alertDialog, newPath)
                        }
                    }
                }
            }
    }

    private fun selectPath(alertDialog: AlertDialog, newPath: String) {
        activity.handleSAFDialogSdk30(newPath) {
            if (!it) {
                return@handleSAFDialogSdk30
            }
            callback(newPath)
            alertDialog.dismiss()
        }
    }
}
