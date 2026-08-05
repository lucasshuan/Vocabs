# Vocabu

Captures words and phrases the moment they show up — gaming, reading, watching — and
turns each into an AI-generated card. Data stays on the device; the server only brokers
the AI call. Interface in English and Brazilian Portuguese.

## Requirements

**Android Studio** — brings the JDK and the Android SDK. Nothing else is needed.

## Setup

**1. Point Gradle at the JDK and the SDK.** For the current terminal only:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
```

To keep them across terminals, set them once in the user environment — through
Windows' "Edit environment variables for your account", or:

```powershell
[Environment]::SetEnvironmentVariable('JAVA_HOME', "C:\Program Files\Android\Android Studio\jbr", 'User')
[Environment]::SetEnvironmentVariable('ANDROID_HOME', "$env:LOCALAPPDATA\Android\Sdk", 'User')
# open a new terminal afterwards
```

Paths differ per machine; check yours if Gradle says `JAVA_HOME is not set`.

**2. Create the `.env`** (git-ignored) and paste the Anthropic key:

```powershell
Copy-Item .env.example .env
```

### Environment variables

Read from `.env` at the repository root. An actual environment variable of the same
name wins, which is how CI overrides without the file.

| Name | Needed by | Required | Default |
|---|---|---|---|
| `ANTHROPIC_API_KEY` | server | yes | — |
| `APP_TOKEN` | server + app | yes | — |
| `PORT` | server | no | 8080 |
| `MODEL` | server | no | `claude-opus-5` |
| `SERVER_LAN` | app build | no | this machine's LAN address, detected |

`APP_TOKEN` is read by the app at build time, so both sides always match.
`MODEL` and `PORT` also take a per-run override that leaves `.env` alone:
`.\gradlew.bat :server:run -PMODEL=claude-haiku-4-5`.

## Running

Three terminals, in order. The first two stay occupied.

```powershell
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd vocabs   # 1. emulator
.\gradlew.bat :server:run                                 # 2. backend on :8080
.\gradlew.bat :androidApp:installDebug                    # 3. build + install
```

Step 1 can also be Android Studio → **Device Manager** → ▶. `installDebug` installs on
every connected device without opening the app — the icon appears in the drawer.

**Physical phone:** enable "USB debugging", connect by cable, accept the popup. Same
Wi-Fi as the PC, port 8080 open in the firewall. No address to configure — the build
detects it.

## Building

| Command | Output | Installs? |
|---|---|---|
| `assembleDebug` | `androidApp\build\outputs\apk\debug\` | Yes, anywhere — shareable today |
| `assembleRelease` | `...\apk\release\` (unsigned) | No, until signed |
| `bundleRelease` | `...\bundle\release\` (`.aab`) | Play Store format, also needs signing |

Debug uses the local debug key — installs anywhere, refused by the Play Store. Release
has no `signingConfig` on purpose: a release key is a secret that should not enter the
repository. Sign with `keytool` + `apksigner` (in `$env:ANDROID_HOME\build-tools\`).

> Release ships **unminified**. Enabling minification without ProGuard rules breaks
> `kotlinx.serialization` silently, and only in production.

## Common problems

| Symptom | Cause |
|---|---|
| `JAVA_HOME is not set` | Step 1 of Setup missing in this terminal. |
| `No connected devices!` | No emulator or phone. Check `adb devices`; `offline` means still booting. |
| `INSTALL_FAILED_USER_RESTRICTED` | Manufacturer lock. On MIUI: Developer options → enable "Install via USB". |
| Mangled accents in the console | Legacy code page; `chcp 65001` fixes the session. |
| Old app still installed after the package rename | `adb uninstall com.jean.vocabs` — Android sees the new package as a different app. |

## Documentation

| File | What it holds |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Modules, the path of one capture, technical decisions |
| [ROADMAP.md](ROADMAP.md) | Phases, scope and exit criteria |
| [NOTES.md](NOTES.md) | What is unproven, what is crooked, loose ideas |
| [docs/PRODUCT.md](docs/PRODUCT.md) | Vision, principles, core loop, retention, monetisation |
| [docs/EXERCISES-AND-METRICS.md](docs/EXERCISES-AND-METRICS.md) | Minigame catalogue and the metrics dashboard |
| [docs/THIRD-PARTY.md](docs/THIRD-PARTY.md) | Dependencies and what each one is for |
