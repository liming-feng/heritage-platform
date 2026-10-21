# -*- coding: utf-8 -*-
"""
生成演示用占位图（SVG，离线可用，不依赖外网图床）

用法：
    python gen_placeholder_images.py

输出目录：../uploads/demo/
"""
import os
import pathlib

BASE = pathlib.Path(__file__).resolve().parent.parent / "uploads" / "demo"
BASE.mkdir(parents=True, exist_ok=True)

# 非遗色系调色板（主色, 辅色）—— 朱砂 / 靛青 / 松绿 / 藤黄 / 赭石 / 紫檀
PALETTE = [
    ("#9c2c2c", "#c8a45c"),
    ("#2f5061", "#7fb3c8"),
    ("#3f6653", "#a8c9a0"),
    ("#b07d2b", "#e6c77a"),
    ("#8a5230", "#d3a37a"),
    ("#5b3a5e", "#b393b8"),
    ("#1f4e5f", "#68a8b8"),
    ("#7a1f20", "#d98b7a"),
]

FONT = "'PingFang SC','Microsoft YaHei',sans-serif"


def svg(w, h, title, subtitle, c1, c2, tag=""):
    """生成一张带渐变背景与中文标题的 SVG"""
    fs_title = int(min(w, h) * 0.10)
    fs_sub = int(min(w, h) * 0.055)
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="{c1}"/>
      <stop offset="100%" stop-color="{c2}"/>
    </linearGradient>
    <pattern id="p" width="40" height="40" patternUnits="userSpaceOnUse">
      <circle cx="20" cy="20" r="1.5" fill="#ffffff" opacity="0.18"/>
    </pattern>
  </defs>
  <rect width="{w}" height="{h}" fill="url(#g)"/>
  <rect width="{w}" height="{h}" fill="url(#p)"/>
  <rect x="{w*0.04:.0f}" y="{h*0.06:.0f}" width="{w*0.92:.0f}" height="{h*0.88:.0f}"
        fill="none" stroke="#ffffff" stroke-opacity="0.35" stroke-width="1.5" rx="8"/>
  <text x="50%" y="47%" text-anchor="middle" fill="#ffffff" opacity="0.96"
        font-family="{FONT}" font-size="{fs_title}" font-weight="600">{title}</text>
  <text x="50%" y="62%" text-anchor="middle" fill="#ffffff" opacity="0.78"
        font-family="{FONT}" font-size="{fs_sub}">{subtitle}</text>
  {'<text x="50%" y="78%" text-anchor="middle" fill="#ffffff" opacity="0.6" font-family="' + FONT + '" font-size="' + str(int(min(w, h) * 0.038)) + '">' + tag + '</text>' if tag else ''}
</svg>
'''


def write(name, content):
    (BASE / name).write_text(content, encoding="utf-8")


counter = [0]


def pick():
    c = PALETTE[counter[0] % len(PALETTE)]
    counter[0] += 1
    return c


# ---------- 1. 头像 ----------
avatars = [
    ("avatar-admin.svg", "管", "管理员"),
    ("avatar-user1.svg", "陈", "非遗爱好者"),
    ("avatar-user2.svg", "刘", "手作小白"),
    ("avatar-c1.svg", "张", "剪纸匠人"),
    ("avatar-c2.svg", "李", "木雕匠人"),
    ("avatar-c3.svg", "王", "陶艺匠人"),
]
for name, char, sub in avatars:
    c1, c2 = pick()
    write(name, f'''<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120">
  <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0%" stop-color="{c1}"/><stop offset="100%" stop-color="{c2}"/>
  </linearGradient></defs>
  <rect width="120" height="120" fill="url(#g)"/>
  <text x="50%" y="56%" text-anchor="middle" fill="#fff" font-family="{FONT}"
        font-size="46" font-weight="600">{char}</text>
  <text x="50%" y="78%" text-anchor="middle" fill="#fff" opacity="0.8"
        font-family="{FONT}" font-size="13">{sub}</text>
</svg>
''')

# ---------- 2. 分类图标 ----------
cats = [
    ("cat-paper.svg", "剪纸"),
    ("cat-wood.svg", "木雕"),
    ("cat-pottery.svg", "陶艺"),
    ("cat-embro.svg", "刺绣"),
    ("cat-weave.svg", "编织"),
    ("cat-lacquer.svg", "漆器"),
]
for name, label in cats:
    c1, c2 = pick()
    write(name, f'''<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 96 96">
  <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0%" stop-color="{c1}"/><stop offset="100%" stop-color="{c2}"/>
  </linearGradient></defs>
  <rect width="96" height="96" rx="20" fill="url(#g)"/>
  <text x="50%" y="58%" text-anchor="middle" fill="#fff" font-family="{FONT}"
        font-size="26" font-weight="600">{label}</text>
</svg>
''')

# ---------- 3. 匠人主页封面 ----------
for i, (nm, label) in enumerate([("craft-cover-1.svg", "剪纸 · 张建"),
                                 ("craft-cover-2.svg", "木雕 · 李木"),
                                 ("craft-cover-3.svg", "陶艺 · 王陶")], start=1):
    c1, c2 = pick()
    write(nm, svg(900, 320, label, "非遗匠人工作室", c1, c2, "INTANGIBLE HERITAGE"))

# ---------- 4. 商品图（正图 + 2 张细节图） ----------
goods_names = [
    "蔚县手工剪纸·喜上眉梢", "剪纸书签·四季花鸟", "大幅剪纸·百子图",
    "东阳木雕·松鹤笔筒", "木雕摆件·如意观音", "木雕茶则·禅意荷塘", "木雕挂屏·山水四条屏",
    "柴烧茶盏·天青釉", "青花分水·缠枝莲盖碗", "手作陶花瓶·粗陶素坯", "陶艺体验套票·亲子课",
    "苏绣团扇·双面绣猫", "刺绣手帕·并蒂莲", "苗绣背扇·蝶恋花",
    "竹编提篮·六角孔", "草编收纳筐·三件套", "柳编花器·落地款",
    "平遥推光漆器·首饰盒", "漆器茶盘·大漆螺钿", "漆艺杯垫·四色一组",
]
suffix = [("", "主图", ""), ("-b", "细节图", "细节"), ("-c", "场景图", "场景")]
for idx, gname in enumerate(goods_names, start=1):
    for suf, tag, sub in suffix:
        c1, c2 = pick()
        write(f"goods-{idx}{suf}.svg",
              svg(600, 450, gname, f"{tag} · 非遗手作", c1, c2, sub))

# ---------- 5. 课程封面 ----------
courses = [
    "蔚县剪纸入门体验课",
    "东阳木雕浅浮雕入门",
    "陶艺拉坯初体验",
    "苏绣基础针法体验",
    "竹编六角孔果篮体验",
]
for idx, cname in enumerate(courses, start=1):
    c1, c2 = pick()
    write(f"course-{idx}.svg", svg(720, 420, cname, "线下体验课 · 可预约", c1, c2, "EXPERIENCE COURSE"))

# ---------- 6. 资讯封面 ----------
articles = [
    "从重展示到重转化：非遗电商化的三条路径",
    "蔚县剪纸：一张纸上的千年刀工",
    "东阳木雕的散点透视",
    "柴烧为什么贵：一次窑变的成本账",
    "平台体验课程预约规则说明",
    "非遗手作入门怎么选",
]
for idx, atitle in enumerate(articles, start=1):
    c1, c2 = pick()
    write(f"article-{idx}.svg", svg(720, 400, atitle, "非遗资讯", c1, c2, "ARTICLE"))

# ---------- 7. 轮播图 ----------
banners = [
    "千年非遗，今日可约",
    "走进匠人的工作室",
    "手作好物 · 限时满减",
    "非遗电商化的三条路径",
]
for idx, btitle in enumerate(banners, start=1):
    c1, c2 = pick()
    write(f"banner-{idx}.svg", svg(1400, 440, btitle, "非遗手工艺品展示与预约体验平台", c1, c2, ""))

files = sorted(os.listdir(BASE))
print(f"生成完成，共 {len(files)} 个文件 -> {BASE}")
for f in files[:10]:
    print("  ", f)
print("   ...")
