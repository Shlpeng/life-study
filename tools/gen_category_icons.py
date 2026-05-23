#!/usr/bin/env python3
"""生成分类用的本地图标 PNG, 放到 app/src/main/res/drawable/.

目前仅奥特曼 (ic_ultraman.png). 设计为透明背景的圆形头像, 192x192 大小,
HomeScreen 用 Modifier.size(...) 控制实际显示尺寸.
"""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
DRAWABLE = ROOT / "app/src/main/res/drawable"


def render_ultraman(out: Path, size: int = 192) -> None:
    s = size
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img, "RGBA")

    # 头部主体: 略椭圆 (高 > 宽 一点点), 红色
    pad_x = int(s * 0.10)
    pad_y_top = int(s * 0.06)
    pad_y_bot = int(s * 0.10)
    head_box = (pad_x, pad_y_top, s - pad_x, s - pad_y_bot)
    d.ellipse(head_box, fill=(220, 60, 60, 255))

    # 银色头冠: 从头顶往两侧延伸的弧带, 中间 V 形凹下
    silver = (228, 232, 238, 255)
    cx = s / 2
    cw = int(s * 0.62)  # 头冠总宽
    ct = int(s * 0.05)  # 头冠顶部 y
    cm = int(s * 0.28)  # V 形最低点 y
    ce = int(s * 0.18)  # 两侧底 y
    d.polygon(
        [
            (cx - cw / 2, ct + int(s * 0.10)),  # 左下外
            (cx - cw / 2, ct + int(s * 0.04)),  # 左上外
            (cx, ct),                            # 顶中
            (cx + cw / 2, ct + int(s * 0.04)),  # 右上外
            (cx + cw / 2, ct + int(s * 0.10)),  # 右下外
            (cx + int(s * 0.18), ce),            # 右下内
            (cx, cm),                            # V 中
            (cx - int(s * 0.18), ce),            # 左下内
        ],
        fill=silver,
    )

    # 双眼: 黄色椭圆, 内倾的"凶相"造型
    eye_w = int(s * 0.22)
    eye_h = int(s * 0.11)
    eye_y = int(s * 0.42)
    eye_gap = int(s * 0.06)
    yellow = (255, 214, 64, 255)
    # 左眼 (外低内高)
    d.polygon(
        [
            (cx - eye_gap - eye_w, eye_y + eye_h),       # 外下
            (cx - eye_gap - eye_w + int(eye_w * 0.15), eye_y),  # 外上
            (cx - eye_gap, eye_y - int(eye_h * 0.2)),    # 内上
            (cx - eye_gap, eye_y + eye_h - 2),           # 内下
        ],
        fill=yellow,
    )
    # 右眼 (镜像)
    d.polygon(
        [
            (cx + eye_gap + eye_w, eye_y + eye_h),
            (cx + eye_gap + eye_w - int(eye_w * 0.15), eye_y),
            (cx + eye_gap, eye_y - int(eye_h * 0.2)),
            (cx + eye_gap, eye_y + eye_h - 2),
        ],
        fill=yellow,
    )

    # 额头色彩定时器 (黄色小圆)
    timer_r = int(s * 0.045)
    timer_y = int(s * 0.34)
    d.ellipse(
        (cx - timer_r, timer_y - timer_r, cx + timer_r, timer_y + timer_r),
        fill=(255, 195, 40, 255),
    )

    # 嘴: 一条横向窄长方形, 暗红色
    mouth_w = int(s * 0.16)
    mouth_h = int(s * 0.025)
    mouth_y = int(s * 0.68)
    d.rounded_rectangle(
        (cx - mouth_w / 2, mouth_y, cx + mouth_w / 2, mouth_y + mouth_h),
        radius=2,
        fill=(150, 30, 30, 255),
    )

    img.save(out, "PNG", optimize=True)


def main() -> None:
    out = DRAWABLE / "ic_ultraman.png"
    render_ultraman(out)
    print(f"  ✓ {out.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
