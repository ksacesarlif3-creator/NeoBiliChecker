package com.minshawi.murattal.lost;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivityV2 extends Activity {
    private static final int GREEN = Color.rgb(12, 59, 50);
    private static final int GREEN_2 = Color.rgb(19, 78, 66);
    private static final int GOLD = Color.rgb(182, 144, 59);
    private static final int GOLD_LIGHT = Color.rgb(247, 224, 137);
    private static final int CREAM = Color.rgb(248, 244, 232);
    private static final int INK = Color.rgb(35, 42, 39);
    private static final int MUTED = Color.rgb(100, 111, 106);

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final boolean[] downloading = new boolean[114];

    private FrameLayout screenHost;
    private LinearLayout listScreen;
    private LinearLayout readerScreen;
    private LinearLayout ruqyahScreen;
    private LinearLayout listContainer;
    private LinearLayout readerBody;
    private LinearLayout ruqyahBody;
    private ScrollView readerScroll;
    private TextView statusText;
    private TextView nowPlayingText;
    private TextView elapsedText;
    private TextView durationText;
    private TextView readerTitle;
    private TextView readerInfo;
    private TextView ruqyahStatus;
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
    private boolean ruqyahVisible = false;
    private boolean readerReturnToRuqyah = false;
    private int readerSurahIndex = 0;
    private int renderedSurahIndex = -1;
    private final Map<String, RenderedText> renderedTexts = new HashMap<>();
    private String highlightedKey = null;
    private int highlightedWord = -1;
    private int highlightedAyah = -1;

    private boolean ruqyahActive = false;
    private int ruqyahSegmentIndex = -1;
    private int ruqyahRepeatIndex = 0;
    private int ruqyahStopMs = -1;
    private boolean ruqyahSingleBlock = false;

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

    private static final class RuqyahSegment {
        final String title;
        final int surah;
        final int startAyah;
        final int endAyah;
        final int repeats;

        RuqyahSegment(String title, int surah, int startAyah, int endAyah, int repeats) {
            this.title = title;
            this.surah = surah;
            this.startAyah = startAyah;
            this.endAyah = endAyah;
            this.repeats = repeats;
        }
    }

    private final RuqyahSegment[] ruqyahPlan = new RuqyahSegment[]{
            new RuqyahSegment("سورة الفاتحة", 1, 1, 7, 7),
            new RuqyahSegment("آية الكرسي", 2, 255, 255, 1),
            new RuqyahSegment("خواتيم سورة البقرة", 2, 285, 286, 1),
            new RuqyahSegment("آيات السحر من سورة الأعراف", 7, 117, 122, 1),
            new RuqyahSegment("آيات السحر من سورة يونس", 10, 81, 82, 1),
            new RuqyahSegment("آية السحر من سورة طه", 20, 69, 69, 1),
            new RuqyahSegment("سورة الكافرون", 109, 1, 6, 1),
            new RuqyahSegment("سورة الإخلاص", 112, 1, 4, 3),
            new RuqyahSegment("سورة الفلق", 113, 1, 5, 3),
            new RuqyahSegment("سورة الناس", 114, 1, 6, 3)
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
        buildUi();
        loadSyncAssets();
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
        ruqyahScreen = buildRuqyahScreen();
        readerScreen.setVisibility(View.GONE);
        ruqyahScreen.setVisibility(View.GONE);
        screenHost.addView(listScreen, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        screenHost.addView(readerScreen, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        screenHost.addView(ruqyahScreen, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(screenHost, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(buildPlayerPanel());
        setContentView(root);
        rebuildList();
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(18), dp(16), dp(18), dp(12));
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
        title.setPadding(0, dp(8), 0, dp(3));
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("الختمة الإذاعية الثانية • مصحف عثماني • رقية شرعية قرآنية");
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

        LinearLayout topButtons = new LinearLayout(this);
        topButtons.setOrientation(LinearLayout.HORIZONTAL);
        topButtons.setGravity(Gravity.CENTER);
        topButtons.setPadding(0, dp(8), 0, 0);

        Button mushaf = smallButton("📖 فتح المصحف", true);
        mushaf.setOnClickListener(v -> showReader(currentIndex >= 0 ? currentIndex : 0));
        topButtons.addView(mushaf, new LinearLayout.LayoutParams(0, dp(42), 1f));

        Button ruqyah = smallButton("🕊 الرقية الشرعية", true);
        ruqyah.setOnClickListener(v -> showRuqyah());
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        rlp.setMarginStart(dp(7));
        topButtons.addView(ruqyah, rlp);
        tools.addView(topButtons);

        LinearLayout secondary = new LinearLayout(this);
        secondary.setOrientation(LinearLayout.HORIZONTAL);
        secondary.setGravity(Gravity.CENTER_VERTICAL);
        secondary.setPadding(0, dp(7), 0, 0);

        lostFilterButton = smallButton("المقاطع المعادة فقط", false);
        lostFilterButton.setOnClickListener(v -> {
            lostOnly = !lostOnly;
            lostFilterButton.setText(lostOnly ? "عرض كل السور" : "المقاطع المعادة فقط");
            rebuildList();
        });
        secondary.addView(lostFilterButton);

        Button info = smallButton("عن النسخة", false);
        info.setOnClickListener(v -> showAbout());
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        ilp.setMarginStart(dp(7));
        secondary.addView(info, ilp);
        tools.addView(secondary);

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
        Button back = smallButton("رجوع", false);
        back.setOnClickListener(v -> hideReader());
        nav.addView(back);
        Button prev = smallButton("السورة السابقة", true);
        prev.setOnClickListener(v -> renderReader((readerSurahIndex + 113) % 114, true));
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        np.setMarginStart(dp(6));
        nav.addView(prev, np);
        Button next = smallButton("السورة التالية", true);
        next.setOnClickListener(v -> renderReader((readerSurahIndex + 1) % 114, true));
        LinearLayout.LayoutParams nn = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
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

    private LinearLayout buildRuqyahScreen() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(CREAM);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(14), dp(9), dp(14), dp(9));
        top.setBackgroundColor(Color.rgb(239, 232, 209));

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        Button back = smallButton("قائمة السور", false);
        back.setOnClickListener(v -> hideRuqyah());
        nav.addView(back);

        Button playAll = smallButton("▶ تشغيل الرقية كاملة", true);
        playAll.setOnClickListener(v -> startRuqyah(0, false));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        pp.setMarginStart(dp(7));
        nav.addView(playAll, pp);

        Button stop = smallButton("■ إيقاف", false);
        stop.setOnClickListener(v -> stopRuqyah(true));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
        sp.setMarginStart(dp(7));
        nav.addView(stop, sp);
        top.addView(nav);

        TextView title = new TextView(this);
        title.setText("الرقية الشرعية القرآنية");
        title.setTextColor(GREEN);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(8), 0, dp(3));
        top.addView(title);

        TextView note = new TextView(this);
        note.setText("بتلاوة الشيخ محمد صديق المنشاوي المرتلة. الترتيب أدناه تنظيم تطبيقي لآيات وسور تُقرأ في الرقية، وليس ادعاءً أن هذا التسلسل المحدد ورد كورد نبوي ثابت.");
        note.setTextColor(MUTED);
        note.setTextSize(11);
        note.setGravity(Gravity.CENTER);
        note.setTextDirection(View.TEXT_DIRECTION_RTL);
        top.addView(note);

        ruqyahStatus = new TextView(this);
        ruqyahStatus.setText("يمكن تشغيل الرقية كاملة أو اختيار مقطع بعينه.");
        ruqyahStatus.setTextColor(GOLD);
        ruqyahStatus.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        ruqyahStatus.setTextSize(12);
        ruqyahStatus.setGravity(Gravity.CENTER);
        ruqyahStatus.setPadding(0, dp(6), 0, 0);
        top.addView(ruqyahStatus);
        screen.addView(top);

        ScrollView scroll = new ScrollView(this);
        ruqyahBody = new LinearLayout(this);
        ruqyahBody.setOrientation(LinearLayout.VERTICAL);
        ruqyahBody.setPadding(dp(12), dp(8), dp(12), dp(24));
        scroll.addView(ruqyahBody, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        screen.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
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
        nowPlayingText.setText("اختر سورة أو افتح الرقية الشرعية");
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
            if (readerVisible) hideReader();
            else showReader(currentIndex >= 0 ? currentIndex : readerSurahIndex);
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
                    if (ruqyahVisible) renderRuqyah();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    syncLoading = false;
                    if (statusText != null) statusText.setText("تعذر تحميل بيانات المصحف المدمجة. أعد تثبيت التطبيق إن استمر الخلل.");
                    if (ruqyahStatus != null) ruqyahStatus.setText("تعذر تحميل بيانات المصحف والتوقيت.");
                    Toast.makeText(this, "تعذر تحميل نص المصحف أو توقيت الكلمات", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showReader(int index) {
        readerReturnToRuqyah = ruqyahVisible;
        readerVisible = true;
        ruqyahVisible = false;
        listScreen.setVisibility(View.GONE);
        ruqyahScreen.setVisibility(View.GONE);
        readerScreen.setVisibility(View.VISIBLE);
        readerToggleButton.setText("رجوع");
        renderReader(Math.max(0, Math.min(113, index)), false);
    }

    private void hideReader() {
        readerVisible = false;
        readerScreen.setVisibility(View.GONE);
        if (readerReturnToRuqyah) {
            ruqyahVisible = true;
            ruqyahScreen.setVisibility(View.VISIBLE);
            listScreen.setVisibility(View.GONE);
            renderRuqyah();
        } else {
            ruqyahVisible = false;
            ruqyahScreen.setVisibility(View.GONE);
            listScreen.setVisibility(View.VISIBLE);
        }
        readerReturnToRuqyah = false;
        readerToggleButton.setText("📖 متابعة النص");
    }

    private void showRuqyah() {
        readerVisible = false;
        ruqyahVisible = true;
        readerScreen.setVisibility(View.GONE);
        listScreen.setVisibility(View.GONE);
        ruqyahScreen.setVisibility(View.VISIBLE);
        readerToggleButton.setText("📖 متابعة النص");
        renderRuqyah();
    }

    private void hideRuqyah() {
        ruqyahVisible = false;
        ruqyahScreen.setVisibility(View.GONE);
        readerScreen.setVisibility(View.GONE);
        listScreen.setVisibility(View.VISIBLE);
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
            TextView wait = readerMessage("جاري تحميل نص المصحف وتوقيت الكلمات…", 17, MUTED);
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

    private void renderRuqyah() {
        if (ruqyahBody == null) return;
        ruqyahBody.removeAllViews();
        if (syncLoading || syncData == null) {
            TextView wait = readerMessage("جاري تجهيز نص الرقية وتوقيت التلاوة…", 16, MUTED);
            wait.setGravity(Gravity.CENTER);
            ruqyahBody.addView(wait);
            return;
        }

        if (ruqyahActive && ruqyahSegmentIndex >= 0 && ruqyahSegmentIndex < ruqyahPlan.length) {
            RuqyahSegment s = ruqyahPlan[ruqyahSegmentIndex];
            ruqyahStatus.setText("الآن: " + s.title + " • التكرار " + toArabicDigits(ruqyahRepeatIndex + 1) + " من " + toArabicDigits(s.repeats));
        } else {
            ruqyahStatus.setText("يمكن تشغيل الرقية كاملة أو اختيار مقطع بعينه.");
        }

        for (int i = 0; i < ruqyahPlan.length; i++) ruqyahBody.addView(buildRuqyahCard(i));

        TextView footer = readerMessage("الاستماع للتسجيل وسيلة مساعدة؛ والرقية تكون بالقراءة والدعاء والنفث مع التوكل على الله، ولا تُعد بديلًا عن الرعاية الطبية عند الحاجة.", 11, MUTED);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(8), dp(12), dp(8), dp(8));
        ruqyahBody.addView(footer);
    }

    private View buildRuqyahCard(int index) {
        RuqyahSegment s = ruqyahPlan[index];
        boolean active = ruqyahActive && index == ruqyahSegmentIndex;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackground(rounded(active ? Color.rgb(255, 248, 218) : Color.WHITE, dp(16), active ? GOLD : Color.rgb(230, 224, 204), active ? dp(2) : dp(1)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        card.setLayoutParams(lp);

        TextView title = new TextView(this);
        title.setText((active ? "● " : "") + s.title);
        title.setTextColor(active ? GREEN : INK);
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(title);

        TextView meta = new TextView(this);
        meta.setText(segmentReference(s) + (s.repeats > 1 ? " • تكرار ×" + toArabicDigits(s.repeats) : " • مرة واحدة"));
        meta.setTextColor(GOLD);
        meta.setTextSize(11);
        card.addView(meta);

        TextView quran = new TextView(this);
        quran.setText(segmentText(s));
        quran.setTextColor(INK);
        quran.setTextSize(18);
        quran.setTextDirection(View.TEXT_DIRECTION_RTL);
        quran.setGravity(Gravity.RIGHT);
        quran.setLineSpacing(dp(3), 1.12f);
        quran.setPadding(0, dp(6), 0, dp(7));
        card.addView(quran);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        Button play = smallButton("▶ تشغيل المقطع", true);
        play.setOnClickListener(v -> startRuqyah(index, true));
        buttons.addView(play, new LinearLayout.LayoutParams(0, dp(40), 1f));
        Button read = smallButton("📖 عرض في المصحف", false);
        read.setOnClickListener(v -> {
            showReader(s.surah - 1);
            mainHandler.postDelayed(() -> seekToAyah(s.surah - 1, s.startAyah), 120);
        });
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        rp.setMarginStart(dp(7));
        buttons.addView(read, rp);
        card.addView(buttons);
        return card;
    }

    private String segmentReference(RuqyahSegment s) {
        String ayah = s.startAyah == s.endAyah
                ? toArabicDigits(s.startAyah)
                : toArabicDigits(s.startAyah) + "–" + toArabicDigits(s.endAyah);
        return "سورة " + surahs[s.surah - 1] + " • الآية " + ayah;
    }

    private String segmentText(RuqyahSegment s) {
        if (syncData == null) return "";
        StringBuilder out = new StringBuilder();
        for (int a = s.startAyah; a <= s.endAyah; a++) {
            String t = syncData.getAyahText(s.surah, a);
            if (t.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(t).append(" ﴿").append(toArabicDigits(a)).append("﴾");
        }
        return out.toString();
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
            playSurah(surahIndex, ms, false);
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

    private void startRuqyah(int segmentIndex, boolean singleBlock) {
        if (syncData == null || syncLoading) {
            Toast.makeText(this, "بيانات المصحف ما زالت تُجهز", Toast.LENGTH_SHORT).show();
            return;
        }
        if (segmentIndex < 0 || segmentIndex >= ruqyahPlan.length) return;
        ruqyahActive = true;
        ruqyahSingleBlock = singleBlock;
        ruqyahSegmentIndex = segmentIndex;
        ruqyahRepeatIndex = 0;
        playCurrentRuqyahSegment();
    }

    private void playCurrentRuqyahSegment() {
        if (!ruqyahActive || syncData == null || ruqyahSegmentIndex < 0 || ruqyahSegmentIndex >= ruqyahPlan.length) return;
        RuqyahSegment s = ruqyahPlan[ruqyahSegmentIndex];
        int start = syncData.firstWordStartMs(s.surah, s.startAyah);
        int end = syncData.lastWordEndMs(s.surah, s.endAyah);
        if (start < 0 || end <= start) {
            advanceRuqyah();
            return;
        }
        ruqyahStopMs = end + 180;
        playSurah(s.surah - 1, start, true);
        nowPlayingText.setText("الرقية • " + s.title + " • " + toArabicDigits(ruqyahRepeatIndex + 1) + "/" + toArabicDigits(s.repeats));
        if (ruqyahVisible) renderRuqyah();
    }

    private void advanceRuqyah() {
        if (!ruqyahActive || ruqyahSegmentIndex < 0 || ruqyahSegmentIndex >= ruqyahPlan.length) return;
        ruqyahStopMs = -1;
        RuqyahSegment current = ruqyahPlan[ruqyahSegmentIndex];
        if (ruqyahRepeatIndex + 1 < current.repeats) {
            ruqyahRepeatIndex++;
            int start = syncData == null ? -1 : syncData.firstWordStartMs(current.surah, current.startAyah);
            int end = syncData == null ? -1 : syncData.lastWordEndMs(current.surah, current.endAyah);
            if (start >= 0 && end > start && mediaPlayer != null && !preparing && currentIndex == current.surah - 1) {
                try {
                    ruqyahStopMs = end + 180;
                    mediaPlayer.seekTo(start);
                    mediaPlayer.start();
                    applyPlaybackSpeed(mediaPlayer);
                    bottomPlayButton.setText("❚❚");
                    nowPlayingText.setText("الرقية • " + current.title + " • " + toArabicDigits(ruqyahRepeatIndex + 1) + "/" + toArabicDigits(current.repeats));
                    if (ruqyahVisible) renderRuqyah();
                    return;
                } catch (Exception ignored) {}
            }
            playCurrentRuqyahSegment();
            return;
        }

        if (ruqyahSingleBlock || ruqyahSegmentIndex >= ruqyahPlan.length - 1) {
            stopRuqyah(false);
            return;
        }

        ruqyahSegmentIndex++;
        ruqyahRepeatIndex = 0;
        playCurrentRuqyahSegment();
    }

    private void stopRuqyah(boolean manual) {
        boolean wasActive = ruqyahActive;
        ruqyahActive = false;
        ruqyahStopMs = -1;
        ruqyahSegmentIndex = -1;
        ruqyahRepeatIndex = 0;
        ruqyahSingleBlock = false;
        if (mediaPlayer != null && !preparing) {
            try { if (mediaPlayer.isPlaying()) mediaPlayer.pause(); } catch (Exception ignored) {}
        }
        bottomPlayButton.setText("▶");
        if (wasActive) {
            nowPlayingText.setText(manual ? "تم إيقاف الرقية الشرعية" : "اكتملت الرقية الشرعية");
            if (ruqyahStatus != null) ruqyahStatus.setText(manual ? "تم إيقاف الرقية." : "اكتملت الرقية بحمد الله.");
            if (ruqyahVisible) renderRuqyah();
        }
    }

    private void playSurah(int index) { playSurah(index, -1, false); }

    private void playSurah(int index, int initialSeekMs, boolean fromRuqyah) {
        if (index < 0 || index >= 114) return;
        if (syncData == null) {
            if (statusText != null) statusText.setText("جاري تجهيز مصدر التلاوة المتزامن…");
            Toast.makeText(this, "بيانات التزامن تُجهز الآن، أعد المحاولة بعد لحظة", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!fromRuqyah) {
            ruqyahActive = false;
            ruqyahStopMs = -1;
            ruqyahSegmentIndex = -1;
            ruqyahRepeatIndex = 0;
            ruqyahSingleBlock = false;
        }

        if (!fromRuqyah && currentIndex == index && mediaPlayer != null && !preparing && initialSeekMs < 0) {
            toggleCurrent();
            rebuildList();
            return;
        }

        currentIndex = index;
        if (readerVisible) renderReader(index, false);
        File local = audioFile(index);
        String source = local.exists() ? local.getAbsolutePath() : getStreamUrl(index);
        if (source == null || source.isEmpty()) {
            Toast.makeText(this, "تعذر تحديد مصدر التلاوة", Toast.LENGTH_LONG).show();
            return;
        }
        if (!fromRuqyah) nowPlayingText.setText("سورة " + surahs[index] + (local.exists() ? " • أوفلاين" : " • المصدر المتزامن"));
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
            if (statusText != null) statusText.setText(local.exists()
                    ? "تشغيل أوفلاين • التزامن كلمة بكلمة يعمل من البيانات المدمجة"
                    : "تشغيل من المصدر الأصلي المطابق لتوقيت الكلمات • التزامن الدقيق مفعّل");
            rebuildList();
        });
        mediaPlayer.setOnCompletionListener(mp -> {
            bottomPlayButton.setText("▶");
            clearHighlight();
            if (ruqyahActive) advanceRuqyah();
            else if (currentIndex < 113) playSurah(currentIndex + 1);
        });
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            preparing = false;
            bottomPlayButton.setText("▶");
            seekBar.setEnabled(false);
            if (statusText != null) statusText.setText("تعذر تشغيل هذه السورة من المصدر. أعد المحاولة بعد التحقق من الاتصال.");
            Toast.makeText(this, "تعذر تشغيل سورة " + surahs[index], Toast.LENGTH_LONG).show();
            return true;
        });
        try {
            mediaPlayer.setDataSource(source);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            preparing = false;
            bottomPlayButton.setText("▶");
            if (statusText != null) statusText.setText("تعذر فتح ملف التلاوة.");
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
        if (mediaPlayer == null || preparing) return;
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                bottomPlayButton.setText("▶");
            } else {
                mediaPlayer.start();
                applyPlaybackSpeed(mediaPlayer);
                bottomPlayButton.setText("❚❚");
            }
        } catch (Exception ignored) {}
        rebuildList();
    }

    private void playRelative(int delta) {
        if (ruqyahActive && ruqyahSegmentIndex >= 0) {
            int next = ruqyahSegmentIndex + delta;
            if (next < 0) next = 0;
            if (next >= ruqyahPlan.length) next = ruqyahPlan.length - 1;
            ruqyahSegmentIndex = next;
            ruqyahRepeatIndex = 0;
            playCurrentRuqyahSegment();
            return;
        }
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
                    if (ruqyahActive && ruqyahStopMs > 0 && pos >= ruqyahStopMs) {
                        advanceRuqyah();
                    }
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
        if (syncData == null) {
            Toast.makeText(this, "بيانات مصدر الصوت ما زالت تُجهز", Toast.LENGTH_SHORT).show();
            return;
        }
        downloading[index] = true;
        updateDownloadButton(button, index);
        statusText.setText("جاري تنزيل سورة " + surahs[index] + " داخل التطبيق…");
        executor.execute(() -> {
            File out = audioFile(index);
            File part = new File(out.getParentFile(), out.getName() + ".part");
            HttpURLConnection c = null;
            try {
                if (!out.getParentFile().exists() && !out.getParentFile().mkdirs()) throw new Exception("Cannot create directory");
                String url = getStreamUrl(index);
                if (url == null) throw new Exception("No audio URL");
                c = (HttpURLConnection) new URL(url).openConnection();
                c.setInstanceFollowRedirects(true);
                c.setConnectTimeout(20000);
                c.setReadTimeout(45000);
                c.setRequestProperty("User-Agent", "AlMinshawiMurattal/1.2 Android");
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
        String message = "التطبيق مخصص للختمة الإذاعية الثانية المرتلة للشيخ محمد صديق المنشاوي، المعروفة حديثًا بالمصحف المرتل المفقود.\n\n"
                + "الإصدار 1.2 يضيف قسم الرقية الشرعية القرآنية بصوت الشيخ المنشاوي، مع تشغيل متتابع وتكرار آلي للمقاطع المختارة، مع استمرار التحكم في السرعة وشريط الزمن ومتابعة النص.\n\n"
                + "الرقية المضمنة تشمل: الفاتحة، آية الكرسي، خواتيم البقرة، آيات السحر من الأعراف ويونس وطه، الكافرون، الإخلاص، الفلق، والناس. ترتيب القائمة تنظيم تطبيقي ولا يُقصد به أن هذا التسلسل المحدد ورد كورد نبوي ثابت.\n\n"
                + "النص عثماني برواية حفص مع تقسيم صفحات مصحف المدينة 1–604، وتوقيت الكلمات متزامن مع التسجيل الأصلي للختمة الجديدة/الثانية.\n\n"
                + "الاستماع للرقية المسجلة وسيلة مساعدة، ولا يغني عن قراءة الإنسان ورقيته لنفسه والدعاء، ولا عن التقييم الطبي عند الحاجة.";
        new AlertDialog.Builder(this)
                .setTitle("عن هذه النسخة")
                .setMessage(message)
                .setPositiveButton("حسنًا", null)
                .show();
    }

    private String getStreamUrl(int index) {
        if (syncData == null) return null;
        return syncData.exactAudioUrl(index + 1);
    }

    private File audioFile(int index) {
        return new File(new File(getFilesDir(), "audio_exact_v11"), String.format(Locale.US, "%03d.mp3", index + 1));
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
        if (readerVisible) hideReader();
        else if (ruqyahVisible) hideRuqyah();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(progressTicker);
        releasePlayer();
        executor.shutdownNow();
        super.onDestroy();
    }
}
