# Rownd SDK for Android

The Rownd SDK for Android provides authentication, account and user profile management, deep linking, encryption and more for native Android applications.

Using the Rownd platform, you can easily bring the same authentication that's on your website to your mobile apps. Or if you only authenticate users on your mobile apps, you can streamline the authentication process using Rownd's passwordless sign-in links, enabling you to seamlessly authenticate users from an app link sent to their email or phone number.

Once a user is authenticated, you can retrieve and update their profile information on the fly using native APIs. Leverage Rownd's pre-built mobile app components to give users profile management tools. Additionally, you can manage the encryption of data on-device before sending it back to Rownd or your backend.


## Installation

In Android Studio, open your app's module-level `build.gradle` file and add the following dependency:

```
implementation 'io.rownd:android:2.3.2'
```

After adding, run a Gradle sync and the Rownd SDK/API should be available within your IDE.

## Usage

### Initializing the Rownd SDK

The Rownd SDK needs access to your application's and current activity's context in order to properly manage state, display UI components, and so on.

The most straightforward way of doing this is to subclass the Android `Application` class and pass the app's primary context.

To initialize Rownd, call the configure method like this:

```kotlin
Rownd.configure(application, "REPLACE_WITH_YOUR_APP_KEY")
```

Here's an example of what that might look like in the initial `Application` class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Rownd.configure(this, "REPLACE_WITH_YOUR_APP_KEY")
    }
}
```

After initialization, your app should typically call `Rownd.requestSignIn()` at some point, if the user is not already authenticated. This will display the Rownd interface for authenticating the user. Once they complete the sign-in process, an access token and the user's profile information will be available to your app.

### Handling authentication

Rownd leverages an observable architecture to expose data to your app using [StateFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow). This means that as the Rownd state changes, an app can dynamically update without complicated logic. For example, a view can display different information based on the user's authentication status.

You can use this StateFlow in both older-style XML layouts as well as Android Jetpack's newer Composable views.

### Using state in XML layout

```xml
// my_layout.xml

    <?xml version="1.0" encoding="utf-8"?>
<layout xmlns:tools="http://schemas.android.com/tools"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:android="http://schemas.android.com/apk/res/android">

    <data>
        <import type="android.view.View" />
        <variable
            name="rownd"
            type="io.rownd.android.Rownd" />
    </data>

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        tools:context=".MainActivity">

        <TextView
            android:id="@+id/textView"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@{@string/hello_rownd_state_user_data(rownd.state.user.data.first_name)}"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintVertical_bias="0.19999999" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</layout>
```

```kotlin
// my_activity.kt
class MyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.lifecycleOwner = this

        binding.rownd = Rownd
    }
}

```

### Using state in a Composable

```kotlin
// some_activity_or_component.kt

class MyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val state = Rownd.state.collectAsState()
            val signInButtonText = if (state.value.auth.isAuthenticated) "Sign out" else "Sign in"
            Button(
                onClick = {
                    if (state.value.auth.isAuthenticated) Rownd.signOut()
                    else Rownd.requestSignIn()
                }
            ) {
                Text(signInButtonText)
            }
        }
    }
}
```

Once you subscribe to the Rownd state via `Rownd.state.collectAsState()`, you can use the various parts of the state tree as needed.

Access the state like this:

```kotlin
val rowndState = Rownd.state.collectAsState()

val isAuthenticated = rowndState.value.auth.isAuthenticated
```


The following classes/properties are available:

#### .auth
```kotlin
data class AuthState(
    val accessToken: String?, // Current, valid access token for the user (valid for one hour)
    val isVerifiedUser: Boolean, // Whether the current user has verified at least one identifier (e.g., email)
    val isAuthenticated: Boolean // Whether the current user is signed in
)
```

#### .user
```kotlin
data class User(
    val id: String?,  // The user's ID as known to Rownd
    val data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?> = HashMap<String, Any?>(),
    val redacted: PersistentList<String> // A list of any profile fields that a user has restricted your app from accessing
)
```

## API reference

In addition to the StateFlow APIs, Rownd provides imperative APIs that you can call to request sign-in, get and retrieve user profile information, retrieve a current access token, or encrypt user data with the user's local key.

### Rownd.requestSignIn(): Void
Opens the Rownd sign-in dialog for authentication.

### Rownd.requestSignIn(RowndSignInOpts(...)): Void
Opens the Rownd sign-in dialog for authentication, as before, but allows passing additional context options as shown below.

- `postSignInRedirect: String` - When the user completes the authentication challenge via email or SMS, they'll be redirected to the URL set for postSignInRedirect. If this is an [Android App Link](https://developer.android.com/training/app-links), it will redirect the user back to your app.

Example:

```kotlin
Rownd.requestSignIn(RowndSignInOpts(
    postSignInRedirect = "https://my-domain.com"
))
```

> NOTE: The following user profile APIs technically accept `Any` as the value of a field. However, that value **must** be serializable using [Kotlin's Serialization](https://kotlinlang.org/docs/serialization.html) library. If the value is not serializable out of the box, you'll need to provide your own serializer implementation as described in the Kotlin documentation.

### Rownd.user.get(): Map<String, Any?>
Returns the entire user profile as a Map

### Rownd.user.get<T>(field: String): T?
Returns the value of a specific field in the user's data Map. `"id"` is a special case that will return the user's ID, even though it's technically not in the Map itself.

Your application code is responsible for knowing which type the value should cast to. If the cast fails or the entry doesn't exist, a `null` value will be returned.

### Rownd.user.set(data: Map<String, Any?>): Void
Replaces the user's data with that contained in the Map. This may overwrite existing values but must match the schema you defined within your Rownd application dashboard. Any fields that are flagged as `encrypted` will be encrypted on-device before storing in Rownd's platform.

### Rownd.user.set(field: String, value: Any): Void
Sets a specific user profile field to the provided value, overwriting if a value already exists. If the field is flagged as `encrypted`, it will be encrypted on-device prior to storing in Rownd's platform.


## Data encryption
As indicated previously, Rownd can automatically assist you in protecting sensitive user data by encrypting it on-device with a user's unique encryption key prior to saving it in Rownd's own platform storage.

When you configure your app within the Rownd platform, you can indicate that it supports on-device encryption. When this flag is set, Rownd will automatically generate a cryptographically secure, unrecoverable encryption key on the user's device after they sign in. The key is stored using Android's native KeyStore mechanisms and all encryption is handled on the device. The key is never transmitted to Rownd's servers and the Rownd SDK does not provide any APIs for your code to programmatically retrieve the encryption key.

Only fields that you designate `encrypted` are encrypted on-device before storing within Rownd. Some identifying fields like email and phone number do not support on-device encryption at this time, since they are frequently used for indexing purposes.

Of course, all data within the Rownd platform is encrypted at rest on disk and in transit, but this does not afford the same privacy guarantees as data encrypted on a user's local device. For especially sensitive data, we recommend enabling field-level encryption.

> NOTE: Data encrypted on-device will not be accessible by you, the app developer, outside of the context of your app. In other words, your app can use encrypted data in its plaintext (decrypted) form while the user is signed in, but you won't be able to retrieve that data from the Rownd servers in a decrypted form. For data that you choose to encrypt, you should never transmit the plain text value across a network.

In some cases, you may want to encrypt data on-device that you'll send to your servers for storage. Rownd provides convenience methods to encrypt and decrypt that data with the same user-owned key.

### Rownd.user.encrypt(plaintext: String): String
Encrypts the provided String `data` using the user's symmetric encryption key and returns the ciphertext as a string. You can encrypt anything that can be represented as a string (e.g., Int, Dictionary, Array, etc), but it's currently up to you to get it into a string format first.

If the encryption fails, an `EncryptionException` will be thrown with a message explaining the failure.

### Rownd.user.decrypt(ciphertext: String): String
Attempts to decrypt the provided String `data`, returning the plaintext as a string. If the data originated as some other type (e.g., Map), you'll need to decode the data back into its original type.

If the decryption fails, an `EncryptionException` will be thrown with a message explaining the failure.

> NOTE: Encryption is only possible once a user has authenticated. Rownd supports multiple levels of authentication (e.g., guest, unverified, and verified), but the lowest level of authentication must be achieved prior to encrypting or decrypting data. If you need to explicitly check whether encryption is possible at a specific point in time, call `Rownd.user.isEncryptionPossible(): Boolean` prior to calling `encrypt()` or `decrypt()`.
