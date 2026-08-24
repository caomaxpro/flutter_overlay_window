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

Create an overlay entry point in your `main.dart`:

```dart
@pragma("vm:entry-point")
void overlayMain() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MaterialApp(
    debugShowCheckedModeBanner: false,
    home: OverlayApp(),
  ));
}
```

> **Important:** `WidgetsFlutterBinding.ensureInitialized()` is required inside the overlay entry point.

## Usage

### Overview

There are two sides to overlay communication:

1. **Main app** — uses `FlutterOverlayWindow` to show/close/send data
2. **Overlay widget** — a separate Flutter engine that renders on top of all apps

They communicate via `shareData()` (main → overlay) and `overlayListener` (overlay → main).

---

### OverlayManager (main app side)

Create an `OverlayManager` class to manage overlay lifecycle:

```dart
import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_overlay_window/flutter_overlay_window.dart';

class OverlayManager with WidgetsBindingObserver {
  StreamSubscription<dynamic>? _overlayListenerSubscription;
  bool _shouldShowOverlay = false;
  bool _closingOverlay = false;

  void init() {
    WidgetsBinding.instance.addObserver(this);
    _overlayListenerSubscription =
        FlutterOverlayWindow.overlayListener.listen((event) {
      _onOverlayEvent(event);
    });
  }

  void dispose() {
    _overlayListenerSubscription?.cancel();
    WidgetsBinding.instance.removeObserver(this);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.hidden) {
      _shouldShowOverlay = true;
      _syncOverlayVisibility();
    } else if (state == AppLifecycleState.detached) {
      unawaited(FlutterOverlayWindow.closeOverlay());
    } else if (state == AppLifecycleState.resumed) {
      _shouldShowOverlay = false;
      _closingOverlay = true;
      FlutterOverlayWindow.shareData(jsonEncode({'action': 'close_overlay'}));
      unawaited(FlutterOverlayWindow.closeOverlay());
      _closingOverlay = false;
    }
  }

  Future<void> _syncOverlayVisibility() async {
    if (_shouldShowOverlay) {
      await _showOverlay();
    } else {
      await FlutterOverlayWindow.closeOverlay();
    }
  }

  Future<void> _showOverlay() async {
    // 1. Check permission
    bool granted = await FlutterOverlayWindow.isPermissionGranted();
    if (!granted) {
      await FlutterOverlayWindow.requestPermission();
      granted = await FlutterOverlayWindow.isPermissionGranted();
      if (!granted) return;
    }

    // 2. Check if already active
    if (await FlutterOverlayWindow.isActive()) return;

    // 3. Persist data BEFORE showing overlay (race condition protection)
    await _persistData();

    // 4. Show overlay with icon size
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

    // 5. Wait for engine to initialize, then send data
    Future.delayed(const Duration(milliseconds: 400), () {
      FlutterOverlayWindow.shareData(jsonEncode({
        'action': 'show',
        'title': 'Now Playing',
        'artist': 'Artist Name',
      }));
    });
  }

  void _onOverlayEvent(dynamic event) {
    if (event is String) {
      try { event = jsonDecode(event); } catch (_) {}
    }
    if (event is! Map) return;

    final data = Map<String, dynamic>.from(event);
    final action = data['action'] as String?;

    if (action == 'solved') {
      FlutterOverlayWindow.closeOverlay();
    }
  }

  Future<void> _persistData() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('overlay_data', jsonEncode({
      'action': 'show',
      'title': 'Now Playing',
      'artist': 'Artist Name',
    }));
  }
}
```

---

### Overlay widget (overlay engine side)

```dart
import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_overlay_window/flutter_overlay_window.dart';
import 'package:shared_preferences/shared_preferences.dart';

class OverlayApp extends StatefulWidget {
  const OverlayApp({super.key});

  @override
  State<OverlayApp> createState() => _OverlayAppState();
}

class _OverlayAppState extends State<OverlayApp> {
  String _title = 'No data';
  bool _isActive = false;

  @override
  void initState() {
    super.initState();

    // Listen for data from main app
    FlutterOverlayWindow.overlayListener.listen(_onData);

    // 1. Read from SharedPreferences (fallback for race condition)
    Future.microtask(() async {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString('overlay_data');
      if (raw != null) {
        final data = _normalizePayload(jsonDecode(raw));
        if (data != null) _onData(data);
      }
    });

    // 2. Request fresh data from main app (backup)
    Future.delayed(const Duration(milliseconds: 150), () {
      FlutterOverlayWindow.shareData(jsonEncode({'action': 'request_state'}));
    });
  }

  /// Handle both String and Map payloads from overlayListener
  Map<String, dynamic>? _normalizePayload(dynamic raw) {
    if (raw == null) return null;

    if (raw is String) {
      try {
        final decoded = jsonDecode(raw);
        if (decoded is Map) return Map<String, dynamic>.from(decoded);
      } catch (_) {}
      return null;
    }

    if (raw is Map) return Map<String, dynamic>.from(raw);
    return null;
  }

  void _onData(dynamic raw) {
    final data = _normalizePayload(raw);
    if (data == null) return;

    final action = data['action'] as String?;
    if (action == 'show') {
      setState(() {
        _title = data['title'] ?? 'No title';
        _isActive = true;
      });
    } else if (action == 'close_overlay') {
      setState(() => _isActive = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!_isActive) return const SizedBox.shrink();
    return Material(
      child: Column(
        children: [
          Text(_title),
          ElevatedButton(
            onPressed: () {
              FlutterOverlayWindow.shareData(jsonEncode({'action': 'solved'}));
              FlutterOverlayWindow.closeOverlay();
            },
            child: const Text('Done'),
          ),
        ],
      ),
    );
  }
}
```

---

### Icon overlay (floating button)

Show a small draggable icon that expands on tap:

```dart
// Show 56x56 icon overlay
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

// Expand to full content
await FlutterOverlayWindow.resizeOverlay(300, 400, true);

// Move overlay
await FlutterOverlayWindow.moveOverlay(OverlayPosition(100, 200));

// Get current position
final pos = await FlutterOverlayWindow.getOverlayPosition();

// Update flag (e.g., make click-through)
await FlutterOverlayWindow.updateFlag(OverlayFlag.clickThrough);
```

---

### Fullscreen overlay (alarm, puzzle, etc.)

```dart
await FlutterOverlayWindow.showOverlay(
  height: WindowSize.fullCover,
  width: WindowSize.matchParent,
  alignment: OverlayAlignment.center,
  flag: OverlayFlag.focusPointer,
  enableDrag: false,
);
```

---

### Data communication

Always use `jsonEncode()` — never send raw Maps:

```dart
// Main app → overlay
await FlutterOverlayWindow.shareData(jsonEncode({'action': 'show', 'data': '...'}));

// Overlay → main app
FlutterOverlayWindow.shareData(jsonEncode({'action': 'solved'}));
```

---

### Race condition protection (SharedPreferences)

The overlay runs in a separate Flutter engine. There's a timing gap between `showOverlay()` and the overlay's listener being ready. Use SharedPreferences as a fallback:

```dart
// Main app: save BEFORE showing overlay
final prefs = await SharedPreferences.getInstance();
await prefs.setString('overlay_data', jsonEncode(stateData));
await FlutterOverlayWindow.showOverlay(...);
await Future.delayed(Duration(milliseconds: 400));
await FlutterOverlayWindow.shareData(jsonEncode(stateData));

// Overlay: read from SharedPreferences on init
final prefs = await SharedPreferences.getInstance();
final raw = prefs.getString('overlay_data');
if (raw != null) {
  final data = _normalizePayload(jsonDecode(raw));
  if (data != null) _onData(data);
}

// Then request fresh data as backup
Future.delayed(Duration(milliseconds: 150), () {
  FlutterOverlayWindow.shareData(jsonEncode({'action': 'request_state'}));
});
```

---

### _normalizePayload helper

The `overlayListener` delivers `dynamic` — it can be a `String`, `Map`, or nested structure. Always normalize:

```dart
Map<String, dynamic>? _normalizePayload(dynamic raw) {
  if (raw == null) return null;

  if (raw is String) {
    try {
      final decoded = jsonDecode(raw);
      if (decoded is Map) return Map<String, dynamic>.from(decoded);
    } catch (_) {}
    return null;
  }

  if (raw is Map) return Map<String, dynamic>.from(raw);
  return null;
}
```

---

### Lifecycle management

```dart
class OverlayManager with WidgetsBindingObserver {
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.hidden) {
      // App backgrounded → show overlay
      _showOverlay();
    } else if (state == AppLifecycleState.resumed) {
      // App foreground → close overlay
      FlutterOverlayWindow.shareData(jsonEncode({'action': 'close_overlay'}));
      FlutterOverlayWindow.closeOverlay();
    } else if (state == AppLifecycleState.detached) {
      FlutterOverlayWindow.closeOverlay();
    }
  }
}
```

---

### Guards against concurrent operations

```dart
bool _closingOverlay = false;

Future<void> _showOverlay() async {
  // Guard: don't show while closing
  if (_closingOverlay) return;
  // ... show logic
}

Future<void> _closeOverlay() async {
  _closingOverlay = true;
  try {
    await FlutterOverlayWindow.closeOverlay();
  } finally {
    _closingOverlay = false;
  }
}
```

---

### IPC with ReceivePort (for background services)

For apps that need overlay to communicate with background isolates (e.g., audio players):

```dart
import 'dart:isolate';
import 'dart:ui';

class OverlayManager {
  static ReceivePort? globalCommandPort;

  void init() {
    _registerGlobalCommandPort();
  }

  void _registerGlobalCommandPort() {
    if (globalCommandPort != null) return;
    globalCommandPort = ReceivePort();
    IsolateNameServer.registerPortWithName(
      globalCommandPort!.sendPort,
      'audio_commands',
    );
    globalCommandPort!.listen((msg) {
      // Handle command from background isolate
    });
  }

  void dispose() {
    if (globalCommandPort != null) {
      IsolateNameServer.removePortNameMapping('audio_commands');
      globalCommandPort!.close();
      globalCommandPort = null;
    }
  }
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
