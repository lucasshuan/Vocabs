# Vocabu

Vocabu is a vocabulary app that captures words and phrases at the moment they show up —
while gaming, reading, watching — and turns each capture into an AI-generated card.

The data stays on the device. The server only brokers the AI call.

**State:** the Vocabu identity and the local-first flow are in. One capture can produce several
cards; photo uses local OCR, audio attempts local transcription on Android 13+, and both keep
manual editing. Review is a typed cloze, the profile shows 84 days of activity, and export produces
a versioned ZIP with JSON and media. The interface ships in English and Brazilian Portuguese, and
follows the device unless you pick a language in Settings.

## Prerequisites

Android Studio (it brings the JDK and the Android SDK). `JAVA_HOME` and `ANDROID_HOME` are already
set on this machine — if `gradlew` complains about `JAVA_HOME is not set`, this is what is missing:

```powershell
[Environment]::SetEnvironmentVariable('JAVA_HOME', "C:\Program Files\Android\Android Studio\jbr", 'User')
[Environment]::SetEnvironmentVariable('ANDROID_HOME', "$env:LOCALAPPDATA\Android\Sdk", 'User')
# open a new terminal afterwards
```

Secrets live in `.env` at the root (git-ignored, template in `.env.example`). Paste the Anthropic
key and you are done — no retyping in every new terminal:

```
ANTHROPIC_API_KEY=sk-ant-...
APP_TOKEN=local-test-token
```

Environment variables, when set, take precedence over the file — that is how CI and production
override without depending on it.

## Running

Three PowerShell tabs, in this order. The first two hold the terminal; the third runs and gives
the prompt back.

**1. Emulator.** `emulator` and `adb` do not join the PATH along with `ANDROID_HOME`, so they go by
full path. `vocabs` is the AVD already created on this machine:

```powershell
& "$env:ANDROID_HOME\emulator\emulator.exe" -list-avds    # see what exists
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd vocabs
```

Without a terminal it comes to the same thing: Android Studio → **Device Manager** → ▶ on `vocabs`.

**2. Server.**

```powershell
.\gradlew.bat :server:run               # backend on localhost:8080
```

**3. Build and install.**

```powershell
.\gradlew.bat :androidApp:installDebug
```

`installDebug` builds the debug APK and **installs it on every connected device** — emulator,
physical phone over USB, or both. It does not start an emulator and does not open the app: the
Vocabu icon appears in the drawer.

For a **physical phone**: enable "USB debugging" in Developer options, connect by cable and accept
the authorisation popup on the device screen. `adb devices -l` then lists the phone alongside any
running emulator.

**No address to configure.** The app works out where the server is: on the emulator it uses
`10.0.2.2` (your machine's localhost as seen from inside it) and on a physical phone it uses this
machine's address on the local network, detected at build time. The token comes from the same
`.env` the server reads, so the two sides always match. If the detection guesses wrong — several
network adapters, server on another machine — set `SERVER_LAN` in `.env` and rebuild. For a
physical phone the two devices have to be on the same Wi-Fi, and you may need to open port 8080 in
the Windows firewall.

### Common problems

**`installDebug` fails with `No connected devices!`** — that is step 1 missing, the cause of very
nearly every failure of it. Check with `& "$env:ANDROID_HOME\platform-tools\adb.exe" devices`:
there has to be a line ending in `device`. An empty list means no device; `offline` means the
emulator is still starting, so wait and repeat.

**`INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`** on a physical phone is a
manufacturer lock, not Gradle. On Xiaomi/MIUI: Settings → Additional settings → Developer options →
enable **"Install via USB"**. If the toggle is greyed out, MIUI requires a signed-in Mi account and
an active internet connection at the moment you turn it on.

**Mangled accents in the console** (`Vari├ível`) — the terminal is on a legacy code page;
`chcp 65001` fixes it for the session.

## Building the APK

`installDebug` is for developing, but it produces nothing you can send to anyone. For that there
are two APKs, and the difference between them is the signature — not the contents.

**Debug: the file you can share today.**

```powershell
.\gradlew.bat :androidApp:assembleDebug
# androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

It comes signed with the debug key Android Studio generates on its own
(`~\.android\debug.keystore`), so it **installs on any device** — send the file and open it, with
"install from unknown sources" allowed. What it is no good for is publishing: the debug key is the
same on every machine in the world and the Play Store refuses it.

**Release: needs a key of your own.**

```powershell
.\gradlew.bat :androidApp:assembleRelease
# androidApp\build\outputs\apk\release\androidApp-release-unsigned.apk
```

The `-unsigned` in the name is not a detail: **this file installs nowhere** until it is signed. The
project has no `signingConfig` in [androidApp/build.gradle.kts](androidApp/build.gradle.kts), on
purpose — a release key is a secret that should not enter the repository.

Create the key once (keep the password; **losing this key means never being able to update the
published app**):

```powershell
& "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -v `
  -keystore $HOME\Vocabu-release.jks -alias Vocabu `
  -keyalg RSA -keysize 2048 -validity 10000
```

And sign it — `apksigner` lives in build-tools, which also does not join the PATH (swap `37.0.0`
for the installed version, visible in `$env:ANDROID_HOME\build-tools`):

```powershell
& "$env:ANDROID_HOME\build-tools\37.0.0\apksigner.bat" sign `
  --ks $HOME\Vocabu-release.jks --ks-key-alias Vocabu `
  --out Vocabu.apk `
  androidApp\build\outputs\apk\release\androidApp-release-unsigned.apk
```

Automating this means pointing a `signingConfig` at the same `.jks`, reading the passwords from
`.env` the way `ANTHROPIC_API_KEY` already is — then `assembleRelease` comes out signed by itself.
For the Play Store the format is `bundleRelease` (`.aab`), not APK.

> `assembleRelease` currently comes out **without minification** (`isMinifyEnabled = false`): the
> APK is larger and the code ships readable. That is the right call while no ProGuard rules are
> written — turning minification on without them breaks `kotlinx.serialization` silently, and the
> app only fails in production.

## Documentation

| File | What it holds |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Modules, the path of one capture, technical decisions |
| [ROADMAP.md](ROADMAP.md) | Phases, scope and exit criteria |
| [NOTES.md](NOTES.md) | What is unproven, what is crooked, loose ideas |
| [docs/PRODUCT.md](docs/PRODUCT.md) | Vision, principles, core loop, retention, monetisation |
| [docs/EXERCISES-AND-METRICS.md](docs/EXERCISES-AND-METRICS.md) | Minigame catalogue and the metrics dashboard |
| [docs/THIRD-PARTY.md](docs/THIRD-PARTY.md) | Dependencies and what each one is for |
