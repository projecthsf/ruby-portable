package io.genai.ruby.sdk

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.util.io.HttpRequests
import com.intellij.util.system.CpuArch

/**
 * Catalogue of installable portable Ruby builds, read live from the GitHub releases of
 * Homebrew/homebrew-portable-ruby so the version list stays current. Falls back to a pinned
 * version when the network is unavailable.
 *
 * See [RubyRelease] for why this source and not `ruby/ruby-builder`.
 */
object RubyDownloads {

    private const val RELEASES = "https://api.github.com/repos/Homebrew/homebrew-portable-ruby/releases?per_page=15"
    private const val DL = "https://github.com/Homebrew/homebrew-portable-ruby/releases/download"

    /** Used when the releases API can't be reached. Known-good at time of writing. */
    private const val FALLBACK_VERSION = "3.4.6"

    fun currentOs(): OsFamily = when {
        SystemInfo.isWindows -> OsFamily.WINDOWS
        SystemInfo.isMac -> OsFamily.MAC
        else -> OsFamily.LINUX
    }

    private fun currentArch(): String = if (CpuArch.isArm64()) "arm64" else "x86_64"

    /**
     * Homebrew's bottle tag for this machine. The macOS tags are named after the oldest supported
     * OS version rather than the current one — `arm64_big_sur` is the Apple-silicon build and
     * `el_capitan` the Intel one; both run on current macOS.
     */
    fun bottleTag(): String? = when (currentOs()) {
        OsFamily.MAC -> if (CpuArch.isArm64()) "arm64_big_sur" else "el_capitan"
        OsFamily.LINUX -> if (CpuArch.isArm64()) "arm64_linux" else "x86_64_linux"
        // portable-ruby has no Windows build, and RubyInstaller only ships .7z / .exe, which the
        // platform's Decompressor can't read. Windows users use "Add from Disk…" for now.
        OsFamily.WINDOWS -> null
    }

    fun isSupportedPlatform(): Boolean = bottleTag() != null

    fun fetchAvailableWithProgress(project: Project?): List<RubyRelease> =
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            ThrowableComputable { fetchAvailable() },
            "Fetching Available Ruby Versions…",
            true,
            project,
        )

    /** Blocking fetch — must be called off the EDT. Returns newest-first. */
    fun fetchAvailable(): List<RubyRelease> {
        val tag = bottleTag() ?: return emptyList()
        val versions = try {
            fetchVersions(tag).ifEmpty { listOf(FALLBACK_VERSION) }
        } catch (e: Exception) {
            listOf(FALLBACK_VERSION)
        }
        return versions.map { release(it, tag) }
    }

    private fun release(version: String, tag: String) = RubyRelease(
        version = version,
        os = currentOs(),
        arch = currentArch(),
        bottle = tag,
        url = "$DL/$version/portable-ruby-$version.$tag.bottle.tar.gz",
    )

    /**
     * Pulls release tags from the GitHub API, keeping only those that actually ship a bottle for
     * [tag] — older releases predate the arm64 Linux build, and offering a version whose asset
     * doesn't exist would 404 at download time.
     */
    private fun fetchVersions(tag: String): List<String> {
        val json = HttpRequests.request(RELEASES)
            .productNameAsUserAgent()
            .readString()
        // Match the asset names directly rather than parsing JSON: an asset called
        // portable-ruby-3.4.6.arm64_big_sur.bottle.tar.gz proves both the version and that this
        // platform is covered by that release.
        val re = Regex("""portable-ruby-(\d+(?:\.\d+)+)\.${Regex.escape(tag)}\.bottle\.tar\.gz""")
        return re.findAll(json).map { it.groupValues[1] }.distinct().toList().sortedWith(VERSION_DESC)
    }

    /** Descending version order (3.4.6 before 3.3.8). */
    private val VERSION_DESC: Comparator<String> = Comparator<String> { a, b ->
        val pa = a.split(".").map { it.toIntOrNull() ?: 0 }
        val pb = b.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val d = pa.getOrElse(i) { 0 }.compareTo(pb.getOrElse(i) { 0 })
            if (d != 0) return@Comparator d
        }
        0
    }.reversed()
}
