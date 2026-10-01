package io.genai.ruby.sdk

import com.intellij.openapi.projectRoots.AdditionalDataConfigurable
import com.intellij.openapi.projectRoots.SdkAdditionalData
import com.intellij.openapi.projectRoots.SdkModel
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.SystemInfo
import org.jdom.Element
import java.io.File
import javax.swing.Icon

/**
 * The "Ruby Portable" SDK type. Downloading/switching interpreters is handled by our own UI
 * (Settings ▸ Ruby Portable), so we keep this type out of the platform's Java-oriented SDK
 * combos: `allowCreationByUser() = false` removes both the "Add" and "Download" actions there.
 */
class RubySdkType : SdkType("Ruby Portable") {

    override fun suggestHomePath(): String? = null

    override fun isValidSdkHome(path: String): Boolean = findRubyExecutable(path) != null

    override fun getVersionString(sdkHome: String): String? {
        val ruby = findRubyExecutable(sdkHome) ?: return null
        return try {
            val process = ProcessBuilder(ruby.absolutePath, "-v")
                .redirectErrorStream(true)
                .also { it.environment().putAll(environment(sdkHome)) }
                .start()
            val out = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            // "ruby 3.4.6 (2025-09-16 revision dbd83256b1) +PRISM [arm64-darwin20]" -> "3.4.6"
            Regex("""ruby\s+(\d+(?:\.\d+)+)""").find(out)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    override fun suggestSdkName(currentSdkName: String?, sdkHome: String): String {
        val version = getVersionString(sdkHome)
        return if (version != null) "Ruby $version" else "Ruby"
    }

    override fun createAdditionalDataConfigurable(
        sdkModel: SdkModel,
        sdkModificator: SdkModificator,
    ): AdditionalDataConfigurable? = null

    override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {}

    override fun getPresentableName(): String = "Ruby Portable"

    override fun getIcon(): Icon = ICON

    override fun allowCreationByUser(): Boolean = false

    companion object {
        private val ICON: Icon = IconLoader.getIcon("/icons/ruby.svg", RubySdkType::class.java.classLoader)

        fun getInstance(): RubySdkType = SdkType.findInstance(RubySdkType::class.java)

        private fun exe(name: String): String = if (SystemInfo.isWindows) "$name.exe" else name

        /** Where gems installed for this interpreter live — GEM_HOME, kept per-SDK. */
        fun gemHome(home: String): File = File(home, "gems")

        fun findRubyExecutable(home: String?): File? = findBinary(home, "ruby")

        fun findGemExecutable(home: String?): File? = findBinary(home, "gem")

        fun findBundleExecutable(home: String?): File? = findBinary(home, "bundle")

        /**
         * Locate a Ruby binary within an SDK home.
         *
         * The Homebrew bottle nests two levels (`<home>/portable-ruby/<version>/bin/ruby`), so we
         * search deeper than the other Portable plugins do. A flat `<home>/bin/ruby` is accepted
         * too, which is what "Add from Disk…" pointed at a system or rbenv install looks like.
         */
        private fun findBinary(home: String?, name: String): File? {
            if (home.isNullOrBlank()) return null
            val root = File(home)
            if (!root.exists()) return null
            val fileName = exe(name)

            fun candidates(dir: File) = listOf(File(File(dir, "bin"), fileName), File(dir, fileName))

            val found = candidates(root).firstOrNull { it.isFile }
            if (found != null) return found

            root.listFiles()?.filter { it.isDirectory }?.forEach { lvl1 ->
                candidates(lvl1).firstOrNull { it.isFile }?.let { return it }
                lvl1.listFiles()?.filter { it.isDirectory }?.forEach { lvl2 ->
                    candidates(lvl2).firstOrNull { it.isFile }?.let { return it }
                }
            }
            return null
        }

        /**
         * Environment for running ruby/gem/bundle against the portable interpreter: an isolated
         * GEM_HOME/GEM_PATH under the SDK home so installs never touch a system gem directory,
         * plus the interpreter's bin and the gem bin dir on PATH.
         *
         * Deliberately does NOT set RUBYLIB or a loader path. portable-ruby resolves its own
         * prefix and library paths from the binary's location at runtime; setting them would only
         * risk overriding correct values. (The `ruby/ruby-builder` builds *do* need
         * DYLD_FALLBACK_LIBRARY_PATH and RUBYLIB to work outside their build prefix — which is
         * part of why they aren't used here. See [RubyRelease].)
         */
        fun environment(home: String?): Map<String, String> {
            if (home.isNullOrBlank()) return emptyMap()
            val gems = gemHome(home)
            val rubyBin = findRubyExecutable(home)?.parentFile
            val existingPath = System.getenv("PATH").orEmpty()
            val path = listOfNotNull(
                rubyBin?.absolutePath,
                File(gems, "bin").absolutePath,
                existingPath.ifBlank { null },
            ).joinToString(File.pathSeparator)
            return mapOf(
                "GEM_HOME" to gems.absolutePath,
                "GEM_PATH" to gems.absolutePath,
                "PATH" to path,
            )
        }
    }
}
