---
name: testing-manipulation-screener
description: Test the Manipulation Screener Android app end-to-end on an emulator. Use when verifying screener UI, API integration, or detection algorithm changes.
---

# Testing Manipulation Screener

## Prerequisites

### Android SDK Setup
The Android SDK must be installed with:
- `cmdline-tools/latest` (for sdkmanager)
- `platform-tools` (for adb)
- `platforms;android-34`
- `build-tools;34.0.0`
- `system-images;android-34;google_apis;x86_64`

Set environment:
```bash
export ANDROID_HOME=/home/ubuntu/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
```

### Emulator Setup
```bash
# Create AVD (one-time)
echo "no" | avdmanager create avd -n test_device -k "system-images;android-34;google_apis;x86_64" --device "pixel_6"

# KVM permissions (required on Ubuntu)
sudo gpasswd -a ubuntu kvm
sudo chmod 666 /dev/kvm

# Start emulator (headless, no window)
$ANDROID_HOME/emulator/emulator -avd test_device -no-window -no-audio -gpu swiftshader_indirect -no-snapshot &
adb wait-for-device
# Wait for boot
while [ "$(adb shell getprop sys.boot_completed 2>/dev/null)" != "1" ]; do sleep 2; done
```

### Build APK
```bash
cd /home/ubuntu/repos/Semasignalbot/ManipulationScreener
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Testing Approach

Since the emulator runs headless (`-no-window`), testing is done via `adb` commands and screenshots — **no screen recording**. Evidence is collected as PNG screenshots.

### Practical UI Tips
- Use Gate.io for emulator runs; Bybit often returns HTTP 403 from datacenter IPs.
- If a tap misses a small Compose chip or icon, use `uiautomator dump` to find the exact bounds first.
- The Android system may show a "System UI isn't responding" dialog on slow emulator boots; dismiss it with `adb shell input tap 540 1363` after confirming the "Wait" button position.
- For toggle-state checks in headless runs, capture screenshots and compare the target region programmatically (for example with Pillow) when text alone does not expose the state.

### Key Commands
```bash
# Launch app
adb shell am start -n com.sema.manipulationscreener/.MainActivity

# Take screenshot
adb exec-out screencap -p > screenshot.png

# Tap at coordinates (native resolution 1080x2400)
adb shell input tap X Y

# Scroll down
adb shell input swipe 540 1800 540 600 500

# Dismiss "System UI isn't responding" dialog (tap "Wait" button)
adb shell input tap 540 1363

# Get UI element bounds
adb shell uiautomator dump /sdcard/ui.xml
adb shell cat /sdcard/ui.xml | tr '>' '>\n' | grep "text="

# Extract text content
adb shell cat /sdcard/ui.xml | grep -oP 'text="[^"]*"' | grep -v 'text=""'

# Force stop app
adb shell am force-stop com.sema.manipulationscreener
```

## API Considerations

- **Bybit API** returns HTTP 403 from datacenter/cloud IPs. This is expected behavior — the app handles it with an error banner. On real Android phones, Bybit works normally.
- **Gate.io API** works from datacenter IPs and is the recommended exchange for emulator testing.
- To test with Gate.io: navigate to Settings → tap "Gate.io" chip → tap "ПРИМЕНИТЬ И СКАНИРОВАТЬ"

## Common UI Paths
- Market Map: bottom navigation → **Карта**
- List view: bottom navigation → **Список**
- Settings: bottom navigation → **Настройки**
- Market Map controls: sort icon, auto-refresh toggle, refresh button, interval chips (`5м/15м/30м/1ч/4ч`), limit chips (`9/16/25`)
- List view metrics: new price-range row (`Диап 5м`, `Диап 15м`, `Изм 5м`) appears above the existing high/base/turnover row

## Test Flow

1. Launch app → verify title "MANIPULATION SCREENER" and "Bybit" subtitle
2. Expect "Error: HTTP 403" from Bybit (proves error handling)
3. Navigate to Settings → switch to Gate.io → apply
4. Verify scanning progress ("Сканирование: X / Y")
5. Verify results ("Найдено: N монет") with coin cards showing pump%, drop%, charts
6. Use `uiautomator dump` to programmatically verify text content

## Common Issues

- **"System UI isn't responding" dialog**: Emulator is slow with swiftshader. Use `adb shell input tap 540 1363` to dismiss. May need to use `uiautomator dump` to find exact "Wait" button coordinates.
- **Emulator boot timeout**: Boot can take 30-60 seconds. Use `getprop sys.boot_completed` polling loop.
- **APK size**: Debug build is ~15MB (normal for Compose + Retrofit dependencies).
- **Gradle wrapper**: If missing, use system gradle to generate: `gradle wrapper --gradle-version 8.5`

## Devin Secrets Needed
None — the app uses public APIs only.

## Devin Secrets Needed
None — all APIs used are public (no authentication required).
