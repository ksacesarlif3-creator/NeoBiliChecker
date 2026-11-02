package com.minshawi.murattal.lost;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/** Lazy offline reader for Quranpedia's per-ayah Al-Tabari (Jami' al-Bayan) data. */
public final class TabariData {
    private int cachedSurah = -1;
    private JSONObject cachedAyahs;

    public synchronized String getTafsir(Context context, int surah, int ayah) throws Exception {
        if (surah < 1 || surah > 114 || ayah < 1) return "";
        if (cachedSurah != surah || cachedAyahs == null) loadSurah(context, surah);
        return cachedAyahs == null ? "" : cachedAyahs.optString(String.valueOf(ayah), "");
    }

    private void loadSurah(Context context, int surah) throws Exception {
        String name = String.format(Locale.US, "tabari/%03d.bin", surah);
        try (InputStream raw = context.getAssets().open(name);
             GZIPInputStream gzip = new GZIPInputStream(raw);
             BufferedReader br = new BufferedReader(new InputStreamReader(gzip, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[32 * 1024];
            int n;
            while ((n = br.read(buf)) != -1) sb.append(buf, 0, n);
            cachedAyahs = new JSONObject(sb.toString());
            cachedSurah = surah;
        }
    }
}
