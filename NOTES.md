# Notes

_Updated: 2026-08-05_

Things that fit neither the roadmap nor the architecture: what has not been
proven, what is crooked, and what might be worth doing. One line each.

## Untested

- **Capture in under 10s** (Phase 1) — only closes with you actually typing; injecting text over adb represents nobody.
- **Capture in under 5s across the 3 contexts** (Phase 1.5) — needs a PC, a Kindle and a DS in hand, with your hands full.
- **The whole of Phase 2** — the maths was checked by ageing the database, but nobody has reviewed for 7 days straight yet.
- **Reaching the target one-handed** — the choice stopped being about angle and now requires getting to the disc, ~150 dp from the `+`. The 44 dp radius forgives the aim, but on a large device nobody has measured how many thumbs reach the photo target without the phone slipping.
- **Switching the interface language on API 26–32.** The 33+ path is the system's own; below that the app wraps `attachBaseContext` itself, and that half has only been reasoned about, not run.

## Known inconsistencies

- **The APK ships no licence notices.** MIT (circle-flags) and OFL 1.1 (both fonts) each require their notice to travel with the software, and the flags and fonts are bundled. There is no "Open source licences" screen, so the obligation is unmet the moment the APK leaves this machine. Attribution is in the README, which is not the distribution. Vocabu also has no licence of its own, so default copyright applies — all rights reserved.

- There is no screen for filling in `source`. It is already displayed ("Photo from Kindle", the card's "Your context") and has no way in — the field was taken out of capture because it was almost never filled, and nothing took its place.
- Two scales use the same three names. In Words and on the Card, "mastered" is memory strength: a word answered correctly once carries the label for a few hours, because the memory really is fresh. In the counts (Home, You, Progress, What's left) "mastered" is step 5, which takes four correct answers. Both are right for the question they answer, and the same word can still appear with different labels on two screens.
- The capture button says "Save 2 captures", but what 2 selections create is 1 capture and 2 entries. The wording is inherited and contradicts the note that "one capture can yield several cards". Worth settling the name.
- **Going back during a recording saves, and nothing on screen says so.** The app's rule is that nothing is discarded without being asked, and the asking has exactly one home — the discard button — so back ends by saving. Anyone who pressed back expecting to leave no trace finds out from the 5 s notice that the capture went in.
- The transcription player's waveform is fixed: ten bars of decorative height, not the audio's amplitude.
- The day streak (`reviewed_day`) is global and appears inside a screen that is per course. That was decided deliberately — the habit is one habit, and reviewing German on Tuesday and English on Wednesday should not break anything — but someone reading "5 days in a row" in the English progress has no way to know. On the You screen this is resolved: the three numbers at the top sum everything and the per-language breakdown comes after.
- OCR and voice do not follow the course. ML Kit's Latin model is the only one bundled, so capturing a photo in Japanese returns rubbish or nothing, and the screen falls back to manual editing without explaining why. The warning got harder to give once capture became a gesture: there is no sheet left to write it on, and the place that remains is the capture row in Pending — after the fact rather than before.
- **"The language comes from the speech" does not.** The gesture design treats language detection as given, and the app has no detector: what exists is the fallback rule — it lands in the course open in the hub — and that became the only path. The recording screen says "goes to the English course" rather than promising detection, and the flag on the 5 s notice shows the same course. The day there is detection, those two sentences are what change.
- **Photo and audio exist only through the gesture.** The sheet that listed the three modes side by side is gone, and with it the only tap route to photo and audio. Anyone who never holds the `+` does not discover they exist — the fan opens after 180 ms of pressure and the "drag and drop onto a target" hint teaches it first time, but nobody has tested how many people get there alone. Home's invitation and Saved's "capture another" open the text drawer, which is the likeliest mode, not the other two.
- The language strip scrolls without limit. The design suggests 5 fixed + "see all" from the sixth course onward; with three it fits, and the cut was not implemented. Anyone enrolled in eight will do a lot of dragging.
- "Remove language" lives inside "Your progress · language" rather than on the You screen. It is reachable from there (row → progress → remove), but removal is described as something belonging to the profile. A list row with a remove action on the right would fix it, at the cost of one more touch target in a list that is already a door.
- The "Saved" screen closes itself after 3.5s **only when every card is ready**. It is meant to always close; making the "building the sense" bar vanish mid-way would contradict the screen itself, so the timer waits. Worth checking whether the behaviour looks stuck when the AI is slow.

## Ideas

- Capture through the system share sheet, without opening the app.
- Transcribe a pending item in one tap. The cheap path is not generative AI: Android's `SpeechRecognizer` transcribes audio for free and ML Kit does OCR on the photo without a network — the same reasoning as the "Where AI is not necessary" section of PRODUCT.md. AI would come in only to clean the text up, if at all. The button puts the result in the fields and you confirm before generating the card: an automatic transcription that is wrong and becomes a card on its own costs money and produces rubbish.
- Accuracy per exercise type, once there is more than one exercise (today there is only the flashcard).
