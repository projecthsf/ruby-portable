package io.genai.ruby.run

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.NotNullLazyValue

class RubyRunConfigurationType : ConfigurationTypeBase(
    "RubyPortableRunConfiguration",
    // "(Portable)" so it's distinct from the official Ruby plugin's run type on RubyMine / Ultimate.
    "Ruby File (Portable)",
    "Run a Ruby file with a portable Ruby interpreter",
    NotNullLazyValue.createValue { AllIcons.Actions.Execute },
) {
    init {
        addFactory(RubyConfigurationFactory(this))
    }
}
