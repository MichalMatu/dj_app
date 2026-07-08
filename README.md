# DJ Gesture Deck

Android prototype for an ambient / DJ gesture controller driven by phone camera
hand gestures.

The app is based on MediaPipe Gesture Recognizer. Camera frames are analyzed on
device, mapped into DJ commands, and shown in the live `DJ Gesture Control`
status panel.

The current engine is a preview, non-audio state engine. It updates Deck A and
crossfader state so gesture behavior can be tested before a real mixer/audio
engine is connected.

Deck volume changes the preview Deck A volume state. It does not change the
phone's physical media volume by default. `AndroidSystemAudioEngine` can still
be configured to adjust system volume for demos, but that path is opt-in.

## Current Gesture Map

```text
Open_Palm still/no movement        -> Deck A Play/Pause
Open_Palm move left/right          -> Crossfader left/right
Closed_Fist hold                   -> Deck A Cue
Thumb_Up hold                      -> Deck A Volume +
Thumb_Down hold                    -> Deck A Volume -
Pointing_Up move up/down           -> Deck A Filter +/-
Victory still/no movement          -> Crossfader Center
Victory move up/down               -> Deck A FX mix +/-
ILoveYou top/middle short hold     -> Deck A FX toggle
ILoveYou bottom long hold          -> Reset Deck A controls
```

## Project Structure

```text
app/src/main/java/.../dj/       gesture interaction, mapping, controller, preview audio engine
app/src/main/java/.../fragment/ camera/gallery UI
app/download_tasks.gradle       MediaPipe model download
```

The DJ layer is split into:

```text
GestureInteractionEngine -> stable gesture, hold time, hand zones, movement delta
DjGestureConfiguration   -> editable gesture + condition + command table
DjGestureMapper          -> turns interactions into command events with cooldowns
DjGestureController      -> connects command events to the active engine
PreviewDjAudioEngine     -> current non-audio state engine, replaceable later
AndroidSystemAudioEngine -> optional physical Android media-volume adapter
```

TODO before publication: the namespace/applicationId still comes from the
MediaPipe sample and should be renamed when the prototype becomes a product app.

## Build

```bash
./gradlew :app:assembleDebug
```

## Unit Tests

```bash
./gradlew test
```

## Install on a connected phone

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant com.google.mediapipe.examples.gesturerecognizer android.permission.CAMERA
adb shell am start -n com.google.mediapipe.examples.gesturerecognizer/.MainActivity
```

## Test on a connected phone

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Next Steps

- Real audio engine
- Touch DJ console UI
- Presets JSON
- Two-hand control
- Gesture arm/clutch mode
