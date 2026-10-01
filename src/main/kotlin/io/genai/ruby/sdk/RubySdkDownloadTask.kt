package io.genai.ruby.sdk

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask
import com.intellij.util.io.Decompressor
import com.intellij.util.io.HttpRequests
import java.nio.file.Files
import java.nio.file.Path

/**
 * Downloads a portable Ruby into [homeDir] and extracts it.
 *
 * Simpler than the Rust plugin's equivalent: this is a plain tarball, no installer to drive. The
 * Homebrew bottle expands into a nested `portable-ruby/<version>/` folder, so the interpreter
 * ends up at [homeDir]/portable-ruby/<version>/bin/ruby — [RubySdkType.findRubyExecutable]
 * searches that far down.
 */
class RubySdkDownloadTask(
    private val release: RubyRelease,
    private val homeDir: Path,
) : SdkDownloadTask {

    override fun getSuggestedSdkName(): String = "Ruby ${release.version}"
    override fun getPlannedHomeDir(): String = homeDir.toString()
    override fun getPlannedVersion(): String = release.version

    override fun doDownload(indicator: ProgressIndicator) {
        indicator.isIndeterminate = false
        indicator.text = "Downloading Ruby ${release.version}…"
        Files.createDirectories(homeDir)

        val tmp = Files.createTempFile("portable-ruby-", ".tar.gz")
        try {
            HttpRequests.request(release.url).saveToFile(tmp.toFile(), indicator)
            indicator.text = "Extracting Ruby ${release.version}…"
            // postProcessor restores the executable bit: Decompressor.Tar keeps POSIX modes, but
            // being explicit here means a future switch to another archive type can't silently
            // produce a non-executable interpreter.
            Decompressor.Tar(tmp).extract(homeDir)
        } finally {
            Files.deleteIfExists(tmp)
        }

        val ruby = RubySdkType.findRubyExecutable(homeDir.toString())
            ?: throw RuntimeException("Download finished but no ruby binary was found under $homeDir.")
        ruby.setExecutable(true)
        RubySdkType.findGemExecutable(homeDir.toString())?.setExecutable(true)
        RubySdkType.findBundleExecutable(homeDir.toString())?.setExecutable(true)
    }
}
