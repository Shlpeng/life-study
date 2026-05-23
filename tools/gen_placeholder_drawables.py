#!/usr/bin/env python3
"""为没有真实图片的条目生成占位 PNG, 落到 app/src/main/res/drawable/<itemId>.png.

设计:
  - 480x360 (与 picsum URL 同尺寸, 不浪费内存)
  - 分类 accent 色的垂直渐变背景
  - 中央偏上一个半透明白色卡片层提升对比度
  - 顶部一行分类大写英文标签 (避免 emoji 在 PIL freetype 下渲染失败)
  - 中央大号中文条目名 (PingFang.ttc, 字号根据宽度自适应)
  - 底部一行英文条目名 / itemId

可重跑: 已存在的同名文件会被覆盖. 已经放过真实 .jpg 的条目不在此清单里,
        所以不会误覆盖。
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
DRAWABLE = ROOT / "app/src/main/res/drawable"

# macOS 自带中英文字体
CN_FONT_REG = "/System/Library/Fonts/PingFang.ttc"      # index 0 Regular
CN_FONT_BOLD = "/System/Library/Fonts/STHeiti Medium.ttc"  # 较粗, 适合大标题

W, H = 480, 360


def hex_to_rgb(s: str) -> tuple[int, int, int]:
    """接受 'AARRGGBB' / 'RRGGBB' / '#RRGGBB'."""
    s = s.lstrip("#")
    if len(s) == 8:
        s = s[2:]  # 丢掉 alpha
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


def lighten(rgb, f: float):
    return tuple(min(255, int(c + (255 - c) * f)) for c in rgb)


def darken(rgb, f: float):
    return tuple(max(0, int(c * (1 - f))) for c in rgb)


def gradient_bg(size, top, bottom) -> Image.Image:
    """简单的上下垂直渐变."""
    w, h = size
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / (h - 1)
        r = int(top[0] + (bottom[0] - top[0]) * t)
        g = int(top[1] + (bottom[1] - top[1]) * t)
        b = int(top[2] + (bottom[2] - top[2]) * t)
        d.line([(0, y), (w, y)], fill=(r, g, b))
    return img


def fit_font(draw, text: str, font_path: str, max_width: int,
             sizes=(80, 72, 64, 56, 48, 42, 36, 32, 28)) -> ImageFont.ImageFont:
    """从大到小试字号, 找到首个能塞下的."""
    for size in sizes:
        f = ImageFont.truetype(font_path, size)
        bbox = draw.textbbox((0, 0), text, font=f)
        if (bbox[2] - bbox[0]) <= max_width:
            return f
    return ImageFont.truetype(font_path, sizes[-1])


def render_card(item_id: str, name: str, tag: str, accent_hex: str, en_name: str,
                out: Path) -> None:
    accent = hex_to_rgb(accent_hex)
    # 从 accent 渐变到更深 (饱和) 的版本, 制造光从顶部洒下的卡片感.
    top = lighten(accent, 0.05)
    bottom = darken(accent, 0.42)
    img = gradient_bg((W, H), top, bottom).convert("RGBA")
    d = ImageDraw.Draw(img, "RGBA")

    pad = 22
    # 细描边, 让卡片在浅色背景中也有边界
    d.rounded_rectangle(
        [(pad, pad), (W - pad, H - pad)],
        radius=24,
        outline=(255, 255, 255, 90),
        width=2,
    )

    # 顶部: 分类大写英文标签 (字符间距用空格模拟更通透)
    tag_text = " ".join(list(tag))
    tag_font = ImageFont.truetype(CN_FONT_REG, 18)
    tb = d.textbbox((0, 0), tag_text, font=tag_font)
    tw = tb[2] - tb[0]
    d.text(((W - tw) / 2, 50), tag_text, font=tag_font, fill=(255, 255, 255, 220))

    # 顶部标签下面的小横线
    line_w = 36
    d.line([((W - line_w) / 2, 84), ((W + line_w) / 2, 84)],
           fill=(255, 255, 255, 180), width=2)

    # 中央: 条目中文名 (粗体, 字号自适应宽度)
    name_font = fit_font(d, name, CN_FONT_BOLD, max_width=W - 80)
    nb = d.textbbox((0, 0), name, font=name_font)
    nw, nh = nb[2] - nb[0], nb[3] - nb[1]
    nx = (W - nw) / 2 - nb[0]
    ny = (H - nh) / 2 + 4 - nb[1]
    # 投影 (深色, 偏移大一点)
    d.text((nx + 3, ny + 3), name, font=name_font,
           fill=(*darken(accent, 0.6), 120))
    # 主体白字
    d.text((nx, ny), name, font=name_font, fill=(255, 255, 255, 255))

    # 底部: 英文名
    if en_name:
        en_font = ImageFont.truetype(CN_FONT_REG, 18)
        eb = d.textbbox((0, 0), en_name, font=en_font)
        ew = eb[2] - eb[0]
        d.text(((W - ew) / 2, H - 64), en_name, font=en_font,
               fill=(255, 255, 255, 210))

    # 右下角 itemId 浅水印 (方便对图debug)
    id_font = ImageFont.truetype(CN_FONT_REG, 11)
    ib = d.textbbox((0, 0), item_id, font=id_font)
    iw = ib[2] - ib[0]
    d.text((W - pad - 10 - iw, H - pad - 20), item_id, font=id_font,
           fill=(255, 255, 255, 140))

    img.convert("RGB").save(out, "PNG", optimize=True)


# (item_id, 中文名, 顶部英文标签, accent_hex, 英文名)
ITEMS = [
    # ── 奥特曼 (FFFF7A7A) ─────────────────────
    ("ultra_original", "初代奥特曼",        "ULTRAMAN", "FFFF7A7A", "Ultraman"),
    ("ultra_seven",    "赛文奥特曼",        "ULTRAMAN", "FFFF7A7A", "Ultraseven"),
    ("ultra_taro",     "泰罗奥特曼",        "ULTRAMAN", "FFFF7A7A", "Ultraman Taro"),
    ("ultra_leo",      "雷欧奥特曼",        "ULTRAMAN", "FFFF7A7A", "Ultraman Leo"),
    ("ultra_tiga",     "迪迦奥特曼",        "ULTRAMAN", "FFFF7A7A", "Ultraman Tiga"),
    ("ultra_dyna",     "戴拿奥特曼",        "ULTRAMAN", "FFFF7A7A", "Ultraman Dyna"),
    ("ultra_gaia",     "盖亚奥特曼",        "ULTRAMAN", "FFFF7A7A", "Ultraman Gaia"),
    ("ultra_cosmos",   "高斯奥特曼",        "ULTRAMAN", "FFFF7A7A", "Ultraman Cosmos"),
    ("ultra_mebius",   "梦比优斯奥特曼",    "ULTRAMAN", "FFFF7A7A", "Ultraman Mebius"),
    ("ultra_zero",     "赛罗奥特曼",        "ULTRAMAN", "FFFF7A7A", "Ultraman Zero"),
    ("ultra_ginga",    "银河奥特曼",        "ULTRAMAN", "FFFF7A7A", "Ultraman Ginga"),
    ("ultra_orb",      "欧布奥特曼",        "ULTRAMAN", "FFFF7A7A", "Ultraman Orb"),

    # ── 药 (FFA8E4D9) ────────────────────────
    ("med_antibiotic", "抗生素",            "MEDICINE", "FFA8E4D9", "Antibiotic"),
    ("med_digest",     "助消化药",          "MEDICINE", "FFA8E4D9", "Digestive Aid"),
    ("med_diarrhea",   "止泻药 / 补液",     "MEDICINE", "FFA8E4D9", "Antidiarrheal & ORS"),
    ("med_antacid",    "胃药",              "MEDICINE", "FFA8E4D9", "Antacid / PPI"),
    ("med_vitamin",    "维生素 / 补剂",     "MEDICINE", "FFA8E4D9", "Vitamin Supplement"),
    ("med_wound",      "创口护理",          "MEDICINE", "FFA8E4D9", "Wound Care"),
    ("med_tcm",        "中成药",            "MEDICINE", "FFA8E4D9", "Chinese Patent Medicine"),
    ("med_eyedrops",   "眼药水",            "MEDICINE", "FFA8E4D9", "Eye Drops"),

    # ── 茶 (FFC9E5B0) ────────────────────────
    ("tea_green",      "绿茶",              "TEA",      "FFC9E5B0", "Green Tea"),
    ("tea_longjing",   "西湖龙井",          "TEA",      "FFC9E5B0", "Longjing"),
    ("tea_biluo",      "碧螺春",            "TEA",      "FFC9E5B0", "Biluochun"),
    ("tea_black",      "红茶",              "TEA",      "FFC9E5B0", "Black Tea"),
    ("tea_keemun",     "祁门红茶",          "TEA",      "FFC9E5B0", "Keemun"),
    ("tea_oolong",     "乌龙茶 / 青茶",     "TEA",      "FFC9E5B0", "Oolong"),
    ("tea_tieguanyin", "安溪铁观音",        "TEA",      "FFC9E5B0", "Tieguanyin"),
    ("tea_matcha",     "抹茶",              "TEA",      "FFC9E5B0", "Matcha"),

    # ── 酒 (FFE6B87C) ────────────────────────
    ("drink_maotai",      "贵州茅台",       "BAIJIU",   "FFE6B87C", "Moutai"),
    ("drink_wuliangye",   "五粮液",         "BAIJIU",   "FFE6B87C", "Wuliangye"),
    ("drink_guojiao",     "国窖 1573",      "BAIJIU",   "FFE6B87C", "Guojiao 1573"),
    ("drink_gujing",      "古井贡酒",       "BAIJIU",   "FFE6B87C", "Gujing Gongjiu"),
    ("drink_tsingtao",    "青岛啤酒",       "BEER",     "FFE6B87C", "Tsingtao"),
    ("drink_snow",        "雪花啤酒",       "BEER",     "FFE6B87C", "Snow Beer"),
    ("drink_harbin",      "哈尔滨啤酒",     "BEER",     "FFE6B87C", "Harbin Beer"),
    ("drink_budweiser",   "百威啤酒",       "BEER",     "FFE6B87C", "Budweiser"),
    ("drink_heineken",    "喜力啤酒",       "BEER",     "FFE6B87C", "Heineken"),
    ("drink_penfolds",    "奔富",           "WINE",     "FFE6B87C", "Penfolds"),
    ("drink_macallan",    "麦卡伦",         "WHISKY",   "FFE6B87C", "Macallan"),
    ("drink_yamazaki",    "山崎",           "WHISKY",   "FFE6B87C", "Yamazaki"),
    ("drink_jackdaniels", "杰克丹尼",       "WHISKY",   "FFE6B87C", "Jack Daniel's"),
    ("drink_hennessy",    "轩尼诗",         "COGNAC",   "FFE6B87C", "Hennessy"),
    ("drink_remy",        "人头马",         "COGNAC",   "FFE6B87C", "Rémy Martin"),
    ("drink_absolut",     "绝对伏特加",     "VODKA",    "FFE6B87C", "Absolut"),
    ("drink_bacardi",     "百加得",         "RUM",      "FFE6B87C", "Bacardi"),
    ("drink_dassai",      "獭祭",           "SAKE",     "FFE6B87C", "Dassai"),
]


def main() -> None:
    DRAWABLE.mkdir(parents=True, exist_ok=True)
    for item_id, name, tag, accent, en in ITEMS:
        out = DRAWABLE / f"{item_id}.png"
        render_card(item_id, name, tag, accent, en, out)
        print(f"  ✓ {out.relative_to(ROOT)}")
    print(f"\nGenerated {len(ITEMS)} placeholder drawables.")


if __name__ == "__main__":
    main()
