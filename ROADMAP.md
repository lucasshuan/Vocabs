# Roadmap

Every phase has an **exit criterion**. Only move to the next one when it is met —
that is what stops a phase being polished indefinitely.

Process rule: review the common traps (at the end of this file) before starting
each new phase.

---

## Phase 0 — Personal validation · 1–2 weeks

Prove the capture habit works **before** building anything.

- [ ] Capture words for 1–2 weeks with whatever tool already exists (notes, spreadsheet)
- [ ] Observe: do you capture in the moment or forget? How many a day?
- [ ] Note the contexts words most often come from

**Exit criterion:** a real list of 20–40 words, with their context identified.

---

## Phase 1 — Capture + card MVP · 2–4 weeks

**Screens:** Capture · Card · List/home

- [x] Local storage on the device, no backend or user authentication
- [x] Capture screen (snippet + target, optional source)
- [x] Card generated automatically: translation, 1–2 definitions, 1 example, IPA
- [x] Automatic word vs phrase classification
- [x] Simple list/home
- [ ] **Exit criterion:** capture in under 10s, card in under 1 minute

> **Card: timed and passed with room to spare.** Measured on 2026-08-01 with
> `claude-opus-5`. End to end inside the app (tapping "Save" until `status =
> READY`): **6.65s**. Straight against the server, 5 samples: 4.61 / 4.98 / 5.10 /
> 5.50 / 6.24s — average 5.05s. The 60s ceiling has nearly 90% headroom left, so
> latency was never the reason to change model — cost was, and the default is now
> `claude-haiku-4-5`. These numbers are the Opus measurement, unrepeated since.
>
> The entry turns `GENERATING` in **0.13s**, meaning saving genuinely does not
> wait for the AI. The app cold-starts in ~1.05s.
>
> **Capture: the human test is still missing.** What is left of the 10s after the
> cold start is ~9s of typing, and that cannot be measured by automation —
> injecting text over adb represents nobody. It only closes with you, the phone in
> hand, and a word that actually turned up.
>
> Deviation from the original plan: there is a backend (`:server`) brokering the
> AI call, so the key does not sit on the device. The vocabulary data stays 100%
> local.

---

## Phase 1.5 — Multi-format inbox · 1–2 weeks

**New screens:** Inbox · Photo capture · Audio capture · Manual processing

- [x] Photo capture (no OCR — you transcribe afterwards)
- [x] Audio memo capture (no automatic transcription)
- [x] Inbox screen, separate from finished cards
- [x] Manual processing screen (see the media and transcribe it)
- [ ] Test in the 3 real contexts: PC, Kindle, DS
- [ ] **Exit criterion:** capture in any of the 3 contexts in under 5s

> **Built and tested on the emulator in all three formats.** Audio records with one
> tap and stops with another; photo uses the system camera app through
> FileProvider; both enter as a draft and become a card after transcription — the
> photo → transcription → card path was walked end to end (`verdant`, WORD).
>
> The inbox came almost free, as expected, but not as `status = 'PENDING'`: it
> became `status != 'READY'`, which also collects drafts and errors. Home then
> listed only what is already a card — that is the separation the phase asked for.
>
> **The field test is missing.** The 5s on a PC, a Kindle and a DS depend on you
> with the devices in hand; the emulator does not simulate having your hands full
> holding a book.

---

## Phase 2 — Active retention · 3–5 weeks

**New screen:** Review/flashcard · **Changed:** Home (review indicator) · Card (points bar)

- [x] Points system (0–100) per word
- [x] Variable decay rate (falls on a correct answer, rises on a wrong one)
- [x] Calculated on demand: `max(0, points − rate × days)`
- [x] Flashcard exercise
- [x] "X to review today" indicator on home
- [x] Progress bar on the card
- [ ] **Exit criterion:** 7 days straight reviewing, feeling that you remember words from 3+ days ago

> **Built and verified on the emulator**, ageing the database by hand rather than
> waiting days. A correct answer returns 100 points and divides the rate by 1.5; a
> wrong one zeroes it and multiplies by 3 — the value that makes a mistake halve
> the interval. The ladder of consecutive correct answers gives 1 · 1.5 · 2.25 ·
> 3.4 · 5.1 · 7.6 days, so each word's third review already lands after 3+ days.
>
> A mistake mid-session brings the card back once, at the end of the queue, but
> **only the first answer is recorded** — the repeat is for you to see the word
> again, not to erase the mistake.
>
> The flashcard shows the captured snippet with the target blanked out, turning it
> into a cloze against your own context; the back returns the whole sentence. That
> is what the product document asks for as the "reading lock", and it costs no AI.
>
> **The exit criterion depends on real use**: 7 days straight cannot be simulated.
> The streak is already recorded (the `reviewed_day` table), so the app counts for
> you — you just have to use it.

---

## Phase 3 — Association network · 3–4 weeks

**New screen:** Build the word (affixes) · **Changed:** Card (related words)

The distinctive part of the idea: words pulling in other words.

- [ ] Synonyms, antonyms, word families
- [ ] Collocations
- [ ] Navigation between connected words on the card
- [ ] Word-building game (root + affixes)
- [ ] **Exit criterion:** "wander" through 3–5 words related to 1 captured one, and learn something

---

## Phase 4 — Easy exercises (AI) · 4–6 weeks

**New screen:** Exercise hub (with an AI-usage indicator)

Prioritise 2–3, **not all 6 at once**.

- [ ] Exercise hub
- [ ] Contextual reading
- [ ] Grammar/cloze
- [ ] Unscramble the sentence
- [ ] Association (reuses the Phase 3 network)
- [ ] Guess from the definition
- [ ] Lightning round
- [ ] True or false usage
- [ ] Register/formality
- [ ] AI-usage indicator in the hub
- [ ] **Exit criterion:** every mastered word has been through 2+ exercise types

---

## Phase 5 — Audio and crosswords · 3–4 weeks

- [ ] TTS from AI-generated sentences (not real audio)
- [ ] Audio dictation
- [ ] Crossword
- [ ] Matching (word/definition, phrase/meaning)
- [ ] Batch review (a story using several of the day's words)
- [ ] **Exit criterion:** a listening exercise available for any word

---

## Phase 6 — Speaking and real content · ongoing, later

The technically most expensive parts. Only once the rest is mature.

- [ ] Native speech recognition
- [ ] Speaking exercise with pronunciation comparison
- [ ] Evaluate external sources for real snippets (mind the licensing)
- [ ] Reading screen with real content
- [ ] **Exit criterion:** no fixed deadline — it enters when the rest is stable in daily use

---

## Phase 7 — Polish and habit · ongoing

- [ ] Automatic OCR on photos
- [ ] Automatic transcription of audio
- [ ] Kindle Vocabulary Builder export
- [ ] Capture through the system share sheet
- [ ] Home with decks (by origin, by status)
- [ ] Light gamification (streak, visual progress)
- [ ] Review notifications
- [ ] Usage transparency panel
- [ ] "AI Boost" add-on
- [ ] "Cloud sync" add-on
- [ ] Data export always available

---

## Phase 8 — Metrics dashboard · 2–3 weeks

Only makes sense with a real volume of data. Details in
[docs/EXERCISES-AND-METRICS.md](docs/EXERCISES-AND-METRICS.md).

- [ ] Conversion funnel and inbox backlog — the most important signals
- [ ] Vocabulary growth and a consistency heatmap
- [ ] Accuracy over time and per exercise type
- [ ] Content origin and difficulty patterns
- [ ] Estimated level (CEFR) — last, it depends on an external source
- [ ] **Exit criterion:** understand the progress at a glance, without doing arithmetic

---

## Common traps

| Trap | Answer |
|---|---|
| "I'll make the card perfect before using it" | No. Ugly and simple validates the loop; aesthetics are a late phase. |
| "I'll build all 6 games at once" | No. Start with 1, use it for a week, only then add. |
| "I need to settle the whole data architecture first" | No. Start with the minimum and evolve it against real need. |
| "I need OCR and speech recognition right away" | Start with a text field. You may not even miss them. |

## Metrics in phases 0–2

Before the full dashboard, track only the essentials — by hand is fine:

- How many words you capture per week (consistency matters more than volume)
- How many reviews per week
- Of 10 words captured two weeks ago, how many do you remember without looking at the card?
