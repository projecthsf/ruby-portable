package io.genai.ruby.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.project.Project

class RubyConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = "RubyPortableRun"

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        RubyRunConfiguration(project, this, "Ruby")

    override fun getOptionsClass(): Class<out RunConfigurationOptions> =
        RubyRunConfigurationOptions::class.java
}
