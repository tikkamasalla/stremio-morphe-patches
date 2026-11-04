# Feature guide

This guide covers the exact rules, thresholds, and fallback logic behind the [README](../README.md). Each section starts with what you see on screen, followed by how the patch makes its decisions.

## Skipping

Depending on your settings, you either see an on-screen Skip button or jump straight past intros, recaps, and credits automatically. The patch gathers timestamps from multiple sources and drops any entry that fails safety checks.

Sources are checked in this order:
1. The video file's own embedded chapters
2. This project's published timings
3. AniSkip (anime)
4. TheIntroDB
5. IntroDB.app (fills in segment types that are still missing)
6. SkipDB (final fallback)

No API keys are required. An online entry is used only when your file's duration matches the source's measured runtime within 2 s. If the player does not know the file runtime yet, the entry is held until the duration loads.

**Chapters and intros.** Chapters named `Opening`, `OP`, `Opening Credits`, `Main Titles`, `Title Sequence`, `Theme`, and close variations count as an intro. Online timestamps snap to nearby chapter edges.

**Credits and post-credit scenes.** Skipping credits jumps to the end of the credits segment rather than the end of the file, so post-credit scenes play normally. Chapters named `post-credit`, `mid-credit`, `stinger`, `epilogue`, or `bonus scene` are never skipped, and any credits segment is trimmed where one of those begins. A chapter named `Ending` is treated as story by default. It counts as credits only when all three conditions are met: it starts in the last 20% of the file, it runs at most 240 s, and another signal confirms it (an online credits segment starting within 15 s, a preview or credits chapter right after it, or it is the final chapter in the file).

**Previews.** A preview from a named chapter or an online source follows your `Skip previews` setting (auto-skip or button). A preview that the patch infers on its own always shows a button, even in Auto mode. Inferred previews cover the short tail after the credits and segments detected from opening dialogue lines in the subtitle text.

**Safety rules.** These checks stop bad or incomplete data from cutting into the story:

* Segments with an unknown end time are ignored, as are intro, recap, and credits segments with an unknown start time.
* Credits with a start time but no end time are used only after the file runtime is known, only if they begin in the last 40% of the file, and only if the source's file length matches your file.
* When two online sources closely agree on a segment, only the overlapping range they both agree on is skipped.
* An early credits entry on an episode with no intro is dropped, preventing an early outro from being offered as `Skip intro`. The only exception is TheIntroDB, which sometimes mislabels a real opening as credits; those entries are treated as the intro.
* When sources disagree on a segment starting at `0:00`, the Skip button is shown instead of skipping automatically.
* A video counts as a series episode only once its id or Stremio's metadata type confirms it. Until then, skipping stays off.
* Turning the `Intro skip` switch off disables all lookups and hides the skip button. On Stremio, the `Next episode` pill still appears for series.

**Playback controls and retries.** Resuming an episode inside an intro still triggers a skip. If you manually rewind or seek into a segment, the player shows a Skip button instead of auto-skipping again. Remote control keys are never blocked.

If an online source is down, playback does not wait for it. Available sources apply right away, and the failed source retries after 5 s, 20 s, and 60 s. Late replies merge cleanly without causing a double skip.

**Anime matching.** Before querying AniSkip, the patch resolves the anime title to the exact season and cour (a broadcast block, including "Part 2" style entries). A recap starting at `0:00` is kept if its duration is plausible (10-180 s).

**Skip settings.** The available toggles are: `Intro skip` (on/off), anime openings (on/off), mode (`Auto` or `Manual`), recaps (on/off), `Skip credits` (`Auto`, `Button`, or `Off`, default `Button`), `Skip previews` (on/off, default off), and `Show skip marks` (on/off, default on, 1.6.12 only). Skip marks appear as colored markers on the progress bar.

### Working out missing skip times

`Work out missing skip times` is off by default. When enabled, as soon as `Prepare next episode` locates the next episode's file, a background process compares the audio at the start and end of that file against other episodes of the show. At least 2 other episodes must match before a segment is reported. Results rank below every other source and follow your `Auto` or `Manual` preference. The analyzer runs in its own isolated process, so a crash inside it never interrupts playback.

Results apply only to the exact file they were calculated for. Quotas cap usage at one try per episode per day, up to 24 episodes per show per day, and at most 100 GB of data read per day. If a result finishes after the episode has already started and you are currently inside the segment, it displays a Skip button and never jumps on its own at that moment.

### Borrowed chapters

`Borrow chapters from other releases` is off by default and works independently of `Work out missing skip times`. It copies intro and credits chapter marks from another release of the same episode after an audio alignment check proves the two files line up. If the check fails, nothing is copied. In an offline test of 40 file pairs, 44 of 45 copied segments landed within 1 s, with a median error of 0.001 s and no story cut, while all 62 deliberately mismatched cases were rejected. It has not yet produced a result on a real device.

### Anime theme-song backup

When `Work out missing skip times` is on and an anime with a Kitsu id still lacks an intro or credits, the app matches the show's official opening and ending songs from AnimeThemes.moe against the episode audio. It fills only missing segment types and does nothing if it cannot confirm the exact show. Because it needs only a single episode, it works where multi-episode audio comparison cannot. Tested standalone on PC across 123 episodes, it found 82 of 114 intros and 80 of 105 credits with no real wrong skips, and took Blood+ from 0 of 5 intros found to 5 of 5. The app's matcher agreed with the PC build on all 99 episodes compared. It has not yet run on a real device.

## Streams

Press play on an episode and the best stream starts without showing a selection list. `Auto-pick best stream` filters out CAM, telesync, and screener copies, streams for a different season or episode, addon warning cards, and external web links.

Remaining streams are ranked in this order:
1. Single-episode file before a season pack
2. Cached before uncached
3. Resolution (requires an explicit 4K label such as `2160p` or `4K`; a bare `UHD` tag does not count)
4. HDR or Dolby Vision
5. File size (no size cap)
6. Codec
7. Seeders

If every stream fails the filter, the stream list stays on screen.

`Auto-pick` waits 4 s when a cached stream is present and 8 s otherwise. It is on by default, and `Always show stream list` is off by default. Long-press an episode to open its stream list manually for that play.

**Preloading the next episode.** While an episode plays, the patch resolves the best stream for the next episode in the background so pressing Next, reaching the end of the episode, or using Binge starts it immediately. This requires `Auto-pick` on, `Always show stream list` off, and its own toggle, `Prepare next episode`, enabled. If the prepared stream fails to start within 25 s, the patch gives up on it.

**Hide spoilers.** An optional switch that hides thumbnails and synopses of unwatched episodes and displays `Episode N` in place of their titles.

## Tracks and subtitles

Pick an audio or subtitle language once, and future episodes of that show start with the same choice. Stremio's built-in player and Vimu share this memory across both IMDb and Kitsu ids.

On 1.6.12, remembered languages are set as the player's preferred tracks before playback begins, avoiding mid-stream track switches and rebuffering. Vimu checks this saved memory before its own default rules. When no saved preference exists for a show, Vimu prefers Japanese audio for anime, then your Stremio audio language, and finally English. For anime subtitles, it selects a full-dialogue track in your language instead of a Signs and Songs track.

`Avoid SDH subtitles` prefers full dialogue tracks over SDH tracks, and strips out sound notes when an SDH track is the only option left. In Vimu, the same rule applies to tracks embedded inside the video file.

When launching Vimu, Stremio passes your chosen subtitle plus up to five fallback tracks (your language first, then English). If the video file has no suitable embedded track in your language, Vimu automatically loads the best addon subtitle and lists the remaining options in its own subtitle menu. Pressing the `GREEN` or `GUIDE` remote key cycles through addon subtitles, embedded subtitles, and subtitles off.

## External players

Vimu, mpv, and Just Player are tested. MX Player and VLC use their public Android intent APIs and have not been tested. If no default player is set and Vimu is not installed, Android shows its system app chooser. Picking MX Player there saves it as the remembered player until you clear it with `Forget remembered player` in Mod settings.

**Progress and crash recovery.** Pressing Back or Home inside an external player never marks an episode as finished. An episode is marked watched only at 90% progress or on a genuine completion. Vimu writes its playback position to a local journal every 10 s and saves immediately on pause, so your progress survives a crash. Stremio reads that journal while the player is open and when Stremio starts up again after being closed (a cold start). Cold-start recovery works on 1.6.12 only.

**Binge watching.** `Binge with external players` is on by default and applies to series only. When an episode genuinely finishes, the next episode starts automatically in the same external player. Pressing Back, Home, or stopping playback before the end never launches the next episode. Because VLC and mpv return no result on a normal completion, the patch relies on the known runtime to tell an early exit from a finished episode; if the runtime is unknown, the next episode will not start.

## Settings and extras

`Mod settings` puts every option from these patches into a single screen.

* **Smoother playback** (Stremio's player): Adds frame-rate matching under the `Match frame rate` setting (`Seamless only`, `Always`, or `Off`).
* **Larger playback buffer** (Stremio's player): Buffers 50-120 s ahead (up to 256 MB), on by default, for fewer stalls on high-bitrate streams.
* **Playback diary:** A compact local log, on by default, that you can fetch with `adb pull`. No stream addresses or account details are written to it.
* **Enable debug mode** (option on the Intro Skip patch): Opens a log window when playback starts showing the detected title, season, episode, and API responses.

## Differences between Stremio versions

`1.6.12` is the reference build. Its patch options appear as native rows inside Stremio Settings (`Interface` for `Hide spoilers`, `Playback` for the rest), skips use an exact seek, and only this build supports progress-bar skip marks and cold-start recovery.

`1.10.4`, `1.12.0`, `2.4.0`, and `2.5.0` use a newer Stremio player screen, so the patches hook the app's core layer and playback engines directly. `Intro Skip`, `Auto-pick`, `Hide spoilers`, `Remember tracks per show`, `Avoid SDH subtitles`, `External player progress`, and `Binge` are all ported to these versions, with four notable differences:

* Skips use the engine's standard seek, which snaps to the nearest keyframe and can land about 1-2 s early.
* `Remember tracks` saves the language only. "Subtitles off" is not remembered, and Stremio's built-in "auto select" setting must be enabled.
* `Avoid SDH` strips hearing-impaired cues from subtitle text on the ExoPlayer engine only. On the mpv engine, it switches on mpv's built-in SDH filter instead.
* If the next episode was prepared while the current one played, it starts immediately. Otherwise, the patch opens the next episode's stream screen and `Auto-pick` starts it.

Versions `1.12.0` and `2.5.0` were added in 1.7. Version `2.4.0` patches cleanly like `2.5.0`, though most testing was done on `2.5.0`.

## Vimu

Vimu `12.00` (`net.gtvbox.videoplayer`) gets seven dedicated patches. `Intro Skip (Vimu)` uses the exact same sources and filtering rules as Stremio. Vimu Settings gains rows for `Skip intros`, anime openings, `Auto`/`Manual` mode, recaps, credits (`Auto`/`Button`/`Off`), previews, and preferred audio language.

`Intro Skip native fix (Vimu)` is a 2 byte patch in `libmediaengine.so` that prevents a native crash during rapid seeks. It is built for both `armeabi-v7a` and `arm64-v8a` (the `arm64` build was added in 1.7).

## Known limits

* A chapter named simply `"Intro"` counts only when AniSkip, TheIntroDB, or IntroDB.app has an intro entry that agrees with it within 15 s. When no online source has an intro, Vimu still accepts a lone `"Intro"` chapter if it is 20-150 s long and starts after `0:00` (typically following a cold open). Stremio ignores lone `"Intro"` chapters when no online source agrees.
* The IMDb id comes from Stremio's own video id. Kitsu ids work for AniSkip, but IntroDB.app and SkipDB require an IMDb id and are not queried for Kitsu-only anime.
* Online credits segments longer than 180 s always show the Skip button instead of auto-skipping, even in `Auto` mode, though a shorter window can still overlap an unmarked scene.

## Reporting a problem

Open an issue with your app version, device model, the Morphe log if patching failed, and the relevant logcat lines. Useful log tags for Stremio are `IntroSkip`, `IntroSkipPort` (hooks on 1.10.4 and newer), `AutoPick`, `TrackMemory`, `ExtProgress`, `BingeDbg`, `SdhAvoid`, and `ModSettings`. For Vimu, filter by `VimuIntroSkip`, `VimuResult`, `VimuTracks`, `VimuAddonSubs`, `VimuSubDialog`, and `VimuFix`. For example:

```bash
adb logcat -s IntroSkip IntroSkipPort AutoPick TrackMemory ExtProgress BingeDbg SdhAvoid ModSettings
```
