# Exercises and metrics

_Updated: 2026-08-05_

## Minigame catalogue

The exercises cover reading, listening, speaking and grammar — but each one costs
wildly different amounts to build. **Pick the order by real complexity, not just by
teaching value.**

### Easy — AI-generated text only

| Exercise | What it tests |
|---|---|
| **Contextual reading** | A short, natural snippet using the word |
| **Grammar** | Cloze, multiple choice, rewrite the sentence |
| **Association** | Related/opposite terms — reuses the Phase 3 network |
| **Odd one out** | 4–5 words, one outside the semantic group — tests the neighbourhood of meaning |
| **Guess from the definition** | The flashcard inverted: forces active recall instead of recognition |
| **True or false usage** | A sentence sometimes right, sometimes odd — catches nuance, not just meaning |
| **Register/formality** | Pick the right word for a formal email vs a conversation |
| **Unscramble the sentence** | Reinforces grammar and usage together |
| **Batch review** | A short text using several of the day's words — shows them coexisting |
| **Lightning round** | Flashcards against the clock — just UI over what already exists |

### Medium — more UI logic, still no external source

| Exercise | Note |
|---|---|
| **Matching** | Words on one side, definitions on the other — good for reviewing in batches |
| **Phrase matching** | Same mechanic, aimed at idiomatic expressions |
| **Build the word (affixes)** | Root + prefixes/suffixes — reinforces word families |

### Medium — depends on an external source

- **Real snippets** (books, newspapers, music): avoids invented quotations, but needs
  an integration with a data source and raises a copyright question.
- **Audio from real snippets**: there is no simple way to find audio containing word X.
  TTS from a generated sentence is far more workable.

### Hard — real-time processing

- **Speaking practice**: needs speech-to-text to compare against the expected
  pronunciation. The most expensive on the list — and the one that most separates
  the app from a generic Anki.
- **Crossword**: not technically complex, but a lot of UI and logic work to do well.

### Suggested order

1. Contextual reading
2. Grammar
3. Association
4. Audio via TTS
5. Crossword
6. Speaking practice — last; it benefits from a mature app
7. Real snippets — a future complement, not a blocker

**Out of scope:** image-based games (abstract vocabulary does not lend itself to them)
and any multiplayer or leaderboard mechanic (you are the only user).

---

## Metrics dashboard

Well-chosen metrics turn into motivation — but only once there is real data, otherwise
the charts sit empty.

### About your target language

**Growth**
- Words captured / learned / mastered over time (cumulative line)
- New words per week (bars)
- Word vs phrase ratio

**Consistency**
- Current streak and record
- Activity heatmap (GitHub contribution-graph style)
- Active days in the month

**Time**
- Total and average time per session
- Time of day you study most

**Performance**
- Accuracy over time (it should climb)
- Accuracy per exercise type (you might do well at flashcards and badly at audio)
- Hardest words (most mistakes, or fast decay even with review)

**Origin and patterns**
- Where your words come from — game, book, series (pie)
- What kind of thing catches you, using the reason for the capture as a tag: verbs
  with several senses, false friends, slang

**Estimated level (CEFR)** — compare mastered words against a public frequency list.
It is not proof of proficiency, it is a motivating proxy. It is the only item that
depends on an external source, but it is a one-off integration.

### About the app, not about your language

This group does not measure your progress — it measures whether the funnel (capture →
processing → review → mastery) is leaking somewhere. **It matters more than it looks:
if the funnel leaks between capture and processing, the problem is not a lack of
motivation on your part, it is friction in the app — and that is actionable.**

- **Conversion funnel**: how many captures become cards, how many cards actually get
  reviewed. Shows exactly where you lose words.
- **Inbox backlog over time**: if that line only grows, you capture faster than you
  process. Probably the single most important warning of the lot.
- **Capture → processing latency**: the longer it is, the less likely you are to come back.
- **Efficiency per method**: which format (photo, audio, text) sits unprocessed longest.
  It might reveal that audio is quick to record and tedious to transcribe.
- **Efficiency per origin**: it might show that Kindle words, which already arrive with
  rich context, reach "mastered" more easily.
- **Duplication**: how often you capture the same word without noticing — a sign the app
  could say "you already have this".
