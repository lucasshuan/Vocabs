# Vocabu architecture

## Names that are not brand

`applicationId` and the database filename (`vocabs.db`) are storage identifiers,
not branding. Renaming the app is find-and-replace; renaming either of those
migrates nothing — it creates an empty place beside what already exists. A
renamed `getSharedPreferences` opens a fresh version with no native language,
none of the courses the person enrolled in and the default theme, with the word
database intact and no screen knowing which language to show it in.

Both were changed anyway during the English rewrite, along with the preference
file, its keys, the media folders and the routes — because the data was being
wiped, so nothing had to be carried across. **That window is closed.** From the
first install anyone cares about, each of them changes only together with a
migration that reads the old one, and `applicationId` not even then: once the app
is published it is permanent, and a different one is a different listing with no
upgrade path.

Two consequences of having used that window:

- The package is `io.github.lucasshuan.vocabu` — reverse-DNS of a namespace that
  is actually controlled, rather than a claim on a domain that is not.
- **`vocabs.db` keeps the old name on purpose.** The app is Vocabu; `vocabs` was
  what it was called first. The filename stayed because renaming it buys nothing
  and costs a migration the day there is data worth keeping. It is a legacy
  identifier, not a second brand — do not "fix" it for consistency.

Routes were never on this list: nothing persists them, so they can be renamed on
their own.

## Modules

```text
:contracts   serialisable DTOs shared by the app and the server
:shared      domain, SQLDelight, retention and the HTTP client
:androidApp  Compose, OCR, voice, media and export
:server      Ktor endpoint and the structured AI call
```

Inside `:androidApp` each screen is a folder with a `Screen` and a `ViewModel`,
and `ui/components` holds what repeats between them — card, list row, metric,
pill, term picker, flags, the language strip and the category colours. No styling
lives in one screen just because it got there first: the same element in two
places becomes a component before it diverges in a third.

### Global conventions

- **One colour per capture type** (`ui/components/Categories.kt`): text is plum,
  audio is mint, photo is the parrot's red. The trio shows up in the `+` fan's
  targets and in Pending's discs, and it is what forms the association at the
  moment of capture. The red is a category, never an error. The single exception
  is the recording screen's discard button — the salmon of "this disappears" —
  and it pays for itself because it exists **only inside the recording screen**,
  where there is no photo target at all: the two meanings never share a screen. A
  second red taken from the theme's `error` would be nearly identical to the
  first, which is the guaranteed way to make both illegible.
- **Every flag in the strip has a badge** (`ui/components/LanguageStrip.kt`): a
  plum number when there is something to review, a mint tick when up to date, a
  grey hourglass when the course has nothing scheduled yet. Never empty and never
  a written "0" — zero is the strip's good news. The order is fixed: the
  carousel's swipe depends on positions not moving.
- **Three durations and three springs** (`ui/components/Motion.kt`): every
  animation comes out of `Motion`. `FAST` (150 ms) for what merely reacts,
  `DEFAULT` (240 ms) for what enters and leaves, `WIDE` (620 ms) only for what is
  read while it runs — the ring's arc, the quota bar, a number counting up. The
  rule that decides between them: **nothing you wait for exceeds `DEFAULT`.**
  Entrances are always longer than exits, because whoever closes has already
  decided to leave.

The third spring is `gestureSpring`, stiffer than the other two: it moves the
fan's targets, and a target that takes 300 ms to arrive is still travelling once
the finger has decided where it is going. `reducedMotion()` reads the system
animation scale — with it at zero the `+`'s halo disappears and the button holds
still.

Four pieces come out of that motion vocabulary and are reused by the screens:
`shrinkOnTouch` (the card yields under the finger — it is inside `ScreenCard`,
not at every call site), `smoothEntrance` (the staggered arrival, capped at 5
items and forbidden in a lazy list, where `animateItem` is the right tool),
`animatedFraction` (returns `State` so a `Canvas` reads the value in the draw
phase instead of recomposing every frame) and `animatedCount` (only for
accumulated achievement; a queue and a debt do not count up from zero, or the
backlog becomes a scoreboard).

### The capture gesture

Capture starts from a **gesture**, not a screen, and every step is already
durable. `ui/capture/` holds the five pieces: `CaptureGesture.kt` is pure
geometry — where the three targets sit and which one a displacement picks;
`CaptureHub.kt` is the `+`, the fan and the veil, in a full-screen overlay that
intercepts no touch outside the gesture beyond the button itself;
`RecordingScreen.kt` is the opaque screen recording opens, with the clock, the
waveform and the two ways to finish; `TextDrawer.kt` is the field a loose tap
opens; `CaptureNotice.kt` is the 5 s card that confirms and offers the shortcut.
After that, selection marks the terms (`Select`) and the confirmation shows what
went in while the AI works (`Saved`).

Four rules of the gesture, and all four pay for themselves:

- **Releasing is the only confirmation, and it holds for all three targets.**
  While the finger moves nothing has been chosen and nothing has started — not
  recording, not the camera. Entering a target paints it; leaving undoes the
  highlight immediately; releasing outside closes without capturing anything and
  without a warning.
- **You have to reach the target.** The 44 dp radius around each disc's centre is
  what marks it, not the angular sector the finger is in. Pointing was enough and
  cost less thumb, but with the audio target occupying the whole central sector a
  short upward slide already marked "record": what was a pointer became a
  trigger. The three sit at the same distance — 152 dp, across a ±54° arc — inside
  what the thumb sweeps without stretching, and the radius forgives the aim.
- **The three targets are born equal** — 68 dp, in the light tone of their own
  action, growing to 76 dp at full colour only once reached. Audio stopped being
  the big green disc: a target already painted before the finger arrives promises
  that something is under way, and nothing is.
- **Recording has its own screen, and finishing is one tap.** It starts when the
  finger leaves the screen, and from then on the hand is free: you can put the
  phone down, move it towards whoever is speaking, or switch hands. Both
  destinations stay in view the whole time and cost one tap each — save filled and
  taking what is left of the width, discard narrow and outlined only. **The
  asymmetry is what separates them**, not the colour: two targets of the same size
  at the foot of a screen glanced at invite the wrong tap, and nearly every
  recording is meant to be kept. A lateral swipe once occupied that base and left:
  it recalled answering a call and charged for learning at the worst possible
  moment. `MIN_RECORDING_MS` (0.8 s) discards what was a slip and captures
  nothing, and it measures in milliseconds: compared against a whole-second
  counter, the cut discarded every short recording and nothing else.

## Captures and cards

```text
capture (context, format, media, transcription, language pair)
   ├── entry (selected range, type, card, retention)
   └── entry (another range, overlapping included)
```

`capture` is the raw signal. `entry` is a selected target and can exist without a
card while generation is pending.

Text creates the capture and all its entries in one transaction. The bounds are
`[start, end)` and stay tied to the original snippet for the cloze. One selected
word becomes `WORD`; two or more contiguous tokens become `PHRASE`. The AI
receives that type and the server reinjects it into the response — there is no
remote classification.

Cards are generated independently behind a two-request semaphore. One entry
failing does not undo its siblings. Only responses stored successfully count
towards the month's `ai_usage`.

When an entry is deleted the repository counts its siblings in the same
transaction. The media is removed only when the last entry, or the whole capture,
disappears.

## Courses

The language pair lives on the **capture**, not the entry: a snippet is in one
language, so every selection inside it inherits that language by construction.
That is what makes it possible to regenerate an old card in the language it was
born in after the person switches courses — the request carries the pair
(`GenerateCardRequest`), and the server refuses a pair outside the catalogue
rather than falling back to a default.

**A course is identified by the language it teaches, not by the pair.** The
native language belongs to the individual card generated in it. Reads filter by
target; writes record the whole pair. Those two sentences are one rule, and
breaking it is invisible to the compiler: keying a read by the whole pair made
switching the native language stop matching every stored card, so the totals read
zero and the lists came back empty — nothing deleted, no way to tell from the
screen.

The language catalogue lives in `:contracts` because both sides need the same
list for different reasons: the interface shows a flag and a name from its own
string resources, the prompt cites `englishName`, the database stores the code.
Of the target language the server needs one more thing only — which notation the
pronunciation is written in (IPA by default, pinyin for Mandarin, kana + romaji
for Japanese).

Which course is open is a device preference (`Preferences`) and enters the
repository as a flow. **Only Home is sliced by it**: it is a carousel with one
page per course, and swiping between pages *is* switching the open course. Words,
Pending and You always show every language together, marked language by language
— a filter that stayed on across a tab change would make words disappear without
anyone asking.

The three slices coexist through `Scope`, a parameter with a default on every
sliceable read of `VocabRepository`:

```text
Scope.ActiveCourse    default — Home, review, generation
Scope.Course(target)  "Your progress · French", without switching the open course
Scope.All             Words, Pending, You
```

The filter is applied in memory rather than in SQL: the open course is a
preference flow, and a query parameterised by it would reopen the cursor on every
swipe of the carousel.

The language is decided **at the moment of recording**, not of selection, and is
**not asked for**: the capture goes to the course open in the hub, frozen at the
instant the finger goes down, and `captureText`/`captureSnippet`/`captureMedia`
take the chosen pair. That is why every Pending row carries a language in its
subtext, and why pasted text that never reached "Save" waits in the queue in the
right language instead of getting lost. `changeCaptureLanguage` fixes the choice
while the capture has not become cards — after that there are entries born in
that pair.

The sheet that asked for a destination before allowing capture was removed along
with the gesture redesign: it charged a tap on every capture to get right the few
where the course was not the one on screen, and the correction already existed in
Pending, where the mistake is visible and undoing costs one tap.

Switching course left the profile; only adding and removing stayed there
(`Preferences.unenroll`, which never empties the list). The base language went to
Settings, alongside the theme and portability: it applies to the whole app rather
than to one course — at the foot of a list where every row opens a course, it read
as one more of them. It now sits beside the app's own interface language, and the
two are explicitly distinguished, including in the dialog that confirms a switch.

## Interface language

Two independent settings, and the difference is the point:

- **App language** — buttons, menus and messages. Defaults to the device and can
  be overridden in Settings. On API 33+ the system is the store
  (`LocaleManager`), so the picker and Android's own per-app language setting are
  the same value; below that a preference-backed `attachBaseContext` wrap does the
  same job. `res/xml/locales_config.xml` is what puts Vocabu in the system list.
- **Language I speak** — what cards are written in. Changing it is a context
  switch, not a reset, which is why it asks first.

Two rules follow from this and neither is enforced by the compiler:

- **Nothing below `:androidApp` produces display text.** A domain module has no
  resources, so a sentence decided there can only ever exist in one language.
  A `String` in `:shared` or `:contracts` is legitimate only when it holds the
  user's own words or an opaque identifier — which is why the server sends an
  error *code* and the app picks the sentence.
- **Read resources through `LocalResources`, never `LocalContext`.**
  `LocalContext.current` does not invalidate when the configuration changes, so
  anything read through it keeps the previous language's text after a switch. Lint
  catches this (`LocalContextGetResourceValueCall`) and it is an error here.

Dates and language names come from `java.time` and the string catalogue with an
explicit locale, read from `LocalConfiguration` — never `Locale.getDefault()`,
which follows the device rather than the in-app picker.

## States

- `Capture`: `TRANSCRIBING`, `AWAITING_SELECTION`, `PROCESSED`.
- `Entry`: `PENDING`, `GENERATING`, `READY`, `ERROR`.

Pending combines the two queues without mixing them: transcription and selection
belong to the capture; generation belongs to the entry. Nothing is discarded
without being asked — cancelling the selection, closing the app or losing the
connection mid-transcription leaves the capture in `AWAITING_SELECTION`, with the
language already chosen, ready to carry on where it stopped. That is what makes
recording first never cost anything.

## Local media

Photos and audio live in `filesDir/captures`. Photos go through ML Kit's bundled
Latin model. Audio is WAV PCM 16 kHz mono. On API 33+ the PCM file is handed to
the local `SpeechRecognizer`; with no API or model, or on failure, the capture
falls through to manual editing.

The export ZIP is created in `cacheDir/exports`, contains `Vocabu.json` with a
`schemaVersion` and the referenced media, and is shared through `FileProvider`
with temporary read permission.

## Retention and activity

Every ready entry keeps points and a decay rate. Below 60 it enters the queue.
Review records only the first attempt; a wrong answer puts the card back exactly
once at the end of the session. `reviewed_day` feeds the streak and the week.
`ai_usage` is keyed `YYYY-MM`, so it turns over naturally in the local month.

There are **two** readings of that same retention, and they answer different
questions:

- **Memory strength** (`pointsAt`): how much is remembered right now. It decays on
  its own over time. It is what the card and the Words list show.
- **Step** (`Steps`, 1 to 5): how far you have got. It comes from the decay rate,
  which is already the history of correct answers compressed, and it only moves
  when a card is answered. It is what "What's left" shows, and what counts the
  mastered words on every screen with a number — memory strength would give a
  different total every hour.

`event` is the timeline, append-only: capture, card ready, correct, incorrect and
level change, each row with the local day already resolved. It exists because
retention stores only the state of right now and cannot answer "what did I do on
Tuesday".

The **daily quota** is not a chosen goal: it is what has already gone out today
plus what is still in the queue. A fixed goal would be unreachable on the day 30
words fall due together, and would already be met on a day with nothing to review.

## Validation

`androidHostTest` uses SQLite JDBC to test batch creation, overlap, media
retention, partial concurrency, activity, monthly turnover, course scope, the
quota, steps, the timeline, and that switching the native language keeps a course
and its counts.

`:androidApp` has a JVM test source set for the pure text-building functions.
Five resource lint checks are errors, and `MissingTranslation` is what makes an
untranslated key fail the build rather than surface on a device.

The schema is a single version-1 `Vocabs.sq` with no migrations: the Portuguese
schema and its chain were dropped along with the data during the English rewrite,
and are recoverable from the `pre-english-schema` tag. `verifyMigrations` is
therefore off — it replays from an empty database, and the old chain never was
replayable, since `1.sqm` opened by altering tables only a long-replaced
`Vocabs.sq` had created. `schemaOutputDirectory` still writes the snapshot, so
the next schema change has something to be verified against; turn the flag on
when a chain exists again.
