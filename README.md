# SMKit Android Demo

> Local 1.9.4 release candidate; not yet publication or runtime sign-off.

## Validate the local candidate

Build the adjacent SDK's local Maven artifacts first, then run from this demo:

```sh
./gradlew :app:assembleDebug :app:assembleRelease -PsmkitLocalRepo=../smkit_android/repo
./gradlew :app:dependencyInsight --configuration releaseRuntimeClasspath --dependency com.sency -PsmkitLocalRepo=../smkit_android/repo
```

When this property is set, Sency modules and the CameraX compatibility artifact
resolve exclusively from that local repository; a missing artifact fails instead
of silently using a published SDK. Omit the property only after publication.
Default fbjni remains 0.7.0. Do not override to upstream 0.3.0 for a 16 KB release.
Validate final APK/AAB native dependencies and 16 KB runtime separately.

This application is the customer-facing reference for SMKit 1.9.4's low-level camera and movement-detection API. It deliberately uses SMKit directly, without SMKitUI, so customers can see the complete lifecycle and own all presentation.

## What the demo covers

- SDK construction, configuration, and pose-estimation preparation
- 2D camera session start/stop, device-agnostic wide-angle control, and camera release
- Exercise selection, typed detector start/switch/stop, and session results
- Default guidance mode and 1.9.4 guidance suggestions
- Guidance suggestion callbacks plus reset/rearm controls
- Guidance vocal coordination, reset, and end controls
- Adaptive ROM, exercise ROM range, and exercise type
- Phone-position and phone-movement APIs
- Exercise-view monitoring and raw algorithm-pipeline monitoring
- Body-calibration states, pose joints, movement data, feedback, and errors
- Feedback exclusions, SDK configuration JSON, and resource-download warning state
- Builder pose model, language, UI mode, auth key, and assessment-insight options

The main integration is in [`ActivityViewModel.kt`](app/src/main/java/com/example/smkitdemoapp/viewModels/ActivityViewModel.kt). The exercise selector drives configuration options, and the workout screen shows the SDK's live telemetry.

The demo uses SMKit Android 1.9.4's public live-camera pipeline.

## Requirements

- Android minSdk 24
- compileSdk/targetSdk 36
- Java 17
- Camera permission
- A Sency SDK key

## Install SMKit 1.9.4

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
    implementation "com.sency.smkit:smkit:1.9.4"
    implementation "com.sency.smbase.data:smbase-data:1.9.4"
    implementation "com.sency.smbase.nativeclient:smbase-native-client:1.9.4"
}
```

`smbase-data` supplies public types such as `RomRange`, `SMBaseExerciseType`, `DownloadModel`, and raw algorithm data. `smbase-native-client` supplies `FormFeedbackType`.

SMKit 1.9.4 is validated with CameraX 1.1.0, AppCompat 1.4.2, and Kotlin Coroutines 1.5.0. CameraX's managed APIs remain at 1.1.0; the SDK resolves `camera-core` to Sency's `1.1.0.1-sency16kb` compatibility artifact, which replaces only CameraX's native image-processing helper for Android 16 KB page-size support. This demo pins that exact compatibility graph so newer Lifecycle or other transitive requirements do not silently upgrade it. Navigation remains an application dependency and is not required by SMKit.

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
    .voiceFeedbackLanguage("en")
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

## Model and asset delivery

SMKit 1.9.4 obtains configuration and required models from the server and validates them before use. Keep the device online for initial configuration and for model/resource downloads that have not completed before. Previously downloaded valid server-derived cache entries may be reused when refresh is unavailable.

The low-level SDK also exposes explicit resource preloading:

```kotlin
lifecycleScope.launch {
    smKit.downloadResources(listOf(/* DownloadModel values */))
}
```

After configuration, check `resourceDownloadWarning` and surface it if the host application needs to tell the user that optional UI resources were unavailable.

## Camera and detection lifecycle

```kotlin
val useWideAngleCamera = true
smKit.setUseWideAngleCamera(useWideAngleCamera)
previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
previewView.scaleType = if (useWideAngleCamera) {
    PreviewView.ScaleType.FIT_CENTER
} else {
    PreviewView.ScaleType.FILL_CENTER
}
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

Use **Select Exercises → Wide-angle camera** to test the widest field of view supported by the current device. SMKit requests CameraX's reported minimum zoom ratio; a device limited to 1× stays at 1× without failing. The demo uses `FIT_CENTER` in wide mode so the complete 4:3 frame is visible, and its skeleton overlay uses the matching transform.

## Body calibration

Android publishes calibration as a flow:

```kotlin
lifecycleScope.launch {
    smKit.observeBodyCalibrationData().collect { state ->
        when (state) {
            is BodyInside -> renderGuide(state.rect, state.frameSize, inPosition = true)
            is BodyOutside -> renderGuide(state.rect, state.frameSize, inPosition = false)
            is Idle -> renderGuide(state.rect, state.frameSize, inPosition = false)
        }
    }
}
```

The demo assessment combines this SDK body-calibration flow with Android sensor-based phone-angle calibration, draws the returned guide rectangle, and allows calibration to be skipped.

## Demo assessment

The **Demo Assessment** flow uses public Android APIs to run Overhead Mobility, Squat Regular Overhead Static, Jefferson Curl, and right/left Standing Side Bend. During each timed exercise it displays calibration, countdown, live feedback, in-position state, rep count where applicable, and the returned ROM range. The final screen shows overall and per-exercise technique, peak ROM, time in position, and detected issues.

## Live data and callbacks

The workout screen demonstrates three complementary channels:

- `SMKitSessionListener` for frame info, positions, movement data, and errors.
- `observeBodyCalibrationData()` for in-frame calibration state.
- `observeAlgoPipeResultData()` for low-level pipeline telemetry.

Optional exercise-view monitoring is enabled with `setExerciseViewMonitoringEnabled`; its correction vocal key is emitted through `AlgoPipeResultData` and displayed by the demo.

The workout screen also displays every public field in Android's `SMKitMovementData` surface that is useful for integration diagnostics: perfect/shallow form, technique score, normalized and raw ROM, intent, position-entry correction, exercise-view correction, guidance step/progress/vocal state, phone movement, and rep completion.

### `SMKitMovementData`

| Property | Description |
|---|---|
| `didFinishMovement` | A dynamic rep completed, or the current movement completion condition fired. |
| `isInPosition` | Static/mobility/body-assessment in-position state. |
| `isPhoneMoved` | Phone-movement gate state. |
| `isShallowRep` | The completed dynamic rep was shallow. |
| `isPerfectForm` | No form correction is active for the current sample. |
| `techniqueScore` | Live normalized technique score. |
| `currentRomValue` | Normalized ROM value. |
| `currentRomRawValue` | Exercise-specific raw ROM value. |
| `feedback` | Typed `FormFeedbackType` corrections. |
| `guidanceStep`, `guidanceAdvanceProgress` | Current guidance phase and progress. |
| `isGuidanceModeActive` | Whether guidance mode is active. |
| `guidanceVocalKey`, `requestGuidanceVocalReplay` | Host audio-coordination information. |
| `exerciseViewCorrectionVocalKey` | Correction emitted by exercise-view monitoring. |
| `intent` | Current typed `ExerciseIntent`. |
| `positionEntryVocalFeedback` | Current regular-mode position-entry correction when supported. |

## Runtime controls

The selector and workout screens exercise these calls directly:

```kotlin
smKit.setUseDefaultGuidanceMode(true)
smKit.setGuidanceSuggestionEnabled(true)
smKit.setGuidanceDebugLogging(BuildConfig.DEBUG)
smKit.resetGuidanceSuggestionTracking()
smKit.rearmGuidanceSuggestionTrackingForCurrentExercise()

smKit.setAdaptiveRomEnabled(true)
smKit.setAdaptiveRomStart(0f)
val range = smKit.getExerciseRange()

smKit.setUseWideAngleCamera(true)

smKit.setPhoneMoved(false)
val phonePosition = smKit.getCurrentPhonePosition()

smKit.setGuidanceVocalPlaying(false)
smKit.resetGuidanceMode()
smKit.endGuidanceMode()

smKit.setConfigString(null)
smKit.setFeedbacksToExclude(emptySet())
```

Resource preloading is also available through the suspending `downloadResources(resources: List<DownloadModel>)` call. The configured instance exposes `resourceDownloadWarning` when the host app needs to surface a resource warning.

## Result models

`stopDetection()` returns `SMExerciseInfo`. Dynamic detection returns `Dynamic`, including performed reps, perfect reps, technique, and feedback counts. Static/mobility/body-assessment detection returns `Static`, including ROM range, time in position, peak normalized ROM, optional raw peak degrees, technique, and feedback counts.

`stopSession()` returns `DetectionSessionResultData?` with the session ID, recorded exercises, start/end times, total time, and total score. The demo renders the complete object as formatted JSON and offers a copy action.

## Android 1.9.4 coverage

The demo covers the public SMKit Android 1.9.4 live-camera lifecycle, device-agnostic wide angle, body calibration, pose joints, skeleton rendering, movement feedback, local assessment orchestration, ROM, guidance/default-policy checks, guidance suggestions and recovery controls, feedback exclusion, phone movement, raw pipeline data, model preparation, resource preloading/warnings, and typed result models.

For support, contact [support@sency.ai](mailto:support@sency.ai).
