package com.minshawi.murattal.lost;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final String ARCHIVE_ITEM = "a00ssssss260908ddd";
    private static final String METADATA_URL = "https://archive.org/metadata/" + ARCHIVE_ITEM;
    private static final String DOWNLOAD_BASE = "https://archive.org/download/" + ARCHIVE_ITEM + "/";

    private static final int GREEN = Color.rgb(12, 59, 50);
    private static final int GREEN_2 = Color.rgb(19, 78, 66);
    private static final int GOLD = Color.rgb(182, 144, 59);
    private static final int CREAM = Color.rgb(248, 244, 232);
    private static final int INK = Color.rgb(35, 42, 39);
    private static final int MUTED = Color.rgb(100, 111, 106);

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String[] streamUrls = new String[114];
    private final boolean[] downloading = new boolean[114];

    private LinearLayout listContainer;
    private TextView statusText;
    private TextView nowPlayingText;
    private Button bottomPlayButton;
    private EditText searchBox;
    private boolean lostOnly = false;
    private Button lostFilterButton;

    private MediaPlayer mediaPlayer;
    private int currentIndex = -1;
    private boolean preparing = false;

    private final String[] surahs = new String[]{
            "الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس",
            "هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه",
            "الأنبياء","الحج","المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم",
            "لقمان","السجدة","الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر",
            "فصلت","الشورى","الزخرف","الدخان","الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق",
            "الذاريات","الطور","النجم","القمر","الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة",
            "الصف","الجمعة","المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج",
            "نوح","الجن","المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات","عبس",
            "التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى","الغاشية","الفجر","البلد",
            "الشمس","الليل","الضحى","الشرح","التين","العلق","القدر","البينة","الزلزلة","العاديات",
            "القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون","الكوثر","الكافرون","النصر",
            "المسد","الإخلاص","الفلق","الناس"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(GREEN);
        getWindow().setNavigationBarColor(GREEN);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        loadCachedUrls();
        buildUi();
        refreshArchiveIndex();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CREAM);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(buildHeader());

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.VERTICAL);
        tools.setPadding(dp(16), dp(12), dp(16), dp(8));
        tools.setBackgroundColor(CREAM);

        searchBox = new EditText(this);
        searchBox.setHint("ابحث عن سورة…");
        searchBox.setTextColor(INK);
        searchBox.setHintTextColor(MUTED);
        searchBox.setSingleLine(true);
        searchBox.setTextSize(16);
        searchBox.setPadding(dp(14), dp(10), dp(14), dp(10));
        searchBox.setBackground(rounded(Color.WHITE, dp(16), Color.rgb(225, 219, 199), dp(1)));
        tools.addView(searchBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setGravity(Gravity.CENTER_VERTICAL);
        filterRow.setPadding(0, dp(8), 0, 0);

        lostFilterButton = smallButton("المقاطع المعادة فقط", false);
        lostFilterButton.setOnClickListener(v -> {
            lostOnly = !lostOnly;
            lostFilterButton.setText(lostOnly ? "عرض كل السور" : "المقاطع المعادة فقط");
            rebuildList();
        });
        filterRow.addView(lostFilterButton);

        Button info = smallButton("عن هذه النسخة", true);
        info.setOnClickListener(v -> showAbout());
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoLp.setMarginStart(dp(8));
        filterRow.addView(info, infoLp);
        tools.addView(filterRow);

        statusText = new TextView(this);
        statusText.setText("جاري التحقق من فهرس نسخة الإذاعة 2026…");
        statusText.setTextColor(MUTED);
        statusText.setTextSize(12);
        statusText.setPadding(0, dp(7), 0, 0);
        tools.addView(statusText);
        root.addView(tools);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(12), dp(4), dp(12), dp(88));
        scroll.addView(listContainer, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(buildPlayerBar());
        setContentView(root);

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { rebuildList(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        rebuildList();
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(18), dp(24), dp(18), dp(20));
        header.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{GREEN, GREEN_2}));

        TextView ornament = new TextView(this);
        ornament.setText("۞  بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ  ۞");
        ornament.setTextColor(Color.rgb(225, 200, 124));
        ornament.setTextSize(15);
        ornament.setGravity(Gravity.CENTER);
        header.addView(ornament);

        TextView title = new TextView(this);
        title.setText("المصحف مرتل محمد صديق المنشاوي");
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(12), 0, dp(7));
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("نسخة الإذاعة 2026 • تتضمن التسجيلات المعادة 1966–1967");
        subtitle.setTextColor(Color.rgb(234, 226, 200));
        subtitle.setTextSize(13);
        subtitle.setGravity(Gravity.CENTER);
        header.addView(subtitle);
        return header;
    }

    private View buildPlayerBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(8), dp(10), dp(8));
        bar.setBackgroundColor(GREEN);
        bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        nowPlayingText = new TextView(this);
        nowPlayingText.setText("اختر سورة للاستماع");
        nowPlayingText.setTextColor(Color.WHITE);
        nowPlayingText.setTextSize(14);
        nowPlayingText.setMaxLines(2);
        bar.addView(nowPlayingText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button prev = playerButton("⏮");
        prev.setOnClickListener(v -> playRelative(-1));
        bar.addView(prev);
        bottomPlayButton = playerButton("▶");
        bottomPlayButton.setOnClickListener(v -> toggleCurrent());
        bar.addView(bottomPlayButton);
        Button next = playerButton("⏭");
        next.setOnClickListener(v -> playRelative(1));
        bar.addView(next);
        return bar;
    }

    private void rebuildList() {
        if (listContainer == null) return;
        String q = searchBox == null ? "" : searchBox.getText().toString().trim();
        listContainer.removeAllViews();
        for (int i = 0; i < surahs.length; i++) {
            if (lostOnly && !containsRerecordedMaterial(i + 1)) continue;
            if (!q.isEmpty() && !surahs[i].contains(q) && !String.valueOf(i + 1).contains(q)) continue;
            listContainer.addView(buildSurahRow(i));
        }
    }

    private View buildSurahRow(int index) {
        int n = index + 1;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(10), dp(11), dp(10), dp(11));
        card.setBackground(rounded(Color.WHITE, dp(17), Color.rgb(230, 224, 204), dp(1)));
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, dp(4), 0, dp(4));
        card.setLayoutParams(cardLp);

        TextView number = new TextView(this);
        number.setText(toArabicDigits(n));
        number.setTextColor(Color.WHITE);
        number.setGravity(Gravity.CENTER);
        number.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        number.setTextSize(14);
        number.setBackground(rounded(GREEN_2, dp(99), GREEN_2, 0));
        card.addView(number, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding(dp(10), 0, dp(10), 0);
        TextView name = new TextView(this);
        name.setText("سورة " + surahs[index]);
        name.setTextColor(INK);
        name.setTextSize(18);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textCol.addView(name);

        TextView sub = new TextView(this);
        File local = audioFile(index);
        if (local.exists()) {
            sub.setText("محفوظة داخل التطبيق • تعمل دون إنترنت");
            sub.setTextColor(Color.rgb(30, 118, 76));
        } else if (containsRerecordedMaterial(n)) {
            sub.setText("تتضمن جزءًا من التسجيلات المعادة 1966–1967");
            sub.setTextColor(Color.rgb(145, 108, 28));
        } else {
            sub.setText("من النسخة الكاملة المتداولة لبث 2026");
            sub.setTextColor(MUTED);
        }
        sub.setTextSize(11);
        textCol.addView(sub);
        card.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button play = smallButton(isCurrentPlaying(index) ? "❚❚" : "▶", true);
        play.setOnClickListener(v -> playSurah(index));
        card.addView(play, new LinearLayout.LayoutParams(dp(48), dp(42)));

        Button download = smallButton("", false);
        updateDownloadButton(download, index);
        download.setOnClickListener(v -> {
            if (audioFile(index).exists()) confirmDelete(index);
            else if (!downloading[index]) downloadSurah(index, download);
        });
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(78), dp(42));
        dlp.setMarginStart(dp(7));
        card.addView(download, dlp);
        return card;
    }

    private void updateDownloadButton(Button b, int index) {
        if (downloading[index]) { b.setText("جارٍ…"); b.setEnabled(false); }
        else if (audioFile(index).exists()) { b.setText("أوفلاين ✓"); b.setEnabled(true); }
        else { b.setText("تنزيل"); b.setEnabled(true); }
    }

    private void playSurah(int index) {
        if (index < 0 || index >= 114) return;
        if (currentIndex == index && mediaPlayer != null && !preparing) {
            try {
                if (mediaPlayer.isPlaying()) { mediaPlayer.pause(); bottomPlayButton.setText("▶"); }
                else { mediaPlayer.start(); bottomPlayButton.setText("❚❚"); }
            } catch (Exception ignored) {}
            rebuildList();
            return;
        }

        currentIndex = index;
        File local = audioFile(index);
        String source = local.exists() ? local.getAbsolutePath() : getStreamUrl(index);
        nowPlayingText.setText("سورة " + surahs[index] + (local.exists() ? " • أوفلاين" : " • بث مباشر"));
        bottomPlayButton.setText("…");
        preparing = true;
        rebuildList();
        releasePlayer();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build());
        mediaPlayer.setOnPreparedListener(mp -> {
            preparing = false;
            mp.start();
            bottomPlayButton.setText("❚❚");
            statusText.setText(local.exists() ? "تشغيل من النسخة المحفوظة داخل التطبيق" : "تشغيل من مصدر نسخة الإذاعة 2026");
            rebuildList();
        });
        mediaPlayer.setOnCompletionListener(mp -> {
            bottomPlayButton.setText("▶");
            if (currentIndex < 113) playSurah(currentIndex + 1);
        });
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            preparing = false;
            bottomPlayButton.setText("▶");
            statusText.setText("تعذر تشغيل هذه السورة من المصدر. أعد المحاولة بعد التحقق من الاتصال.");
            Toast.makeText(this, "تعذر تشغيل سورة " + surahs[index], Toast.LENGTH_LONG).show();
            return true;
        });
        try {
            mediaPlayer.setDataSource(source);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            preparing = false;
            bottomPlayButton.setText("▶");
            statusText.setText("تعذر فتح ملف التلاوة.");
        }
    }

    private void toggleCurrent() {
        if (currentIndex < 0) { playSurah(0); return; }
        if (!preparing) playSurah(currentIndex);
    }

    private void playRelative(int delta) {
        if (currentIndex < 0) { playSurah(delta > 0 ? 0 : 113); return; }
        int next = currentIndex + delta;
        if (next < 0) next = 113;
        if (next > 113) next = 0;
        playSurah(next);
    }

    private void downloadSurah(int index, Button button) {
        downloading[index] = true;
        updateDownloadButton(button, index);
        statusText.setText("جاري تنزيل سورة " + surahs[index] + " داخل التطبيق…");
        executor.execute(() -> {
            File out = audioFile(index);
            File part = new File(out.getParentFile(), out.getName() + ".part");
            try {
                if (!out.getParentFile().exists() && !out.getParentFile().mkdirs()) throw new Exception("Cannot create directory");
                HttpURLConnection c = (HttpURLConnection) new URL(getStreamUrl(index)).openConnection();
                c.setInstanceFollowRedirects(true);
                c.setConnectTimeout(20000);
                c.setReadTimeout(45000);
                c.setRequestProperty("User-Agent", "AlMinshawiMurattal/1.0 Android");
                c.connect();
                int code = c.getResponseCode();
                if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
                long total = c.getContentLengthLong();
                long done = 0;
                int lastPct = -1;
                try (InputStream in = new BufferedInputStream(c.getInputStream()); FileOutputStream fos = new FileOutputStream(part)) {
                    byte[] buf = new byte[64 * 1024];
                    int read;
                    while ((read = in.read(buf)) != -1) {
                        fos.write(buf, 0, read);
                        done += read;
                        if (total > 0) {
                            int pct = (int) ((done * 100) / total);
                            if (pct >= lastPct + 5) {
                                lastPct = pct;
                                int finalPct = pct;
                                mainHandler.post(() -> button.setText(toArabicDigits(finalPct) + "%"));
                            }
                        }
                    }
                    fos.flush();
                }
                c.disconnect();
                if (!part.renameTo(out)) throw new Exception("Rename failed");
                mainHandler.post(() -> {
                    downloading[index] = false;
                    statusText.setText("تم تنزيل سورة " + surahs[index] + " • ستعمل دون إنترنت من داخل التطبيق");
                    Toast.makeText(this, "تم حفظ سورة " + surahs[index], Toast.LENGTH_SHORT).show();
                    rebuildList();
                });
            } catch (Exception e) {
                part.delete();
                mainHandler.post(() -> {
                    downloading[index] = false;
                    statusText.setText("فشل تنزيل سورة " + surahs[index] + ". تحقق من الإنترنت أو أعد المحاولة.");
                    Toast.makeText(this, "فشل التنزيل", Toast.LENGTH_LONG).show();
                    rebuildList();
                });
            }
        });
    }

    private void confirmDelete(int index) {
        new AlertDialog.Builder(this)
                .setTitle("حذف النسخة الأوفلاين؟")
                .setMessage("سيُحذف ملف سورة " + surahs[index] + " المحفوظ داخل التطبيق فقط، وستظل السورة متاحة عبر الإنترنت.")
                .setPositiveButton("حذف", (d, w) -> { audioFile(index).delete(); statusText.setText("تم حذف النسخة المحفوظة لسورة " + surahs[index]); rebuildList(); })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("عن المصحف المرتل المفقود")
                .setMessage("يعتمد التطبيق النسخة الكاملة المتداولة من بث إذاعة القرآن الكريم المصرية في 2026.\n\nالتعبير «المصحف المفقود» لا يعني أن السور الـ114 كلها تسجيل جديد مستقل؛ فقد أعاد الشيخ محمد صديق المنشاوي تسجيل أجزاء من الختمة في 1966–1967، وظلت هذه التسجيلات غير متداولة على نطاق واسع لعقود.\n\nالسور المعلَّمة باللون الذهبي تتقاطع مع المقاطع المعاد تسجيلها، وقد يكون التسجيل المعاد جزءًا من السورة لا السورة كاملة.\n\nمصدر الصوت المستخدم: نسخة Archive.org المرتبطة ببث 2026. الملفات التي تنزّلها تُحفظ داخل مساحة التطبيق الخاصة وتُحذف عند إزالة التطبيق.")
                .setPositiveButton("حسنًا", null).show();
    }

    private void refreshArchiveIndex() {
        executor.execute(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(METADATA_URL).openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(20000);
                c.setRequestProperty("User-Agent", "AlMinshawiMurattal/1.0 Android");
                c.connect();
                if (c.getResponseCode() < 200 || c.getResponseCode() >= 300) throw new Exception("HTTP " + c.getResponseCode());
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                    String line; while ((line = br.readLine()) != null) sb.append(line);
                }
                c.disconnect();
                JSONArray files = new JSONObject(sb.toString()).optJSONArray("files");
                if (files == null) throw new Exception("No files");
                String[] bestName = new String[114];
                int[] bestScore = new int[114];
                for (int i = 0; i < files.length(); i++) {
                    JSONObject f = files.optJSONObject(i);
                    if (f == null) continue;
                    String name = f.optString("name", "");
                    if (!name.toLowerCase(Locale.ROOT).endsWith(".mp3")) continue;
                    int track = extractTrack(f, name);
                    if (track < 1 || track > 114) continue;
                    int score = scoreCandidate(f, name, track);
                    if (bestName[track - 1] == null || score > bestScore[track - 1]) { bestName[track - 1] = name; bestScore[track - 1] = score; }
                }
                int found = 0;
                android.content.SharedPreferences.Editor editor = getSharedPreferences("audio_index", MODE_PRIVATE).edit();
                for (int i = 0; i < 114; i++) {
                    if (bestName[i] != null) {
                        String u = DOWNLOAD_BASE + Uri.encode(bestName[i], "._-()[]");
                        streamUrls[i] = u;
                        editor.putString("url_" + (i + 1), u);
                        found++;
                    }
                }
                editor.apply();
                int finalFound = found;
                mainHandler.post(() -> statusText.setText(finalFound >= 100
                        ? "تم ربط " + toArabicDigits(finalFound) + " سورة من فهرس نسخة الإذاعة 2026 • التنزيل يحفظ داخل التطبيق"
                        : "تم تحديث الفهرس جزئيًا؛ سيجرب التطبيق الرابط المباشر للسور غير المفهرسة."));
            } catch (Exception e) {
                mainHandler.post(() -> statusText.setText("تعذر تحديث فهرس المصدر الآن؛ سيستخدم التطبيق الروابط المحفوظة أو المباشرة."));
            }
        });
    }

    private int extractTrack(JSONObject f, String name) {
        Matcher mt = Pattern.compile("^(\\d{1,3})").matcher(f.optString("track", "").trim());
        if (mt.find()) try { return Integer.parseInt(mt.group(1)); } catch (Exception ignored) {}
        Matcher mn = Pattern.compile("^(\\d{1,3})(?:\\D|$)").matcher(name);
        if (mn.find()) try { return Integer.parseInt(mn.group(1)); } catch (Exception ignored) {}
        Matcher mtitle = Pattern.compile("^(\\d{1,3})(?:\\D|$)").matcher(f.optString("title", "").trim());
        if (mtitle.find()) try { return Integer.parseInt(mtitle.group(1)); } catch (Exception ignored) {}
        return -1;
    }

    private int scoreCandidate(JSONObject f, String name, int track) {
        int score = 0;
        String lower = name.toLowerCase(Locale.ROOT);
        String fmt = f.optString("format", "").toLowerCase(Locale.ROOT);
        String source = f.optString("source", "").toLowerCase(Locale.ROOT);
        if (name.equals(String.format(Locale.US, "%03d.mp3", track))) score += 100;
        if (lower.startsWith(String.format(Locale.US, "%03d", track))) score += 25;
        if (source.equals("original")) score += 30;
        if (fmt.contains("vbr mp3")) score += 20;
        if (fmt.contains("mp3")) score += 10;
        if (lower.contains("64kb")) score -= 15;
        if (lower.contains("sample")) score -= 50;
        return score;
    }

    private void loadCachedUrls() {
        android.content.SharedPreferences p = getSharedPreferences("audio_index", MODE_PRIVATE);
        for (int i = 0; i < 114; i++) streamUrls[i] = p.getString("url_" + (i + 1), null);
    }

    private String getStreamUrl(int index) {
        if (streamUrls[index] != null && !streamUrls[index].isEmpty()) return streamUrls[index];
        return DOWNLOAD_BASE + String.format(Locale.US, "%03d.mp3", index + 1);
    }

    private File audioFile(int index) {
        return new File(new File(getFilesDir(), "audio"), String.format(Locale.US, "%03d.mp3", index + 1));
    }

    private boolean containsRerecordedMaterial(int n) {
        return (n >= 1 && n <= 3) || n == 4 || (n >= 5 && n <= 9) || n == 18 || (n >= 19 && n <= 20)
                || (n >= 38 && n <= 41) || (n >= 47 && n <= 57) || (n >= 60 && n <= 68);
    }

    private boolean isCurrentPlaying(int index) {
        if (index != currentIndex || mediaPlayer == null || preparing) return false;
        try { return mediaPlayer.isPlaying(); } catch (IllegalStateException e) { return false; }
    }

    private Button smallButton(String text, boolean gold) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTextColor(gold ? GREEN : Color.WHITE);
        b.setPadding(dp(8), 0, dp(8), 0);
        b.setMinWidth(0);
        b.setMinHeight(0);
        b.setBackground(rounded(gold ? Color.rgb(224, 196, 111) : GREEN_2, dp(12), gold ? GOLD : GREEN_2, 0));
        return b;
    }

    private Button playerButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(18);
        b.setTextColor(GREEN);
        b.setAllCaps(false);
        b.setPadding(0, 0, 0, 0);
        b.setMinWidth(0);
        b.setMinHeight(0);
        b.setBackground(rounded(Color.rgb(232, 207, 135), dp(99), Color.rgb(232, 207, 135), 0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
        lp.setMargins(dp(3), 0, dp(3), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private GradientDrawable rounded(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (strokeWidth > 0) d.setStroke(strokeWidth, strokeColor);
        return d;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private String toArabicDigits(int value) {
        String western = String.valueOf(value);
        char[] ar = {'٠','١','٢','٣','٤','٥','٦','٧','٨','٩'};
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < western.length(); i++) {
            char c = western.charAt(i);
            out.append(c >= '0' && c <= '9' ? ar[c - '0'] : c);
        }
        return out.toString();
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignored) {}
            try { mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        executor.shutdownNow();
        super.onDestroy();
    }
}
