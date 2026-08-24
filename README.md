# SMKit Android Demo

This application is the customer-facing reference for SMKit 1.8.0's low-level camera and movement-detection API. It deliberately uses SMKit directly, without SMKitUI, so customers can see the complete lifecycle and own all presentation.

## What the demo covers

- SDK construction, configuration, and pose-estimation preparation
- 2D camera session start/stop and camera release
- Exercise selection, typed detector start/switch/stop, and session results
- Default guidance mode and 1.8.0 guidance suggestions
- Guidance suggestion callbacks plus reset/rearm controls
- Guidance vocal coordination, reset, and end controls
- Adaptive ROM, exercise ROM range, and exercise type
- Phone-position and phone-movement APIs
- Exercise-view monitoring and raw algorithm-pipeline monitoring
- Body-calibration states, pose joints, movement data, feedback, and errors
- Feedback exclusions, SDK configuration JSON, and resource-download warning state
- Builder pose model, language, UI mode, auth key, and assessment-insight options

The main integration is in [`ActivityViewModel.kt`](app/src/main/java/com/example/smkitdemoapp/viewModels/ActivityViewModel.kt). The exercise selector drives configuration options, and the workout screen shows the SDK's live telemetry.

The **Start 3D Session** home action remains a clearly labelled placeholder because the public Android SDK exposes the 2D camera pipeline.

## Requirements

- Android minSdk 24
- compileSdk/targetSdk 36
- Java 17
- Camera permission
- A Sency SDK key

## Install SMKit 1.8.0

Add Sency's Maven repository:

```groovy
repositories {
    google()
    mavenCentral()
    maven { url "https://artifacts.sency.ai/artifactory/release" }
}
```

Add SMKit and the public model dependencies referenced by SMKit's typed API:

```groovy
dependencies {
    implementation "com.sency.smkit:smkit:1.8.0"
    implementation "com.sency.smbase.data:smbase-data:1.8.0"
    implementation "com.sency.smbase.nativeclient:smbase-native-client:1.8.0"
}
```

`smbase-data` supplies public types such as `RomRange`, `SMBaseExerciseType`, `DownloadModel`, and raw algorithm data. `smbase-native-client` supplies `FormFeedbackType`.

The app also keeps the native-library packaging rule used by the SDK:

```groovy
packagingOptions {
    pickFirst "**/*.so"
}
```

## Configure the demo

Create or update the untracked `local.properties` file:

```properties
sdk_auth_key=your_sency_sdk_key_here
```

The app maps this to `BuildConfig.sdk_auth_key`; never commit a real key.

```kotlin
val smKit = SMKit.Builder(applicationContext)
    .authKey(BuildConfig.sdk_auth_key)
    .isUI(false)
    .poseModelChoice(PoseModelChoice.AdaptiveChoice)
    .language(SMLanguage.English)
    .includeAssessmentInsights(false)
    .build()

smKit.smKitSessionListener(sessionListener)
smKit.configure(object : ConfigurationResult {
    override fun onSuccess() {
        smKit.preparePoseEstimation()
    }

    override fun onFailure() = Unit
    override fun onFailure(error: String) = Unit
})
```

SMKit must finish configuration before a session starts. Preparing pose estimation after configuration reduces the first-session warm-up.

## Camera and detection lifecycle

```kotlin
smKit.startSession(lifecycleOwner, previewView.surfaceProvider)

val sessionListener = object : SMKitSessionListener, SMKitGuidanceSuggestionListener {
    override fun handleGuidanceSuggestion(suggestion: SMKitGuidanceSuggestion) {
        // Offer guidance mode in the host application's UI.
    }

    override fun captureSessionDidSet(frameInfo: FrameInfo) = Unit
    override fun captureSessionDidStop() = Unit
    override fun handleDetectionData(movementData: SMKitMovementData?) = Unit
    override fun handlePositionData(poseData: Map<SMKitJoint, PointF>?) = Unit
    override fun handleSessionErrors() = Unit
}
smKit.smKitSessionListener(sessionListener)

val (romRange, exerciseType) = smKit.startDetection(
    exercise = "SquatRegular",
    configString = null,
    guidanceMode = null,
    guidanceSuggestionEnabled = true,
)

val exerciseResult = smKit.stopDetection()
val sessionResult = smKit.stopSession()
smKit.stopCamera()
```

Pass `guidanceMode = null` to let the SDK's defaults apply, `true` to force it on for a supported exercise, or `false` to disable it. The demo also shows `switchDetectionWithoutRecording` with the same typed options.

## Live data and callbacks

The workout screen demonstrates three complementary channels:

- `SMKitSessionListener` for frame info, positions, movement data, and errors.
- `observeBodyCalibrationData()` for in-frame calibration state.
- `observeAlgoPipeData()` for low-level pipeline telemetry.

Optional exercise-view monitoring is enabled with `setExerciseViewMonitoringEnabled`; its correction vocal key is emitted through `AlgoPipeResultData` and displayed by the demo.

## Runtime controls

The selector and workout screens exercise these calls directly:

```kotlin
smKit.setUseDefaultGuidanceMode(true)
smKit.setGuidanceModeSuggestionEnabled(true)
smKit.setGuidanceDebugLogging(BuildConfig.DEBUG)
smKit.resetGuidanceModeSuggestion()
smKit.rearmGuidanceModeSuggestion()

smKit.setAdaptiveRomEnabled(true)
smKit.setAdaptiveRomStart(0f)
val range = smKit.getExerciseRange()

smKit.setPhoneMoved(false)
val phonePosition = smKit.getCurrentPhonePosition()

smKit.setGuidanceVocalPlaying(false)
smKit.resetGuidanceMode()
smKit.endGuidanceMode()

smKit.setConfigString(null)
smKit.setFeedbacksToExclude(emptySet())
```

Resource preloading is also available through the suspending `downloadResources(resources: List<DownloadModel>)` call. The configured instance exposes `resourceDownloadWarning` when the host app needs to surface a resource warning.

## Test against an unpublished local SDK

The demo normally resolves artifacts from Sency Artifactory. SDK maintainers can test a local Maven publication without changing checked-in repository URLs:

```bash
./gradlew assembleDebug -PsmkitLocalRepo=/absolute/path/to/smkit_android/repo
```

The equivalent environment variable is `SMKIT_LOCAL_REPO`. The local repository is used only when the property or environment variable is supplied.

For support, contact [support@sency.ai](mailto:support@sency.ai).
