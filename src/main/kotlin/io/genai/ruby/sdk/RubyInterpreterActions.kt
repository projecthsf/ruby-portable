package io.genai.ruby.sdk

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Files

/**
 * Interactive install flows shared by the settings panel and the editor banner.
 * All entry points run on the EDT; [onComplete] fires on the EDT after registration.
 */
object RubyInterpreterActions {

    fun downloadInteractively(project: Project?, onComplete: (Sdk?) -> Unit) {
        val releases = RubyDownloads.fetchAvailableWithProgress(project)
        if (releases.isEmpty()) {
            Messages.showInfoMessage("No portable Ruby interpreters are listed for this OS.", "Download Ruby")
            return
        }
        val dialog = RubyDownloadDialog(releases)
        if (!dialog.showAndGet()) return
        val release = dialog.selected ?: return
        val home = RubySdkManager.plannedHome(release.version)

        ProgressManager.getInstance().run(object : Task.Modal(project, "Downloading Ruby ${release.version}", true) {
            override fun run(indicator: ProgressIndicator) {
                RubySdkDownloadTask(release, home).doDownload(indicator)
            }

            override fun onSuccess() {
                val sdk = RubySdkManager.registerFromHome(home.toString())
                if (sdk == null) {
                    Messages.showErrorDialog(
                        "Download finished but no cargo executable was found under\n$home",
                        "Download Ruby",
                    )
                }
                onComplete(sdk)
            }

            override fun onThrowable(error: Throwable) {
                Messages.showErrorDialog(error.message ?: error.toString(), "Download Ruby Failed")
            }
        })
    }

    fun addFromDisk(onComplete: (Sdk?) -> Unit) {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle("Select Ruby Interpreter Directory")
            .withDescription("Pick a Ruby interpreter folder (contains cargo/bin/cargo, or bin/cargo).")
        val root = RubySdkManager.downloadRoot()
        Files.createDirectories(root)
        val toSelect = LocalFileSystem.getInstance().findFileByNioFile(root)
        val chosen = FileChooser.chooseFile(descriptor, null, toSelect) ?: return
        val home = chosen.path
        if (RubySdkType.findRubyExecutable(home) == null) {
            Messages.showErrorDialog("No cargo executable found under\n$home", "Add Ruby Interpreter")
            return
        }
        onComplete(RubySdkManager.registerFromHome(home))
    }
}
