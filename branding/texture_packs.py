"""Builds Sythix's built-in texture packs from modern (1.21) resource packs.

Usage:
  python3 branding/texture_packs.py NAME ORIGINAL.zip CONVERTED.zip VANILLA_ASSETS OUT_DIR [--keep PATH]... [--exclude PATH]...

--keep copies a folder of the original pack unchanged (for OptiFine's own folders such as
assets/minecraft/optifine, which 1.12's OptiFine reads as they are). --exclude leaves files out
(for example parts whose authors do not allow redistribution).

ORIGINAL.zip is the 1.21 pack, CONVERTED.zip is the same pack after Sythix's Modern Pack
Converter (Texture Pack Manager > Modern Pack Converter, or ModernPackConverter.convert), and
VANILLA_ASSETS is the folder holding 1.12's assets/minecraft (build/resources/main).

The converter renames single textures. This script adds what it cannot do by renaming: 1.12 keeps
particles, the explosion animation, the hotbar and the HUD icons in combined sheets, while 1.14+
split them into separate files. It copies the pack's pieces into the right places of the 1.12
sheets, so particles (crits, potion effects, smoke, sparkles, explosions) and the hotbar/food/air/XP
bars get the pack's look too. Requires Pillow.
"""
import io
import json
import os
import shutil
import sys
import zipfile

from PIL import Image

PARTICLE_CELL = 16  # pixels per particle in the rebuilt particles.png (1.12 uses 8)
EXPLOSION_FRAME = 64  # pixels per frame in the rebuilt explosion.png (1.12 uses 32)


def particle_index(name):
    """Index of a 1.21 particle file in 1.12's particles.png (16 per row), or None."""
    base = name[:-4]
    for prefix, start in (("generic_", 0), ("effect_", 128), ("glitter_", 160)):
        if base.startswith(prefix) and base[len(prefix):].isdigit():
            n = int(base[len(prefix):])
            return start + n if n < 8 else None
    return {"critical_hit": 65, "enchanted_hit": 66}.get(base)


def open_png(data):
    return Image.open(io.BytesIO(data)).convert("RGBA")


def fit(img, w, h):
    return img if img.size == (w, h) else img.resize((w, h), Image.LANCZOS if img.width > w else Image.NEAREST)


def build_particles(files, vanilla):
    names = [n for n in files if n.startswith("assets/minecraft/textures/particle/") and n.endswith(".png") and particle_index(n.rsplit("/", 1)[1]) is not None]
    if not names:
        return None
    sheet = fit(Image.open(os.path.join(vanilla, "textures/particle/particles.png")).convert("RGBA"), 16 * PARTICLE_CELL, 16 * PARTICLE_CELL)
    for n in names:
        i = particle_index(n.rsplit("/", 1)[1])
        img = open_png(files[n])
        # animated strips (one frame under another): use the first frame
        if img.height > img.width:
            img = img.crop((0, 0, img.width, img.width))
        cell = fit(img, PARTICLE_CELL, PARTICLE_CELL)
        x, y = (i % 16) * PARTICLE_CELL, (i // 16) * PARTICLE_CELL
        sheet.paste((0, 0, 0, 0), (x, y, x + PARTICLE_CELL, y + PARTICLE_CELL))
        sheet.paste(cell, (x, y))
    return sheet, len(names)


def build_explosion(files):
    frames = {}
    for i in range(16):
        n = "assets/minecraft/textures/particle/explosion_%d.png" % i
        if n in files:
            frames[i] = open_png(files[n])
    if len(frames) < 16:
        return None
    sheet = Image.new("RGBA", (4 * EXPLOSION_FRAME, 4 * EXPLOSION_FRAME), (0, 0, 0, 0))
    for i, img in frames.items():
        sheet.paste(fit(img, EXPLOSION_FRAME, EXPLOSION_FRAME), ((i % 4) * EXPLOSION_FRAME, (i // 4) * EXPLOSION_FRAME))
    return sheet


# 1.21 HUD sprites -> (sheet, x, y, w, h) in 1.12's 256x256 widgets.png / icons.png
SPRITES = {
    "hud/hotbar": ("widgets", 0, 0, 182, 22),
    "hud/hotbar_selection": ("widgets", 0, 22, 24, 22),
    "hud/hotbar_offhand_left": ("widgets", 24, 22, 29, 24),
    "hud/hotbar_offhand_right": ("widgets", 53, 22, 29, 24),
    "widget/button_disabled": ("widgets", 0, 46, 200, 20),
    "widget/button": ("widgets", 0, 66, 200, 20),
    "widget/button_highlighted": ("widgets", 0, 86, 200, 20),
    "hud/crosshair": ("icons", 0, 0, 15, 15),
    "hud/air": ("icons", 16, 18, 9, 9),
    "hud/air_bursting": ("icons", 25, 18, 9, 9),
    "hud/food_empty": ("icons", 16, 27, 9, 9),
    "hud/food_full": ("icons", 52, 27, 9, 9),
    "hud/food_half": ("icons", 61, 27, 9, 9),
    "hud/food_full_hunger": ("icons", 88, 27, 9, 9),
    "hud/food_half_hunger": ("icons", 97, 27, 9, 9),
    "hud/food_empty_hunger": ("icons", 133, 27, 9, 9),
    "hud/experience_bar_background": ("icons", 0, 64, 182, 5),
    "hud/experience_bar_progress": ("icons", 0, 69, 182, 5),
    "hud/jump_bar_background": ("icons", 0, 84, 182, 5),
    "hud/jump_bar_progress": ("icons", 0, 89, 182, 5),
}


def build_sheets(files, vanilla):
    found = {}
    for key, spec in SPRITES.items():
        n = "assets/minecraft/textures/gui/sprites/%s.png" % key
        if n in files:
            img = open_png(files[n])
            # animated sprites: first frame
            frame_h = img.width * spec[4] // spec[3]
            if img.height > frame_h * 1.5:
                img = img.crop((0, 0, img.width, frame_h))
            found[key] = img
    sheets = {}
    for name in ("widgets", "icons"):
        keys = [k for k in found if SPRITES[k][0] == name]
        if not keys:
            continue
        # keep the pack's resolution: scale the 1.12 sheet up to match its biggest sprite
        scale = max(1, max(round(found[k].width / SPRITES[k][3]) for k in keys))
        sheet = Image.open(os.path.join(vanilla, "textures/gui/%s.png" % name)).convert("RGBA")
        sheet = sheet.resize((sheet.width * scale, sheet.height * scale), Image.NEAREST)
        for k in keys:
            _, x, y, w, h = SPRITES[k]
            box = (x * scale, y * scale, (x + w) * scale, (y + h) * scale)
            img = found[k]
            # 1.21's hotbar selection is 23 pixels high, 1.12's 22: keep the top part
            if img.height * w != img.width * h:
                img = img.crop((0, 0, img.width, img.width * h // w))
            sheet.paste((0, 0, 0, 0), box)
            sheet.paste(fit(img, w * scale, h * scale), box[:2])
        sheets[name] = (sheet, len(keys))
    return sheets


def main():
    name, original, converted, vanilla, out = sys.argv[1:6]
    keep, exclude, rest = [], [], sys.argv[6:]
    while rest:
        flag, value, rest = rest[0], rest[1], rest[2:]
        (keep if flag == "--keep" else exclude).append(value.rstrip("/") + "/")
    vanilla = os.path.join(vanilla, "assets/minecraft")
    with zipfile.ZipFile(original) as z:
        files = {n: z.read(n) for n in z.namelist() if not n.endswith("/") and not any(n.startswith(x) for x in exclude)}
    if os.path.isdir(out):
        shutil.rmtree(out)
    os.makedirs(out)
    with zipfile.ZipFile(converted) as z:
        z.extractall(out)
    for x in exclude:
        shutil.rmtree(os.path.join(out, x), ignore_errors=True)
    kept = 0
    for n, data in files.items():
        if any(n.startswith(k) for k in keep):
            dst = os.path.join(out, n)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            with open(dst, "wb") as f:
                f.write(data)
            kept += 1
    textures = os.path.join(out, "assets/minecraft/textures")
    notes = ["%d OptiFine files kept" % kept] if kept else []

    particles = build_particles(files, vanilla)
    if particles:
        os.makedirs(os.path.join(textures, "particle"), exist_ok=True)
        particles[0].save(os.path.join(textures, "particle/particles.png"))
        notes.append("%d particles" % particles[1])
    explosion = build_explosion(files)
    if explosion:
        os.makedirs(os.path.join(textures, "entity"), exist_ok=True)
        explosion.save(os.path.join(textures, "entity/explosion.png"))
        notes.append("explosion animation")
    for sheet, (img, count) in build_sheets(files, vanilla).items():
        os.makedirs(os.path.join(textures, "gui"), exist_ok=True)
        img.save(os.path.join(textures, "gui/%s.png" % sheet))
        notes.append("%d %s sprites" % (count, sheet))
    # title screen panorama (some packs keep it with the sprites)
    for i in range(6):
        for src in ("assets/minecraft/textures/gui/sprites/title/background/panorama_%d.png" % i, "assets/minecraft/textures/gui/title/background/panorama_%d.png" % i):
            if src in files:
                dst = os.path.join(textures, "gui/title/background/panorama_%d.png" % i)
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                with open(dst, "wb") as f:
                    f.write(files[src])
                break

    # keep the authors' credits with the pack
    for credit in ("Credits.txt", "pack.credits.txt", "credits.txt"):
        if credit in files:
            with open(os.path.join(out, credit), "wb") as f:
                f.write(files[credit])
    with open(os.path.join(out, "pack.mcmeta"), "w") as f:
        json.dump({"pack": {"pack_format": 3, "description": name + " - built into Sythix"}}, f)
    # list of files, so the client can answer "does this pack have X?" without searching the jar
    index = sorted(os.path.relpath(os.path.join(d, f), out).replace(os.sep, "/") for d, _, fs in os.walk(out) for f in fs if f != "index.txt")
    with open(os.path.join(out, "index.txt"), "w") as f:
        f.write("\n".join(index) + "\n")
    count = len(index)
    print("%s: %d files (%s)" % (name, count, ", ".join(notes) or "textures only"))


if __name__ == "__main__":
    main()
