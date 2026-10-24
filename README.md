# Coby Smart Remote

Package: `com.ejpapas.cobysmartremote` · Kotlin · Jetpack Compose · Material 3 · minSdk 26 · targetSdk 35

Status: **Stages 1-4 implemented, never run on hardware.** The source was written without an Android SDK, so it has not been compiled or tested. Expect to fix a few compile errors on first sync. Nothing in this project has been verified against your actual TV.

## Findings: which control method is realistic

### HONOR 400 (phone)
- Independent spec sources (Notebookcheck review, SmartPrix) list an **IR blaster** on the HONOR 400, and Honor's own spec page has an "Infrared Remote Control" entry.
- Whether third-party apps can use it is decided at runtime by `ConsumerIrManager.hasIrEmitter()`. Open **Diagnostics → Re-check IR** to see the real answer on your phone.
- IR is one-way. The phone gets no confirmation that the TV received anything.
- This app contains **no IR codes**. The Star Coby remote's codes aren't something I could verify, so you enter NEC codes (`ADDR:CMD` in hex) and the app marks a button usable only after you confirm the TV reacted. The phone has no IR receiver, so it cannot copy codes from your physical remote.

### The TV (Android 9, Hi3751V352F, 512 MB RAM, 4 GB storage)
- The About screen and top menu (SEARCH / HOME / APPS / VIDEO / MUSIC / SOURCE) show a custom launcher, not Google TV or certified Android TV. The Android TV Remote v2 protocol (ports 6466/6467, used by Google's own remote app) normally depends on Google's TV services and is **probably not present**. The port probe in Diagnostics will tell you.
- The one generic, documented control path on a plain Android 9 device is **ADB over the network** (TCP 5555), which injects real key events. It needs Developer options and network debugging switched on at the TV, plus an on-screen "Allow debugging?" approval for this phone. That is the normal authorization flow; nothing is bypassed.
- Many cheap rebrands hide or strip Developer options or network debugging. If yours does, Wi-Fi control isn't possible and IR is the fallback.
- Bluetooth: not implemented. There is no sign the TV exposes a remote-control Bluetooth service, and pairing alone doesn't create one.

### Plan used by the app
1. Wi-Fi (ADB) is the primary path. IR is the fallback, with user-supplied codes.
2. First verifiable command: **Volume up**. For ADB the app reads the TV's music volume before and after and only auto-verifies if the number actually changed. Otherwise it asks you to confirm what you saw on the TV.
3. "Sent" is never treated as "worked". The status line says so explicitly.

## Project layout
```
app/src/main/java/com/ejpapas/cobysmartremote/
  MainActivity.kt
  model/RemoteKey.kt          all buttons + Android key codes
  data/SettingsStore.kt       DataStore: IP, method, haptics, verified keys, IR profile
  net/AdbController.kt        ADB over TCP (dadb), key auth, keyevent, volume readback
  net/PortProbe.kt            fixed port list, one private IPv4 host only
  net/Discovery.kt            passive mDNS + SSDP listening (no scanning)
  ir/IrController.kt          ConsumerIrManager + NEC encoder (no built-in codes)
  ui/                         Theme, CobyApp (tabs), Home, Remote, Setup, Diagnostics, ViewModel
  util/DiagLog.kt, NetUtil.kt in-app log (no secrets), private-IP check
```

## Build a debug APK
Requirements: Android Studio (Koala or newer) with Android SDK 35 and JDK 17.

1. Open the `CobySmartRemote` folder in Android Studio and let Gradle sync. Studio will create the Gradle wrapper files it needs (`gradle-wrapper.properties` is included; the wrapper jar and scripts are not). It also needs internet access to fetch dependencies, including `dev.mobile:dadb:1.2.10`; if that version isn't found, pick the newest `dadb` on Maven Central.
2. **Build → Build Bundle(s) / APK(s) → Build APK(s)**, or run `./gradlew assembleDebug` after the wrapper exists.
3. Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Install on the HONOR 400
1. On the phone: Settings → About phone → tap the build number 7 times, then enable **USB debugging** under Developer options (or just copy the APK to the phone and open it).
2. With a cable and USB debugging: `adb install -r app-debug.apk`. Or open the copied APK from a file manager and allow "Install unknown apps" for that app.
3. Connect the phone to the **same Wi-Fi** as the TV.

## First run (do these in order)
1. **Diagnostics:** note whether IR is available. Tap **Probe ports** (uses the IP on Home) and see whether 5555 is open.
2. **TV:** unlock Developer options (Settings → About → select "Android Version" / build number repeatedly), enable USB/network debugging.
3. **Home:** confirm the IP (`192.168.100.227` is only a starting value; check your router if it fails), choose **Wi-Fi (ADB)**, tap **Connect**, and accept the prompt on the TV (tick "Always allow").
4. **Setup:** tap **Test** on *Volume up*. Watch the TV. Volume up unlocks the button on the Remote screen once confirmed.
5. Test the remaining buttons one at a time, then use the **Remote** tab.

## Testing checklist
Tick each only after you saw the TV react. Record notes for anything that doesn't.

| Button | Expected on TV | Worked? | Notes |
|---|---|---|---|
| Volume up | volume bar rises | | |
| Volume down | volume bar falls | | |
| Mute | sound mutes/unmutes | | |
| Power | TV sleeps/wakes (may not wake over Wi-Fi) | | |
| Home | launcher appears | | |
| Back | previous screen | | |
| Up / Down / Left / Right | focus moves in a menu | | |
| OK / Enter | highlighted item opens | | |
| Menu | menu opens | | |
| Settings | settings opens | | |
| Source / Input | input list appears | | |
| Channel up / down | channel changes (TV tuner source only) | | |
| Play / Pause | media app pauses/resumes | | |
| Rewind / Fast-forward | media app seeks | | |
| 0-9 | digits entered in channel/number field | | |

## Known limitations
- Power: `KEYCODE_POWER` may only put the screen to sleep, and a TV that is fully off usually won't accept ADB at all. IR is the realistic way to turn it on.
- Source and Settings use the standard Android codes (178, 176); this firmware may map them differently or ignore them.
- Some TVs reset the debugging setting after a reboot or firmware update.
- Passive discovery will often find nothing on a generic TV.
- A Wi-Fi TV address obtained by DHCP can change; set a reservation in your router.
- The ADB private key is stored in the app's private storage and is never logged or shown.
