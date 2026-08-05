# Vocabu architecture

_Updated: 2026-08-05_

```text
:contracts   DTOs + language catalogue. android() -> :shared, jvm() -> :server
:shared      domain, SQLDelight, retention, HTTP client
:androidApp  Compose, capture, media, export
:server      Ktor endpoint, the AI call
```

## General

### Names that are not brand

`applicationId` and `vocabs.db` are storage identifiers. Renaming either migrates
nothing — it creates an empty place beside what exists. Both were changed during
the English rewrite because the data was being wiped; **that window is closed**.
From here each changes only with a migration that reads the old one, and
`applicationId` not even then: once published it is permanent.

`vocabs.db` keeps the old name deliberately — the app is Vocabu, `vocabs` is what
it was called first. Renaming buys nothing and costs a migration. Do not "fix" it.

Routes were never on this list; nothing persists them.

### Data model

```text
capture (context, format, media, transcription, language pair)
   ├── entry (selected range, type, card, retention)
   └── entry (another range, overlapping included)
```

The pair lives on the **capture**: a snippet is in one language, so every
selection inside it inherits that language. One word is `WORD`, two or more
contiguous tokens are `PHRASE` — decided on the device, reinjected by the server,
never classified remotely.

### Enrolled languages

You enrol in target languages. Each one has a summary — total, mastered, queued —
built from the cards whose capture carries that target.

**Grouping is by target alone. The native language belongs to the individual
card. Reads filter by target; writes record the pair.**

Nothing enforces this. Keying a read by the whole pair made switching native
language stop matching every stored card — totals read zero, lists came back
empty, nothing deleted, no way to tell from the screen.

The code still says `Course` (`CourseSummary`, `Scope.ActiveCourse`,
`enrolledCourses`) — a stray metaphor, not a concept. Read it as "enrolled
language". Renaming is free whenever someone wants it.

### Interface language

Two independent settings: **App language** (buttons and menus, defaults to the
device) and **Language I speak** (what cards are written in). Changing the second
is a context switch, so it asks first.

Two rules, neither compiler-enforced:

- **Nothing below `:androidApp` produces display text.** A domain module has no
  resources, so a sentence decided there exists in one language forever. This is
  why the server sends an error *code*.
- **Read resources through `LocalResources`, never `LocalContext`.** The latter
  does not invalidate on configuration change, so text goes stale after a switch.
  Lint catches it (`LocalContextGetResourceValueCall`), as an error.

Locales come from `LocalConfiguration`, never `Locale.getDefault()` — that follows
the device, not the in-app picker.

### Comments

Caveman. Keep only what the code cannot show: an external limit, a rejected
alternative, an expiry date. Delete anything restating a name.

```kotlin
// Bad — says what the next line says.
// Sorts the languages by name.
languages.sortedBy { it.name }

// Good — a fact from outside this file.
// Haiku 4.5 rejects `effort` with a 400.
if (supportsEffort) effort(LOW)
```

Never invent a constraint. A rule without an expiry becomes permanent by
accident: write "off during the schema rewrite", not "off".

### States

| Type | Values |
|---|---|
| `Capture` | `TRANSCRIBING` · `AWAITING_SELECTION` · `PROCESSED` |
| `Entry` | `PENDING` · `GENERATING` · `READY` · `ERROR` |

Pending shows both queues without mixing them: transcription belongs to the
capture, generation to the entry. Nothing is discarded unasked — a lost
connection leaves the capture in `AWAITING_SELECTION`, ready to resume.

## Contracts

`:contracts` — wire DTOs and the 43-language catalogue. Shared because both sides need the same
list for different reasons: the app shows a flag and a name from its own string
resources, the prompt cites `englishName`, the database stores the code.

`ErrorCode` travels as a `String` on the wire, not an enum — kotlinx.serialization
throws on unknown values, so a newer server would crash older clients.

## Shared

`:shared` — domain, persistence, retention and the HTTP client. Android is the
only target.

**Scope** — a parameter with a default on every sliceable read:

| Value | Used by |
|---|---|
| `Scope.ActiveCourse` | Home, review, generation |
| `Scope.Course(target)` | "Your progress · French" |
| `Scope.All` | Words, Pending, You |

Filtered in memory, not SQL: the active language is a preference flow, and a query
parameterised by it would reopen the cursor on every carousel swipe.

**Retention** has two readings answering different questions:

| Reading | Means | Shown by |
|---|---|---|
| `pointsAt` | how much is remembered now; decays with time | Card, Words |
| `Steps` (1–5) | how far you got; moves only on an answer | What's left, every count |

Counting mastered words by memory strength would give a different total every hour.

**Schema** — one version-1 `Vocabs.sq`, no migrations. The Portuguese schema and
its chain went with the data. `verifyMigrations` is on: every migration from here
must replay from empty, which the old chain never could.

The **daily quota** is not a goal: it is what went out today plus what is queued.

## Android App

`:androidApp` — one folder per screen (`Screen` + `ViewModel`). Fourteen screens; the shared
pieces live in `ui/components`.

| Component | Holds |
|---|---|
| `Motion` | Every animation. `FAST` 150ms · `DEFAULT` 240ms · `WIDE` 620ms, plus 3 springs |
| `Categories` | One colour per capture type: text plum, audio mint, photo red |
| `Common` | Relative time, capture titles, level labels, entry titles |
| `Base` | Card, list row, pill, buttons, section label |
| `Progress` | Ring, week strip, meters |
| `LanguageStrip` | Language chips — fixed order, always badged |
| `SwipeToDelete` | The only way a queue row leaves |
| `Selection` | Term picker |
| `Flags` | 43 circular flags |
| `AppIcons` | Vector icons |
| `BottomBar` | Tabs |
| `ErrorText` | `ErrorCode` to a string resource |

**Motion rules.** Nothing you wait for exceeds `DEFAULT`. Entrances are longer
than exits — whoever closes already decided to leave. `gestureSpring` is stiffer
than the rest: a target taking 300ms to arrive is still travelling once the finger
has chosen. `reducedMotion()` honours the system animation scale.

**Capture is a gesture, not a screen** (`ui/capture/`). Every step is durable.

| Rule | Why |
|---|---|
| Releasing is the only confirmation | Nothing starts while the finger moves — no recording, no camera |
| You must reach the target (44dp radius) | Pointing made a short slide up trigger "record"; a pointer became a trigger |
| The three targets start equal | A pre-painted target promises something is under way, and nothing is |
| Recording gets its own screen | The hand is free; both exits cost one tap. **Asymmetry separates them, not colour** |

`MIN_RECORDING_MS` (0.8s) drops slips, and measures in millis — against a
whole-second counter the cut discarded every short recording.

**Media.** Photos and audio in `filesDir/captures`; ML Kit's bundled Latin model
for OCR; WAV PCM 16kHz mono, handed to the local `SpeechRecognizer` on API 33+.
Export ZIP in `cacheDir/exports`, shared through `FileProvider`.

**Colour exception.** The recording screen's discard button is salmon, not the
category red. It pays for itself only because there is no photo target on that
screen — the two meanings never share one.

## Server

`:server` — one Ktor endpoint. Holds the Anthropic key so the device never does.

The **prompt is English** regardless of the languages involved: instruction
language and output language are independent, and one calibrated prompt beats N
translated copies.

Default model is `claude-haiku-4-5`; `MODEL` overrides. Haiku rejects `effort`
with a 400, so the code branches on it.

Errors return a **code**, not a sentence — the app owns the wording. Provider text
rides along untranslated as the only diagnostic there is.

## Validation

`androidHostTest` covers batch creation, overlap, media retention, partial
concurrency, activity, monthly turnover, scope, quota, steps, timeline, and that
switching native language keeps an enrolled language and its counts.

`:androidApp` has a JVM test set for the pure text builders. Five resource lint
checks are errors; `MissingTranslation` fails the build rather than the device.
