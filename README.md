# smkit-android-demo

# [smkit-android-demo](https://github.com/sency-ai/smkit-android-demo)

Demo app for **SMKit** (core SDK only; no SMKitUI). Aligned with the iOS demo: Welcome screen with Start 2D Session, Start 3D Session (stub), and Demo Assessment (fixed exercise list using SMKit only).

## Features
- **Start 2D Session**: Pick exercises, run a workout with camera and rep counting, view session result JSON.
- **Demo Assessment**: Runs a fixed list (OverheadMobility, SquatRegularOverheadStatic, JeffersonCurl, StandingSideBendRight, StandingSideBendLeft) with the same workout/result flow.
- **1.7.1 runtime controls**: Toggle default guidance mode, adaptive ROM, phone-movement gating, and switch exercises without recording the current attempt.
- **Guidance telemetry**: The workout screen displays guidance step, progress, vocal key, current phone position, ROM, and feedback data from `SMKitMovementData`.
- **Start 3D Session**: Placeholder (3D not yet available in the public SDK).

## Table of contents
1. [ Installation ](#inst)
2. [ Setup ](#setup)
3. [ Configure ](#conf)
4. [ Start ](#start)
5. [ Body calibration ](#body)
6. [ Data ](#data)

<a name="inst"></a>
## 1. Installation

### Gradle
Latest available version of the SMKit:

| Project | Version |
|---------|:-------:|
| smkit   |  1.7.1  |

### Version 1.7.1
- More resilient pose-estimation warmup when configure data is not ready yet.

In your project's dependencies source block please add our SDK artifactory endpoint
```groovy
repositories {
    google()
    mavenCentral()
    maven {
        url "https://artifacts.sency.ai/artifactory/release"
    }
}
```

In your app's `build.gradle` android block insert packagingOptions block 
```groovy
packagingOptions {
    pickFirst '**/*.so'
}
```

And Finally import `smkit` to your project
```groovy
dependencies {
    implementation "com.sency.smkit:smkit:$latest_version"
}
```

<a name="setup"></a>
## 2. Setup

### SDK auth key (required to run the app)
Create a `local.properties` file in the project root (if it does not exist) and add your SDK key:
```properties
sdk_auth_key=your_sency_sdk_key_here
```
The app reads this via `BuildConfig.sdk_auth_key`. Do not commit `local.properties` with a real key.

### Camera
Add camera permission and camera feature to `AndroidManifest.xml`
```xml
<uses-permission android:name="android.permission.CAMERA" />

<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

<a name="conf"></a>
## 3. Configure
```kotlin
val smKit = SMKit.Builder(context)
    .authKey(BuildConfig.sdk_auth_key)  // from local.properties
    .build()

smKit.configure(object: ConfigurationResult {
    override fun onFailure() {
        // Configuration Failed
    }

    override fun onFailure(error: String) {
        // Configuration Failed with a detailed error message
    }

    override fun onSuccess() {
        // Configuration Successed
    }
})
```
To reduce wait time we recommend to call `configure` on app launch.

**⚠️ SMKit will not work if you don't first call configure.**

<a name="start"></a>
## 4. Start
### Start exercise detection

Implement **SMKitSessionListener** and inject it into `SMKit`
```kotlin
smKit?.smKitSessionListener(object : SMKitSessionListener {
    // When session initialized this function will be called with FrameInfo
    // FrameInfo holds data about camera's properties.
    override fun captureSessionDidSet(frameInfo: FrameInfo) {}
    
    // When session is stopped this function will be called.
    override fun captureSessionDidStop() {}
    
    // This function will be called when SMKit detects movementData.
    override fun handleDetectionData(movementData: SMKitMovementData?) {}
    
    // This function will be called with the user joints location.
    // Please notice the locations of joint are relative to video resolution.
    // FrameInfo object holds the video resolution data.
    // The skeleton model (standard 28 joints vs extended) is chosen automatically; see SMKitJoint below to detect and branch.
    override fun handlePositionData(poseData: Map<SMKitJoint, PointF>?) {}
    
    // This function will be called if any error occured.
    override fun handleSessionErrors() {}
})
```
Now we can start the exercise.

```kotlin
var smKit: SMKit? = null

// First you will need to start the session.
// Please provide lifecycleOwner and surfaceProvider in order for SMKit to attach a Camera
fun startSession(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
    smKit?.startSession(lifecycleOwner, surfaceProvider)
}

//Then call startDetection to start the exercise detection.
fun startDetection() {
    try{
        val exerciseName: String = exerciseName() 
        val guidanceMode = if (smKit?.exerciseHasDefaultGuidanceMode(exerciseName) == true) true else null
        smKit?.startDetection(exerciseName, configString = null, guidanceMode = guidanceMode)
    } catch (e: IllegalStateException) {
        stopSession()
    }
}

//When you are ready to stop the exercise call stopDetection.
fun stopDetection() {
    val exerciseInfo: SMExerciseInfo = smKit?.stopDetection()
    Log.d(TAG, "Exercise Results: $exerciseInfo")
}

//When you are ready to stop the session call stopSession.
fun stopSession() {
    val results: DetectionSessionResultData = smKit?.stopSession()
    Log.d(TAG, "Session Results: $results")
}
```

### 4.1 Runtime controls added in 1.6.5+

These APIs can be set before detection starts. Some of them can also be updated while detection is running.

```kotlin
// Built-in guidance mode for supported exercises.
smKit.setUseDefaultGuidanceMode(true)
smKit.setGuidanceDebugLogging(BuildConfig.DEBUG)
val guidanceMode = if (smKit.exerciseHasDefaultGuidanceMode(exerciseName)) true else null
val (romRange, exerciseType) = smKit.startDetection(exerciseName, null, guidanceMode)

// Adaptive ROM feedback.
smKit.setAdaptiveRomEnabled(true)
smKit.setAdaptiveRomStart(0f)
val activeRange = smKit.getExerciseRange()

// Phone movement gating. Pass true while the hosting app detects that the phone moved.
smKit.setPhoneMoved(isPhoneMoved = true)
val phonePosition = smKit.getCurrentPhonePosition()

// Switch to another exercise without saving the current exercise result.
smKit.switchDetectionWithoutRecording(nextExercise, configString = null, guidanceMode = guidanceMode)

// Guidance audio coordination for apps that play guidance vocals themselves.
smKit.setGuidanceVocalPlaying(true)
smKit.endGuidanceMode()
smKit.resetGuidanceMode()
```

The demo app exposes these controls in the exercise-selection and workout screens.

## 5. Body calibration <a name="models"></a>

### Body calibration
*Body calibration* is used to get information about the users' location during the session.
You can observe BodyCalibration's state from SMKit

#### Observe **BodyCalibrationState**

```kotlin
smKit.observeBodyCalibrationData().onEachLaunch { state: BodyCalibrationState ->
    when (state) {
        is Idle -> {
            // BodyCalibration is ready for interaction
        }
        is BodyOutside -> {
            // Major Character in Frame is outside the Frame
        }
        is BodyInside ->  {
            // Major Character in Frame is inside the Frame
            // Body Calibration worked 
        }
    }
}
```

## 6. Available Data Types <a name="data"></a>

#### `SMKitSessionSettings`
| Type                | Format                                                       | Description                                                                                                  |
|---------------------|--------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| phonePosition       | `PhonePositionMode?`                                         | The session PhonePositionMode.                                                                              |
| jumpRefPoint        | `String?`                                                    | The session jumpRefPoint                                                                                    |
| isInPosition        | `jumpHeightThreshold?`                                       | The session jumpHeightThreshold                                                                             |
| userHeight        | `jumpHeightThreshold?`                                         | The session userHeight                                                                                      |

#### `MovementFeedbackData`
| Type                | Format                                                       | Description                                                                                                  |
|---------------------|--------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| didFinishMovement   | `Bool?`                                                      | Will be true when the user finish a dynamic movment.                                                         |
| isShallowRep        | `Bool?`                                                      | Will be true when the user finish a shallow dynamic movment.                                                 |
| isInPosition        | `Bool?`                                                      | Will be true if the user is in the currect position                                                          |
| isPerfectForm       | `Bool?`                                                      | Will be true if the user did not have any mistakes                                                           |
| techniqueScore      | `Float?`                                                     | The score representing the user's technique during the exercise.                                             |
| detectionConfidence | `Float?`                                                     | The confidence score                                                                                         |
| feedback            | `[FormFeedbackTypeBr]?`                                      | Array of feedback of the user movment.                                                                       |
| currentRomValue     | `Float?`                                                     | The current Range Of Motion of the user.                                                                     |
| guidanceStep        | `GuidanceStep`                                               | Current guidance phase: `None`, `Orient`, `Setup`, `Prepare`, `Action`, or `Hold`.                           |
| guidanceAdvanceProgress | `Float`                                                  | Progress toward advancing the current guidance step, from 0 to 1.                                            |
| isGuidanceModeActive | `Bool`                                                      | True while the current detection is running in guidance mode.                                                |
| guidanceVocalKey    | `String?`                                                    | Vocal asset key requested by guidance mode.                                                                  |
| requestGuidanceVocalReplay | `Bool`                                                | True when the current guidance vocal should be replayed.                                                     |
| specialParams       | `[String:Float?]`                                            | Some dynamic exercises will have some special params for example the exercise "Jumps" has "JumpPeakHeight" and "currHeight". |

#### `GuidanceStep`
| Type    | Description                         |
|---------|-------------------------------------|
| None    | Guidance is inactive.               |
| Orient  | User orientation / camera view.     |
| Setup   | Stance or setup phase.              |
| Prepare | Pre-movement preparation.           |
| Action  | Main movement instruction.          |
| Hold    | Hold / terminal success phase.      |

#### `SMExerciseInfo`
| Type                | Format                                                       | Description                                                                                                  |
|---------------------|--------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| sessionId           | `String`                                                     | The identifier for the session in which the exercise was performed.                                          |
| startTime           | `String`                                                     | The start time of the exercise session in "YYYY-MM-dd HH:mm:ss.SSSZ" format.                                 |
| endTime             | `String`                                                     | The end time of the exercise session in "YYYY-MM-dd HH:mm:ss.SSSZ" format.                                   |
| totalTime           | `Double`                                                     | The total time taken for the exercise session in seconds.                                                    |

#### `SMExerciseStaticInfo` type of `SMExerciseInfo`
| Type                   | Format                                                       | Description                                                                                                  |
|------------------------|--------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| sessionId              | `String`                                                     | The identifier for the session in which the exercise was performed.                                          |
| startTime              | `String`                                                     | The start time of the exercise session in "YYYY-MM-dd HH:mm:ss.SSSZ" format.                                 |
| endTime                | `String`                                                     | The end time of the exercise session in "YYYY-MM-dd HH:mm:ss.SSSZ" format.                                   |
| totalTime              | `Double`                                                     | The total time taken for the exercise session in seconds.                                                    |
| timeInActiveZone       | `Double`                                                     | The time the user was in position.                                                                           |
| positionTechniqueScore | `Double`                                                     | The user score.                                                                                              |
| inPosition             | `[StaticData]`                                               | Array of static data.                                                                                        |


#### `StaticData`
| Type                     | Format                                                       | Description                                                                                                  |
|--------------------------|--------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| detectionStartTime       | `String`                                                     | The start time of the detection.                                                                             |
| detectionEndTime         | `String`                                                     | The end time of detection.                                                                                   |
| detectionConfidenceScore | `Float`                                                      | The Confidence in the detection.                                                                             |
| inGreenZone              | `Bool`                                                       | Will be true if the user is in the success zone.                                                             |
| romScore                 | `Float`                                                      | The ROM score.                                                                                               |
| techniqueScore           | `Float`                                                      | The user technic score.                                                                                      |
| inPosition               | `Bool`                                                       | Will be true if the user in position.                                                                        |
| isGood                   | `Bool`                                                       | Is good detection                                                                                            |
| feedback                 | `[FormFeedbackTypeBr]?`                                      | Array of feedback of the user movment.                                                                       |

#### `SMExerciseDynamicInfo` type of `SMExerciseInfo`
| Type                   | Format                                                       | Description                                                                                                  |
|------------------------|--------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| sessionId              | `String`                                                     | The identifier for the session in which the exercise was performed.                                          |
| startTime              | `String`                                                     | The start time of the exercise session in "YYYY-MM-dd HH:mm:ss.SSSZ" format.                                 |
| endTime                | `String`                                                     | The end time of the exercise session in "YYYY-MM-dd HH:mm:ss.SSSZ" format.                                   |
| totalTime              | `Double`                                                     | The total time taken for the exercise session in seconds.                                                    |
| performedReps          | `[RepData]`                                                  | Array of RepData.                                                                                            |
| numberOfPerformedReps  | `Int?`                                                       | The number of times the user repeated the exercise.                                                          |
| repsTechniqueScore     | `[Double]`                                                   | The exercise score.                                                                                          |

#### `RepData`
| Type                     | Format                                                       | Description                                                                                                  |
|--------------------------|--------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| detectionStartTime       | `String`                                                     | The start time of the detection.                                                                             |
| detectionEndTime         | `String`                                                     | The end time of detection.                                                                                   |
| detectionConfidenceScore | `Float`                                                      | The Confidence in the detection.                                                                             |
| isShallowRep             | `Bool`                                                       | Will be true if the Rep is shallow                                                                           |
| romScore                 | `Float`                                                      | The ROM score.                                                                                               |
| techniqueScore           | `Float`                                                      | The user technic score.                                                                                      |
| isGood                   | `Bool`                                                       | Is good detection                                                                                            |
| feedback                 | `[FormFeedbackTypeBr]?`                                      | Array of feedback of the user movment.                                                                       |

#### `DetectionSessionResultData`
| Type                | Format                                                       | Description                                                                                                  |
|---------------------|--------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| exercises           | `[SMExerciseInfo]`                                           | Array of all the exerxises.                                                                                  |
| startTime           | `String`                                                     | The start time of the session session in "YYYY-MM-dd HH:mm:ss.SSSZ" format.                                 |
| endTime             | `String`                                                     | The end time of the session session in "YYYY-MM-dd HH:mm:ss.SSSZ" format.                                   |

#### `SMKitJoint` (skeleton model)

The SDK chooses the skeleton model automatically based on device capability. Your app receives `handlePositionData(poseData: Map<SMKitJoint, PointF>?)`; you can detect which model is in use by checking which keys are present in `poseData`, then branch with a `when` (or switch) on the result.

**Full pose — Standard (28 joints)** — used on newer devices. Complete set of joints in order:

Head, REye, LEye, LEar, REar, Nose, Neck, RShoulder, RElbow, RWrist, LShoulder, LElbow, LWrist, UpperSpine, MiddleSpine1, Hip, RHip, RKnee, RAnkle, RHeel, RBigToe, RSmallToe, LHip, LKnee, LAnkle, LHeel, LBigToe, LSmallToe.

**Full pose — Extended (33 joints)** — used on older devices. Complete set of joints in order:

Nose, LEyeInner, LEye, LEyeOuter, REyeInner, REye, REyeOuter, LEar, REar, LMouth, RMouth, LShoulder, RShoulder, LElbow, RElbow, LWrist, RWrist, LPinky, RPinky, LIndex, RIndex, LThumb, RThumb, LHip, RHip, LKnee, RKnee, LAnkle, RAnkle, LHeel, RHeel, LBigToe, RBigToe.

In code, detect the skeleton type (e.g. extended if any of the extra joints appear in `poseData`) and use a `when (skeletonType)` to handle drawing or processing for each case.

### `ExerciseType` <a name ="ExerciseType)"></a>
| Type                |
|---------------------|
| Dynamic             |
| Static              |
| BodyAssessment      |
| Mobility            |
| Other               |

Having issues? [Contact us](mailto:support@sency.ai) and let us know what the problem is.
