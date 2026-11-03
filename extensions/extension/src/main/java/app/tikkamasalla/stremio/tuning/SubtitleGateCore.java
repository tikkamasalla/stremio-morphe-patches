package app.tikkamasalla.stremio.tuning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure decision for the subtitle re-open gate, unit tested standalone: may stock replace the media item
 * to add just-delivered addon subtitles now? Stock re-opens the stream on every subtitle-count change
 * (0.5-1.3 s frozen picture), usually for subtitles nothing will show.
 *
 * <p>Before the first frame the item goes in at once, as stock. After it, a re-open needs one of: the
 * core's saved pick or TrackMemory's remembered subtitle is in this delivery, the delivery beats the
 * selection in Media3's text ranking (nothing selected, or a better preferred language), or a ranking
 * that acts on it (SdhAvoid, the selector's forced and caption rules) would replace the selection. Text
 * off, a text override and an equal-language track already showing hold. A reason fires once per media.
 * Addon items carry only id, mime, language and label (PlayerFragment.onPlayerState), so Media3's flag
 * and role paths cannot fire for them. Languages arrive Media3-normalized.
 */
final class SubtitleGateCore {
    private static final String WHY_MEMORY = "memory:";

    static final class Sub {
        final String id;
        final String lang;
        final String label;
        final int kind;

        Sub(String id, String lang, String label, int kind) {
            this.id = id;
            this.lang = lang;
            this.label = label;
            this.kind = kind;
        }
    }

    /** One look at the player. pickId: null = could not read, "" = no saved pick. */
    static final class In {
        List<String> curIds = new ArrayList<String>();
        List<Sub> newSubs = new ArrayList<Sub>();
        boolean tracksKnown = true;
        boolean textDisabled;
        boolean textOverride;
        boolean textSelected;
        String selLang = "";
        String selLabel = "";
        int selKind;
        boolean selBitmap;
        boolean rankable;
        List<String> preferred = new ArrayList<String>();
        String pickId = "";
        String memLang = "";
        String memLabel = "";
    }

    /** Per-media gate state, driven by the wrapper's events. */
    static final class State {
        String media;
        boolean firstFrame;
        /** A re-open was started and its tracks are not known yet. */
        boolean awaitingTracks;
        /** Addon id to select once its re-open has landed: neither stock nor the selector would. */
        String target;
        String lastWhy = "";
        /** The media a subtitle dialog is waiting on for its tracks; null = none waiting. */
        private String dialogMedia;
        private Object held;
        private String heldKey = "";
        private final Set<String> spent = new HashSet<String>();

        /** A different media must not inherit anything from the previous one. */
        void sync(String mediaKey) {
            if (media != null && !media.equals(mediaKey)) {
                firstFrame = false;
                awaitingTracks = false;
                target = null;
                dialogMedia = null;
                held = null;
                heldKey = "";
                spent.clear();
            }
            media = mediaKey;
        }

        void dialogWaits(String mediaKey) {
            dialogMedia = mediaKey;
        }

        /** Back, a dismissed dialog or a closed screen: nothing is shown later. */
        void dialogCancel() {
            dialogMedia = null;
        }

        /** The waiting dialog may be shown now: once, and only on the media it was opened on. */
        boolean dialogDue(String mediaKey) {
            boolean due = dialogMedia != null && dialogMedia.equals(mediaKey);
            dialogMedia = null;
            return due;
        }

        /** Same delivery as the held one (stock retries on every state update): nothing to judge. */
        boolean holdsSame(List<Sub> subs, Object item) {
            if (held == null || !firstFrame || !heldKey.equals(keyOf(subs))) return false;
            held = item;
            return true;
        }

        /** Why the item goes to the player now, or null: it is held. */
        String offer(In in, Object item) {
            String why = judge(in, firstFrame, spent);
            if (why != null) {
                commit(why);
                return why;
            }
            held = item;
            heldKey = keyOf(in.newSubs);
            return null;
        }

        /** Judges the held item again (tracks became known or changed): the item to apply now, or null. */
        Object rejudge(In in) {
            String why = held == null ? null : judge(in, firstFrame, spent);
            return why == null ? null : commit(why);
        }

        /** The viewer opened the subtitle dialog: whatever is held goes in, whatever the judgement. */
        Object takeHeld() {
            return held == null ? null : commit("dialog");
        }

        Object held() {
            return held;
        }

        private Object commit(String why) {
            Object item = held;
            held = null;
            heldKey = "";
            awaitingTracks = true;
            lastWhy = why;
            spent.add(why);
            target = why.startsWith(WHY_MEMORY) ? why.substring(WHY_MEMORY.length()) : null;
            return item;
        }
    }

    private SubtitleGateCore() {
    }

    /** Reason to apply now, or null to hold. {@code spent}: reasons already paid for on this media. */
    static String judge(In in, boolean firstFrame, Set<String> spent) {
        if (!firstFrame) return "pre-frame";
        // Mid re-open the selection is unknown: the wrapper judges again once tracks are known.
        if (!in.tracksKnown || in.textDisabled || in.textOverride) return null;
        Set<String> cur = new HashSet<String>(in.curIds);
        List<Sub> added = new ArrayList<Sub>();
        for (Sub s : in.newSubs) {
            if (!cur.contains(s.id)) added.add(s);
        }
        for (String why : reasons(in, added)) {
            if (!spent.contains(why)) return why;
        }
        return null;
    }

    private static List<String> reasons(In in, List<Sub> added) {
        List<String> out = new ArrayList<String>();
        if (added.isEmpty()) return out;
        if (in.pickId == null) out.add("stream-unknown");
        int[] sel = in.textSelected ? rank(in.selLang, in) : null;
        for (Sub s : added) {
            if (in.pickId != null && in.pickId.length() > 0 && stripPeriod(in.pickId).equals(s.id)) out.add("pick:" + s.id);
            if (in.memLabel.length() > 0 && in.memLabel.equals(s.label) && !in.memLabel.equals(in.selLabel)
                    && sameLang(s.lang, in.memLang)) out.add(WHY_MEMORY + s.id);
        }
        for (Sub s : added) {
            int[] r = rank(s.lang, in);
            if (r[0] == Integer.MAX_VALUE) continue;
            if (sel == null || (!sameLang(s.lang, in.selLang) && (r[0] < sel[0] || (r[0] == sel[0] && r[1] > sel[1])))) {
                out.add("lang:" + in.preferred.get(r[0]));
            }
        }
        for (Sub s : added) {
            if (in.rankable && sel != null && sameLang(s.lang, in.selLang)
                    && (s.kind < in.selKind || (s.kind == in.selKind && in.selBitmap))) out.add("rank:" + in.selLang);
        }
        return out;
    }

    /**
     * Is this track the viewer's saved choice: the core's saved pick, or TrackMemory's remembered addon subtitle
     * (same label, same language)? SdhAvoid leaves such a track alone; with no saved pick it ranks as before.
     */
    static boolean isSaved(String pickId, String memLabel, String memLang, String id, String label, String lang) {
        if (pickId != null && pickId.length() > 0 && id != null && stripPeriod(pickId).equals(stripPeriod(id))) return true;
        return memLabel != null && memLabel.length() > 0 && memLabel.equals(label) && sameLang(lang, memLang);
    }

    /** {index into preferred, score} as DefaultTrackSelector.TextTrackInfo computes them. */
    private static int[] rank(String lang, In in) {
        for (int i = 0; i < in.preferred.size(); i++) {
            int score = langScore(lang, in.preferred.get(i), false); // the app never sets selectUndetermined
            if (score > 0) return new int[]{i, score};
        }
        return new int[]{Integer.MAX_VALUE, 0};
    }

    /** Media3 getFormatLanguageScore on already-normalized codes. */
    static int langScore(String formatLang, String preferred, boolean allowUndetermined) {
        if (preferred != null && preferred.length() > 0 && preferred.equals(formatLang)) return 4;
        String p = undToNull(preferred);
        String f = undToNull(formatLang);
        if (f == null || p == null) return allowUndetermined && f == null ? 1 : 0;
        if (f.startsWith(p) || p.startsWith(f)) return 3;
        return primary(f).equals(primary(p)) ? 2 : 0;
    }

    /** en and en-US are one language; zh-Hant and zh-Hans are not. */
    static boolean sameLang(String a, String b) {
        if (a == null || b == null || a.length() == 0 || b.length() == 0) return false;
        return a.equals(b) || (primary(a).equals(primary(b)) && (a.indexOf('-') < 0 || b.indexOf('-') < 0));
    }

    /**
     * A track id without MergingMediaPeriod's "period:" prefix (0 is the stream, i+1 is subtitle
     * configuration i): "3:http://x" and "http://x" are one subtitle, "0:5" and "5" one track.
     */
    static String stripPeriod(String id) {
        if (id == null) return "";
        int colon = id.indexOf(':');
        for (int i = 0; i < colon; i++) {
            if (id.charAt(i) < '0' || id.charAt(i) > '9') return id;
        }
        return colon > 0 ? id.substring(colon + 1) : id;
    }

    private static String keyOf(List<Sub> subs) {
        StringBuilder sb = new StringBuilder();
        for (Sub s : subs) sb.append(s.id).append('\n');
        return sb.toString();
    }

    private static String undToNull(String lang) {
        return lang == null || lang.length() == 0 || "und".equals(lang) ? null : lang;
    }

    private static String primary(String lang) {
        int dash = lang.indexOf('-');
        return dash < 0 ? lang : lang.substring(0, dash);
    }
}
