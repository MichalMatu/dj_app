# DJ Gesture Deck

Android prototype for controlling DJ actions with phone camera hand gestures.

The app is based on MediaPipe Gesture Recognizer. Camera frames are analyzed on
device, mapped into DJ commands, and shown in the live `DJ Gesture Control`
status panel.

## Current Gesture Map

```text
Open_Palm still          -> Deck A Play/Pause
Open_Palm move left/right -> Crossfader left/right
Closed_Fist hold         -> Deck A Cue
Thumb_Up hold            -> Deck A Volume +
Thumb_Down hold          -> Deck A Volume -
Pointing_Up move up/down -> Deck A Filter +/-
Victory still            -> Crossfader Center
Victory move up/down     -> Deck A FX mix +/-
ILoveYou                 -> Deck A FX toggle
ILoveYou bottom hold     -> Reset Deck A controls
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
AndroidSystemAudioEngine -> physical Android media-volume adapter for Volume +/-
```

`Thumb_Up` and `Thumb_Down` currently update both the Deck A preview state and
the phone's `STREAM_MUSIC` media volume. Other DJ controls still update preview
state only until a real deck/effects engine is connected.

## Build

```bash
./gradlew :app:assembleDebug
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
