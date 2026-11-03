# Stremio Morphe Patches

Patches for Stremio and Vimu, for use with Morphe. Made for Android TV, with phone support from 1.7: auto-skips intros, recaps and credits, avoids SDH subtitles, picks the best stream, and passes your episode, subtitles and audio/subtitle choices to external players.

I made this for my own TV. It just skips the boring bits so playback keeps going.

## Supported apps and versions

| App | Version | Notes |
|---|---|---|
| Stremio TV (com.stremio.one, Android TV) | 1.6.12 | Original target, all hooks native to this build. |
| Stremio TV | 1.10.4 | arm64 and armeabi-v7a builds. |
| Stremio TV | 1.12.0 | arm64 and armeabi-v7a (bundle). |
| Stremio phone (com.stremio.one, touch UI) | 2.4.0, 2.5.0 | Touch skip button, Mod settings from the launcher shortcut. |
| Vimu Media Player (net.gtvbox.videoplayer) | 12.00 | Patched with its own set, see Vimu support. |

The eleven Stremio patches that apply by default apply on all five Stremio versions above, and all seven Vimu patches apply on Vimu 12.00. A twelfth Stremio patch, Side-by-side install, is off by default (see Settings and extras). If your copy does not patch cleanly, open an issue and attach the Morphe log.

How well each version is covered:

* **1.6.12** is the reference. Every feature below works here, with the settings as rows in Stremio Settings (Interface for Hide spoilers, Playback for the rest). Two things are only available on 1.6.12: skip marks on the progress bar, and recovering the saved position from an external player after Stremio was restarted (cold start).
* **1.10.4, 1.12.0, 2.4.0, 2.5.0** use a newer Stremio player screen, so the patches hook the app's core layer and player engines instead of the old screens. Intro Skip, Auto-pick, Hide spoilers, Remember tracks, Avoid SDH, External player progress and Binge are all ported. Differences you will notice:
  * Skips use the engine's normal seek, so they can land on the nearest keyframe, about 1-2 s early. 1.6.12 uses an exact seek.
  * No skip marks on the progress bar.
  * The settings live in one "Mod settings" screen instead of rows inside Stremio Settings. TV: first row of Stremio Settings (or the MENU key). Phone: long-press the launcher icon, or App info > settings.
  * Remember tracks keeps the language only. "Subtitles off" is not remembered on these builds, and Stremio's "auto select" setting has to be on.
  * Avoid SDH cleans cue text on the ExoPlayer engine only. With the mpv engine mpv's own SDH filter is switched on, with VLC only the track choice applies.
  * Next episode: when the next episode was prepared while this one played, it starts at once. Otherwise it opens the next episode's stream page and Auto-pick starts it.
* **Phone 2.4.0** applies cleanly like 2.5.0. Most of the checking was done on 2.5.0, so 2.4.0 has had less.
* **1.12.0 and 2.5.0 are new in 1.7.** 1.10.4 and 1.12.0 played with an intro skip and the Next pill on an emulator; 2.5.0 played with prepared next on an emulator. 1.6.12 and Vimu are tested on real hardware. Reports are welcome, see Reporting a problem.
* **Vimu 12.00** is the only supported Vimu version. The native seek-crash fix is built for both armeabi-v7a and arm64-v8a.

External players that Stremio hands over to:

* Tested on an emulator: Vimu, mpv, Just Player.
* Documented from the player's public intent API, not tested here: MX Player, VLC. VLC could not be run on the test emulator at all (no video output), so VLC and MX behaviour is by the book only.
* With no default player set and Vimu not installed, Android shows its chooser. After you pick MX Player in it, MX becomes the remembered player until you use "Forget remembered player" in Mod settings.

## Features

### Skipping

* Skip sources, in this order: the video file's own chapters, AniSkip (anime), TheIntroDB, IntroDB.app (fills the segment types that are still missing), then SkipDB as the last fallback. No API keys are needed. The online sources need internet; chapters work offline.
* Chapters: Opening, OP, Opening Credits, Main Titles, Title Sequence, Theme and similar names. A bare "Intro" chapter is weaker (see Known limits).
* Online timings snap to chapter edges when both exist.
* Credits: the skip jumps to the end of the credits, not to the end of the file, so an after-credits scene still plays. The Next episode pill follows.
* Post-credit scenes are never skipped. In both apps, chapters named post-credit, mid-credit, stinger, epilogue or bonus scene are never skipped, and a credits segment stops where such a chapter begins.
* A chapter literally named "Ending" is treated as story, not credits. It counts as credits only when it starts in the last 20% of the file, runs at most 240 s, and something agrees with it: an online credits segment starting within 15 s, a preview or credits chapter right after it, or it is the last chapter (or runs to the end of the file).
* Previews: a preview from a named chapter or an online source follows the Skip previews setting (auto-skip or button). A preview the patch only infers is button-only, even with previews on and Auto: the short tail after the credits (no chapters, or a generic last chapter such as "Chapter 07") and the first dialogue lines found in the subtitle text. It is never skipped on its own.
* Segments with an unknown end are ignored, and so are intro, recap and credits segments with an unknown start, so incomplete online data can never skip a post-credit scene or a whole episode.
* Movies are never skipped. A title counts as a series only once its id (or Stremio's type) proves it; until then skipping stays off. With the Intro skip switch off nothing is looked up and no skip button shows, but on Stremio the Next episode pill still appears for series.
* Resuming inside an intro still skips. Seeking back on purpose shows a "Skip intro" button instead of skipping again. Remote keys are never blocked.
* Seeking into a segment yourself does not skip you back out: the auto-skip is held and the Skip button shows instead. Playing into the segment normally still skips.
* Own times: this project's own published timings, tried before the online sources. An entry is used only when your file's length matches the length it was measured on (within 2 s); while the length is unknown the entry is held, not guessed.
* When two online sources give the same segment and agree closely, only the part both agree on is skipped, so one long timing cannot eat into the story.
* If sources disagree about a segment at 0:00 (one puts it at the start, another puts the same segment later in the episode), it is not auto-skipped: the Skip button shows instead.
* Credits sent with a start but no end are used only once the runtime is known, only when they start in the last 40% of the file, and only when the source's file length fits this file. Otherwise they are dropped.
* If a source is down the player does not wait for it: the other sources apply at once and only the failed source is retried (after 5 s, 20 s and 60 s, three tries). A late answer is merged without skipping twice.
* Anime matching: the title is resolved to the right season and cour (including "Part 2" style entries) before AniSkip is asked. Shows that are clearly not anime never query the anime databases. Kitsu ids (kitsu:ID:EP) are mapped to the right entry. A recap that starts at 0:00 is kept when its length is plausible (10-180 s).
* Settings: on/off, anime on/off, Auto or Manual, recaps on/off, Skip credits Auto/Button/Off (default Button), Skip previews on/off (default off), Show skip marks on/off (1.6.12 only, default on).
* Skip marks (1.6.12): coloured marks on the progress bar for intro, credits and previews while the controls are visible.

### Streams

* Auto-pick best stream: plays the top-ranked stream without showing the list. Rules: never a CAM, telesync or screener; never a stream for another season or episode; never an addon warning card or external link; a single-episode file before a season pack (a pack with the exact episode file counts as that file); cached before uncached; then resolution (a 4K label such as 2160p or 4K is required, a bare "UHD" is not enough), HDR or Dolby Vision, size, codec, seeders. There is no size cap. If every stream is bad the list stays on screen. Short release names such as "ReZero" match "Re:Zero", and "Agents of SHIELD" matches the Marvel title.
* Auto-pick waits 4 s when a cached stream is there and 8 s otherwise. Pressing Back from the player no longer starts the same stream again. Settings: on/off (default on) and Always show stream list (default off). Long-press an episode to open its list once.
* Instant next: while an episode plays, the next episode's best stream is prepared in the background, so Next, the end of the episode or Binge starts it at once instead of waiting on the stream list. Needs Auto-pick on and Always show stream list off; there is a separate on/off setting for preparing.
* Hide spoilers: optional switch that hides thumbnails and synopses of unwatched episodes and shows "Episode N" instead of unwatched titles.

### Tracks and subtitles

* Remember tracks per show: the last audio and subtitle language for a show is reused for its other episodes. The memory is shared between Stremio's player and Vimu, for IMDb and Kitsu ids, so a choice made in one is applied in the other. Subtitles-off chosen in Vimu is remembered for Vimu. Movies keep a memory under their IMDb id too, and in Vimu it is applied for movies and with the Intro skip switch off (only skipping is off then).
* The remembered tracks win at the start of an episode. On 1.6.12 they are set as the player's preferred audio and subtitle languages before playback starts, so the first track choice is already right and nothing switches (and rebuffers) after it begins; only the subtitle kind (full dialogue, not SDH or signs) is corrected afterwards, subtitles only. Vimu applies the shared memory before its own default rules. A track you pick yourself afterwards is kept and stored, and is not overridden again. On the newer Stremio builds the memory fills in a track the app's own saved stream state does not have.
* Default audio: with no memory, Vimu prefers Japanese audio for anime, then your Stremio audio language, then English, so a dub is not picked just because it is the first track. Anime also gets a full-dialogue subtitle in your language rather than Signs and Songs.
* Avoid SDH subtitles: picks full dialogue subtitles over hearing-impaired tracks and strips sound notes when no other track remains. In Vimu the same rule applies to the file's embedded tracks.
* On 1.6.12, addon subtitles that arrive after playback has started no longer re-open or freeze the stream. They are drawn directly, about a second or two after the addon answers, and the subtitle menu lists them.
* Addon subtitles in Vimu: Stremio passes the subtitle you chose plus up to five more (your language first, then English). If the file has no good embedded track in your language, Vimu loads the best addon subtitle itself. The addon subtitles are also listed in Vimu's own subtitle menu. The GREEN or GUIDE remote key cycles addon, embedded and off.

### External players and progress

* Stremio passes the show, episode, start position and subtitles to the external player, so Vimu skips intros and picks the right subtitle.
* Progress is saved for Vimu, mpv, Just Player and the players that return a result (MX Player, VLC). Stremio stays alive while the player is open. Back or Home in the player never marks the episode as finished; an episode is marked watched at 90% or on a real completion.
* Vimu writes its position to a journal every 10 s, so progress survives a crash or being killed. Pausing in Vimu also saves the position at once. Stremio reads it while the player is open and when Stremio is started again (cold start recovery, 1.6.12 only).
* Binge with external players: when an episode really finishes (the player says it ended, or it reached the last minute), the next episode starts in the same player. Back, Home or a stop before the end never starts the next episode. Setting on/off (default on). Series only.
* Players that return no position on a normal end (MX Player, Just Player, Next Player) are detected through the end flag and the known runtime.

### Settings and extras

* Mod settings screen with every setting of these patches (see the version notes above for how to open it). Row "Show skip marks" switches the progress-bar marks (they exist on 1.6.12 only). Row "Forget remembered player" clears the remembered external player so Android asks again.
* Vimu native fixes: a 2 byte fix in libmediaengine.so for a native crash on rapid seeks, for both armeabi-v7a and arm64-v8a (the arm64 build is new in 1.7), plus deferred prepare for the tunneling crash on cold start, media session release on exit and seek timer cancel.
* Smoother playback, in Stremio's own player: frame-rate matching (Seamless only, Always or Off) under a Match frame rate setting, so the display switch does not blank the picture with a popup in Seamless mode.
* Larger playback buffer, in Stremio's own player: 50-120 s of read-ahead, up to 256 MB, on by default, for fewer stalls on high-bitrate streams.
* Playback diary: an on/off setting (on by default) for a small local log of playback, kept in the app's external files folder and readable with `adb pull`. No stream addresses or accounts are written to it.
* Side-by-side install (off by default): enable this patch in Morphe to install the patched app as "Stremio Morphe" next to stock Stremio instead of replacing it.
* Optional "Enable debug mode" option on the Intro Skip patch opens a log window when playback starts.

## Install with Morphe Manager

1. Add this repo as a patch source in Morphe Manager:
   https://morphe.software/add-source?github=tikkamasalla/stremio-morphe-patches
2. Pick the Stremio app (or Vimu), enable the patches you want (for example "Intro Skip"), patch and install.
3. Optional: the "Enable debug mode" patch option opens a log window when playback starts, useful to see the detected title, season, episode and API responses.

## Known limits

* Skip marks and cold start recovery are 1.6.12 only; on the newer builds skips can land 1-2 s early (see Supported apps and versions).
* File chapters are read from Matroska/WebM files only (MP4 and HLS have none for these patches), including nested chapters (a chapter with sub-chapters) on Stremio. Chapters need an http(s) stream.
* A chapter named just "Intro" is treated as weak. It only counts when AniSkip, TheIntroDB or IntroDB.app has an intro that agrees with it within 15 s. If no online source has an intro, a lone "Intro" chapter counts in Vimu only when it is 20-150 s long and does not start at 0:00 (a chapter at 0:00 is usually a cold open). On Stremio it is not used when no online source has data.
* If an online source has nothing for an episode (new releases, rarely watched shows), there is nothing to skip. The keyless Cinemeta name lookup is remembered for 60 days.
* The IMDb id comes from Stremio's own video id. Kitsu ids work for AniSkip, but IntroDB.app and SkipDB need an IMDb id, so they are not asked for Kitsu-only anime.
* Remember tracks in Stremio's own player stores languages, not exact tracks. Vimu also matches the track label.
* Stremio's own "subtitles off" is not recorded.
* Vimu: the audio and subtitle menus are Vimu's own, so the choice shown there may lag the active track for a moment after our automatic pick.
* External players: Vimu reports the end position as the full duration when a file ends early (broken or truncated files), so that case looks like a real completion. MX Player and VLC are untested here. VLC and mpv give no result on a normal end, so a runtime is needed to tell an early quit from a real end; with an unknown runtime it counts as not finished and no next episode starts.
* Exact seek (1.6.12 only): if the exact seek fails it falls back to a normal seek.
* The prepared-next limit is 25 s: a prepared stream that has not started by then is given up on. A start you paused is not counted.
* On the 2.5.0 player a dead prepared stream waits out the full 25 s.
* Online credits times can run over a mid-credits or post-credits scene. Credits over 180 s from an online source show the Skip button even in Auto mode, but a shorter window can still contain a scene.
* Hide spoilers only changes what Stremio displays; the currently playing unwatched episode is hidden too.
* Dependent on Stremio's own settings: Remember tracks needs "auto select" on (newer builds), Auto-pick needs the stream list setting off.

## Vimu support

The same bundle carries seven patches for Vimu 12.00 (net.gtvbox.videoplayer):

* Intro Skip (Vimu): file chapters, AniSkip, TheIntroDB, IntroDB.app (per missing segment type) and SkipDB, with the rules above. Auto mode skips at once, Manual mode shows a Skip button. AniSkip openings and endings are both used. Rows in Vimu Settings: Skip intros, anime openings, Auto/Manual mode, recaps, credits Auto/Button/Off, previews, and the preferred audio language.
* External player result (Vimu): returns position and duration so Stremio saves progress, and Back or Home never counts as finished.
* Progress journal provider (Vimu): a read-only provider that only Stremio can read, used for crash-proof progress.
* Intro Skip native fix (Vimu): the libmediaengine fix for both CPU types.
* Intro Skip settings row (Vimu): the settings entry.
* Addon subtitles in subtitle menu (Vimu): lists Stremio's addon subtitles in Vimu's own menu.
* Re-sign check (Vimu): lets the patched app run after re-signing. Stock Vimu exits when the signing certificate differs, which every patched build does. Runs with Intro Skip (Vimu).

Patch your own copy of the Vimu 12.00 APK. Copies that were already modified by someone else may carry extra checks that this patch does not remove, so test on your own copy.

## What the patches connect to

Besides Stremio's own traffic, your addons and your debrid service, the patched code only contacts:

* Skip times: api.aniskip.com, api.theintrodb.org, api.introdb.app, api.skipdb.tv, and raw.githubusercontent.com for this project's own times file.
* Finding the right show: graphql.anilist.co, kitsu.app, v3-cinemeta.strem.io.

Lookups send the show id or title, season, episode and, where the source uses it, the file's length. No account, stream address or debrid key is sent to any of these. The list comes from the patch source, checked against the DNS log of a test device.

## Reporting a problem

Open an issue with the app version, device, the Morphe log if patching failed, and the relevant logcat lines. Useful tags: IntroSkip, IntroSkipPort (hooks on 1.10.4 and newer), AutoPick, TrackMemory, ExtProgress, BingeDbg, SdhAvoid, ModSettings. Vimu: VimuIntroSkip, VimuResult, VimuTracks, VimuAddonSubs, VimuSubDialog, VimuFix. For example:

```bash
adb logcat -s IntroSkip IntroSkipPort AutoPick TrackMemory ExtProgress BingeDbg SdhAvoid ModSettings
```

## Build locally

```bash
./gradlew :patches:build
```

The bundle lands in `patches/build/libs/patches-*.mpp`. Apply it with Morphe Desktop.

<!-- PATCHES_START -->
> **[v1.7.1-dev.1](https://github.com/tikkamasalla/stremio-morphe-patches/releases/tag/v1.7.1-dev.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;19 patches total
<details open>
<summary>📦 Vimu&nbsp;&nbsp;•&nbsp;&nbsp;7 patches</summary>
<br>

**🎯 Supported versions:**

| 🧪&nbsp;12.00 ST | 🧪&nbsp;12.00 |
| :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Addon subtitles in subtitle menu (Vimu)](#addon-subtitles-in-subtitle-menu-vimu) | Lists the addon subtitles from Stremio in Vimu's subtitle menu. Runs with the Intro Skip (Vimu) patch. |  |
| [External player result (Vimu)](#external-player-result-vimu) | Returns position and duration from Vimu so Stremio saves progress. Runs with the Intro Skip (Vimu) patch. |  |
| [Intro Skip (Vimu)](#intro-skip-vimu) | Adds intro and recap skip for TV shows, movies and anime in Vimu.<br><br>Intro Skip v1.0.0<br>Repository: https://github.com/tikkamasalla/stremio-morphe-patches |  |
| [Intro Skip native fix (Vimu)](#intro-skip-native-fix-vimu) | Fixes native crashes on rapid seeks in the Vimu media engine. Runs with the Intro Skip (Vimu) patch. |  |
| [Intro Skip settings row (Vimu)](#intro-skip-settings-row-vimu) | Adds the Intro Skip settings to Vimu Settings. Runs with the Intro Skip (Vimu) patch. |  |
| [Progress journal provider (Vimu)](#progress-journal-provider-vimu) | Declares the progress journal provider. Runs with External player result (Vimu). |  |
| [Re-sign check (Vimu)](#re-sign-check-vimu) | Lets the app run after re-signing. |  |

</details>

<details open>
<summary>📦 Stremio&nbsp;&nbsp;•&nbsp;&nbsp;12 patches</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Auto-pick best stream](#auto-pick-best-stream) | Skips the stream list and plays the top ranked stream. Enable Always show stream list to see every stream. |  |
| [Avoid SDH subtitles](#avoid-sdh-subtitles) | Prefers full dialogue subtitles over SDH/CC tracks and strips sound notes when none remain.<br><br>Avoid SDH v1.0.0<br>Repository: https://github.com/tikkamasalla/stremio-morphe-patches |  |
| [Binge with external players](#binge-with-external-players) | Auto-plays the next episode in the same external player when an episode finishes in Vimu, MX Player, VLC or mpv. |  |
| [External player progress](#external-player-progress) | Saves watch progress in Stremio when playing in an external player, and starts the player at the saved position. |  |
| [External player progress manifest](#external-player-progress-manifest) | Declares the keep-alive service for External player progress. Runs with that patch. |  |
| [Hide spoilers](#hide-spoilers) | Hides thumbnails and synopses for unwatched episodes in Stremio.<br><br>Hide spoilers v1.0.0<br>Repository: https://github.com/tikkamasalla/stremio-morphe-patches |  |
| [Intro Skip](#intro-skip) | Adds intro and recap skip controls for TV shows, movies and anime in Stremio.<br><br>Intro Skip v1.0.0<br>Repository: https://github.com/tikkamasalla/stremio-morphe-patches | • Enable debug mode |
| [Mod settings](#mod-settings) | Adds one Mod settings screen with every setting of these patches: from Stremio Settings on TV 1.10.4/1.12.0, from the launcher shortcut or App info on phone 2.x. |  |
| [Mod settings manifest](#mod-settings-manifest) | Declares the Mod settings screen in the manifest. Runs with Mod settings. |  |
| [Remember tracks per show](#remember-tracks-per-show) | Remembers the last audio and subtitle choice for each series and applies it to other episodes. |  |
| [Side-by-side install](#side-by-side-install) | Installs the patched app separately as Stremio Morphe (package com.stremio.one.morphe) so it can sit next to the normal Stremio instead of replacing it. Off by default. |  |
| [Smoother playback](#smoother-playback) | Frame-rate matching (Seamless only / Always / Off), a larger, debrid-friendly playback buffer, and no stream re-open freeze when arriving addon subtitles would not change what is on screen, for Stremio's own player. |  |

</details>

<!-- PATCHES_END -->

## Credits

Started from aniskip-stremio by AzukiSensei (https://github.com/AzukiSensei/aniskip-stremio), GPLv3. Rewritten with chapter-based skipping, IntroDB support and new skip logic.

SDH annotation rules adapted from liongalahad-nuviotv-morphe-patches (GPLv3).

## License

GPLv3, see [LICENSE](LICENSE).
