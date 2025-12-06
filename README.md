# *Kioto*

#### A lightweight ui framework for compose multiplatform mobile projects.

Kioto is a lightweight and robust ui framework that streamlines navigation management in Compose Multiplatform Mobile applications.
Its structured approach promotes a clear separation of concerns and improves code maintainability across your mobile projects.

## Why Kioto?

In the evolving landscape of Compose Multiplatform Mobile development, managing complex navigation flows and maintaining a clean architecture can be challenging.
Kioto addresses these challenges by offering a lightweight and opinionated solution designed to:

* **Simplify Navigation** Define your application's UI as modular Node components, making navigation intuitive and easy to reason about.
* **Promote Clean Architecture** Enforce a strict separation of concerns, leading to more maintainable and testable code.
* **Ensure Multiplatform Consistency** Provide a unified navigation API that works seamlessly across Android and iOS, reducing platform-specific boilerplate.
* **Enhance State Management** Leverage a robust state management system within each Node, ensuring predictable UI updates and a responsive user experience.

Kioto helps you build scalable and robust Compose Multiplatform Mobile applications with confidence and efficiency.

| Android                                                        | iOS                                                        |
|----------------------------------------------------------------|------------------------------------------------------------|
| <img src="assets/android_demo.gif" alt="alt text" width="380"> | <img src="assets/ios_demo.gif" alt="alt text" width="380"> |

## Kioto at work

### Node

A `Node` represents a modular portion of your application's UI.
Nodes are presentation logic components that encapsulate their own state and behavior.

To create a Node, you need to extend the `Node` class and define its state.

```kotlin
class MovieAdvisor : Node<MovieAdvisor.State>() {

    data class State(
        val movie: Movie? = null
    )

}
```

States are considered the core component of a Node. Let's see how to work with them.

```kotlin
class MovieAdvisor : Node<MovieAdvisor.State>() {

    data class State(
        val movie: Movie? = null
    )

    init {
        subscribe(::suggestMovie) { updateState { state.copy(movie = this) } } // this refers to the result of the suspend function suggestMovie
    }

}
```

In the example above, when the node is initialized, it subscribes to a suspend function `suggestMovie`.
When this function completes, it updates the node's state with the result. Let's break down in detail the init block:

* Subscriptions (**subscribe** and **flowSubscribe**)

Instances of Node can subscribe to suspend functions and flows by calling `subscribe` and `flowSubscribe` methods respectively.
These subscriptions are cancelled automatically when the node is cleared.

```kotlin
protected fun <T> subscribe(
    source: suspend () -> T,   // The suspend function to subscribe to
    onError: () -> Unit = { }, // Optional error handler
    onCompleted: T.() -> Unit  // The action to perform when the source function completes
)

protected fun <T> flowSubscribe(
    source: suspend () -> Flow<T>, // The suspend function that returns a Flow to subscribe to
    onError: () -> Unit = { },     // Optional error handler
    onNext: T.() -> Unit           // The action to perform on each emitted value
) 
```

* **updateState**

The `updateState` function is used to atomically update the Node's state.  
You can access the current state via the `state` property.

```kotlin
protected fun updateState(
    update: () -> S // A lambda that returns the new state for the node
)
```

#### NodeView

To render a Node, you need to implement a `NodeView` designed for that Node's state. The `NodeView` automatically recompose whenever the Node's state changes, ensuring your UI is always up-to-date.

```kotlin
class MovieAdvisorView(
    private val listener: ViewListener // A listener to send events back to the Node
) : NodeView<MovieAdvisor.State> {

    @Composable
    override fun Compose(state: MovieAdvisor.State) {
        // Your composable UI code here
    }

    interface ViewListener {
        fun onSeeMore(movie: Movie)
    }

}
```

#### NodeToken

So far, you are able to create a Node and the component that renders it. So it's time to introduce the last Node component that wires up everything together: the `NodeToken`.

```kotlin
class MovieAdvisor : Node<MovieAdvisor.State>() {

    data class State(
        val movie: Movie? = null
    )

    object Token : NodeToken {
        override fun node() = node(::MovieAdvisor, ::State) {
            MovieAdvisorView(object : MovieAdvisorView.ViewListener {
                override fun onSeeMore(movie: Movie) = Unit // Handle the request from the view
            })
        }
    }

    init {
        subscribe(::suggestMovie) { updateState { state.copy(movie = this) } }
    }

}
```

NodeTokens are the glue that binds the Node, its state, and its view together. It tells the system how to instantiate the Node, what its initial state is, and how to create the
corresponding NodeView.
You need to implement the `NodeToken` and override the `node` method to return the result of calling the `node` function.

```kotlin
fun <N : Node<S>, S : Any> node(
    node: () -> N,              // The function that creates the Node instance
    initialState: () -> S,      // The function that provides the initial state for the Node
    view: N.() -> NodeView<S>   // The function that creates the NodeView for the Node, which receives the Node instance as a receiver
): NodeToken.Node
```

NodeTokens can be parameterized, allowing you to pass parameters to the Node's constructor and its initial state.
Look at the example below that takes a `Movie` as a parameter which is used to initialize both, the Node and its view.

```kotlin
class MovieDetail(
    private val movieId: String
) : Node<MovieDetail.State>() {

    object State // Empty state for this example

    // This token accepts a 'Movie' object
    class Token(val movie: Movie) : NodeToken {
        override fun node() = node({ MovieDetail(movie.id) }, { State }) {
            MovieView(movie = movie)
        }
    }

}
```

#### Hosted nodes

A `Node` can host one or more child nodes to build complex screens composed of independent, reusable components. 
This is perfect for layouts like tabbed interfaces, dashboards with multiple sections, or any screen that combines several distinct pieces of functionality.
A hosted node can in turn hosts its own children nodes. 

* Key characteristics of hosted nodes:
- **Inherited Navigator:** Hosted nodes automatically inherit the `Navigator` instance from their host node.
This means any navigation operations initiated from a hosted node will be forwarded to the host node.
- **Lifecycle Management:** Hosted nodes are automatically cleared when their host node is cleared.

```kotlin
class Landing : Node<Landing.State>() {

    object State //Empty state for this example

    object Token : NodeToken {
        override fun node() = node(::Landing, { State }) { LandingView() }
    }

    init {
        // This node hosts two child nodes, the Demo and About nodes
        host( 
            Demo.Token, 
            About.Token
        )
    }

}
```

To host nodes, host node must call `host(vararg nodes: NodeToken)` and pass the tokens of the nodes to be hosted. 
Calling `host()` again will clear any previously hosted nodes and establish the new ones. You can call `host()` with no arguments to clear all children.

To display hosted nodes in your `NodeView`, you can use of the predefined solutions or build a custom layout with the `HostedNodesHost` composable.

*Built-in predefined Host Composables*

Kioto provides ready-to-use solutions for common hosting patterns: 

- **BoxHost**
A composable that displays the hosted node at first index. It supports custom loading/empty states and animations.
```kotlin
@Composable
private fun BoxHostSample() {
    BoxHost(modifier = Modifier.fillMaxSize())
}
```

- **PagerHost**
A composable that displays a collections of hosted nodes one at a time, using a pager-like interface.
It supports custom loading/empty states and animations.
```kotlin
@Composable
private fun PagerHostSample() {
    Column(modifier = Modifier.fillMaxSize()) {
        val pagerHostState = rememberPagerHostState()
        PrimaryTabRow(selectedTabIndex = pagerHostState.currentPage, modifier = Modifier.fillMaxWidth()) {
            repeat(3) { tabIndex ->
                Tab(selected = pagerHostState.currentPage == tabIndex, onClick = { pagerHostState.currentPage = tabIndex }, text = { Text(text = "Tab $tabIndex") })
            }
        }
        PagerHost(state = pagerHostState, modifier = Modifier.fillMaxSize().weight(1F))
    }
}
```

- Build your own solution

`HostedNodesHost` gives you full control over how your hosted nodes are rendered. 
It provides the count of active nodes and a function to render each one by its index. 

```kotlin
// Renders hosted nodes in a vertically stacked layout, each taking equal space
Column(modifier = Modifier.fillMaxSize()) {
  HostedNodesHost { nodes, render ->
      // `nodes` represents the number of currently hosted nodes, or `null` if host node didn't ask to host any nodes
      // `render(index)` is a lambda that renders a specific node
    repeat(nodes ?: 0) { index ->
      Box(modifier = Modifier.fillMaxSize().weight(1F)) {
        render(index)
      }
    }
  }
}
```

#### Navigation

Nodes can be navigated using the `nav` property available within the `Node` class, which provides a simple API to navigate between nodes.
Let's see how to navigate to another node as a result of an action in the view.

```kotlin
class MovieAdvisor : Node<MovieAdvisor.State>() {

    data class State(
        val movie: Movie? = null
    )

    object Token : NodeToken {
        override fun node() = node(::MovieAdvisor, ::State) {
            // The 'this' context here is the MovieAdvisor Node instance
            MovieAdvisorView(object : MovieAdvisorView.ViewListener {
                override fun onSeeMore(movie: Movie) = nav.navigate { MovieDetail.Token(movie) }
            })
        }
    }

    init {
        subscribe(::suggestMovie) { updateState { state.copy(movie = this) } }
    }

}
```

In the example above, when the user requests to see more details about a movie, the `onSeeMore` method is called, which uses the `nav.navigate` method to navigate to the
`MovieDetail` node.
Note that to navigate to a node, you need to provide the `NodeToken` that represents the destination node. The way tokens are created and the files they are declared is up to you,
although the recommended way to do it is as shown in the examples above.

This navigation solution follows a stack of stacks navigation model, meaning that each node belongs to a stack, which could be shared with other nodes.
The first node added to the first stack is known as the root node, this node is a bit special node because it isn't allowed to share its stack with other nodes.

Let's review the available navigation methods for Nodes:

* `resetNavigation`
  Pops all the stacks and their nodes, and starts a new navigation stack.

<div style="text-align:center"><img src="assets/nav_reset_navigation.png" alt="alt text" width="380"></div>

* `navigate`
  Transitions to the specified node within the current navigation stack, except if called from a root node, in which case it begins a new stack.

<div style="text-align:center">
<img src="assets/nav_navigate.png" alt="alt text" width="380">
<img src="assets/nav_navigate_root.png" alt="alt text" width="380">
</div>

* `beginStack`
  Starts a new stack with the specified node as the stack root node.

<div style="text-align:center"><img src="assets/nav_begin_stack.png" alt="alt text" width="380"></div>

* `replace`
  Replaces the current node with the specified node. Replacing a root node behaves as if `resetNavigation` was called.

<div style="text-align:center"><img src="assets/nav_replace.png" alt="alt text" width="380"></div>

* `replaceStack`
  Starts a new stack replacing the node's current stack. Replacing the stack of a root node behaves as if `resetNavigation` was called.

<div style="text-align:center"><img src="assets/nav_replace_stack.png" alt="alt text" width="380"></div>

* `navigateBack`
  Pops the node from the stack.

<div style="text-align:center"><img src="assets/nav_navigate_back.png" alt="alt text" width="380"></div>

* `navigateUp`
  Pops all the nodes belonging to the node's stack.

<div style="text-align:center"><img src="assets/nav_navigate_up.png" alt="alt text" width="380"></div>

* `popToRoot`
  Pops all the nodes in the stack until the root node is reached, unless the node that initiates the navigation is a root node, in which case it behaves as if
  `navigateBack` was called.

<div style="text-align:center"><img src="assets/nav_pop_to_root.png" alt="alt text" width="380"></div>

Navigation events are usually triggered by the latest added node, however, there's nothing by design that prevents from navigating from any previous node in the stack.
In that case, all subsequent nodes will first be popped until the node requesting the navigation is reached before handling the navigation request.

### NodeNav

`NodeNav` is the core component that manages navigation under the hood.
NodeNav runs regardless of the UI is rendering nodes or not, you just need to create an instance of it and set a `Node` as the root node to start the navigation process.
There's no limit on the number of `NodeNav` instances you can create, so it's up to you to determine the approach that works best for your application,
you could have a single global reference to a `NodeNav` instance in your application file or use more elaborated solutions like dependency injection to manage it.
For most applications, a single NodeNav instance at the application root is sufficient, but more advanced scenarios might benefit from multiple instances.
If a `NodeNav` instance's lifecycle is shorter than app's lifecycle, it is necessary to call `release` to ensure proper cleanup of active nodes.

```kotlin
val nodeNav = NodeNav.newInstance().apply {
    setNavigation { Landing.Token } // Set the initial navigation token
}
```

In the example above, a `NodeNav` instance is created and `setNavigation` is called to set the initial navigation node.
Calling `setNavigation` again will clear any previous navigation state and start a new one.

#### Present

Sometimes you may find scenarios where you need your users to interact with the UI from events triggered from outside of the regular UI navigation flow.
Such as opening a push notification from the notifications center, a second factor authentication request, or a required pending terms acceptance that you discover during a login
process.
For these cases, you can leverage `presentStack` or `awaitPresentStack` to start a new stack with a node that will be presented to the user, allowing them to interact with it and
then return to the previous navigation state.

* `presentStack` presents a new stack on top of any existing stacks.

```kotlin
fun presentDebitCardPaymentOperation(operationId: String) {
    // Presents a new stack with the DebitCardPaymentDetail node on top of any existing stacks if any
    nodeNav.present { DebitCardPaymentOperation.Token(operationId = operationId) }
}
```

`awaitPresentStack` is a suspend function, coroutine execution is suspended until the presented stack and its replacements, if any, are dismissed.

```kotlin
suspend fun authenticate(authenticate: (code: String) -> Boolean) {
    nodeNav.present { SecondFactorAuthentication.Token(authenticate) } // Suspended until SecondFactorAuthentication is dismissed
}

class SecondFactorAuthentication(
    private val auth: suspend (String) -> Boolean
) : Node<SecondFactorAuthentication.State>() {

    data class State(
        val isAuthenticating: Boolean = false,
        val invalidCodeError: Boolean = false
    )

    class Token(val auth: (String) -> Boolean) : NodeToken {
        override fun node() = node({ SecondFactorAuthentication(auth) }, ::State) {
            SecondFactorAuthenticationView(
                listener = object : SecondFactorAuthenticationView.ViewListener {
                    override fun onAuthenticate(code: String) = authenticate(code)
                }
            )
        }
    }

    fun authenticate(code: String) {
        updateState { state.copy(isAuthenticating = true) }
        subscribe(
            source = { auth(code) },
            onError = { updateState { state.copy(isAuthenticating = false, invalidCodeError = true) } },
            onCompleted = { nav.navigateUp() }
        )
    }

}
```

#### Predictive back gesture

Kioto supports predictive back gestures, allowing users to navigate back in the navigation stack by swiping from the edges (from the right only available on Android) of the screen.
Follow official Android documentation to enable predictive back gesture in your Android
application: [Predictive back gesture](https://developer.android.com/guide/navigation/predictive-back-gesture).

| Android                                                              | iOS                                                              |
|----------------------------------------------------------------------|------------------------------------------------------------------|
| <img src="assets/predictive_android.gif" alt="alt text" width="380"> | <img src="assets/predictive_ios.gif" alt="alt text" width="380"> |

#### Node context

`NodeNav` can provide a scoped context to the `Node` instances it hosts. Contexts are unbounded, since they can hold any number of properties.
DI graphs, configuration parameters, node's dependencies, are just a few examples of how you can take advantage of this feature.

Context properties are identified by a `ProvidableContext` key. `Node`s uses this key to retrieve the context properties they need.

```kotlin
val logger = contextKeyOf<Logger>() // Declare a context key for Logger

val nodeNav = NodeNav(
    context = context(
        // Associate logger key to Logger supplier lambda, instances are created lazily and reused.
        logger provides { Logger(::println) }
    )
)

class MovieAdvisor : Node<MovieAdvisor.State>() {

    data class State(
        val movie: Movie? = null
    )

    object Token : NodeToken {
        override fun node() = node(::MovieAdvisor, ::State) {
            MovieAdvisorView(object : MovieAdvisorView.ViewListener {
                override fun onSeeMore(movie: Movie) = seeMore(movie)
            })
        }
    }

    init {
        subscribe(::suggestMovie) { updateState { state.copy(movie = this) } }
    }

    private fun seeMore(movie: Movie) {
        nav.navigate { MovieDetail.Token(movie) }.also {
            // Retrieve the context property by invoking () the key  
            logger().log("See more about ${movie.title}")
        }
    }

}

fun interface Logger {
    fun log(message: String)
}
```

#### Dismissing a root node

Once a `NodeNav` is initialized by adding a first node, aka root node, it cannot be emptied anymore. Any attempt to empty the navigation by dismissing its root node has no effect.

However, it's common you may start a navigation from a node that is not the usual root node, for example, when opening the app from a notification or a deep link and you may want
your users to be directed to a specific node after leaving the app, for example, a home or dashboard node. To deal with these situations, provide a `handleRootDismissTry` lambda
when instantiating `NodeNav`. `handleRootDismissTry` is called everytime a new root node is set. This lambda should return another lambda which will be invoked if an attempt is
made to dismiss that root node, allowing you to define the desired behavior.


```kotlin
val nodeNav = NodeNav.newInstance(
    context = context(logger provides { Logger(::println) })
) { rootToken ->
    // Any attempt to dismiss a root node other than Demo, will take users to Demo node by setting a new navigation. 
    // Having this way Demo as the only exit point for the application.
    if (rootToken != Demo.Token) return@newInstance { setNavigation { Demo.Token } } else null
}.apply {
    // Initialize the navigation with the first node.
    setNavigation { Demo.Token }
}
```

## Add kioto to your project

Add the dependency to your project `build.gradle` file:

```kotlin
kotlin {
    //...
    sourceSets {
        commonMain.dependencies {
            implementation("com.wokdsem.kioto:kioto:0.5.2")
        }
    }
}
```

Available on [Maven Central](https://central.sonatype.com/artifact/com.wokdsem.kioto/kioto)

### Android

Use `NodeHost` composable function to render a `NodeNav` instance in your Android application.

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NodeHost(
                nodeNav = nodeNav // The NodeNav instance to be rendered 
            )
        }
    }

}
```

### iOS

Use `nodeHost` function which returns a `UIViewController` to render a `NodeNav` instance in your iOS application. 
Override the default composable nodeHost wrapper if you need to customize the nodeHost holder.   

```kotlin
object ExampleIOsApplication {

    fun getNodeNavUIViewController() = nodeHost(
        nodeNav = nodeNav // The NodeNav instance to be rendered
    )

}
```

```swift
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            NodeNavRenderer()
                .ignoresSafeArea(.all)
        }
    }
}

struct NodeNavRenderer: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        ExampleIOsApplication().getNodeNavUIViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
```

#### CompositionLocals

Kioto provides the following composition locals:

* `LocalNodeHost` Provides access to the current `NodeHost` instance, allowing any composable to react to the host's state. The `NodeHost` exposes the following properties:
    * `platform: Platform`: The host platform the app is currently running on (ANDROID or IOS).
    * `isTransitionInProgress`: A flag that is `true` if a navigation transition between nodes is currently active.

This is particularly useful for building platform-specific UI or for disabling certain interactions during a navigation transition.

```kotlin
val nodeHost = LocalNodeHost.current ?: return
if (!nodeHost.isTransitionInProgress) {
    val platform = when (nodeHost.platform) {
        Platform.ANDROID -> "Android"
        Platform.IOS -> "iOS"
    }
    Text(text = "Platform: $platform")
}
```

* `LocalNodeScope` Provides the current `NodeScope` instance, which can be used to request a back navigation and determine if the current node is hosted by another.

```kotlin
val nodeScope = LocalNodeScope.current
if (nodeScope?.isHosted == false) {
    IconButton(onClick = { nodeScope.navigateBack() }) {
        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
    }
}
```

## License

```
Copyright 2025 Wokdsem

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
