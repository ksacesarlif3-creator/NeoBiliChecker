package com.minshawi.murattal.lost;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Loads the exact Quran word boundaries used by Qur'anic Universal Audio and
 * the word timestamps for Muhammad Siddiq al-Minshawi's 1967 second/new
 * murattal recording. These data are packaged inside the APK.
 */
public final class QuranSyncData {
    public static final class Ayah {
        public final int number;
        public final String text;
        public final int page;

        Ayah(int number, String text, int page) {
            this.number = number;
            this.text = text;
            this.page = page;
        }
    }

    public static final class WordMoment {
        /** a:NN for Quran ayah, b for basmala opener, i for isti'adha opener. */
        public final String key;
        public final int ayah;
        public final int wordIndex;
        public final int startMs;
        public final int endMs;

        WordMoment(String key, int ayah, int wordIndex, int startMs, int endMs) {
            this.key = key;
            this.ayah = ayah;
            this.wordIndex = wordIndex;
            this.startMs = startMs;
            this.endMs = endMs;
        }
    }

    @SuppressWarnings("unchecked")
    private final ArrayList<WordMoment>[] moments = new ArrayList[114];
    private JSONObject quranText;
    private JSONObject quranPages;
    private JSONObject catalog;
    private JSONObject specials;
    private final int[] offsets = new int[114];

    private QuranSyncData() {
        for (int i = 0; i < 114; i++) moments[i] = new ArrayList<>();
    }

    public static QuranSyncData load(Context context) throws Exception {
        QuranSyncData d = new QuranSyncData();
        d.quranText = new JSONObject(readAsset(context, "quran_display.json", false));
        d.quranPages = new JSONObject(readAsset(context, "quran_pages.json", false));
        d.catalog = new JSONObject(readAsset(context, "minshawi_catalog.json", false));
        d.specials = new JSONObject(readAsset(context, "minshawi_specials.json", false));
        d.loadOffsets();
        d.loadWordMoments(context);
        d.loadSpecialMoments();
        for (int i = 0; i < 114; i++) {
            Collections.sort(d.moments[i], Comparator.comparingInt(a -> a.startMs));
        }
        return d;
    }

    private static String readAsset(Context context, String name, boolean gzip) throws Exception {
        try (InputStream raw = context.getAssets().open(name);
             InputStream in = gzip ? new GZIPInputStream(raw) : raw;
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[32 * 1024];
            int n;
            while ((n = br.read(buf)) != -1) sb.append(buf, 0, n);
            return sb.toString();
        }
    }

    private void loadOffsets() {
        JSONObject audio = catalog.optJSONObject("audio");
        JSONObject o = audio == null ? null : audio.optJSONObject("chapter_offsets_ms");
        if (o == null) return;
        for (int s = 1; s <= 114; s++) offsets[s - 1] = o.optInt(String.valueOf(s), 0);
    }

    private void loadWordMoments(Context context) throws Exception {
        // Use a .bin extension so Android's asset packager does not strip the .gz suffix.
        JSONObject doc = new JSONObject(readAsset(context, "minshawi_word_timestamps.bin", true));
        JSONArray rows = doc.getJSONArray("rows");
        for (int r = 0; r < rows.length(); r++) {
            JSONArray row = rows.getJSONArray(r);
            String ref = row.getString(0);
            int colon = ref.indexOf(':');
            if (colon < 1) continue;
            int surah;
            int ayah;
            try {
                surah = Integer.parseInt(ref.substring(0, colon));
                ayah = Integer.parseInt(ref.substring(colon + 1));
            } catch (Exception ignored) {
                continue;
            }
            if (surah < 1 || surah > 114) continue;
            int offset = offsets[surah - 1];
            JSONArray words = row.optJSONArray(5);
            if (words == null) continue;
            for (int j = 0; j < words.length(); j++) {
                JSONArray w = words.optJSONArray(j);
                if (w == null || w.length() < 3) continue;
                moments[surah - 1].add(new WordMoment(
                        "a:" + ayah,
                        ayah,
                        w.optInt(0),
                        w.optInt(1) + offset,
                        w.optInt(2) + offset
                ));
            }
        }
    }

    private void loadSpecialMoments() {
        JSONObject chapters = specials.optJSONObject("chapters");
        if (chapters == null) return;
        for (int s = 1; s <= 114; s++) {
            JSONObject chapter = chapters.optJSONObject(String.valueOf(s));
            if (chapter == null) continue;
            if (s != 1) addSpecial(s, chapter.optJSONObject("basmala"), "b");
            addSpecial(s, chapter.optJSONObject("istiadha"), "i");
        }
    }

    private void addSpecial(int surah, JSONObject obj, String key) {
        if (obj == null) return;
        JSONArray words = obj.optJSONArray("words");
        if (words == null) return;
        int offset = offsets[surah - 1];
        for (int i = 0; i < words.length(); i++) {
            JSONArray w = words.optJSONArray(i);
            if (w == null || w.length() < 3) continue;
            moments[surah - 1].add(new WordMoment(
                    key,
                    0,
                    w.optInt(0),
                    w.optInt(1) + offset,
                    w.optInt(2) + offset
            ));
        }
    }

    public List<Ayah> getSurah(int surah) {
        ArrayList<Ayah> out = new ArrayList<>();
        if (surah < 1 || surah > 114) return out;
        for (int ayah = 1; ayah < 400; ayah++) {
            String k = key(surah, ayah);
            if (!quranText.has(k)) break;
            out.add(new Ayah(ayah, quranText.optString(k, ""), quranPages.optInt(k, 0)));
        }
        return out;
    }

    public String getAyahText(int surah, int ayah) {
        return quranText.optString(key(surah, ayah), "");
    }

    public int getPage(int surah, int ayah) {
        return quranPages.optInt(key(surah, ayah), 0);
    }

    public List<WordMoment> getMoments(int surah) {
        if (surah < 1 || surah > 114) return Collections.emptyList();
        return moments[surah - 1];
    }

    public int firstWordStartMs(int surah, int ayah) {
        String key = "a:" + ayah;
        int best = Integer.MAX_VALUE;
        for (WordMoment w : getMoments(surah)) {
            if (w.key.equals(key) && w.startMs < best) best = w.startMs;
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    public int chapterOffsetMs(int surah) {
        return surah >= 1 && surah <= 114 ? offsets[surah - 1] : 0;
    }

    /** Returns the exact source audio URL used to create the timing data. */
    public String exactAudioUrl(int surah) {
        if (surah < 1 || surah > 114 || catalog == null) return null;
        JSONObject audio = catalog.optJSONObject("audio");
        if (audio == null) return null;
        Object raw = audio.opt("chapter_urls");
        String url = null;
        if (raw instanceof JSONObject) {
            url = ((JSONObject) raw).optString(String.valueOf(surah), null);
        } else if (raw instanceof JSONArray) {
            JSONArray a = (JSONArray) raw;
            if (a.length() >= surah) url = a.optString(surah - 1, null);
        }
        if (url == null || url.isEmpty()) return null;

        Matcher m = Pattern.compile("/d/([^/]+)").matcher(url);
        if (m.find()) {
            return "https://drive.usercontent.google.com/download?id=" + m.group(1)
                    + "&export=download&confirm=t";
        }
        return url;
    }

    public String basmalaText() {
        return getAyahText(1, 1);
    }

    public String istiadhahText() {
        return "أَعُوذُ بِٱللَّهِ مِنَ ٱلشَّيْطَانِ ٱلرَّجِيمِ";
    }

    private static String key(int surah, int ayah) {
        return String.format(Locale.US, "%03d%03d", surah, ayah);
    }
}
