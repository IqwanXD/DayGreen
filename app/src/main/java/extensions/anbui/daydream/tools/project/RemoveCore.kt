package extensions.anbui.daydream.tools.project

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.besome.sketch.lib.base.BasePermissionAppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import extensions.anbui.daydream.activity.project.DayDreamBackupTool
import extensions.anbui.daydream.configs.Configs
import extensions.anbui.daydream.file.FilesTools
import extensions.anbui.daydream.file.FileUtils
import extensions.anbui.daydream.project.GetProjectInfo
import extensions.anbui.daydream.ui.DialogUtils
import pro.sketchware.R
import pro.sketchware.activities.main.activities.MainActivity
import java.nio.file.Path

object RemoveCore {
    @JvmStatic
    val TAG: String = "${Configs.universalTAG}RemoveCore"

    @JvmStatic
    fun showDialogNow(activity: Activity, projectID: String?) {
        DialogUtils.threeDialog(
            activity,
            "Remove ${GetProjectInfo.getProjectName(projectID)}",
            "Do you want to remove the project? The data will not be restored. You can back it up before removing.",
            "Remove",
            "Cancel",
            "Backup",
            true,
            R.drawable.ic_mtrl_delete,
            true,
            { startNow(activity, projectID) },
            null,
            {
                val intent = Intent(activity, DayDreamBackupTool::class.java).apply {
                    putExtra("sc_id", projectID)
                }
                activity.startActivity(intent)
            },
            null
        )
    }

    @JvmStatic
    fun startNow(activity: Activity?, projectID: String?) {
        var progressDialog: AlertDialog? = null

        val progressView = LayoutInflater.from(activity).inflate(R.layout.progress_msg_box, null)
        val progressText = progressView.findViewById<TextView>(R.id.tv_progress)?.apply {
            text = if (projectID == Configs.defaultQuickLookProjectID) "Cleaning up..." else "Removing..."
        }

        activity?.let {
            progressDialog = MaterialAlertDialogBuilder(it)
                .setView(progressView)
                .setCancelable(false)
                .show()
        }

        Thread {
            startRemove(projectID, progressText)
            activity?.runOnUiThread {
                progressDialog?.dismiss()

                if (projectID != Configs.defaultQuickLookProjectID) {
                    DialogUtils.oneDialog(
                        activity,
                        "Done",
                        "Removed your project.",
                        "OK",
                        true,
                        R.drawable.ic_mtrl_check,
                        true,
                        null,
                        null
                    )
                }

                (activity as? MainActivity)?.refreshProjectsList()
            }
        }.start()
    }

    @JvmStatic
    fun startRemove(projectID: String?, statusTextView: TextView?) {
        if (projectID.isNullOrEmpty()) return

        val basePath = FileUtils.getInternalStorageDir()
        val foldersToDelete = listOf(
            "Removing data..." to Configs.projectDataFolderDir,
            "Removing project info..." to Configs.projectInfoFolderDir,
            "Removing fonts..." to Configs.resFontsFolderDir,
            "Removing icons..." to Configs.resIconsFolderDir,
            "Removing images..." to Configs.resImagesFolderDir,
            "Removing sounds..." to Configs.resSoundsFolderDir,
            "Removing unsaved data..." to Configs.projectUnsavedDataFolderDir,
            "Removing temporary files..." to Configs.projectMySourceFolderDir,
            "Removing Git..." to Configs.gitFolderDir
        )

        for ((statusMessage, subDir) in foldersToDelete) {
            updateStatus(statusTextView, statusMessage)
            safelyDeleteDirectory(basePath + subDir + projectID)
        }
    }

    private fun safelyDeleteDirectory(pathString: String) {
        try {
            FilesTools.deleteFileOrDirectory(Path.of(pathString))
        } catch (e: Exception) {
            Log.e(TAG, "Removing failed: $pathString", e)
        }
    }

    private fun updateStatus(statusTextView: TextView?, msg: String) {
        Log.i(TAG, "updateStatus: $msg")
        val context = statusTextView?.context as? Activity ?: return
        context.runOnUiThread {
            statusTextView.text = msg
        }
    }
}
