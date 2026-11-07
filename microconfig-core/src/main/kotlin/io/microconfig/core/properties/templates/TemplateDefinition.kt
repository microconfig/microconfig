package io.microconfig.core.properties.templates

import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.TypedProperties
import io.microconfig.core.properties.templates.impl.BinaryTemplate
import io.microconfig.core.properties.templates.impl.MustacheTemplateProcessor
import io.microconfig.core.properties.templates.impl.StringTemplate
import io.microconfig.core.templates.Template
import java.io.File

class TemplateDefinition
@JvmOverloads
constructor(
    private val templateType: String,
    val templateName: String,
    private val templatePattern: TemplatePattern,
    private val postProcessor: TemplateContentPostProcessor? = null,
) {
    var fromFile: File? = null
        private set

    var toFile: File? = null
        private set

    fun resolve(resolver: Resolver, properties: TypedProperties): Template {
        val component = properties.declaringComponent
        return try {
            resolveTemplate(resolver, properties, component)
        } catch (error: RuntimeException) {
            throw IllegalStateException(
                "Template error: $this\nComponent: $component\nCause: ${error.message}",
                error,
            )
        }
    }

    private fun resolveTemplate(
        resolver: Resolver,
        properties: TypedProperties,
        component: DeclaringComponent,
    ): Template {
        val source = templateFile()
        val destination = destinationFile(component, resolver)
        if (templateType == "file") return BinaryTemplate(templateName, source, destination)
        val template =
            StringTemplate(templateName, source, destination, templatePattern.placeholderPattern)
                .resolveBy(resolver, component)
        val processor =
            postProcessor ?: MustacheTemplateProcessor { content ->
                template.withContent(content).resolveBy(resolver, component).content
            }
        return template.postProcessContent(processor, templateType, properties)
    }

    private fun templateFile(): File {
        check(fromFile != null && toFile != null) { "Incomplete template def: $this" }
        val source = fromFile!!
        require(source.isAbsolute) {
            "Using relative path for template '$source'. Template path must be absolute. Consider using '\${this@configRoot}\\..' or '\${component_name@configDir}\\..' to build absolute path"
        }
        check(source.isFile) { "Missing template file: $source" }
        return source
    }

    private fun destinationFile(component: DeclaringComponent, resolver: Resolver): File {
        val destination = toFile!!
        return if (destination.isAbsolute) destination
        else File(resolver.resolve("\${this@resultDir}", component, component), destination.path)
    }

    fun setFromFile(path: String) {
        fromFile = File(path)
        if (toFile == null) setToFile(fromFile!!.name)
    }

    fun setToFile(path: String) {
        toFile = File(path.replace("\${templateName}", templateName))
    }

    override fun toString(): String =
        "templateName: '$templateName', file: '$fromFile' -> '$toFile'"
}
