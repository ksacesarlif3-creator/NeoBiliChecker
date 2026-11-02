from pathlib import Path

p = Path('quranapp/src/main/java/com/minshawi/murattal/lost/MainActivityV2.java')
s = p.read_text(encoding='utf-8')
old = '''                readerScroll.post(() -> {\n                    int y = Math.max(0, now.view.getTop() - dp(95));\n                    readerScroll.smoothScrollTo(0, y);\n                });'''
new = '''                readerScroll.post(() -> {\n                    int y = now.view.getTop();\n                    if (now.view.getParent() instanceof View) y += ((View) now.view.getParent()).getTop();\n                    readerScroll.smoothScrollTo(0, Math.max(0, y - dp(95)));\n                });'''
if old not in s:
    raise SystemExit('scroll patch target not found')
p.write_text(s.replace(old, new, 1), encoding='utf-8')
print('v1.4 parent-aware reader scroll applied')
