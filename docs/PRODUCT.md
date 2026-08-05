# Product: Vocabu

Vocabu turns terms found in games, books, series and conversations into cards tied
to personal context. There is no fixed curriculum: the content comes out of the
person's life.

## Principles

1. Capture before anything else; saving closes the screen immediately.
2. Capture and review are separate moments.
3. Personal context is worth more than an isolated definition.
4. Local data and local automation are the default; AI cost is transparent.
5. A failure never erases work that already succeeded.
6. Export stays available without an account.

## Mental model

A **capture** is the raw snippet — text, photo or speech. It can give rise to
several **entries**: `fence` and `on the fence`, for example, can both be chosen
from the same context and their ranges may overlap.

The type is objective and decided on the device:

- one selected token → `WORD`;
- two or more contiguous tokens → `PHRASE`.

Punctuation outside the target does not take part; internal apostrophes and
hyphens do. Duplicates are flagged, never blocked.

## Core loop

```text
Capture → select targets → generate cards → review by cloze → master
```

Photo uses offline OCR. Audio uses local recognition when the platform offers file
input, and always keeps manual typing as the safe way out.

## Card and review

The card holds the term, its pronunciation, the type, a contextual translation,
memory strength, the personal snippet, definitions, an example and 3–6 related
terms. The device's TTS does the reading aloud.

Review blanks out exactly the selected range and asks for the answer to be typed.
The comparison ignores case and repeated spaces, but preserves letters, accents
and meaningful punctuation. A wrong answer and "I don't remember" both reveal the
answer, count once, and put the card back once.

## Transparency

The profile shows vocabulary, mastered words, accuracy, 84 days of activity, the
language pair and the month's AI generations. `100` is only a visual reference:
it is not a security quota and it does not block generation. The exported ZIP
includes versioned JSON and media so the data is genuinely portable.
