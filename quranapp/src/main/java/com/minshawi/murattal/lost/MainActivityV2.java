package com.minshawi.murattal.lost;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivityV2 extends Activity {
    private static final String ARCHIVE_ITEM = "a00ssssss260908ddd";
    private static final String METADATA_URL = "https://archive.org/metadata/" + ARCHIVE_ITEM;
    private static final String DOWNLOAD_BASE = "https://archive.org/download/" + ARCHIVE_ITEM + "/";

    private static final int GREEN = Color.rgb(12, 59, 50);
    private static final int GREEN_2 = Color.rgb(19, 78, 66);
    private static final int GOLD = Color.rgb(182, 144, 59);
    private static final int GOLD_LIGHT = Color.rgb(247, 224, 137);
    private static final int CREAM = Color.rgb(248, 244, 232);
    private static final int INK = Color.rgb(35, 42, 39);
    private static final int MUTED = Color.rgb(100, 111, 106);

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String[] streamUrls = new String[114];
    private final boolean[] downloading = new boolean[114];

    private FrameLayout screenHost;
    private LinearLayout listScreen;
    private LinearLayout readerScreen;
    private LinearLayout listContainer;
    private LinearLayout readerBody;
    private ScrollView readerScroll;
    private TextView statusText;
    private TextView nowPlayingText;
    private TextView elapsedText;
    private TextView durationText;
    private TextView readerTitle;
    private TextView readerInfo;
    private Button bottomPlayButton;
    private Button speedButton;
    private Button readerToggleButton;
    private EditText searchBox;
    private SeekBar seekBar;
    private Button lostFilterButton;

    private MediaPlayer mediaPlayer;
    private int currentIndex = -1;
    private boolean preparing = false;
    private boolean userSeeking = false;
    private boolean lostOnly = false;
    private float playbackSpeed = 1.0f;

    private QuranSyncData syncData;
    private boolean syncLoading = true;
    private boolean readerVisible = false;
    private int readerSurahIndex = 0;
    private int renderedSurahIndex = -1;
    private final Map<String, RenderedText> renderedTexts = new HashMap<>();
    private String highlightedKey = null;
    private int highlightedWord = -1;
    private int highlightedAyah = -1;

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

    private static final class RenderedText {
        final TextView view;
        final String text;
        final int[] starts;
        final int[] ends;

        RenderedText(TextView view, String text) {
            this.view = view;
            this.text = text;
            String trimmed = text.trim();
            String[] words = trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
            starts = new int[words.length];
            ends = new int[words.length];
            int cursor = 0;
            for (int i = 0; i < words.length; i++) {
                int at = text.indexOf(words[i], cursor);
                if (at < 0) at = cursor;
                starts[i] = at;
                ends[i] = Math.min(text.length(), at + words[i].length());
                cursor = ends[i];
            }
        }

        void paint(int oneBasedWord) {
            if (oneBasedWord <= 0 || oneBasedWord > starts.length) {
                view.setText(text);
                return;
            }
            SpannableString s = new SpannableString(text);
            int st = starts[oneBasedWord - 1];
            int en = ends[oneBasedWord - 1];
            if (st >= 0 && en > st && en <= text.length()) {
                s.setSpan(new BackgroundColorSpan(GOLD_LIGHT), st, en, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                s.setSpan(new ForegroundColorSpan(GREEN), st, en, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                s.setSpan(new StyleSpan(Typeface.BOLD), st, en, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            view.setText(s);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(GREEN);
        getWindow().setNavigationBarColor(GREEN);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        loadCachedUrls();
        buildUi();
        loadSyncAssets();
        refreshArchiveIndex();
        mainHandler.post(progressTicker);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CREAM);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(buildHeader());

        screenHost = new FrameLayout(this);
        listScreen = buildListScreen();
        readerScreen = buildReaderScreen();
        readerScreen.setVisibility(View.GONE);
        screenHost.addView(listScreen, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        screenHost.addView(readerScreen, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(screenHost, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(buildPlayerPanel());
        setContentView(root);
        rebuildList();
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(18), dp(18), dp(18), dp(14));
        header.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{GREEN, GREEN_2}));

        TextView ornament = new TextView(this);
        ornament.setText("۞  بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ  ۞");
        ornament.setTextColor(Color.rgb(225, 200, 124));
        ornament.setTextSize(14);
        ornament.setGravity(Gravity.CENTER);
        header.addView(ornament);

        TextView title = new TextView(this);
        title.setText("المصحف مرتل محمد صديق المنشاوي");
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextSize(23);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(9), 0, dp(4));
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("الختمة الإذاعية الثانية • نص عثماني • تزامن كلمة بكلمة");
        subtitle.setTextColor(Color.rgb(234, 226, 200));
        subtitle.setTextSize(12);
        subtitle.setGravity(Gravity.CENTER);
        header.addView(subtitle);
        return header;
    }

    private LinearLayout buildListScreen() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(CREAM);

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.VERTICAL);
        tools.setPadding(dp(16), dp(10), dp(16), dp(6));

        searchBox = new EditText(this);
        searchBox.setHint("ابحث عن سورة…");
        searchBox.setTextColor(INK);
        searchBox.setHintTextColor(MUTED);
        searchBox.setSingleLine(true);
        searchBox.setTextSize(16);
        searchBox.setPadding(dp(14), dp(9), dp(14), dp(9));
        searchBox.setBackground(rounded(Color.WHITE, dp(16), Color.rgb(225, 219, 199), dp(1)));
        tools.addView(searchBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        buttons.setPadding(0, dp(8), 0, 0);

        Button mushaf = smallButton("📖 فتح المصحف", true);
        mushaf.setOnClickListener(v -> showReader(currentIndex >= 0 ? currentIndex : 0));
        buttons.addView(mushaf);

        lostFilterButton = smallButton("المقاطع المعادة فقط", false);
        lostFilterButton.setOnClickListener(v -> {
            lostOnly = !lostOnly;
            lostFilterButton.setText(lostOnly ? "عرض كل السور" : "المقاطع المعادة فقط");
            rebuildList();
        });
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.setMarginStart(dp(7));
        buttons.addView(lostFilterButton, flp);

        Button info = smallButton("عن النسخة", false);
        info.setOnClickListener(v -> showAbout());
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ilp.setMarginStart(dp(7));
        buttons.addView(info, ilp);
        tools.addView(buttons);

        statusText = new TextView(this);
        statusText.setText("يتم تجهيز نص المصحف وتوقيت الكلمات داخل التطبيق…");
        statusText.setTextColor(MUTED);
        statusText.setTextSize(12);
        statusText.setPadding(0, dp(6), 0, 0);
        tools.addView(statusText);
        screen.addView(tools);

        ScrollView scroll = new ScrollView(this);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(12), dp(3), dp(12), dp(12));
        scroll.addView(listContainer, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        screen.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { rebuildList(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        return screen;
    }

    private LinearLayout buildReaderScreen() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(Color.rgb(252, 249, 239));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(12), dp(8), dp(12), dp(8));
        top.setBackgroundColor(Color.rgb(239, 232, 209));

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        Button back = smallButton("قائمة السور", false);
        back.setOnClickListener(v -> hideReader());
        nav.addView(back);
        Button prev = smallButton("السورة السابقة", true);
        prev.setOnClickListener(v -> renderReader((readerSurahIndex + 113) % 114, true));
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        np.setMarginStart(dp(6));
        nav.addView(prev, np);
        Button next = smallButton("السورة التالية", true);
        next.setOnClickListener(v -> renderReader((readerSurahIndex + 1) % 114, true));
        LinearLayout.LayoutParams nn = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nn.setMarginStart(dp(6));
        nav.addView(next, nn);
        top.addView(nav);

        readerTitle = new TextView(this);
        readerTitle.setTextColor(GREEN);
        readerTitle.setTextSize(21);
        readerTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        readerTitle.setGravity(Gravity.CENTER);
        readerTitle.setPadding(0, dp(7), 0, dp(2));
        top.addView(readerTitle);

        readerInfo = new TextView(this);
        readerInfo.setTextColor(MUTED);
        readerInfo.setTextSize(11);
        readerInfo.setGravity(Gravity.CENTER);
        top.addView(readerInfo);
        screen.addView(top);

        readerScroll = new ScrollView(this);
        readerScroll.setFillViewport(true);
        readerBody = new LinearLayout(this);
        readerBody.setOrientation(LinearLayout.VERTICAL);
        readerBody.setPadding(dp(16), dp(12), dp(16), dp(30));
        readerScroll.addView(readerBody, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        screen.addView(readerScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return screen;
    }

    private View buildPlayerPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(9), dp(6), dp(9), dp(7));
        panel.setBackgroundColor(GREEN);

        LinearLayout first = new LinearLayout(this);
        first.setGravity(Gravity.CENTER_VERTICAL);
        nowPlayingText = new TextView(this);
        nowPlayingText.setText("اختر سورة للاستماع");
        nowPlayingText.setTextColor(Color.WHITE);
        nowPlayingText.setTextSize(13);
        nowPlayingText.setMaxLines(1);
        first.addView(nowPlayingText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        speedButton = smallButton("١٫٠×", true);
        speedButton.setOnClickListener(v -> showSpeedDialog());
        first.addView(speedButton, new LinearLayout.LayoutParams(dp(66), dp(36)));
        panel.addView(first);

        LinearLayout seekRow = new LinearLayout(this);
        seekRow.setGravity(Gravity.CENTER_VERTICAL);
        elapsedText = timeText("٠٠:٠٠");
        durationText = timeText("٠٠:٠٠");
        seekBar = new SeekBar(this);
        seekBar.setMax(1000);
        seekBar.setProgress(0);
        seekBar.setEnabled(false);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) elapsedText.setText(formatTime(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { userSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                int target = seekBar.getProgress();
                userSeeking = false;
                if (mediaPlayer != null && !preparing) {
                    try {
                        mediaPlayer.seekTo(target);
                        updateWordHighlight(target);
                    } catch (Exception ignored) {}
                }
            }
        });
        seekRow.addView(elapsedText, new LinearLayout.LayoutParams(dp(58), ViewGroup.LayoutParams.WRAP_CONTENT));
        seekRow.addView(seekBar, new LinearLayout.LayoutParams(0, dp(34), 1f));
        seekRow.addView(durationText, new LinearLayout.LayoutParams(dp(58), ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(seekRow);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        Button prev = playerButton("⏮");
        prev.setOnClickListener(v -> playRelative(-1));
        controls.addView(prev);
        bottomPlayButton = playerButton("▶");
        bottomPlayButton.setOnClickListener(v -> toggleCurrent());
        controls.addView(bottomPlayButton);
        Button next = playerButton("⏭");
        next.setOnClickListener(v -> playRelative(1));
        controls.addView(next);
        readerToggleButton = smallButton("📖 متابعة النص", true);
        readerToggleButton.setOnClickListener(v -> {
            if (readerVisible) hideReader(); else showReader(currentIndex >= 0 ? currentIndex : readerSurahIndex);
        });
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
        rlp.setMarginStart(dp(10));
        controls.addView(readerToggleButton, rlp);
        panel.addView(controls);
        return panel;
    }

    private TextView timeText(String value) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(Color.rgb(235, 226, 198));
        t.setTextSize(11);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private void rebuildList() {
        if (listContainer == null) return;
        String q = searchBox == null ? "" : searchBox.getText().toString().trim();
        listContainer.removeAllViews();
        for (int i = 0; i < 114; i++) {
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
        card.setPadding(dp(9), dp(9), dp(9), dp(9));
        card.setBackground(rounded(Color.WHITE, dp(16), Color.rgb(230, 224, 204), dp(1)));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, dp(3), 0, dp(3));
        card.setLayoutParams(cardLp);

        TextView number = new TextView(this);
        number.setText(toArabicDigits(n));
        number.setTextColor(Color.WHITE);
        number.setGravity(Gravity.CENTER);
        number.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        number.setTextSize(14);
        number.setBackground(rounded(GREEN_2, dp(99), GREEN_2, 0));
        card.addView(number, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding(dp(9), 0, dp(9), 0);
        TextView name = new TextView(this);
        name.setText("سورة " + surahs[index]);
        name.setTextColor(INK);
        name.setTextSize(17);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textCol.addView(name);
        TextView sub = new TextView(this);
        if (audioFile(index).exists()) {
            sub.setText("محفوظة أوفلاين • النص والتزامن متاحان دون إنترنت");
            sub.setTextColor(Color.rgb(30, 118, 76));
        } else if (containsRerecordedMaterial(n)) {
            sub.setText("تتضمن من مواد الختمة المعاد تسجيلها 1966–1967");
            sub.setTextColor(Color.rgb(145, 108, 28));
        } else {
            sub.setText("الختمة الإذاعية الثانية • اضغط 📖 للقراءة");
            sub.setTextColor(MUTED);
        }
        sub.setTextSize(10);
        textCol.addView(sub);
        textCol.setOnClickListener(v -> showReader(index));
        card.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button read = smallButton("📖", true);
        read.setOnClickListener(v -> showReader(index));
        card.addView(read, new LinearLayout.LayoutParams(dp(44), dp(40)));

        Button play = smallButton(isCurrentPlaying(index) ? "❚❚" : "▶", true);
        play.setOnClickListener(v -> playSurah(index));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(dp(44), dp(40));
        plp.setMarginStart(dp(5));
        card.addView(play, plp);

        Button download = smallButton("", false);
        updateDownloadButton(download, index);
        download.setOnClickListener(v -> {
            if (audioFile(index).exists()) confirmDelete(index);
            else if (!downloading[index]) downloadSurah(index, download);
        });
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(72), dp(40));
        dlp.setMarginStart(dp(5));
        card.addView(download, dlp);
        return card;
    }

    private void updateDownloadButton(Button b, int index) {
        if (downloading[index]) { b.setText("جارٍ…"); b.setEnabled(false); }
        else if (audioFile(index).exists()) { b.setText("أوفلاين ✓"); b.setEnabled(true); }
        else { b.setText("تنزيل"); b.setEnabled(true); }
    }

    private void loadSyncAssets() {
        syncLoading = true;
        executor.execute(() -> {
            try {
                QuranSyncData loaded = QuranSyncData.load(this);
                mainHandler.post(() -> {
                    syncData = loaded;
                    syncLoading = false;
                    if (statusText != null) statusText.setText("نص المصحف وتوقيت الكلمات جاهزان • ٦٠٤ صفحة • يعملان أوفلاين");
                    if (readerVisible) renderReader(readerSurahIndex, false);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    syncLoading = false;
                    if (statusText != null) statusText.setText("تعذر تحميل بيانات المصحف المدمجة. أعد تثبيت التطبيق إن استمر الخلل.");
                    Toast.makeText(this, "تعذر تحميل نص المصحف أو توقيت الكلمات", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showReader(int index) {
        readerVisible = true;
        listScreen.setVisibility(View.GONE);
        readerScreen.setVisibility(View.VISIBLE);
        readerToggleButton.setText("قائمة السور");
        renderReader(Math.max(0, Math.min(113, index)), false);
    }

    private void hideReader() {
        readerVisible = false;
        readerScreen.setVisibility(View.GONE);
        listScreen.setVisibility(View.VISIBLE);
        readerToggleButton.setText("📖 متابعة النص");
    }

    private void renderReader(int index, boolean fromNavigation) {
        readerSurahIndex = Math.max(0, Math.min(113, index));
        renderedSurahIndex = readerSurahIndex;
        highlightedKey = null;
        highlightedWord = -1;
        highlightedAyah = -1;
        renderedTexts.clear();
        readerTitle.setText("سورة " + surahs[readerSurahIndex]);
        readerBody.removeAllViews();

        if (syncLoading || syncData == null) {
            TextView wait = readerMessage("جاري تحميل نص مصحف المدينة وتوقيت الكلمات…", 17, MUTED);
            readerBody.addView(wait);
            readerInfo.setText("البيانات مدمجة داخل التطبيق");
            return;
        }

        int surahNo = readerSurahIndex + 1;
        List<QuranSyncData.Ayah> ayahs = syncData.getSurah(surahNo);
        if (ayahs.isEmpty()) {
            readerBody.addView(readerMessage("تعذر العثور على نص السورة.", 17, MUTED));
            return;
        }

        int firstPage = ayahs.get(0).page;
        int lastPage = ayahs.get(ayahs.size() - 1).page;
        readerInfo.setText("مصحف المدينة النبوية • حفص عن عاصم • الصفحات " + toArabicDigits(firstPage) + (lastPage == firstPage ? "" : "–" + toArabicDigits(lastPage)));

        TextView note = readerMessage("تضيء الكلمة الموافقة للصوت تلقائيًا. اضغط على أي آية للانتقال إلى بدايتها.", 11, MUTED);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, 0, 0, dp(8));
        readerBody.addView(note);

        if (hasMomentKey(surahNo, "i")) addSpecialReaderLine("i", syncData.istiadhahText(), false);
        if (surahNo != 1 && surahNo != 9 && hasMomentKey(surahNo, "b")) addSpecialReaderLine("b", syncData.basmalaText(), true);

        int page = -1;
        for (QuranSyncData.Ayah a : ayahs) {
            if (a.page != page) {
                page = a.page;
                TextView pageHead = readerMessage("۞  الصفحة " + toArabicDigits(page) + " من ٦٠٤  ۞", 12, GOLD);
                pageHead.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                pageHead.setGravity(Gravity.CENTER);
                pageHead.setPadding(0, dp(9), 0, dp(6));
                readerBody.addView(pageHead);
            }
            final int ayahNo = a.number;
            String display = a.text + "  ﴿" + toArabicDigits(a.number) + "﴾";
            TextView verse = new TextView(this);
            verse.setText(display);
            verse.setTextColor(INK);
            verse.setTextSize(24);
            verse.setGravity(Gravity.RIGHT);
            verse.setTextDirection(View.TEXT_DIRECTION_RTL);
            verse.setLineSpacing(dp(5), 1.16f);
            verse.setPadding(dp(8), dp(8), dp(8), dp(8));
            verse.setBackground(rounded(Color.rgb(255, 253, 246), dp(12), Color.rgb(232, 224, 199), dp(1)));
            LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            vlp.setMargins(0, dp(3), 0, dp(3));
            verse.setLayoutParams(vlp);
            verse.setOnClickListener(v -> seekToAyah(readerSurahIndex, ayahNo));
            readerBody.addView(verse);
            renderedTexts.put("a:" + a.number, new RenderedText(verse, display));
        }

        if (currentIndex == readerSurahIndex && mediaPlayer != null && !preparing) {
            try { updateWordHighlight(mediaPlayer.getCurrentPosition()); } catch (Exception ignored) {}
        } else if (fromNavigation) {
            readerScroll.post(() -> readerScroll.scrollTo(0, 0));
        }
    }

    private void addSpecialReaderLine(String key, String text, boolean gold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(gold ? GREEN : MUTED);
        v.setTextSize(gold ? 22 : 17);
        v.setGravity(Gravity.CENTER);
        v.setTextDirection(View.TEXT_DIRECTION_RTL);
        v.setPadding(dp(8), dp(7), dp(8), dp(7));
        if (gold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        readerBody.addView(v);
        renderedTexts.put(key, new RenderedText(v, text));
    }

    private TextView readerMessage(String text, int size, int color) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTextDirection(View.TEXT_DIRECTION_RTL);
        return v;
    }

    private boolean hasMomentKey(int surah, String key) {
        if (syncData == null) return false;
        for (QuranSyncData.WordMoment m : syncData.getMoments(surah)) if (m.key.equals(key)) return true;
        return false;
    }

    private void seekToAyah(int surahIndex, int ayah) {
        if (syncData == null) return;
        int ms = syncData.firstWordStartMs(surahIndex + 1, ayah);
        if (ms < 0) return;
        if (currentIndex != surahIndex || mediaPlayer == null) {
            playSurah(surahIndex, ms);
        } else if (!preparing) {
            try {
                mediaPlayer.seekTo(ms);
                seekBar.setProgress(ms);
                updateWordHighlight(ms);
            } catch (Exception ignored) {}
        }
    }

    private void updateWordHighlight(int positionMs) {
        if (!readerVisible || syncData == null || renderedSurahIndex != currentIndex || currentIndex < 0) return;
        List<QuranSyncData.WordMoment> list = syncData.getMoments(currentIndex + 1);
        if (list.isEmpty()) return;

        int lo = 0, hi = list.size() - 1, candidate = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (list.get(mid).startMs <= positionMs) { candidate = mid; lo = mid + 1; }
            else hi = mid - 1;
        }

        QuranSyncData.WordMoment active = null;
        if (candidate >= 0) {
            // Walk backwards a few entries in case a reviewed occurrence overlaps a boundary.
            for (int i = candidate; i >= Math.max(0, candidate - 4); i--) {
                QuranSyncData.WordMoment m = list.get(i);
                if (positionMs >= m.startMs && positionMs <= m.endMs) { active = m; break; }
            }
        }

        if (active == null) {
            clearHighlight();
            return;
        }
        if (active.key.equals(highlightedKey) && active.wordIndex == highlightedWord) return;

        RenderedText old = highlightedKey == null ? null : renderedTexts.get(highlightedKey);
        if (old != null) old.paint(-1);
        highlightedKey = active.key;
        highlightedWord = active.wordIndex;
        RenderedText now = renderedTexts.get(active.key);
        if (now != null) {
            now.paint(active.wordIndex);
            if (active.ayah > 0 && active.ayah != highlightedAyah) {
                highlightedAyah = active.ayah;
                int page = syncData.getPage(currentIndex + 1, active.ayah);
                readerInfo.setText("مصحف المدينة النبوية • الصفحة " + toArabicDigits(page) + " من ٦٠٤ • الآية " + toArabicDigits(active.ayah));
                readerScroll.post(() -> {
                    int y = Math.max(0, now.view.getTop() - dp(95));
                    readerScroll.smoothScrollTo(0, y);
                });
            }
        }
    }

    private void clearHighlight() {
        if (highlightedKey != null) {
            RenderedText old = renderedTexts.get(highlightedKey);
            if (old != null) old.paint(-1);
        }
        highlightedKey = null;
        highlightedWord = -1;
    }

    private void playSurah(int index) { playSurah(index, -1); }

    private void playSurah(int index, int initialSeekMs) {
        if (index < 0 || index >= 114) return;
        if (currentIndex == index && mediaPlayer != null && !preparing && initialSeekMs < 0) {
            try {
                if (mediaPlayer.isPlaying()) { mediaPlayer.pause(); bottomPlayButton.setText("▶"); }
                else { mediaPlayer.start(); applyPlaybackSpeed(mediaPlayer); bottomPlayButton.setText("❚❚"); }
            } catch (Exception ignored) {}
            rebuildList();
            return;
        }

        currentIndex = index;
        if (readerVisible) renderReader(index, false);
        File local = audioFile(index);
        String source = local.exists() ? local.getAbsolutePath() : getStreamUrl(index);
        nowPlayingText.setText("سورة " + surahs[index] + (local.exists() ? " • أوفلاين" : " • بث مباشر"));
        bottomPlayButton.setText("…");
        preparing = true;
        seekBar.setEnabled(false);
        seekBar.setProgress(0);
        elapsedText.setText("٠٠:٠٠");
        durationText.setText("٠٠:٠٠");
        rebuildList();
        releasePlayer();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build());
        mediaPlayer.setOnPreparedListener(mp -> {
            preparing = false;
            int duration = mp.getDuration();
            seekBar.setMax(Math.max(1, duration));
            seekBar.setEnabled(true);
            durationText.setText(formatTime(duration));
            if (initialSeekMs >= 0 && initialSeekMs < duration) mp.seekTo(initialSeekMs);
            mp.start();
            applyPlaybackSpeed(mp);
            bottomPlayButton.setText("❚❚");
            statusText.setText(local.exists() ? "تشغيل أوفلاين • التزامن كلمة بكلمة يعمل من البيانات المدمجة" : "تشغيل من نسخة الإذاعة 2026 • التزامن كلمة بكلمة مفعّل");
            rebuildList();
        });
        mediaPlayer.setOnCompletionListener(mp -> {
            bottomPlayButton.setText("▶");
            clearHighlight();
            if (currentIndex < 113) playSurah(currentIndex + 1);
        });
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            preparing = false;
            bottomPlayButton.setText("▶");
            seekBar.setEnabled(false);
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

    private void applyPlaybackSpeed(MediaPlayer mp) {
        if (mp == null) return;
        try {
            boolean wasPlaying = mp.isPlaying();
            PlaybackParams params = new PlaybackParams();
            params.setSpeed(playbackSpeed);
            params.setPitch(1.0f);
            mp.setPlaybackParams(params);
            if (!wasPlaying) mp.pause();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر تطبيق سرعة التشغيل على هذا الجهاز", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSpeedDialog() {
        final float[] speeds = {0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
        final String[] labels = {"0.75×  أبطأ", "1.00×  طبيعي", "1.25×", "1.50×", "2.00×  أسرع"};
        int checked = 1;
        for (int i = 0; i < speeds.length; i++) if (Math.abs(speeds[i] - playbackSpeed) < 0.001f) checked = i;
        new AlertDialog.Builder(this)
                .setTitle("سرعة التلاوة")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    playbackSpeed = speeds[which];
                    speedButton.setText(speedLabel(playbackSpeed));
                    if (mediaPlayer != null && !preparing) applyPlaybackSpeed(mediaPlayer);
                    dialog.dismiss();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private String speedLabel(float speed) {
        if (Math.abs(speed - 0.75f) < 0.01f) return "٠٫٧٥×";
        if (Math.abs(speed - 1.0f) < 0.01f) return "١٫٠×";
        if (Math.abs(speed - 1.25f) < 0.01f) return "١٫٢٥×";
        if (Math.abs(speed - 1.5f) < 0.01f) return "١٫٥×";
        return "٢٫٠×";
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

    private final Runnable progressTicker = new Runnable() {
        @Override public void run() {
            if (mediaPlayer != null && !preparing) {
                try {
                    int pos = mediaPlayer.getCurrentPosition();
                    int dur = mediaPlayer.getDuration();
                    if (!userSeeking) {
                        if (seekBar.getMax() != dur && dur > 0) seekBar.setMax(dur);
                        seekBar.setProgress(Math.max(0, pos));
                        elapsedText.setText(formatTime(pos));
                        durationText.setText(formatTime(dur));
                    }
                    updateWordHighlight(pos);
                } catch (Exception ignored) {}
            }
            mainHandler.postDelayed(this, 90);
        }
    };

    private String formatTime(int ms) {
        int total = Math.max(0, ms) / 1000;
        int h = total / 3600;
        int m = (total % 3600) / 60;
        int s = total % 60;
        String western = h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, s) : String.format(Locale.US, "%02d:%02d", m, s);
        return toArabicDigits(western);
    }

    private void downloadSurah(int index, Button button) {
        downloading[index] = true;
        updateDownloadButton(button, index);
        statusText.setText("جاري تنزيل سورة " + surahs[index] + " داخل التطبيق…");
        executor.execute(() -> {
            File out = audioFile(index);
            File part = new File(out.getParentFile(), out.getName() + ".part");
            HttpURLConnection c = null;
            try {
                if (!out.getParentFile().exists() && !out.getParentFile().mkdirs()) throw new Exception("Cannot create directory");
                c = (HttpURLConnection) new URL(getStreamUrl(index)).openConnection();
                c.setInstanceFollowRedirects(true);
                c.setConnectTimeout(20000);
                c.setReadTimeout(45000);
                c.setRequestProperty("User-Agent", "AlMinshawiMurattal/1.1 Android");
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
                if (!part.renameTo(out)) throw new Exception("Rename failed");
                mainHandler.post(() -> {
                    downloading[index] = false;
                    statusText.setText("تم تنزيل سورة " + surahs[index] + " • الصوت والنص والتزامن يعملون دون إنترنت");
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
            } finally {
                if (c != null) c.disconnect();
            }
        });
    }

    private void confirmDelete(int index) {
        new AlertDialog.Builder(this)
                .setTitle("حذف النسخة الأوفلاين؟")
                .setMessage("سيُحذف ملف سورة " + surahs[index] + " المحفوظ داخل التطبيق فقط. نص المصحف وتوقيت الكلمات سيبقيان متاحين أوفلاين.")
                .setPositiveButton("حذف", (d, w) -> {
                    audioFile(index).delete();
                    statusText.setText("تم حذف النسخة الصوتية المحفوظة لسورة " + surahs[index]);
                    rebuildList();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("عن هذه النسخة")
                .setMessage("التطبيق مخصص للختمة الإذاعية الثانية المرتلة للشيخ محمد صديق المنشاوي، المعروفة حديثًا بالمصحف المرتل المفقود.\n\nالإصدار 1.1 يضيف:\n• التحكم في سرعة التشغيل من 0.75× إلى 2× مع ثبات طبقة الصوت.\n• شريطًا للتحريك داخل مدة السورة.\n• نصًا عثمانيًا لمصحف المدينة النبوية برواية حفص مع تقسيم الصفحات 1–604.\n• إضاءة الكلمة المطابقة للصوت اعتمادًا على توقيتات كلمة بكلمة للختمة الجديدة 1967.\n\nملاحظة: عرض النص داخل التطبيق هو عرض رقمي للآيات بالرسم العثماني وتقسيم الصفحات، وليس صورة ممسوحة ضوئيًا للصفحة المطبوعة.\n\nمصدر الصوت: نسخة بث 2026 على Archive.org. توقيتات الكلمات: Qur’anic Universal Audio. النص والصفحات: بيانات مصحف المدينة النبوية/حفص.")
                .setPositiveButton("حسنًا", null).show();
    }

    private void refreshArchiveIndex() {
        executor.execute(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(METADATA_URL).openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(20000);
                c.setRequestProperty("User-Agent", "AlMinshawiMurattal/1.1 Android");
                c.connect();
                if (c.getResponseCode() < 200 || c.getResponseCode() >= 300) throw new Exception("HTTP " + c.getResponseCode());
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
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
                    if (bestName[track - 1] == null || score > bestScore[track - 1]) {
                        bestName[track - 1] = name;
                        bestScore[track - 1] = score;
                    }
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
                mainHandler.post(() -> {
                    if (!syncLoading && statusText != null) {
                        statusText.setText(finalFound == 114
                                ? "تم ربط ١١٤ سورة • النص والتزامن جاهزان • يمكنك تنزيل أي سورة للأوفلاين"
                                : "تم ربط المصدر جزئيًا؛ سيستخدم التطبيق الروابط المحفوظة عند الحاجة.");
                    }
                });
            } catch (Exception ignored) {
                mainHandler.post(() -> {
                    if (!syncLoading && statusText != null) statusText.setText("نص المصحف والتزامن جاهزان؛ تعذر تحديث فهرس الصوت الآن.");
                });
            }
        });
    }

    private void loadCachedUrls() {
        android.content.SharedPreferences p = getSharedPreferences("audio_index", MODE_PRIVATE);
        for (int i = 0; i < 114; i++) streamUrls[i] = p.getString("url_" + (i + 1), null);
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
        return score;
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
        b.setPadding(dp(7), 0, dp(7), 0);
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

    private String toArabicDigits(int value) { return toArabicDigits(String.valueOf(value)); }

    private String toArabicDigits(String western) {
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
    public void onBackPressed() {
        if (readerVisible) hideReader(); else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(progressTicker);
        releasePlayer();
        executor.shutdownNow();
        super.onDestroy();
    }
}
