package io.microconfig.core.properties.templates.impl

import com.samskivert.mustache.Mustache
import io.microconfig.core.properties.TypedProperties
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.io.yaml.YamlTree
import io.microconfig.core.properties.templates.TemplateContentPostProcessor
import io.microconfig.utils.IoUtils.readFully
import io.microconfig.utils.Logger
import java.io.File
import java.io.Reader
import java.util.function.UnaryOperator
import org.yaml.snakeyaml.Yaml

class MustacheTemplateProcessor
@JvmOverloads
constructor(private val partialProcessor: UnaryOperator<String> = UnaryOperator.identity()) :
    TemplateContentPostProcessor {
    override fun process(
        templateType: String,
        source: File,
        templateContent: String,
        properties: TypedProperties,
    ): String {
        if (templateType != "mustache" && source.extension != "mustache") return templateContent
        Logger.info(
            "Using mustache template for ${properties.declaringComponent.component}/${source.name}"
        )
        return compile(source, templateContent).execute(templateModel(properties))
    }

    private fun compile(source: File, content: String): com.samskivert.mustache.Template =
        Mustache.compiler()
            .withLoader { name -> readPartial(source, name) }
            .compile(content)

    private fun readPartial(source: File, name: String): Reader =
        partialProcessor.apply(readFully(resolvePartial(source, name))).reader()

    private fun resolvePartial(source: File, name: String): File =
        File(name).let {
            if (it.isAbsolute) it else File(source.parentFile, name)
        }

    private fun templateModel(properties: TypedProperties): Map<String, Any?> =
        Yaml()
            .load(
                YamlTree.compact()
                    .toYaml(
                        properties.properties.associate {
                            YamlScalars.serializedKey(it) to YamlScalars.serializedValue(it)
                        }
                    )
            )
}
