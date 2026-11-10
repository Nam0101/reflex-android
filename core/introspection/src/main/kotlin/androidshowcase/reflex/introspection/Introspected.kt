package androidshowcase.reflex.introspection

import androidshowcase.reflex.navigation.ScreenModel

/**
 * Put on a screen ViewModel: the `introspection-processor` generates its `<ViewModel>IntrospectionNode`, with one
 * action per subclass of the ViewModel's sealed `Action` (`ToggleFavorite` → `toggleFavorite`; a class with input
 * must be `@Serializable`), and registers it so the shell finds it without any hand-written wiring.
 *
 * [name] is the screen name drivers and behavior tests wait for; by default the class name without `ViewModel`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Introspected(val name: String = "")

/** What `ls` shows for an action, when the words of its name are not enough (`LoadMore` → "Scroll to the end"). */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ActionTitle(val value: String)

/**
 * Generated once per module that has [Introspected] ViewModels and published through `META-INF/services`;
 * the shell loads every provider with `ServiceLoader`.
 */
interface IntrospectionNodeProvider {
  /** The node of [model], or null when the model belongs to another module. */
  fun nodeFor(model: ScreenModel): IntrospectionNode?
}
