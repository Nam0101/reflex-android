package androidshowcase.reflex.introspection.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.Modifier

private const val INTROSPECTION = "androidshowcase.reflex.introspection"
private const val INTROSPECTED = "$INTROSPECTION.Introspected"
private const val ACTION_TITLE = "$INTROSPECTION.ActionTitle"
private const val NODE_PROVIDER = "$INTROSPECTION.IntrospectionNodeProvider"
private const val VIEW_MODEL = "androidshowcase.reflex.viewmodel.ReflexViewModel"
private const val SCREEN_MODEL = "androidshowcase.reflex.navigation.ScreenModel"
private const val SERIALIZABLE = "kotlinx.serialization.Serializable"
private const val PROVIDER_CLASS = "GeneratedIntrospectionNodes"

class IntrospectionProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
    IntrospectionProcessor(environment.codeGenerator, environment.logger)
}

/**
 * For every `@Introspected class FooViewModel : ReflexViewModel<State, Action, Message>` writes
 * `FooViewModelIntrospectionNode` next to it, then one `GeneratedIntrospectionNodes` provider per module and its
 * `META-INF/services` entry. A ViewModel it cannot describe is a compile error, never a missing screen at runtime.
 */
private class IntrospectionProcessor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) :
  SymbolProcessor {
  private class Action(val name: String, val title: String, val type: String, val hasInput: Boolean)

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val viewModels = resolver.getSymbolsWithAnnotation(INTROSPECTED)
      .filterIsInstance<KSClassDeclaration>()
      .sortedBy { it.qualifiedName?.asString() }
      .toList()
    // Later rounds only see the generated files, which carry no annotation.
    if (viewModels.isEmpty()) return emptyList()
    val described = viewModels.filter(::writeNode)
    if (described.isNotEmpty()) writeProvider(described)
    return emptyList()
  }

  /** False (after reporting why) when [viewModel] cannot have a node. */
  private fun writeNode(viewModel: KSClassDeclaration): Boolean {
    val type = viewModel.qualifiedName?.asString()
    val arguments = viewModel.superTypes.map { it.resolve() }
      .firstOrNull { it.declaration.qualifiedName?.asString() == VIEW_MODEL }
      ?.arguments
    if (type == null || viewModel.parentDeclaration != null || arguments == null) {
      logger.error("@Introspected goes on a top-level class that extends ReflexViewModel directly", viewModel)
      return false
    }
    val state = arguments[0].type?.resolve()?.declaration?.qualifiedName?.asString()
    val actionType = arguments[1].type?.resolve()?.declaration as? KSClassDeclaration
    val actionName = actionType?.qualifiedName?.asString()
    if (state == null || actionType == null || actionName == null) {
      logger.error("State and Action of an @Introspected ViewModel must be classes", viewModel)
      return false
    }
    val actions = if (actionName == "kotlin.Nothing") emptyList() else actionsOf(actionType) ?: return false
    actions.groupBy { it.name }.filterValues { it.size > 1 }.keys.forEach {
      logger.error("Two actions of $actionName are both named '$it'", actionType)
      return false
    }

    val annotation = viewModel.annotations.first { it.shortName.asString() == "Introspected" }
    val screenName = (annotation.arguments.firstOrNull { it.name?.asString() == "name" }?.value as? String)
      ?.takeIf { it.isNotEmpty() }
      ?: viewModel.simpleName.asString().removeSuffix("ViewModel")
    val simpleName = viewModel.simpleName.asString()
    val actionLines = actions.joinToString("\n") {
      val argument = if (it.hasInput) "${it.type}.serializer()" else it.type
      "    action(${it.name.quoted()}, ${it.title.quoted()}, $argument),"
    }
    val source = """
      |// Generated from @Introspected $simpleName. Do not edit.
      |package ${viewModel.packageName.asString()}
      |
      |class ${simpleName}IntrospectionNode(viewModel: $type) :
      |  $INTROSPECTION.ViewModelIntrospectionNode<$state, $actionName>(viewModel, $state.serializer()) {
      |  override val name = ${screenName.quoted()}
      |
      |  override val actions: List<$INTROSPECTION.IntrospectionAction> = listOf(
      |$actionLines
      |  )
      |}
      |
    """.trimMargin()
    // Not aggregating: the node depends only on its ViewModel's file (KSP tracks the types resolved from it).
    val file = listOfNotNull(viewModel.containingFile).toTypedArray()
    codeGenerator.createNewFile(
      Dependencies(false, *file),
      viewModel.packageName.asString(),
      "${simpleName}IntrospectionNode",
    )
      .use { it.write(source.toByteArray()) }
    return true
  }

  /** The leaves of a sealed `Action` hierarchy, or null (after reporting why) when one cannot be an action. */
  private fun actionsOf(type: KSClassDeclaration): List<Action>? {
    if (Modifier.SEALED !in type.modifiers) {
      logger.error("Action must be a sealed interface or class, so that every user action is listed", type)
      return null
    }
    // In the order they are declared, which is the order `ls` lists them in.
    return type.getSealedSubclasses().sortedBy {
      (it.location as? FileLocation)?.lineNumber
    }.toList().flatMap { subclass ->
      if (Modifier.SEALED in subclass.modifiers) return@flatMap actionsOf(subclass) ?: return null
      val hasInput = subclass.classKind != ClassKind.OBJECT
      val serializable = subclass.annotations.any {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == SERIALIZABLE
      }
      if (hasInput && !serializable) {
        logger.error("An action with input must be @Serializable: drivers send its input as JSON", subclass)
        return null
      }
      val simpleName = subclass.simpleName.asString()
      val title = subclass.annotations
        .firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == ACTION_TITLE }
        ?.arguments?.firstOrNull()?.value as? String
      listOf(
        Action(
          name = simpleName.replaceFirstChar(Char::lowercase),
          title = title ?: simpleName.toWords(),
          type = checkNotNull(subclass.qualifiedName).asString(),
          hasInput = hasInput,
        ),
      )
    }
  }

  private fun writeProvider(viewModels: List<KSClassDeclaration>) {
    // One provider per module, in the package of its first ViewModel.
    val packageName = viewModels.first().packageName.asString()
    val branches = viewModels.joinToString("\n") {
      val type = checkNotNull(it.qualifiedName).asString()
      "    is $type -> ${type}IntrospectionNode(model)"
    }
    val source = """
      |// Generated from the @Introspected ViewModels of this module. Do not edit.
      |package $packageName
      |
      |class $PROVIDER_CLASS : $NODE_PROVIDER {
      |  override fun nodeFor(model: $SCREEN_MODEL): $INTROSPECTION.IntrospectionNode? = when (model) {
      |$branches
      |    else -> null
      |  }
      |}
      |
    """.trimMargin()
    // Aggregating: adding or removing any @Introspected ViewModel rewrites both files.
    val files: Array<KSFile> = viewModels.mapNotNull { it.containingFile }.toTypedArray()
    val dependencies = Dependencies(true, *files)
    codeGenerator.createNewFile(dependencies, packageName, PROVIDER_CLASS).use { it.write(source.toByteArray()) }
    codeGenerator.createNewFileByPath(dependencies, "META-INF/services/$NODE_PROVIDER", extensionName = "")
      .use { it.write("$packageName.$PROVIDER_CLASS\n".toByteArray()) }
  }
}

/** `ToggleFavorite` → `Toggle favorite`. */
private fun String.toWords(): String =
  replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ").lowercase().replaceFirstChar(Char::uppercase)

private fun String.quoted(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""
