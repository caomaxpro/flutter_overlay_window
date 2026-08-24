<p align="center">
  <img src="https://github.com/X-SLAYER/flutter_overlay_window/assets/22800380/d22ae453-e83d-4da6-ba68-f4eaef666ef1" height="170" alt="flutter_overlay_window">
</p>

<p align="center">
  <a href="https://img.shields.io/badge/License-MIT-green">
    <img src="https://img.shields.io/badge/License-MIT-green" alt="MIT License">
  </a>
  <a href="https://pub.dev/packages/flutter_overlay_window">
    <img src="https://img.shields.io/pub/v/flutter_overlay_window.svg?label=pub&color=orange" alt="pub version">
  </a>
</p>

<p align="center">
Flutter plugin for displaying your Flutter app over other apps on the screen
</p>

---

> **Forked from** [X-SLAYER/flutter_overlay_window](https://github.com/X-SLAYER/flutter_overlay_window)
> Maintained by [caomaxpro](https://github.com/caomaxpro)

## Preview

| TrueCaller overlay example | click-through overlay example | Messanger chat-head example |
| :---: | :---: | :---: |
| <img src='https://user-images.githubusercontent.com/22800380/165636217-8957396b-dc54-4e6d-aa50-e8bfdb9383cf.gif' height='600' width='300' /> | <img src='https://user-images.githubusercontent.com/22800380/165636120-dcd9ee13-5fca-4f8a-a562-b2f53c0b5e24.gif' height='600' width='300'/> | <img src='https://user-images.githubusercontent.com/22800380/178730917-40f267bb-63a2-4ad3-ba69-f7c1285a1882.gif' height='600' width='300'/> |

## Installation

Add package to your pubspec:

```yaml
dependencies:
  flutter_overlay_window:
    git:
      url: https://github.com/caomaxpro/flutter_overlay_window.git
```

### Android

You'll need to add the `SYSTEM_ALERT_WINDOW` permission and `OverlayService` to your Android Manifest.
Replace `explanation_for_special_use` with your custom explanation.

```XML
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<application>
    ...
    <service android:name="flutter.overlay.window.flutter_overlay_window.OverlayService" 
        android:exported="false"
        android:foregroundServiceType="specialUse">
        <property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
            android:value="explanation_for_special_use"/>
    </service>
</application>
```

### Entry point

Inside `main.dart` create an entry point for your Overlay widget:

```dart
@pragma("vm:entry-point")
void overlayMain() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MaterialApp(
    debugShowCheckedModeBanner: false,
    home: Material(child: Text("My overlay")),
  ));
}
```

> **Important:** `WidgetsFlutterBinding.ensureInitialized()` is required inside the overlay entry point.

## Usage

### Basic

```dart
// Check if overlay permission is granted
final bool status = await FlutterOverlayWindow.isPermissionGranted();

// Request overlay permission
// Opens the overlay settings page, returns true once granted.
final bool status = await FlutterOverlayWindow.requestPermission();

// Show overlay
await FlutterOverlayWindow.showOverlay(
  height: WindowSize.fullCover,
  width: WindowSize.matchParent,
  alignment: OverlayAlignment.center,
  flag: OverlayFlag.focusPointer,
  overlayTitle: 'My App',
  overlayContent: 'Overlay active',
  enableDrag: false,
);

// Close overlay
await FlutterOverlayWindow.closeOverlay();

// Broadcast data to overlay (main → overlay)
await FlutterOverlayWindow.shareData(jsonEncode({'action': 'show', 'data': '...'}));

// Listen for messages from overlay (overlay → main)
FlutterOverlayWindow.overlayListener.listen((event) {
  log("Received: $event");
});

// Check if overlay is currently active
final bool active = await FlutterOverlayWindow.isActive();
```

### Fullscreen overlay (alarm, puzzle, etc.)

```dart
await FlutterOverlayWindow.showOverlay(
  height: WindowSize.fullCover,   // covers status bar + nav bar
  width: WindowSize.matchParent,
  alignment: OverlayAlignment.center,
  flag: OverlayFlag.focusPointer, // allows keyboard input
  enableDrag: false,
);
```

### Icon overlay (floating button)

```dart
final dpr = WidgetsBinding.instance.platformDispatcher.views.first.devicePixelRatio;
await FlutterOverlayWindow.showOverlay(
  height: (56 * dpr).toInt(),
  width: (56 * dpr).toInt(),
  flag: OverlayFlag.focusPointer,
  enableDrag: true,
  positionGravity: PositionGravity.auto,
  alignment: OverlayAlignment.topLeft,
  startPosition: const OverlayPosition(0, 60),
  overlayTitle: 'My App',
  overlayContent: 'Tap to control',
);

// Resize dynamically
await FlutterOverlayWindow.resizeOverlay(300, 400, true);

// Move overlay
await FlutterOverlayWindow.moveOverlay(OverlayPosition(100, 200));

// Get current position
final pos = await FlutterOverlayWindow.getOverlayPosition();

// Update flag
await FlutterOverlayWindow.updateFlag(OverlayFlag.clickThrough);
```

## Data communication

Use `shareData()` + `overlayListener` for bidirectional communication between main app and overlay.

### Main app → Overlay

```dart
await FlutterOverlayWindow.shareData(jsonEncode({'action': 'show', 'uuid': '123'}));
```

### Overlay → Main app

```dart
FlutterOverlayWindow.shareData(jsonEncode({'action': 'solved', 'uuid': '123'}));
```

### Recommended pattern (SharedPreferences fallback)

Since the overlay runs in a separate Flutter engine, there's a race condition between `showOverlay()` and the overlay's listener being ready. Use SharedPreferences as a fallback:

```dart
// Main app: persist BEFORE showing overlay
final prefs = await SharedPreferences.getInstance();
await prefs.setString('overlay_data', jsonEncode(alarmData));
await FlutterOverlayWindow.showOverlay(...);
await Future.delayed(Duration(milliseconds: 600));
await FlutterOverlayWindow.shareData(jsonEncode(alarmData));

// Overlay: read from SharedPreferences on startup
final prefs = await SharedPreferences.getInstance();
final raw = prefs.getString('overlay_data');
if (raw != null) {
  final data = jsonDecode(raw);
  // apply data...
}
```

## Enums

### OverlayFlag

| Flag | Description |
|------|-------------|
| `clickThrough` | Window never receives touch events (click-through overlay) |
| `defaultFlag` | Window won't get key input focus |
| `focusPointer` | Allows pointer events + keyboard input (use for text fields) |

### PositionGravity

| Gravity | Description |
|---------|-------------|
| `none` | Overlay can be positioned anywhere |
| `right` | Snaps to right side of screen |
| `left` | Snaps to left side of screen |
| `auto` | Snaps to nearest side based on position |

### NotificationVisibility

| Visibility | Description |
|------------|-------------|
| `visibilityPublic` | Show full notification on all lockscreens |
| `visibilitySecret` | Don't reveal on secure lockscreens |
| `visibilityPrivate` | Show notification but conceal sensitive info |

## License

MIT License - see [LICENSE](LICENSE)
