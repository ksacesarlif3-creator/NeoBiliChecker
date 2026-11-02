from pathlib import Path

p = Path('quranapp/src/main/java/com/minshawi/murattal/lost/MainActivityV2.java')
s = p.read_text(encoding='utf-8')


def rep(old, new, label):
    global s
    if old not in s:
        raise SystemExit('v1.4 patch target not found: ' + label)
    s = s.replace(old, new, 1)


rep('    private Button repeatButton;\n', '''    private Button repeatButton;\n    private Button downloadAllButton;\n''', 'download button field')

rep('    private final Map<String, RenderedText> renderedTexts = new HashMap<>();\n', '''    private final Map<String, RenderedText> renderedTexts = new HashMap<>();\n    private final Map<String, TextView> tafsirViews = new HashMap<>();\n    private final TabariData tabariData = new TabariData();\n''', 'tafsir fields')

rep('    private int repeatEndMs = -1;\n', '''    private int repeatEndMs = -1;\n\n    private volatile boolean bulkDownloadActive = false;\n    private volatile boolean bulkCancelRequested = false;\n''', 'bulk fields')

rep('''        subtitle.setText("الختمة الإذاعية الثانية • مصحف عثماني • رقية شرعية قرآنية");''', '''        subtitle.setText("الختمة الإذاعية الثانية • مصحف عثماني • تفسير الطبري • رقية شرعية");''', 'header subtitle')

rep('''        topButtons.addView(ruqyah, rlp);\n        tools.addView(topButtons);\n\n        LinearLayout secondary = new LinearLayout(this);''', '''        topButtons.addView(ruqyah, rlp);\n        tools.addView(topButtons);\n\n        downloadAllButton = smallButton("⬇ تنزيل المصحف كاملاً", true);\n        downloadAllButton.setOnClickListener(v -> {\n            if (bulkDownloadActive) {\n                bulkCancelRequested = true;\n                downloadAllButton.setText("جارٍ إيقاف التنزيل…");\n            } else {\n                confirmBulkDownload(allSurahIndices(), "المصحف الكامل");\n            }\n        });\n        LinearLayout.LayoutParams allLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40));\n        allLp.setMargins(0, dp(7), 0, 0);\n        tools.addView(downloadAllButton, allLp);\n\n        LinearLayout secondary = new LinearLayout(this);''', 'download all UI')

rep('''        top.addView(nav);\n\n        TextView title = new TextView(this);''', '''        top.addView(nav);\n\n        Button downloadRuqyah = smallButton("⬇ تنزيل الرقية كاملة للأوفلاين", true);\n        downloadRuqyah.setOnClickListener(v -> confirmBulkDownload(ruqyahAudioIndices(), "الرقية الشرعية"));\n        LinearLayout.LayoutParams drp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40));\n        drp.setMargins(0, dp(7), 0, 0);\n        top.addView(downloadRuqyah, drp);\n\n        TextView title = new TextView(this);''', 'ruqyah download UI')

rep('''        if (listContainer == null) return;\n        String q = searchBox == null ? "" : searchBox.getText().toString().trim();''', '''        if (listContainer == null) return;\n        updateBulkDownloadButton();\n        String q = searchBox == null ? "" : searchBox.getText().toString().trim();''', 'update bulk button in list')

rep('''        download.setOnClickListener(v -> {\n            if (audioFile(index).exists()) confirmDelete(index);\n            else if (!downloading[index]) downloadSurah(index, download);\n        });''', '''        download.setOnClickListener(v -> {\n            if (bulkDownloadActive) {\n                Toast.makeText(this, "تنزيل مجموعة من السور جارٍ الآن", Toast.LENGTH_SHORT).show();\n            } else if (audioFile(index).exists()) confirmDelete(index);\n            else if (!downloading[index]) downloadSurah(index, download);\n        });''', 'prevent individual during bulk')

rep('''        renderedTexts.clear();\n        readerTitle.setText("سورة " + surahs[readerSurahIndex]);''', '''        renderedTexts.clear();\n        tafsirViews.clear();\n        readerTitle.setText("سورة " + surahs[readerSurahIndex]);''', 'clear tafsir views')

rep('''        TextView note = readerMessage(repeatRangeActive && repeatSurahIndex == readerSurahIndex\n                ? "التكرار المستمر مفعّل من الآية " + toArabicDigits(repeatStartAyah) + " إلى " + toArabicDigits(repeatEndAyah) + ". اضغط مطولًا على آية لتغيير المدى."\n                : "تضيء الكلمة الموافقة للصوت تلقائيًا. اضغط على الآية للانتقال إليها، واضغط مطولًا لاختيار مدى للتكرار.", 11, MUTED);''', '''        TextView note = readerMessage(repeatRangeActive && repeatSurahIndex == readerSurahIndex\n                ? "التكرار المستمر مفعّل من الآية " + toArabicDigits(repeatStartAyah) + " إلى " + toArabicDigits(repeatEndAyah) + ". اضغط على الآية لإظهار تفسير الطبري، واضغط مطولًا لتغيير المدى."\n                : "تضيء الكلمة الموافقة للصوت تلقائيًا. اضغط على الآية للانتقال إليها وإظهار تفسير الطبري أسفلها، واضغط مطولًا لاختيار مدى للتكرار.", 11, MUTED);''', 'reader tafsir note')

rep('''            verse.setOnClickListener(v -> seekToAyah(readerSurahIndex, ayahNo));\n            verse.setOnLongClickListener(v -> {\n                showRepeatDialog(readerSurahIndex, ayahNo);\n                return true;\n            });\n            readerBody.addView(verse);\n            renderedTexts.put("a:" + a.number, new RenderedText(verse, display));''', '''            TextView tafsir = new TextView(this);\n            tafsir.setVisibility(View.GONE);\n            tafsir.setTextColor(INK);\n            tafsir.setTextSize(15);\n            tafsir.setTextDirection(View.TEXT_DIRECTION_RTL);\n            tafsir.setGravity(Gravity.RIGHT);\n            tafsir.setLineSpacing(dp(3), 1.12f);\n            tafsir.setPadding(dp(12), dp(10), dp(12), dp(12));\n            tafsir.setBackground(rounded(Color.rgb(242, 237, 219), dp(12), Color.rgb(221, 210, 176), dp(1)));\n\n            verse.setOnClickListener(v -> {\n                seekToAyah(readerSurahIndex, ayahNo);\n                toggleTafsir(surahNo, ayahNo, tafsir);\n            });\n            verse.setOnLongClickListener(v -> {\n                showRepeatDialog(readerSurahIndex, ayahNo);\n                return true;\n            });\n\n            LinearLayout verseBlock = new LinearLayout(this);\n            verseBlock.setOrientation(LinearLayout.VERTICAL);\n            verseBlock.addView(verse);\n            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);\n            tlp.setMargins(0, 0, 0, dp(4));\n            verseBlock.addView(tafsir, tlp);\n            readerBody.addView(verseBlock);\n            tafsirViews.put("a:" + a.number, tafsir);\n            renderedTexts.put("a:" + a.number, new RenderedText(verse, display));''', 'tafsir view under ayah')

marker = '    private void seekToAyah(int surahIndex, int ayah) {'
if marker not in s:
    raise SystemExit('v1.4 patch target not found: tafsir method marker')

t = r'''    private void toggleTafsir(int surah, int ayah, TextView target) {
        if (target == null) return;
        if (target.getVisibility() == View.VISIBLE) {
            target.setVisibility(View.GONE);
            return;
        }
        target.setVisibility(View.VISIBLE);
        target.setText("جاري فتح تفسير الطبري…");
        final String key = "a:" + ayah;
        executor.execute(() -> {
            try {
                String text = tabariData.getTafsir(this, surah, ayah);
                mainHandler.post(() -> {
                    if (readerSurahIndex + 1 != surah || tafsirViews.get(key) != target) return;
                    if (text == null || text.trim().isEmpty()) {
                        target.setText("لم يتوفر نص تفسير الطبري لهذه الآية في البيانات المدمجة.");
                    } else {
                        target.setText("تفسير الطبري — جامع البيان\n\n" + text
                                + "\n\nالمصدر النصي: الموسوعة القرآنية Quranpedia • نسخة البيانات 2026-08-10");
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (tafsirViews.get(key) == target) target.setText("تعذر فتح تفسير الطبري لهذه الآية.");
                });
            }
        });
    }

'''
s = s.replace(marker, t + marker, 1)

marker = '    private void downloadSurah(int index, Button button) {'
if marker not in s:
    raise SystemExit('v1.4 patch target not found: bulk methods marker')

bulk = r'''    private int[] allSurahIndices() {
        int[] out = new int[114];
        for (int i = 0; i < 114; i++) out[i] = i;
        return out;
    }

    private int[] ruqyahAudioIndices() {
        // Same files used by the main mushaf; no duplicate audio is stored.
        return new int[]{0, 1, 6, 9, 19, 108, 111, 112, 113};
    }

    private int downloadedCount(int[] indices) {
        int count = 0;
        for (int i : indices) if (i >= 0 && i < 114 && audioFile(i).exists()) count++;
        return count;
    }

    private void updateBulkDownloadButton() {
        if (downloadAllButton == null) return;
        if (bulkDownloadActive) {
            downloadAllButton.setText(bulkCancelRequested ? "جارٍ إيقاف التنزيل…" : "■ إيقاف تنزيل المصحف");
        } else {
            int have = downloadedCount(allSurahIndices());
            downloadAllButton.setText("⬇ تنزيل المصحف كاملاً  " + toArabicDigits(have) + "/١١٤");
        }
    }

    private void confirmBulkDownload(int[] indices, String label) {
        if (syncData == null || syncLoading) {
            Toast.makeText(this, "بيانات مصدر الصوت ما زالت تُجهز", Toast.LENGTH_SHORT).show();
            return;
        }
        if (bulkDownloadActive) {
            Toast.makeText(this, "تنزيل مجموعة أخرى جارٍ الآن", Toast.LENGTH_SHORT).show();
            return;
        }
        int have = downloadedCount(indices);
        int remaining = Math.max(0, indices.length - have);
        if (remaining == 0) {
            Toast.makeText(this, label + " محفوظ بالفعل للأوفلاين", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("تنزيل " + label + "؟")
                .setMessage("سيتم تنزيل " + toArabicDigits(remaining) + " ملفًا متبقيًا بالتتابع، مع تخطي ما هو محفوظ سابقًا. "
                        + "ملفات الرقية تستخدم نفس ملفات السور ولا تُكرر التخزين. التنزيل الكامل قد يحتاج مساحة كبيرة على الهاتف.")
                .setPositiveButton("بدء التنزيل", (d, w) -> startBulkDownload(indices, label))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void startBulkDownload(int[] indices, String label) {
        if (bulkDownloadActive) return;
        bulkDownloadActive = true;
        bulkCancelRequested = false;
        updateBulkDownloadButton();
        executor.execute(() -> {
            int successes = 0;
            int failed = 0;
            int skipped = 0;
            for (int order = 0; order < indices.length; order++) {
                if (bulkCancelRequested) break;
                int index = indices[order];
                if (index < 0 || index >= 114) continue;
                if (audioFile(index).exists()) {
                    skipped++;
                    continue;
                }
                final int shownOrder = order + 1;
                mainHandler.post(() -> {
                    if (statusText != null) statusText.setText("تنزيل " + label + " • سورة " + surahs[index]
                            + " • " + toArabicDigits(shownOrder) + "/" + toArabicDigits(indices.length));
                });
                downloading[index] = true;
                boolean ok = downloadSurahBlocking(index);
                downloading[index] = false;
                if (bulkCancelRequested) break;
                if (ok) successes++; else failed++;
                mainHandler.post(this::rebuildList);
            }
            final int okCount = successes;
            final int failCount = failed;
            final int skipCount = skipped;
            final boolean cancelled = bulkCancelRequested;
            mainHandler.post(() -> {
                bulkDownloadActive = false;
                bulkCancelRequested = false;
                updateBulkDownloadButton();
                rebuildList();
                if (ruqyahVisible) renderRuqyah();
                if (statusText != null) {
                    if (cancelled) {
                        statusText.setText("تم إيقاف تنزيل " + label + " • الملفات المكتملة بقيت محفوظة");
                    } else if (failCount == 0) {
                        statusText.setText("اكتمل تنزيل " + label + " • الجديد " + toArabicDigits(okCount)
                                + " • المحفوظ سابقًا " + toArabicDigits(skipCount));
                    } else {
                        statusText.setText("انتهى تنزيل " + label + " • نجح " + toArabicDigits(okCount)
                                + " • تعذر " + toArabicDigits(failCount) + " • يمكنك إعادة المحاولة");
                    }
                }
                Toast.makeText(this, cancelled ? "تم إيقاف التنزيل" : "انتهى تنزيل " + label, Toast.LENGTH_LONG).show();
            });
        });
    }

    private boolean downloadSurahBlocking(int index) {
        File out = audioFile(index);
        File part = new File(out.getParentFile(), out.getName() + ".part");
        HttpURLConnection c = null;
        try {
            if (!out.getParentFile().exists() && !out.getParentFile().mkdirs()) return false;
            if (part.exists()) part.delete();
            String url = getStreamUrl(index);
            if (url == null || url.isEmpty()) return false;
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setInstanceFollowRedirects(true);
            c.setConnectTimeout(20000);
            c.setReadTimeout(45000);
            c.setRequestProperty("User-Agent", "AlMinshawiMurattal/1.4 Android");
            c.connect();
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) return false;
            try (InputStream in = new BufferedInputStream(c.getInputStream()); FileOutputStream fos = new FileOutputStream(part)) {
                byte[] buf = new byte[64 * 1024];
                int read;
                while ((read = in.read(buf)) != -1) {
                    if (bulkCancelRequested) {
                        part.delete();
                        return false;
                    }
                    fos.write(buf, 0, read);
                }
                fos.flush();
            }
            if (bulkCancelRequested) {
                part.delete();
                return false;
            }
            if (out.exists()) out.delete();
            return part.renameTo(out);
        } catch (Exception e) {
            part.delete();
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

'''
s = s.replace(marker, bulk + marker, 1)

rep('''                c.setRequestProperty("User-Agent", "AlMinshawiMurattal/1.2 Android");''', '''                c.setRequestProperty("User-Agent", "AlMinshawiMurattal/1.4 Android");''', 'individual user agent')

rep('''        String message = "التطبيق مخصص للختمة الإذاعية الثانية المرتلة للشيخ محمد صديق المنشاوي، المعروفة حديثًا بالمصحف المرتل المفقود.\\\n\\\n"''', '''        String message = "التطبيق مخصص للختمة الإذاعية الثانية المرتلة للشيخ محمد صديق المنشاوي، المعروفة حديثًا بالمصحف المرتل المفقود.\\\n\\\n"''', 'about anchor')

# Add v1.4 feature paragraph before the existing v1.3/ruqyah details without depending on exact prior wording.
about_anchor = '        new AlertDialog.Builder(this)\n                .setTitle("عن هذه النسخة")'
if about_anchor not in s:
    raise SystemExit('v1.4 patch target not found: about dialog')
insert = '''        message += "\\n\\nالإصدار 1.4 يضيف تنزيل المصحف كاملًا، وتنزيل الرقية كاملة باستخدام نفس ملفات السور دون تكرار، وتفسير الطبري (جامع البيان) أسفل الآية عند الضغط عليها. تفسير الطبري مدمج للعمل دون إنترنت ومصدره بيانات الموسوعة القرآنية Quranpedia.";\n'''
s = s.replace(about_anchor, insert + about_anchor, 1)

rep('''    protected void onDestroy() {\n        mainHandler.removeCallbacks(progressTicker);''', '''    protected void onDestroy() {\n        bulkCancelRequested = true;\n        mainHandler.removeCallbacks(progressTicker);''', 'cancel bulk on destroy')

p.write_text(s, encoding='utf-8')
print('v1.4 enhancements applied:', s.count('toggleTafsir'), s.count('bulkDownloadActive'))
