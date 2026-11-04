# Stremio Morphe Patches

Patches for Stremio and Vimu that skip intros, recaps, and credits, auto-pick the best stream, filter out SDH subtitles (subtitles for the deaf and hard of hearing), and sync playback progress and language choices with external players. Built for Android TV, with phone support added in 1.7. You apply them to your own APK using Morphe, an Android app patcher.

## Install

1. Add this repository as a patch source in Morphe Manager: https://morphe.software/add-source?github=tikkamasalla/stremio-morphe-patches
2. Select Stremio (or Vimu) and enable the patches you want, such as "Intro Skip".
3. Patch and install.

By default, the patched build replaces your existing Stremio app. To keep both installed, enable the `Side-by-side install` patch (off by default). It installs the patched build as "Stremio Morphe" (`com.stremio.one.morphe`) alongside the original app.

For Vimu, always patch an untouched Vimu 12.00 APK. Pre-modified APKs from third parties often include extra tamper checks that these patches do not strip. Stock Vimu also exits whenever its signing certificate changes, which happens on every patched build. The `Re-sign check (Vimu)` patch removes that check so the app runs normally.

## Supported versions

| App | Version | Notes |
|---|---|---|
| Stremio TV (com.stremio.one, Android TV) | 1.6.12 | Reference version. Every feature works here. |
| Stremio TV | 1.10.4 | arm64 and armeabi-v7a builds. |
| Stremio TV | 1.12.0 | arm64 and armeabi-v7a (bundle). |
| Stremio phone (com.stremio.one, touch UI) | 2.4.0, 2.5.0 | Touch skip button. Mod settings from the launcher shortcut. |
| Vimu Media Player (net.gtvbox.videoplayer) | 12.00 | Seven patches of its own. |

Eleven Stremio patches are enabled by default and work across all five supported Stremio versions. A twelfth patch, `Side-by-side install`, is off by default. Support for 1.12.0 and 2.5.0 arrived in 1.7. If your APK fails to patch, open an issue and attach the Morphe log.

On every Stremio version except 1.6.12, skips can land 1-2 s early and the progress bar does not display skip marks. All patch options live in a single Mod settings screen. On TV, open it from the top row of Stremio Settings or press the MENU key. On a phone, long-press the app launcher icon. See the [feature guide](docs/FEATURES.md) for a full comparison across versions.

## What you get

**Skipping.** Intros, recaps, and credits skip automatically, or show an on-screen Skip button in Manual mode. Timestamps come from embedded video chapters first, then [IntroKiller Engine](https://github.com/tikkamasalla/introkiller-engine), and finally AniSkip, TheIntroDB, IntroDB.app, and SkipDB. No API keys are needed. Movies and post-credit scenes are never skipped.

**Working out missing skip times.** This optional setting (off by default) fills in the blanks when no online database has timestamps for an episode. While you watch one episode, the app analyzes the next episode in the background by comparing its audio against other episodes of the same show. Detected timestamps act as the lowest-priority source and respect your Auto or Manual preference.

| Tested on one Streamer, real streams | Time for one episode | Data read through your debrid service | Result |
|---|---:|---:|---|
| 1080p, comparison episodes already stored | about 6 minutes | about 0.4 GB | Intro skipped automatically |
| 1080p, one comparison episode to fetch | about 19 minutes | about 0.9 GB | Intro and credits found, correct on frames |
| 4K remux, nothing stored | about 40 minutes | about 5 GB | Finished, found nothing on that show |

Each result applies strictly to the exact file that was analyzed, and each episode gets at most one attempt per day. If a slow analysis finishes after you have already started the next episode, its intro skip may not be ready in time.

Three related upgrades arrived in 1.8:

| Feature | What it does | What we measured |
|---|---|---|
| Borrow chapters from other releases (off by default) | Copies intro and credits marks from another release of the same episode after an audio check verifies that both files line up. | Offline test of 40 file pairs: 44 of 45 copied segments landed within 1 s, median error 0.001 s, no story cut. All 62 deliberately mismatched cases were refused. It has not yet produced a result on a real device. |
| Anime theme-song backup (runs with Work out missing skip times) | Finds a missing intro or credits by matching the show's official theme songs. Needs only one episode, works only for shows with Kitsu ids, and never overrides another source. | The PC version, used alone on 123 episodes, found 82 of 114 intros and 80 of 105 credits with no real wrong skips, and rescued Blood+ from 0 of 5 intros to 5 of 5. The app's matcher agreed with it on all 99 episodes compared. It has not yet run on a real device. |
| Early outro fix | Prevents an outro near the start of an episode from being offered as "Skip intro". An opening that TheIntroDB mislabels as credits is still treated as the intro, because that case happens in the wild. | Covered by self-tests in both Stremio's player and Vimu. |

**Streams.** `Auto-pick best stream` launches the highest-ranked stream immediately without showing the stream list. It never picks a CAM, telesync, or screener.

**Tracks and subtitles.** Both Stremio's built-in player and Vimu remember your chosen audio and subtitle languages per show. `Avoid SDH subtitles` prefers standard dialogue tracks over hearing-impaired subtitles.

**External players.** Stremio passes the show title, episode, start position, and subtitles to your external player, records your playback progress, and automatically starts the next episode in the same player.

The patches also include `Hide spoilers`, `Smoother playback`, and a unified Mod settings screen.

## Known limits

* Embedded file chapters are read only from Matroska and WebM files streamed over `http(s)`.
* If no online source has timestamps for an episode (such as a brand-new release), nothing can be skipped unless `Work out missing skip times` is enabled.
* Online credits timestamps can occasionally skip over a mid-credits or post-credits scene.
* `Remember tracks` requires Stremio's built-in "auto select" option to be enabled on newer builds. `Auto-pick` requires `Always show stream list` to be turned off.

## What the patches connect to

Outside of Stremio's normal traffic, your addons, and your debrid service, the patches connect only to these hosts:

* **Skip timestamps:** `api.aniskip.com`, `api.theintrodb.org`, `api.introdb.app`, `api.skipdb.tv`, and `raw.githubusercontent.com` (for this project's published timings file).
* **Anime theme-song backup (only when `Work out missing skip times` is on):** `kitsu.app`, `graphql.anilist.co`, `graphql.animethemes.moe`, and AnimeThemes' audio host.
* **Show matching:** `graphql.anilist.co`, `kitsu.app`, and `v3-cinemeta.strem.io`.

Requests send only the show id or title, season, episode number, and (where used by the source) the file duration. No account credentials, stream URLs, or debrid keys are ever sent.

## Help

Open an issue with your app version, device model, the Morphe log if patching failed, and the relevant logcat lines. The feature guide lists the log tags to filter by. To build the patches from source, run `./gradlew :patches:build` and load the generated bundle (`patches/build/libs/patches-*.mpp`) in Morphe Desktop.

<!-- PATCHES_START -->
> **[v1.8.0](https://github.com/tikkamasalla/stremio-morphe-patches/releases/tag/v1.8.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;19 patches total
<details open>
<summary> Vimu&nbsp;&nbsp;•&nbsp;&nbsp;7 patches</summary>
<br>

**Supported versions:**

| 12.00 ST | 12.00 |
| :---: | :---: |

| Patch | Description | Options |
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
<summary> Stremio&nbsp;&nbsp;•&nbsp;&nbsp;12 patches</summary>
<br>

| Patch | Description | Options |
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

Started from [aniskip-stremio](https://github.com/AzukiSensei/aniskip-stremio) by AzukiSensei, GPLv3. Rewritten with chapter-based skipping, IntroDB support and new skip logic. The SDH annotation rules are adapted from liongalahad-nuviotv-morphe-patches (GPLv3).

## License

GPLv3, see [LICENSE](LICENSE).
