"""Sythix Client branding asset generator."""
import sys, os, math, random
from PIL import Image, ImageDraw, ImageFilter
# Usage (from the repository root):
#   python3 branding/make_assets.py branding/logo-source.webp src/main/resources/assets/minecraft/textures/gui/sythix
# Requires Pillow (pip install pillow). Regenerates logo.png, icon_16.png, icon_32.png and background.png.
src, outdir = sys.argv[1], sys.argv[2]
os.makedirs(outdir, exist_ok=True)
logo = Image.open(src).convert("RGBA")
a = logo.split()[3].point(lambda v: 255 if v > 8 else 0)
logo = logo.crop(a.getbbox())                       # 615x837

# 1) logo.png: 512x512 power-of-two canvas, content scaled to full height, centered
h = 512; w = round(logo.width * h / logo.height)
canvas = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
canvas.alpha_composite(logo.resize((w, h), Image.LANCZOS), ((512 - w) // 2, 0))
canvas.save(os.path.join(outdir, "logo.png"), optimize=True)
print("logo content width in 512 tex:", w)

# 2) emblem (hexagon + scythe only) for window icons
cut = int(logo.height * 0.775)                      # above the SYTHIX text
em = logo.crop((0, 0, logo.width, cut))
em = em.crop(em.split()[3].point(lambda v: 255 if v > 8 else 0).getbbox())
s = max(em.size); sq = Image.new("RGBA", (s, s), (0, 0, 0, 0)); sq.alpha_composite(em, ((s - em.width) // 2, (s - em.height) // 2))
for n in (16, 32):
    sq.resize((n, n), Image.LANCZOS).save(os.path.join(outdir, "icon_%d.png" % n))

# 3) background.png 1024x512: charcoal->black, red moon glow, blue haze, voxel skyline, embers
W, H = 1024, 512
random.seed(1337)
bg = Image.new("RGB", (W, H))
px = bg.load()
for y in range(H):
    t = y / (H - 1)
    for x in range(W):
        c = [22 + (11 - 22) * t, 22 + (11 - 22) * t, 27 + (14 - 27) * t]
        # red glow (moon) lower centre
        d = math.hypot((x - W * 0.5) / (W * 0.42), (y - H * 0.78) / (H * 0.55))
        g = max(0.0, 1 - d) ** 2.2
        c[0] += 150 * g; c[1] += 10 * g; c[2] += 18 * g
        # blue haze upper left / right
        for cx, cy, k in ((0.08, 0.05, 1.0), (0.95, 0.12, 0.7)):
            d2 = math.hypot((x - W * cx) / (W * 0.45), (y - H * cy) / (H * 0.6))
            b = max(0.0, 1 - d2) ** 2.5 * k
            c[0] += 8 * b; c[1] += 40 * b; c[2] += 110 * b
        # vignette
        v = math.hypot((x / W - 0.5) * 1.3, (y / H - 0.5) * 1.1)
        f = max(0.0, 1 - v * v * 0.9)
        px[x, y] = tuple(int(max(0, min(255, ch * f))) for ch in c)
dr = ImageDraw.Draw(bg)
# voxel skyline silhouette with red rim light (16px blocks)
B = 16
heights = []
hgt = 3
for i in range(W // B + 1):
    hgt = max(1, min(6, hgt + random.choice((-1, -1, 0, 0, 1, 1))))
    heights.append(hgt)
for i, hb in enumerate(heights):
    x0 = i * B; top = H - hb * B
    dr.rectangle((x0, top, x0 + B - 1, H), fill=(9, 7, 9))
    rim = (150, 16, 22) if abs(i * B - W / 2) < W * 0.38 else (70, 12, 16)
    dr.rectangle((x0, top, x0 + B - 1, top + 1), fill=rim)
    for yy in range(top + B, H, B):               # faint block seams
        dr.line((x0, yy, x0 + B - 1, yy), fill=(16, 12, 15))
    dr.line((x0, top, x0, H), fill=(16, 12, 15))
# embers (red) and sparks (light blue): small pixel squares
for _ in range(140):
    x = random.randrange(W); y = random.randrange(H - heights[min(len(heights)-1, x // B)] * B - 6)
    sz = random.choice((2, 2, 3, 4))
    col = random.choice(((216, 24, 32), (160, 8, 16), (216, 24, 32), (108, 198, 255), (47, 107, 255)))
    k = random.uniform(0.25, 0.8)
    base = px[x, y]
    col = tuple(int(base[j] * (1 - k) + col[j] * k) for j in range(3))
    dr.rectangle((x, y, x + sz - 1, y + sz - 1), fill=col)
bg.save(os.path.join(outdir, "background.png"), optimize=True)
print("done")
