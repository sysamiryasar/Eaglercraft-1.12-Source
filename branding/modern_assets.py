#!/usr/bin/env python3
"""
Generates the assets for the extra 1.21 blocks and items in Sythix (net.sythix.modern):
blockstates, block/item models, placeholder textures, English names and crafting recipes.

The textures are original placeholders made by recolouring and combining the 1.12 textures in
this repository (no 1.21 artwork is included). Players can replace them with the real 1.21
textures from a pack or Minecraft jar they own using Texture Pack Manager > Modern Pack Converter.

Run from the repository root:  python3 branding/modern_assets.py
"""
import json
import os
import random
import colorsys

from PIL import Image, ImageDraw

ROOT = "src/main/resources/assets/minecraft"
TEX = ROOT + "/textures"
rng = random.Random(1211)


# ---- image helpers --------------------------------------------------------------------------

def load(path):
    return Image.open(f"{TEX}/{path}.png").convert("RGBA")


def frame(img):
    """First 16x16 frame of an (animated) texture."""
    return img.crop((0, 0, img.width, img.width)) if img.height > img.width else img


def save(img, path):
    full = f"{TEX}/{path}.png"
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def lum(p):
    return (0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]) / 255.0


def colorize(img, rgb, contrast=1.0, bright=1.0):
    """Keeps the shading of img but uses the colour rgb."""
    out = Image.new("RGBA", img.size)
    px, po = img.load(), out.load()
    for y in range(img.height):
        for x in range(img.width):
            p = px[x, y]
            l = max(0.0, min(1.5, (lum(p) - 0.5) * contrast + 0.5)) * bright
            po[x, y] = (min(255, int(rgb[0] * l * 1.6)), min(255, int(rgb[1] * l * 1.6)), min(255, int(rgb[2] * l * 1.6)), p[3])
    return out


def hue(img, h_shift=0.0, s_mul=1.0, v_mul=1.0, s_add=0.0):
    out = Image.new("RGBA", img.size)
    px, po = img.load(), out.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            h = (h + h_shift) % 1.0
            s = max(0, min(1, s * s_mul + s_add))
            v = max(0, min(1, v * v_mul))
            r2, g2, b2 = colorsys.hsv_to_rgb(h, s, v)
            po[x, y] = (int(r2 * 255), int(g2 * 255), int(b2 * 255), a)
    return out


def noise(size, base, spread, seed=0, alpha=255):
    r = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    p = img.load()
    for y in range(size):
        for x in range(size):
            d = r.randint(-spread, spread)
            p[x, y] = tuple(max(0, min(255, c + d)) for c in base) + (alpha,)
    return img


def spots(ore, stone):
    """Pixels of ore that differ from stone (the ore specks)."""
    o, s = frame(load(ore)), frame(load(stone))
    mask = Image.new("RGBA", o.size, (0, 0, 0, 0))
    po, ps, pm = o.load(), s.load(), mask.load()
    for y in range(o.height):
        for x in range(o.width):
            if sum(abs(po[x, y][i] - ps[x, y][i]) for i in range(3)) > 40:
                pm[x, y] = po[x, y]
    return mask


def paste(base, top):
    out = base.copy()
    out.alpha_composite(top)
    return out


def border(img, rgb, width=1):
    out = img.copy()
    d = ImageDraw.Draw(out)
    for i in range(width):
        d.rectangle([i, i, out.width - 1 - i, out.height - 1 - i], outline=rgb + (255,))
    return out


def bands(img, rgb, rows):
    out = img.copy()
    d = ImageDraw.Draw(out)
    for r in rows:
        d.line([(0, r), (out.width - 1, r)], fill=rgb + (255,))
    return out


def rings(base_rgb, ring_rgb, seed):
    r = random.Random(seed)
    img = noise(16, base_rgb, 10, seed)
    d = ImageDraw.Draw(img)
    for k in (6, 4, 2):
        d.ellipse([8 - k, 8 - k, 7 + k, 7 + k], outline=tuple(max(0, c - 25 * (k % 3)) for c in ring_rgb) + (255,))
    return img


def flower(color, center, stem=(60, 140, 40), seed=0, petals=5):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.line([(8, 15), (8, 7)], fill=stem + (255,))
    d.line([(8, 12), (5, 10)], fill=stem + (255,))
    d.line([(8, 11), (11, 9)], fill=stem + (255,))
    r = random.Random(seed)
    for i in range(petals):
        x, y = 8 + r.randint(-2, 2), 5 + r.randint(-2, 1)
        d.rectangle([x - 1, y - 1, x, y], fill=color + (255,))
    d.point([(8, 5)], fill=center + (255,))
    return img


def item_shape(kind, rgb, seed=0):
    """Small original item icons."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    dark = tuple(int(c * 0.55) for c in rgb)
    light = tuple(min(255, int(c * 1.35) + 20) for c in rgb)
    if kind == "raw":
        r = random.Random(seed)
        d.polygon([(3, 9), (6, 4), (11, 3), (13, 7), (12, 12), (6, 13)], fill=rgb + (255,), outline=dark + (255,))
        for _ in range(7):
            x, y = r.randint(5, 11), r.randint(5, 11)
            d.point([(x, y)], fill=(light if r.random() < 0.5 else dark) + (255,))
    elif kind == "ingot":
        d.polygon([(2, 10), (8, 6), (14, 8), (8, 12)], fill=rgb + (255,), outline=dark + (255,))
        d.line([(4, 9), (8, 7), (12, 8)], fill=light + (255,))
    elif kind == "shard":
        d.polygon([(5, 13), (4, 7), (8, 2), (11, 6), (10, 12)], fill=rgb + (255,), outline=dark + (255,))
        d.line([(6, 11), (8, 4)], fill=light + (255,))
    elif kind == "orb":
        d.ellipse([3, 3, 12, 12], fill=rgb + (255,), outline=dark + (255,))
        d.ellipse([5, 5, 7, 7], fill=light + (255,))
    elif kind == "shell":
        d.ellipse([2, 4, 13, 13], fill=rgb + (255,), outline=dark + (255,))
        d.arc([5, 7, 10, 12], 0, 300, fill=dark + (255,))
    elif kind == "membrane":
        d.polygon([(2, 12), (6, 3), (14, 5), (11, 13)], fill=rgb + (200,), outline=dark + (255,))
    elif kind == "sherd":
        d.polygon([(3, 3), (13, 2), (12, 13), (2, 12)], fill=rgb + (255,), outline=dark + (255,))
        r = random.Random(seed)
        pts = [(r.randint(5, 10), r.randint(5, 10)) for _ in range(3)]
        d.line(pts, fill=dark + (255,))
    elif kind == "disc":
        d.pieslice([2, 2, 13, 13], 200, 300, fill=rgb + (255,), outline=dark + (255,))
    return img


# ---- textures for the blocks ----------------------------------------------------------------

def textures():
    stone = frame(load("blocks/stone"))
    deep = colorize(stone, (78, 78, 88), contrast=1.6, bright=0.8)
    deep = bands(deep, (52, 52, 60), [3, 9, 13])
    save(deep, "blocks/deepslate")
    save(colorize(stone, (84, 84, 92), contrast=1.2, bright=0.85), "blocks/deepslate_top")
    save(colorize(load("blocks/cobblestone"), (80, 80, 90), contrast=1.5, bright=0.8), "blocks/cobbled_deepslate")
    save(colorize(load("blocks/stone_andesite"), (110, 112, 100), contrast=1.4), "blocks/tuff")
    save(colorize(load("blocks/stone_diorite_smooth"), (220, 220, 214), contrast=0.6, bright=1.05), "blocks/calcite")
    save(colorize(load("blocks/sandstone_top"), (140, 106, 86), contrast=1.6), "blocks/dripstone_block")
    save(colorize(load("blocks/stone_slab_top"), (64, 64, 70), contrast=0.8), "blocks/smooth_basalt")
    save(border(colorize(stone, (60, 64, 66), contrast=2.0, bright=0.7), (36, 160, 140)), "blocks/reinforced_deepslate_side")
    save(border(colorize(stone, (50, 54, 56), contrast=1.6, bright=0.7), (36, 160, 140), 2), "blocks/reinforced_deepslate_top")
    dirt = load("blocks/dirt")
    rooted = dirt.copy()
    dd = ImageDraw.Draw(rooted)
    for pts in ([(2, 1), (4, 6), (3, 11)], [(10, 2), (11, 8), (13, 14)], [(7, 9), (6, 15)]):
        dd.line(pts, fill=(170, 120, 80, 255))
    save(rooted, "blocks/rooted_dirt")
    save(colorize(dirt, (60, 52, 50), contrast=0.8), "blocks/mud")
    for name, base in (("suspicious_sand", "sand"), ("suspicious_gravel", "gravel")):
        img = load("blocks/" + base)
        d = ImageDraw.Draw(img)
        for x, y in ((4, 5), (11, 9), (7, 12)):
            d.rectangle([x, y, x + 1, y], fill=(120, 90, 60, 255))
        save(img, "blocks/" + name)
    save(colorize(load("blocks/stonebrick"), (140, 104, 76), contrast=1.2), "blocks/mud_bricks")
    save(load("blocks/quartz_block_bottom"), "blocks/smooth_quartz")

    logs = {"mangrove": (110, 40, 34), "cherry": (60, 36, 46), "pale_oak": (90, 84, 80)}
    planks = {"mangrove": (120, 52, 48), "cherry": (226, 178, 172), "pale_oak": (226, 218, 212)}
    for w, rgb in logs.items():
        save(colorize(load("blocks/log_oak"), rgb, contrast=1.3), f"blocks/{w}_log")
        save(colorize(load("blocks/log_oak_top"), planks[w], contrast=1.1), f"blocks/{w}_log_top")
        save(colorize(load("blocks/planks_oak"), planks[w], contrast=1.0), f"blocks/{w}_planks")
    save(colorize(load("blocks/planks_oak"), (176, 140, 84)), "blocks/stripped_oak_log")
    save(load("blocks/log_oak_top"), "blocks/stripped_oak_log_top")
    bamboo = noise(16, (168, 176, 62), 8, 3)
    bamboo = bands(bamboo, (120, 130, 40), [0, 5, 10, 15])
    save(bamboo, "blocks/bamboo_block")
    save(border(noise(16, (190, 186, 90), 8, 4), (130, 140, 40)), "blocks/bamboo_block_top")
    leaves = load("blocks/leaves_oak")
    save(colorize(leaves, (60, 120, 40), contrast=1.2), "blocks/mangrove_leaves")
    save(colorize(leaves, (240, 170, 200), contrast=1.0), "blocks/cherry_leaves")
    save(colorize(leaves, (120, 128, 118), contrast=1.0), "blocks/pale_oak_leaves")
    az = colorize(leaves, (90, 130, 50), contrast=1.2)
    save(az, "blocks/azalea_leaves")
    fl = az.copy()
    d = ImageDraw.Draw(fl)
    for x, y in ((3, 3), (11, 4), (6, 10), (13, 12), (2, 13)):
        d.rectangle([x, y, x + 1, y + 1], fill=(220, 120, 200, 255))
    save(fl, "blocks/flowering_azalea_leaves")
    save(colorize(load("blocks/grass_top"), (90, 130, 40), contrast=1.4), "blocks/moss_block")

    save(flower((70, 110, 230), (240, 220, 80), seed=1), "blocks/cornflower")
    save(flower((245, 245, 245), (230, 230, 200), seed=2, petals=4), "blocks/lily_of_the_valley")
    save(flower((240, 130, 40), (250, 220, 60), seed=3, petals=6), "blocks/torchflower")
    save(flower((70, 160, 190), (120, 90, 200), seed=4, petals=7), "blocks/pitcher_plant")
    petals = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(petals)
    r = random.Random(9)
    for _ in range(14):
        x, y = r.randint(1, 13), r.randint(1, 13)
        d.rectangle([x, y, x + 1, y + 1], fill=(240, 160, 200, 255))
    save(petals, "blocks/pink_petals")

    save(colorize(load("blocks/ice_packed"), (110, 160, 250), contrast=1.2), "blocks/blue_ice")
    save(colorize(load("blocks/snow"), (250, 252, 255), contrast=0.5, bright=1.05), "blocks/powder_snow")

    ores = {"coal": "coal_ore", "iron": "iron_ore", "gold": "gold_ore", "redstone": "redstone_ore",
            "lapis": "lapis_ore", "diamond": "diamond_ore", "emerald": "emerald_ore"}
    for k, v in ores.items():
        save(paste(deep, spots("blocks/" + v, "blocks/stone")), f"blocks/deepslate_{k}_ore")
    cop = colorize(spots("blocks/iron_ore", "blocks/stone"), (200, 110, 70), contrast=1.2)
    save(paste(stone, cop), "blocks/copper_ore")
    save(paste(deep, cop), "blocks/deepslate_copper_ore")
    gold = spots("blocks/gold_ore", "blocks/stone")
    save(paste(frame(load("blocks/netherrack")), gold), "blocks/nether_gold_ore")
    save(rings((96, 66, 58), (120, 86, 74), 5), "blocks/ancient_debris_top")
    save(bands(noise(16, (98, 68, 60), 12, 6), (70, 46, 40), [2, 6, 11]), "blocks/ancient_debris_side")

    furnace_side = load("blocks/furnace_side")
    for name, rgb in (("blast_furnace", (150, 150, 160)), ("smoker", (120, 100, 80))):
        side = colorize(furnace_side, rgb, contrast=1.2)
        save(side, f"blocks/{name}_side")
        save(colorize(load("blocks/furnace_top"), rgb, contrast=1.1), f"blocks/{name}_top")
        save(colorize(load("blocks/furnace_front_off"), rgb, contrast=1.2), f"blocks/{name}_front")
        save(colorize(load("blocks/furnace_front_on"), rgb, contrast=1.0) if False else paste(colorize(load("blocks/furnace_front_off"), rgb, contrast=1.2), spots("blocks/furnace_front_on", "blocks/furnace_front_off")), f"blocks/{name}_front_on")
    save(colorize(load("blocks/stone_slab_top"), (140, 140, 140)), "blocks/stonecutter_top")
    save(bands(colorize(load("blocks/stone_slab_side"), (120, 120, 120)), (80, 80, 80), [8]), "blocks/stonecutter_side")
    saw = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ImageDraw.Draw(saw).polygon([(1, 15), (4, 9), (8, 7), (12, 9), (15, 15)], fill=(190, 190, 200, 255), outline=(110, 110, 120, 255))
    save(saw, "blocks/stonecutter_saw")
    planks_dark = colorize(load("blocks/planks_big_oak"), (70, 50, 40))
    save(border(planks_dark, (40, 40, 48), 2), "blocks/smithing_table_top")
    save(bands(planks_dark, (50, 50, 58), [3, 12]), "blocks/smithing_table_side")
    save(colorize(load("blocks/stone"), (130, 130, 130), contrast=1.4), "blocks/grindstone_side")
    save(colorize(load("blocks/planks_big_oak"), (90, 60, 40)), "blocks/grindstone_pivot")
    comp = colorize(load("blocks/planks_oak"), (140, 100, 60))
    save(bands(comp, (90, 64, 40), [0, 7, 15]), "blocks/composter_side")
    save(border(Image.new("RGBA", (16, 16), (0, 0, 0, 0)), (110, 80, 50), 2), "blocks/composter_top")
    save(comp, "blocks/composter_bottom")
    save(noise(16, (90, 70, 40), 14, 7), "blocks/composter_compost")
    save(paste(noise(16, (90, 70, 40), 14, 7), noise(16, (230, 230, 220), 20, 8, alpha=90)), "blocks/composter_ready")
    loom = colorize(load("blocks/planks_birch"), (200, 170, 120))
    save(loom, "blocks/loom_side")
    save(border(loom, (150, 110, 70)), "blocks/loom_top")
    lf = loom.copy()
    ImageDraw.Draw(lf).rectangle([4, 3, 11, 12], fill=(200, 80, 80, 255), outline=(240, 230, 220, 255))
    save(lf, "blocks/loom_front")
    ct = colorize(load("blocks/planks_big_oak"), (80, 60, 44))
    save(ct, "blocks/cartography_table_side")
    ctt = ct.copy()
    ImageDraw.Draw(ctt).rectangle([2, 2, 13, 13], fill=(220, 210, 170, 255), outline=(120, 90, 60, 255))
    save(ctt, "blocks/cartography_table_top")
    ft = colorize(load("blocks/planks_birch"), (200, 190, 140))
    save(ft, "blocks/fletching_table_side")
    ftt = ft.copy()
    ImageDraw.Draw(ftt).line([(3, 12), (12, 3)], fill=(120, 80, 50, 255), width=2)
    save(ftt, "blocks/fletching_table_top")
    barrel = bands(colorize(load("blocks/planks_spruce"), (110, 80, 50)), (60, 60, 64), [2, 13])
    save(barrel, "blocks/barrel_side")
    bt = border(colorize(load("blocks/planks_spruce"), (120, 90, 56)), (70, 70, 74), 2)
    save(bt, "blocks/barrel_top")
    save(bt, "blocks/barrel_bottom")
    target = Image.new("RGBA", (16, 16), (232, 220, 200, 255))
    d = ImageDraw.Draw(target)
    for k, col in ((7, (200, 40, 40)), (5, (232, 220, 200)), (3, (200, 40, 40)), (1, (232, 220, 200))):
        d.ellipse([8 - k - 1, 8 - k - 1, 7 + k + 1, 7 + k + 1], fill=col + (255,))
    save(target, "blocks/target_side")
    save(colorize(load("blocks/hay_block_top"), (220, 210, 170)), "blocks/target_top")
    save(colorize(load("blocks/planks_oak"), (200, 160, 100)), "blocks/scaffolding_side")
    sc = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ImageDraw.Draw(sc).rectangle([0, 0, 15, 15], outline=(200, 170, 90, 255), width=2)
    save(sc, "blocks/scaffolding_top")
    chain = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(chain)
    for y in (0, 6, 12):
        d.rectangle([6, y, 9, y + 4], outline=(70, 74, 90, 255))
    save(chain, "blocks/chain")
    save(colorize(load("blocks/glass"), (60, 40, 70), contrast=1.3), "blocks/tinted_glass")
    tinted = load("blocks/tinted_glass")
    px = tinted.load()
    for y in range(16):
        for x in range(16):
            r_, g_, b_, a_ = px[x, y]
            px[x, y] = (r_, g_, b_, max(a_, 120))
    save(tinted, "blocks/tinted_glass")

    save(hue(load("blocks/torch_on"), 0.48, 1.4), "blocks/soul_torch")
    for name, rgb in (("lantern", (250, 200, 90)), ("soul_lantern", (90, 220, 230))):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, 5, 6], fill=rgb + (255,), outline=(60, 60, 70, 255))
        d.rectangle([0, 9, 5, 10], fill=(70, 70, 80, 255))
        d.rectangle([6, 0, 11, 6], fill=(60, 60, 70, 255))
        save(img, "blocks/" + name)
    log = load("blocks/log_oak")
    save(log, "blocks/campfire_log")
    fire = frame(load("blocks/fire_layer_0"))
    save(fire, "blocks/campfire_fire")
    save(hue(fire, 0.48, 1.2), "blocks/soul_campfire_fire")
    save(noise(16, (240, 150, 60), 30, 11), "blocks/shroomlight")
    for name, rgb in (("ochre", (240, 210, 140)), ("verdant", (200, 230, 160)), ("pearlescent", (240, 210, 230))):
        side = bands(noise(16, rgb, 10, len(name)), tuple(int(c * 0.8) for c in rgb), [4, 11])
        save(side, f"blocks/{name}_froglight")
        save(border(noise(16, rgb, 6, len(name) + 1), tuple(int(c * 0.8) for c in rgb), 2), f"blocks/{name}_froglight_top")

    cage = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(cage)
    d.rectangle([0, 0, 15, 15], outline=(60, 62, 70, 255), width=2)
    for x in (5, 10):
        d.line([(x, 0), (x, 15)], fill=(60, 62, 70, 255))
    save(cage, "blocks/trial_spawner_side")
    save(border(noise(16, (70, 74, 80), 8, 12), (230, 140, 60), 1), "blocks/trial_spawner_top")
    save(hue(cage, 0.0), "blocks/ominous_trial_spawner_side")
    save(border(noise(16, (70, 74, 80), 8, 13), (60, 120, 230), 1), "blocks/ominous_trial_spawner_top")
    vault = cage.copy()
    ImageDraw.Draw(vault).rectangle([5, 5, 10, 10], fill=(230, 180, 60, 255))
    save(vault, "blocks/vault_side")
    save(border(noise(16, (70, 74, 80), 8, 14), (230, 180, 60), 1), "blocks/vault_top")
    sculk = noise(16, (16, 40, 50), 8, 15)
    d = ImageDraw.Draw(sculk)
    for x, y in ((3, 4), (10, 6), (6, 12), (13, 13)):
        d.point([(x, y)], fill=(40, 200, 210, 255))
    save(sculk, "blocks/sculk")
    save(border(sculk, (20, 60, 70)), "blocks/sculk_sensor_top")
    save(bands(sculk, (40, 120, 130), [0]), "blocks/sculk_sensor_side")
    ten = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(ten)
    d.line([(4, 15), (3, 6)], fill=(40, 200, 210, 255), width=2)
    d.line([(11, 15), (12, 6)], fill=(40, 200, 210, 255), width=2)
    save(ten, "blocks/sculk_sensor_tendril")
    save(border(noise(16, (210, 210, 190), 10, 16), (16, 40, 50), 3), "blocks/sculk_shrieker_top")
    save(bands(sculk, (200, 200, 180), [0, 1]), "blocks/sculk_shrieker_side")
    obs = load("blocks/obsidian")
    ra = obs.copy()
    ImageDraw.Draw(ra).rectangle([4, 4, 11, 11], fill=(120, 40, 160, 255))
    save(ra, "blocks/respawn_anchor_top")
    save(bands(obs, (120, 40, 160), [7, 8]), "blocks/respawn_anchor_side")
    save(obs, "blocks/respawn_anchor_bottom")
    save(border(colorize(load("blocks/stonebrick_carved"), (120, 120, 130)), (70, 70, 80)), "blocks/lodestone_top")
    save(colorize(load("blocks/stonebrick_carved"), (120, 120, 130)), "blocks/lodestone_side")
    save(noise(16, (190, 160, 110), 18, 17), "blocks/conduit")

    hive = colorize(load("blocks/planks_birch"), (220, 180, 90))
    nest = colorize(load("blocks/hay_block_side"), (210, 170, 80))
    for name, img in (("bee_nest", nest), ("beehive", hive)):
        save(img, f"blocks/{name}_side")
        save(border(img, (150, 110, 50)), f"blocks/{name}_top")
        front = img.copy()
        ImageDraw.Draw(front).rectangle([6, 9, 9, 12], fill=(40, 30, 20, 255))
        save(front, f"blocks/{name}_front")
    honey = noise(16, (240, 170, 40), 10, 18, alpha=200)
    save(honey, "blocks/honey_block_side")
    save(border(honey, (220, 140, 30)), "blocks/honey_block_top")
    stalk = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(stalk)
    d.rectangle([6, 0, 8, 15], fill=(120, 170, 50, 255))
    d.line([(6, 7), (8, 7)], fill=(90, 130, 30, 255))
    d.line([(9, 4), (13, 1)], fill=(110, 160, 40, 255))
    save(stalk, "blocks/bamboo_stalk")
    kelp = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(kelp)
    d.line([(7, 15), (6, 8), (8, 0)], fill=(60, 120, 40, 255), width=2)
    d.line([(7, 10), (11, 6)], fill=(80, 140, 50, 255))
    save(kelp, "blocks/kelp")
    save(noise(16, (110, 150, 50), 15, 19), "blocks/sea_pickle")
    save(colorize(load("blocks/sponge"), (50, 90, 220)), "blocks/tube_coral_block")
    save(colorize(load("blocks/sponge"), (220, 110, 170)), "blocks/brain_coral_block")
    save(colorize(load("blocks/sponge"), (130, 124, 120)), "blocks/dead_tube_coral_block")

    am = colorize(frame(load("blocks/prismarine_rough")), (150, 100, 210), contrast=1.4)
    save(am, "blocks/amethyst_block")
    bud = am.copy()
    d = ImageDraw.Draw(bud)
    for x, y in ((3, 3), (11, 4), (5, 11), (12, 12)):
        d.rectangle([x, y, x + 1, y + 1], fill=(240, 200, 255, 255))
    save(bud, "blocks/budding_amethyst")
    for name, h in (("small_amethyst_bud", 4), ("medium_amethyst_bud", 6), ("large_amethyst_bud", 9), ("amethyst_cluster", 13)):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.polygon([(4, 15), (6, 15 - h), (8, 15 - h - 1), (10, 15 - h), (12, 15)], fill=(170, 120, 230, 255), outline=(110, 60, 170, 255))
        d.line([(8, 15), (8, 16 - h)], fill=(230, 200, 255, 255))
        save(img, "blocks/" + name)

    copper_base = colorize(load("blocks/iron_block"), (200, 110, 70), contrast=1.3)
    save(copper_base, "blocks/copper_block")
    save(hue(copper_base, 0.03, 0.8), "blocks/exposed_copper")
    save(hue(copper_base, 0.28, 0.6, 0.95), "blocks/weathered_copper")
    save(hue(copper_base, 0.42, 0.7, 0.95), "blocks/oxidized_copper")

    sap = load("blocks/sapling_oak")
    save(colorize(sap, (80, 130, 40), contrast=1.2), "blocks/mangrove_propagule")
    save(colorize(sap, (230, 160, 190)), "blocks/cherry_sapling")
    save(colorize(sap, (140, 150, 136)), "blocks/pale_oak_sapling")

    # ---- items ----
    for name, kind, rgb in (("raw_iron", "raw", (200, 170, 150)), ("raw_gold", "raw", (240, 200, 60)), ("raw_copper", "raw", (200, 110, 70)),
                            ("copper_ingot", "ingot", (210, 120, 80)), ("netherite_ingot", "ingot", (70, 64, 66)), ("netherite_scrap", "raw", (100, 74, 66)),
                            ("amethyst_shard", "shard", (170, 120, 230)), ("echo_shard", "shard", (20, 90, 100)), ("disc_fragment_5", "disc", (60, 70, 80)),
                            ("phantom_membrane", "membrane", (200, 200, 180)), ("nautilus_shell", "shell", (230, 210, 180)), ("heart_of_the_sea", "orb", (40, 120, 230)),
                            ("pottery_shard_archer", "sherd", (180, 90, 60)), ("pottery_shard_prize", "sherd", (180, 90, 60)),
                            ("pottery_shard_arms_up", "sherd", (180, 90, 60)), ("pottery_shard_skull", "sherd", (180, 90, 60))):
        save(item_shape(kind, rgb, seed=len(name)), "items/" + name)


# ---- blockstates and models ----------------------------------------------------------------

def write(path, data):
    full = f"{ROOT}/{path}.json"
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w") as f:
        json.dump(data, f, indent=2)


def model(name, parent, textures, **extra):
    data = {"parent": parent, "textures": textures}
    data.update(extra)
    write("models/block/" + name, data)


def state(name, variants):
    write("blockstates/" + name, {"variants": variants})


def item_block(name, parent=None):
    write("models/item/" + name, {"parent": parent or "block/" + name})


def item_flat(name, texture, handheld=False):
    write("models/item/" + name, {"parent": "item/handheld" if handheld else "item/generated", "textures": {"layer0": texture}})


def cube_all(name, tex=None):
    model(name, "block/cube_all", {"all": "blocks/" + (tex or name)})
    state(name, {"normal": {"model": name}})
    item_block(name)


def column(name, side=None, end=None):
    model(name, "block/cube_column", {"side": "blocks/" + (side or name), "end": "blocks/" + (end or name + "_top")})
    state(name, {"axis=y": {"model": name}, "axis=z": {"model": name, "x": 90}, "axis=x": {"model": name, "x": 90, "y": 90}})
    item_block(name)


def cross(name, tex=None):
    model(name, "block/cross", {"cross": "blocks/" + (tex or name)})
    state(name, {"normal": {"model": name}})
    item_flat(name, "blocks/" + (tex or name))


def horizontal(name, model_name, lit_model=None):
    v = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        if lit_model:
            v[f"facing={facing},lit=false"] = {"model": model_name, "y": y} if y else {"model": model_name}
            v[f"facing={facing},lit=true"] = {"model": lit_model, "y": y} if y else {"model": lit_model}
        else:
            v[f"facing={facing}"] = {"model": model_name, "y": y} if y else {"model": model_name}
    state(name, v)


SIX = (("up", 0, 0), ("down", 180, 0), ("north", 90, 0), ("south", 90, 180), ("west", 90, 270), ("east", 90, 90))


def six(name, model_name):
    state(name, {f"facing={f}": dict({"model": model_name}, **({"x": x} if x else {}), **({"y": y} if y else {})) for f, x, y in SIX})


def box(frm, to, faces, tex):
    return {"from": frm, "to": to, "faces": {f: {"texture": tex} for f in faces}}


ALL = ("down", "up", "north", "south", "west", "east")


def models():
    for n in ("cobbled_deepslate", "tuff", "calcite", "dripstone_block", "smooth_basalt", "rooted_dirt", "mud", "suspicious_sand", "suspicious_gravel",
              "mud_bricks", "smooth_quartz", "mangrove_planks", "cherry_planks", "pale_oak_planks", "moss_block", "blue_ice", "powder_snow",
              "deepslate_coal_ore", "deepslate_iron_ore", "copper_ore", "deepslate_copper_ore", "deepslate_gold_ore", "deepslate_redstone_ore",
              "deepslate_lapis_ore", "deepslate_diamond_ore", "deepslate_emerald_ore", "nether_gold_ore", "shroomlight", "sculk", "conduit",
              "tube_coral_block", "brain_coral_block", "dead_tube_coral_block", "amethyst_block", "budding_amethyst", "copper_block",
              "exposed_copper", "weathered_copper", "oxidized_copper"):
        cube_all(n)
    cube_all("waxed_copper_block", "copper_block")
    cube_all("tinted_glass")
    column("deepslate")
    column("reinforced_deepslate", "reinforced_deepslate_side", "reinforced_deepslate_top")
    state("reinforced_deepslate", {"normal": {"model": "reinforced_deepslate"}})
    for n in ("mangrove_log", "cherry_log", "pale_oak_log", "stripped_oak_log", "bamboo_block"):
        column(n)
    for n in ("ochre_froglight", "verdant_froglight", "pearlescent_froglight"):
        column(n)
    model("ancient_debris", "block/cube_column", {"side": "blocks/ancient_debris_side", "end": "blocks/ancient_debris_top"})
    state("ancient_debris", {"normal": {"model": "ancient_debris"}})
    item_block("ancient_debris")
    for n in ("mangrove_leaves", "cherry_leaves", "pale_oak_leaves", "azalea_leaves", "flowering_azalea_leaves"):
        model(n, "block/leaves", {"all": "blocks/" + n})
        state(n, {"normal": {"model": n}})
        item_block(n)
    model("moss_carpet", "block/carpet", {"wool": "blocks/moss_block"})
    state("moss_carpet", {"normal": {"model": "moss_carpet"}})
    item_block("moss_carpet")
    for n in ("cornflower", "lily_of_the_valley", "torchflower", "pitcher_plant", "kelp", "mangrove_propagule", "cherry_sapling", "pale_oak_sapling"):
        cross(n)
    write("models/block/pink_petals", {"ambientocclusion": False, "textures": {"particle": "blocks/pink_petals", "p": "blocks/pink_petals"},
                                       "elements": [{"from": [0, 0.5, 0], "to": [16, 0.5, 16], "faces": {"up": {"texture": "#p"}, "down": {"texture": "#p"}}}]})
    state("pink_petals", {"normal": {"model": "pink_petals"}})
    item_flat("pink_petals", "blocks/pink_petals")

    for n in ("blast_furnace", "smoker"):
        model(n, "block/orientable", {"front": f"blocks/{n}_front", "side": f"blocks/{n}_side", "top": f"blocks/{n}_top"})
        model(n + "_on", "block/orientable", {"front": f"blocks/{n}_front_on", "side": f"blocks/{n}_side", "top": f"blocks/{n}_top"})
        horizontal(n, n, n + "_on")
        item_block(n)
    for n, front, side, top in (("loom", "loom_front", "loom_side", "loom_top"), ("bee_nest", "bee_nest_front", "bee_nest_side", "bee_nest_top"),
                                ("beehive", "beehive_front", "beehive_side", "beehive_top")):
        model(n, "block/orientable", {"front": "blocks/" + front, "side": "blocks/" + side, "top": "blocks/" + top})
        horizontal(n, n)
        item_block(n)
    for n, top, side in (("smithing_table", "smithing_table_top", "smithing_table_side"), ("cartography_table", "cartography_table_top", "cartography_table_side"),
                         ("fletching_table", "fletching_table_top", "fletching_table_side"), ("target", "target_top", "target_side"),
                         ("respawn_anchor", "respawn_anchor_top", "respawn_anchor_side"), ("lodestone", "lodestone_top", "lodestone_side"),
                         ("honey_block", "honey_block_top", "honey_block_side")):
        model(n, "block/cube_column", {"end": "blocks/" + top, "side": "blocks/" + side})
        if n != "target":
            state(n, {"normal": {"model": n}})
        item_block(n)
    state("target", {f"power={i}": {"model": "target"} for i in range(16)})
    model("barrel", "block/cube_bottom_top", {"top": "blocks/barrel_top", "bottom": "blocks/barrel_bottom", "side": "blocks/barrel_side"})
    six("barrel", "barrel")
    item_block("barrel")

    # Shaped models
    write("models/block/stonecutter", {"parent": "block/block", "textures": {"particle": "blocks/stonecutter_top", "top": "blocks/stonecutter_top", "side": "blocks/stonecutter_side", "saw": "blocks/stonecutter_saw"},
                                       "elements": [box([0, 0, 0], [16, 9, 16], ALL, "#side") | {"faces": {"down": {"texture": "#side"}, "up": {"texture": "#top"}, "north": {"texture": "#side"}, "south": {"texture": "#side"}, "west": {"texture": "#side"}, "east": {"texture": "#side"}}},
                                                    {"from": [1, 9, 8], "to": [15, 16, 8], "faces": {"north": {"texture": "#saw"}, "south": {"texture": "#saw"}}}]})
    horizontal("stonecutter", "stonecutter")
    item_block("stonecutter")
    write("models/block/grindstone", {"parent": "block/block", "textures": {"particle": "blocks/grindstone_side", "s": "blocks/grindstone_side", "p": "blocks/grindstone_pivot"},
                                      "elements": [box([4, 4, 2], [12, 16, 14], ALL, "#s"), box([2, 0, 6], [4, 13, 10], ALL, "#p"), box([12, 0, 6], [14, 13, 10], ALL, "#p")]})
    horizontal("grindstone", "grindstone")
    item_block("grindstone")
    for lvl in range(9):
        h = 1 + min(lvl, 7) * 2 if lvl < 8 else 15
        elems = [box([0, 0, 0], [16, 2, 16], ALL, "#bottom"), box([0, 0, 0], [2, 16, 16], ALL, "#side"), box([14, 0, 0], [16, 16, 16], ALL, "#side"),
                 box([2, 0, 0], [14, 16, 2], ALL, "#side"), box([2, 0, 14], [14, 16, 16], ALL, "#side")]
        if lvl > 0:
            elems.append(box([2, 2, 2], [14, h, 14], ("up",), "#ready" if lvl == 8 else "#compost"))
        write(f"models/block/composter_{lvl}", {"parent": "block/block", "textures": {"particle": "blocks/composter_side", "side": "blocks/composter_side", "bottom": "blocks/composter_bottom",
                                                                                   "compost": "blocks/composter_compost", "ready": "blocks/composter_ready"}, "elements": elems})
    state("composter", {f"level={i}": {"model": f"composter_{i}"} for i in range(9)})
    item_block("composter", "block/composter_0")
    write("models/block/oak_hanging_sign", {"parent": "block/block", "textures": {"particle": "blocks/planks_oak", "w": "blocks/planks_oak", "c": "blocks/chain"},
                                            "elements": [box([1, 0, 7], [15, 10, 9], ALL, "#w"), {"from": [3, 10, 8], "to": [5, 16, 8], "faces": {"north": {"texture": "#c"}, "south": {"texture": "#c"}}},
                                                         {"from": [11, 10, 8], "to": [13, 16, 8], "faces": {"north": {"texture": "#c"}, "south": {"texture": "#c"}}}]})
    horizontal("oak_hanging_sign", "oak_hanging_sign")
    item_flat("oak_hanging_sign", "blocks/planks_oak")
    write("models/block/scaffolding", {"parent": "block/block", "textures": {"particle": "blocks/scaffolding_top", "t": "blocks/scaffolding_top", "s": "blocks/scaffolding_side"},
                                       "elements": [box([0, 14, 0], [16, 16, 16], ALL, "#t"), box([0, 0, 0], [2, 14, 2], ALL, "#s"), box([14, 0, 0], [16, 14, 2], ALL, "#s"),
                                                    box([0, 0, 14], [2, 14, 16], ALL, "#s"), box([14, 0, 14], [16, 14, 16], ALL, "#s")]})
    state("scaffolding", {"normal": {"model": "scaffolding"}})
    item_block("scaffolding")
    write("models/block/chain", {"parent": "block/block", "textures": {"particle": "blocks/chain", "c": "blocks/chain"},
                                 "elements": [{"from": [6.5, 0, 8], "to": [9.5, 16, 8], "faces": {"north": {"texture": "#c"}, "south": {"texture": "#c"}}},
                                              {"from": [8, 0, 6.5], "to": [8, 16, 9.5], "faces": {"west": {"texture": "#c"}, "east": {"texture": "#c"}}}]})
    state("chain", {"axis=y": {"model": "chain"}, "axis=z": {"model": "chain", "x": 90}, "axis=x": {"model": "chain", "x": 90, "y": 90}})
    item_flat("chain", "blocks/chain")
    model("soul_torch", "block/torch", {"torch": "blocks/soul_torch"})
    model("soul_torch_wall", "block/torch_wall", {"torch": "blocks/soul_torch"})
    state("soul_torch", {"facing=up": {"model": "soul_torch"}, "facing=east": {"model": "soul_torch_wall"}, "facing=south": {"model": "soul_torch_wall", "y": 90},
                         "facing=west": {"model": "soul_torch_wall", "y": 180}, "facing=north": {"model": "soul_torch_wall", "y": 270}})
    item_flat("soul_torch", "blocks/soul_torch")
    for n in ("lantern", "soul_lantern"):
        write("models/block/" + n, {"parent": "block/block", "textures": {"particle": "blocks/" + n, "l": "blocks/" + n},
                                    "elements": [{"from": [5, 0, 5], "to": [11, 7, 11], "faces": {f: {"texture": "#l", "uv": [0, 0, 6, 7] if f not in ("up", "down") else [0, 9, 6, 15]} for f in ALL}},
                                                 {"from": [6, 7, 6], "to": [10, 9, 10], "faces": {f: {"texture": "#l", "uv": [6, 0, 10, 2]} for f in ALL}}]})
        state(n, {"normal": {"model": n}})
        item_flat(n, "blocks/" + n)
    for n, fire in (("campfire", "campfire_fire"), ("soul_campfire", "soul_campfire_fire")):
        write("models/block/" + n, {"parent": "block/block", "textures": {"particle": "blocks/campfire_log", "log": "blocks/campfire_log", "fire": "blocks/" + fire},
                                    "elements": [box([1, 0, 0], [5, 4, 16], ALL, "#log"), box([11, 0, 0], [15, 4, 16], ALL, "#log"),
                                                 box([0, 3, 1], [16, 7, 5], ALL, "#log"), box([0, 3, 11], [16, 7, 15], ALL, "#log"),
                                                 {"from": [0.8, 1, 8], "to": [15.2, 17, 8], "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True}, "faces": {"north": {"texture": "#fire"}, "south": {"texture": "#fire"}}},
                                                 {"from": [8, 1, 0.8], "to": [8, 17, 15.2], "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True}, "faces": {"west": {"texture": "#fire"}, "east": {"texture": "#fire"}}}]})
        state(n, {"normal": {"model": n}})
        item_block(n)
    for n in ("trial_spawner", "ominous_trial_spawner", "vault"):
        model(n, "block/cube_column", {"end": f"blocks/{n}_top", "side": f"blocks/{n}_side"})
        state(n, {"normal": {"model": n}})
        item_block(n)
    write("models/block/sculk_sensor", {"parent": "block/block", "textures": {"particle": "blocks/sculk_sensor_top", "t": "blocks/sculk_sensor_top", "s": "blocks/sculk_sensor_side", "x": "blocks/sculk_sensor_tendril"},
                                        "elements": [{"from": [0, 0, 0], "to": [16, 8, 16], "faces": {"up": {"texture": "#t"}, "down": {"texture": "#t"}, "north": {"texture": "#s", "uv": [0, 8, 16, 16]}, "south": {"texture": "#s", "uv": [0, 8, 16, 16]}, "west": {"texture": "#s", "uv": [0, 8, 16, 16]}, "east": {"texture": "#s", "uv": [0, 8, 16, 16]}}},
                                                     {"from": [0, 8, 8], "to": [16, 16, 8], "faces": {"north": {"texture": "#x"}, "south": {"texture": "#x"}}}]})
    for n in ("sculk_sensor", "calibrated_sculk_sensor"):
        state(n, {"powered=false": {"model": "sculk_sensor"}, "powered=true": {"model": "sculk_sensor"}})
        item_block(n, "block/sculk_sensor")
    write("models/block/sculk_shrieker", {"parent": "block/block", "textures": {"particle": "blocks/sculk_shrieker_top", "t": "blocks/sculk_shrieker_top", "s": "blocks/sculk_shrieker_side"},
                                          "elements": [{"from": [0, 0, 0], "to": [16, 8, 16], "faces": {"up": {"texture": "#t"}, "down": {"texture": "#s"}, "north": {"texture": "#s", "uv": [0, 8, 16, 16]}, "south": {"texture": "#s", "uv": [0, 8, 16, 16]}, "west": {"texture": "#s", "uv": [0, 8, 16, 16]}, "east": {"texture": "#s", "uv": [0, 8, 16, 16]}}}]})
    state("sculk_shrieker", {"normal": {"model": "sculk_shrieker"}})
    item_block("sculk_shrieker")
    write("models/block/conduit", {"parent": "block/block", "textures": {"particle": "blocks/conduit", "c": "blocks/conduit"}, "elements": [box([5, 5, 5], [11, 11, 11], ALL, "#c")]})
    write("models/block/bamboo", {"parent": "block/block", "textures": {"particle": "blocks/bamboo_stalk", "b": "blocks/bamboo_stalk"},
                                  "elements": [{"from": [6.5, 0, 6.5], "to": [9.5, 16, 9.5], "faces": {f: {"texture": "#b", "uv": [6, 0, 9, 16]} for f in ALL}},
                                               {"from": [0, 0, 8], "to": [16, 16, 8], "faces": {"north": {"texture": "#b"}, "south": {"texture": "#b"}}}]})
    state("bamboo", {"normal": {"model": "bamboo"}})
    item_flat("bamboo", "blocks/bamboo_stalk")
    write("models/block/sea_pickle", {"parent": "block/block", "textures": {"particle": "blocks/sea_pickle", "p": "blocks/sea_pickle"}, "elements": [box([6, 0, 6], [10, 6, 10], ALL, "#p")]})
    state("sea_pickle", {"normal": {"model": "sea_pickle"}})
    item_block("sea_pickle")
    for n in ("small_amethyst_bud", "medium_amethyst_bud", "large_amethyst_bud", "amethyst_cluster"):
        model(n, "block/cross", {"cross": "blocks/" + n})
        six(n, n)
        item_flat(n, "blocks/" + n)
    state("conduit", {"normal": {"model": "conduit"}})
    item_block("conduit")

    for n in ("raw_iron", "raw_gold", "raw_copper", "copper_ingot", "amethyst_shard", "netherite_scrap", "netherite_ingot", "echo_shard", "disc_fragment_5",
              "phantom_membrane", "nautilus_shell", "heart_of_the_sea", "pottery_shard_archer", "pottery_shard_prize", "pottery_shard_arms_up", "pottery_shard_skull"):
        item_flat(n, "items/" + n)


# ---- names ----------------------------------------------------------------------------------

def pretty(name):
    return " ".join(w.capitalize() if w not in ("of", "the", "on", "a") else w for w in name.split("_"))


def lang():
    blocks = """deepslate cobbled_deepslate tuff calcite dripstone_block smooth_basalt reinforced_deepslate rooted_dirt mud suspicious_sand suspicious_gravel
    mud_bricks smooth_quartz mangrove_log cherry_log pale_oak_log stripped_oak_log bamboo_block mangrove_planks cherry_planks pale_oak_planks mangrove_leaves
    cherry_leaves pale_oak_leaves azalea_leaves flowering_azalea_leaves moss_block moss_carpet cornflower lily_of_the_valley torchflower pitcher_plant pink_petals
    blue_ice powder_snow deepslate_coal_ore deepslate_iron_ore copper_ore deepslate_copper_ore deepslate_gold_ore deepslate_redstone_ore deepslate_lapis_ore
    deepslate_diamond_ore deepslate_emerald_ore nether_gold_ore ancient_debris blast_furnace smoker stonecutter smithing_table grindstone composter loom
    cartography_table fletching_table barrel target oak_hanging_sign scaffolding chain tinted_glass soul_torch lantern soul_lantern campfire soul_campfire
    shroomlight ochre_froglight verdant_froglight pearlescent_froglight trial_spawner ominous_trial_spawner vault sculk sculk_sensor calibrated_sculk_sensor
    sculk_shrieker respawn_anchor lodestone conduit bee_nest beehive honey_block bamboo kelp sea_pickle tube_coral_block brain_coral_block dead_tube_coral_block
    amethyst_block budding_amethyst small_amethyst_bud medium_amethyst_bud large_amethyst_bud amethyst_cluster copper_block exposed_copper weathered_copper
    oxidized_copper waxed_copper_block mangrove_propagule cherry_sapling pale_oak_sapling""".split()
    items = """raw_iron raw_gold raw_copper copper_ingot amethyst_shard netherite_scrap netherite_ingot echo_shard disc_fragment_5 phantom_membrane nautilus_shell
    heart_of_the_sea""".split()
    names = {"oak_hanging_sign": "Oak Hanging Sign", "waxed_copper_block": "Waxed Block of Copper", "copper_block": "Block of Copper",
             "amethyst_block": "Block of Amethyst", "disc_fragment_5": "Disc Fragment", "tnt": "TNT"}
    lines = ["", "# Sythix: blocks and items from Minecraft 1.13-1.21 (generated by branding/modern_assets.py)"]
    for b in blocks:
        lines.append(f"tile.m.{b}.name={names.get(b, pretty(b))}")
    for i in items:
        lines.append(f"item.m.{i}.name={names.get(i, pretty(i))}")
    for i, motif in (("archer", "Archer"), ("prize", "Prize"), ("arms_up", "Arms Up"), ("skull", "Skull")):
        lines.append(f"item.m.pottery_shard_{i}.name={motif} Pottery Sherd")
    lines += ["container.m.blast_furnace=Blast Furnace", "container.m.smoker=Smoker", "container.m.barrel=Barrel"]
    path = ROOT + "/lang/en_us.lang"
    text = open(path, encoding="utf-8").read()
    marker = "\n# Sythix: blocks and items from Minecraft 1.13-1.21"
    if marker in text:
        text = text[:text.index(marker)].rstrip("\n") + "\n"
    with open(path, "w", encoding="utf-8") as f:
        f.write(text.rstrip("\n") + "\n" + "\n".join(lines) + "\n")


# ---- recipes --------------------------------------------------------------------------------

VARIANTS = {"planks": 32767, "log": 32767, "wooden_slab": 32767, "stone": 0, "stone_slab": 0, "coal": 32767, "dirt": 0, "potion": 32767}


def item(name, data=None):
    if data is None and name in VARIANTS:
        data = VARIANTS[name]
    d = {"item": "minecraft:" + name}
    if data is not None:
        d["data"] = data
    return d


def shaped(name, pattern, key, result, count=1, data=None):
    res = item(result, data)
    res["count"] = count
    write("recipes/sythix_" + name, {"type": "crafting_shaped", "pattern": pattern, "key": {k: v for k, v in key.items()}, "result": res})


def shapeless(name, ingredients, result, count=1):
    res = item(result)
    res["count"] = count
    write("recipes/sythix_" + name, {"type": "crafting_shapeless", "ingredients": ingredients, "result": res})


def recipes():
    for w in ("mangrove", "cherry", "pale_oak"):
        shaped(w + "_planks", ["#"], {"#": item(w + "_log")}, w + "_planks", 4)
    shaped("copper_block", ["###", "###", "###"], {"#": item("copper_ingot")}, "copper_block")
    shapeless("copper_ingot", [item("copper_block")], "copper_ingot", 9)
    shapeless("waxed_copper", [item("copper_block"), item("slime_ball")], "waxed_copper_block")
    shaped("amethyst_block", ["##", "##"], {"#": item("amethyst_shard")}, "amethyst_block")
    shaped("tinted_glass", [" # ", "#G#", " # "], {"#": item("amethyst_shard"), "G": item("glass")}, "tinted_glass", 2)
    shaped("lantern", ["###", "#T#", "###"], {"#": item("iron_nugget"), "T": item("torch")}, "lantern")
    shaped("soul_lantern", ["###", "#T#", "###"], {"#": item("iron_nugget"), "T": item("soul_torch")}, "soul_lantern")
    shaped("soul_torch", ["C", "S", "D"], {"C": item("coal"), "S": item("stick"), "D": item("soul_sand")}, "soul_torch", 4)
    shaped("chain", ["N", "I", "N"], {"N": item("iron_nugget"), "I": item("iron_ingot")}, "chain")
    shaped("campfire", [" S ", "SCS", "LLL"], {"S": item("stick"), "C": item("coal"), "L": item("log")}, "campfire")
    shaped("soul_campfire", [" S ", "SDS", "LLL"], {"S": item("stick"), "D": item("soul_sand"), "L": item("log")}, "soul_campfire")
    shaped("barrel", ["PSP", "P P", "PSP"], {"P": item("planks"), "S": item("wooden_slab")}, "barrel")
    shaped("blast_furnace", ["III", "IFI", "SSS"], {"I": item("iron_ingot"), "F": item("furnace"), "S": item("stone")}, "blast_furnace")
    shaped("smoker", [" L ", "LFL", " L "], {"L": item("log"), "F": item("furnace")}, "smoker")
    shaped("composter", ["S S", "S S", "SSS"], {"S": item("wooden_slab")}, "composter")
    shaped("loom", ["SS", "PP"], {"S": item("string"), "P": item("planks")}, "loom")
    shaped("smithing_table", ["II", "PP", "PP"], {"I": item("iron_ingot"), "P": item("planks")}, "smithing_table")
    shaped("fletching_table", ["FF", "PP", "PP"], {"F": item("flint"), "P": item("planks")}, "fletching_table")
    shaped("cartography_table", ["MM", "PP", "PP"], {"M": item("paper"), "P": item("planks")}, "cartography_table")
    shaped("stonecutter", [" I ", "SSS"], {"I": item("iron_ingot"), "S": item("stone")}, "stonecutter")
    shaped("grindstone", ["SBS", "P P"], {"S": item("stick"), "B": item("stone_slab"), "P": item("planks")}, "grindstone")
    shaped("target", [" R ", "RHR", " R "], {"R": item("redstone"), "H": item("hay_block")}, "target")
    shaped("scaffolding", ["BSB", "B B", "B B"], {"B": item("bamboo"), "S": item("string")}, "scaffolding", 6)
    shaped("moss_carpet", ["MM"], {"M": item("moss_block")}, "moss_carpet", 3)
    shapeless("netherite_ingot", [item("netherite_scrap")] * 4 + [item("gold_ingot")] * 4, "netherite_ingot")
    shaped("lodestone", ["SSS", "SNS", "SSS"], {"S": item("stonebrick", 3), "N": item("netherite_ingot")}, "lodestone")
    shaped("respawn_anchor", ["OOO", "GGG", "OOO"], {"O": item("obsidian"), "G": item("glowstone")}, "respawn_anchor")
    shaped("conduit", ["NNN", "NHN", "NNN"], {"N": item("nautilus_shell"), "H": item("heart_of_the_sea")}, "conduit")
    shaped("bamboo_block", ["BBB", "BBB", "BBB"], {"B": item("bamboo")}, "bamboo_block")
    shaped("mud_bricks", ["MM", "MM"], {"M": item("mud")}, "mud_bricks", 4)
    shapeless("mud", [item("dirt"), item("potion")], "mud")
    shaped("beehive", ["PPP", "HHH", "PPP"], {"P": item("planks"), "H": item("honey_block")}, "beehive")
    shapeless("stripped_oak_log", [item("log", 0), item("flint")], "stripped_oak_log")
    shapeless("blue_ice", [item("packed_ice")] * 9, "blue_ice")
    shaped("cornflower_dye", ["#"], {"#": item("cornflower")}, "dye", 1, data=4)
    shaped("lily_of_the_valley_dye", ["#"], {"#": item("lily_of_the_valley")}, "dye", 1, data=15)
    shaped("torchflower_dye", ["#"], {"#": item("torchflower")}, "dye", 1, data=14)


# ---- part 2: items ---------------------------------------------------------------------------

NETHERITE = (74, 66, 70)
TOOLS = ("sword", "pickaxe", "axe", "shovel", "hoe")
ARMOR = ("helmet", "chestplate", "leggings", "boots")
DISCS = {"otherside": "Lena Raine - otherside", "5": "Samuel \u00c5berg - 5", "pigstep": "Lena Raine - Pigstep", "relic": "Aaron Cherof - Relic",
         "creator": "Lena Raine - Creator", "creator_music_box": "Lena Raine - Creator (Music Box)", "precipice": "Aaron Cherof - Precipice"}
HANDHELD = {"netherite_" + t for t in TOOLS} | {"mace", "spear", "trident", "brush", "warped_fungus_on_a_stick"}


def item_textures():
    for t in TOOLS:
        save(colorize(load("items/diamond_" + t), NETHERITE, contrast=1.3, bright=1.1), "items/netherite_" + t)
    for a in ARMOR:
        save(colorize(load("items/diamond_" + a), NETHERITE, contrast=1.3, bright=1.1), "items/netherite_" + a)
    for layer in ("1", "2"):
        save(colorize(load(f"models/armor/diamond_layer_{layer}"), NETHERITE, contrast=1.3, bright=1.1), f"models/armor/netherite_layer_{layer}")
    save(colorize(load("models/armor/leather_layer_1"), (60, 150, 60), contrast=1.2), "models/armor/turtle_layer_1")
    save(colorize(load("models/armor/leather_layer_2"), (60, 150, 60), contrast=1.2), "models/armor/turtle_layer_2")
    save(colorize(load("items/leather_helmet"), (60, 150, 60), contrast=1.2), "items/turtle_helmet")
    # Mace: a heavy head on a handle
    mace = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(mace)
    d.line([(3, 13), (9, 7)], fill=(110, 80, 50, 255), width=2)
    d.rectangle([8, 2, 14, 8], fill=(150, 150, 160, 255), outline=(70, 70, 80, 255))
    d.point([(10, 4), (12, 6)], fill=(220, 220, 230, 255))
    save(mace, "items/mace")
    spear = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(spear)
    d.line([(2, 14), (11, 5)], fill=(120, 90, 60, 255), width=1)
    d.polygon([(10, 3), (14, 1), (13, 6)], fill=(200, 200, 210, 255), outline=(110, 110, 120, 255))
    save(spear, "items/spear")
    trident = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(trident)
    d.line([(2, 14), (10, 6)], fill=(60, 140, 130, 255), width=1)
    for a, b in (((10, 6), (14, 2)), ((9, 5), (9, 1)), ((11, 7), (15, 7))):
        d.line([a, b], fill=(90, 190, 170, 255))
    save(trident, "items/trident")
    def crossbow(pull, arrow):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.line([(3, 13), (12, 4)], fill=(110, 80, 50, 255), width=2)
        bend = 3 - pull
        d.line([(4 + bend, 3), (9, 8), (13 - bend, 12)], fill=(90, 90, 100, 255), width=1)
        d.line([(5 + bend, 3), (13, 11 - bend)], fill=(220, 220, 220, 255))
        if arrow:
            d.line([(4, 12), (13, 3)], fill=(200, 200, 210, 255))
        return img
    save(crossbow(0, False), "items/crossbow_standby")
    for i in range(3):
        save(crossbow(i + 1, False), f"items/crossbow_pulling_{i}")
    save(crossbow(3, True), "items/crossbow_arrow")
    wind = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(wind)
    d.ellipse([3, 3, 12, 12], fill=(200, 220, 250, 230), outline=(140, 160, 220, 255))
    d.arc([5, 5, 10, 10], 30, 300, fill=(255, 255, 255, 255))
    save(wind, "items/wind_charge")
    brush = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(brush)
    d.line([(2, 14), (8, 8)], fill=(120, 90, 60, 255), width=2)
    d.rectangle([8, 6, 10, 8], fill=(200, 120, 80, 255))
    d.polygon([(9, 5), (14, 1), (15, 4), (11, 8)], fill=(240, 240, 230, 255))
    save(brush, "items/brush")
    spy = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(spy)
    d.line([(3, 13), (12, 4)], fill=(200, 110, 70, 255), width=3)
    d.ellipse([10, 2, 14, 6], fill=(170, 120, 230, 255))
    save(spy, "items/spyglass")
    for i in range(32):
        save(colorize(load("items/compass_%02d" % i), (40, 160, 170), contrast=1.2), "items/recovery_compass_%02d" % i)
    save(colorize(load("items/carrot_on_a_stick"), (40, 160, 150), contrast=1.1), "items/warped_fungus_on_a_stick")
    save(colorize(load("items/iron_horse_armor"), (170, 120, 90)), "items/wolf_armor")
    for name, rgb in (("sweet_berries", (200, 30, 40)), ("glow_berries", (250, 190, 60))):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        for x, y in ((4, 7), (8, 4), (10, 9), (6, 11)):
            d.ellipse([x, y, x + 3, y + 3], fill=rgb + (255,), outline=tuple(int(c * 0.6) for c in rgb) + (255,))
        d.line([(9, 1), (8, 4)], fill=(60, 120, 40, 255))
        save(img, "items/" + name)
    save(colorize(load("items/seeds_wheat"), (40, 80, 30), contrast=1.4), "items/dried_kelp")
    save(colorize(load("items/mushroom_stew"), (170, 110, 80), contrast=1.0), "items/suspicious_stew")
    honey = load("items/potion_bottle_empty")
    hd = ImageDraw.Draw(honey)
    hd.rectangle([5, 8, 10, 13], fill=(240, 170, 40, 255))
    save(honey, "items/honey_bottle")
    omin = load("items/potion_bottle_empty")
    ImageDraw.Draw(omin).rectangle([5, 8, 10, 13], fill=(30, 110, 110, 255))
    save(omin, "items/ominous_bottle")
    save(colorize(load("items/seeds_pumpkin"), (230, 120, 40)), "items/torchflower_seeds")
    save(colorize(load("items/seeds_melon"), (60, 140, 170)), "items/pitcher_pod")
    snowb = load("items/bucket_water")
    px = snowb.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a and b > r + 30:
                px[x, y] = (245, 248, 255, a)
    save(snowb, "items/powder_snow_bucket")
    for wood, rgb in (("mangrove", (120, 52, 48)), ("cherry", (226, 178, 172)), ("pale_oak", (226, 218, 212))):
        save(colorize(load("items/oak_boat"), rgb, contrast=1.1), f"items/{wood}_boat")
        save(colorize(load("entity/boat/boat_oak"), rgb, contrast=1.1), f"entity/boat/boat_{wood}")
    hues = [0.0, 0.1, 0.3, 0.5, 0.6, 0.75, 0.9]
    for (disc, _), h in zip(DISCS.items(), hues):
        save(hue(load("items/record_13"), h, 1.2), "items/music_disc_" + disc)


def item_models():
    for t in TOOLS:
        item_flat("netherite_" + t, "items/netherite_" + t, True)
    for a in ARMOR:
        item_flat("netherite_" + a, "items/netherite_" + a)
    for n in ("turtle_helmet", "mace", "spear", "trident", "wind_charge", "brush", "spyglass", "warped_fungus_on_a_stick", "wolf_armor",
              "sweet_berries", "glow_berries", "dried_kelp", "suspicious_stew", "honey_bottle", "ominous_bottle", "torchflower_seeds", "pitcher_pod",
              "powder_snow_bucket", "mangrove_boat", "cherry_boat", "pale_oak_boat"):
        item_flat(n, "items/" + n, n in HANDHELD)
    for disc in DISCS:
        item_flat("music_disc_" + disc, "items/music_disc_" + disc)
    bow = json.load(open(ROOT + "/models/item/bow.json"))
    display = bow["display"]
    write("models/item/crossbow", {"parent": "item/generated", "textures": {"layer0": "items/crossbow_standby"}, "display": display,
                                   "overrides": [{"predicate": {"pulling": 1}, "model": "item/crossbow_pulling_0"},
                                                 {"predicate": {"pulling": 1, "pull": 0.58}, "model": "item/crossbow_pulling_1"},
                                                 {"predicate": {"pulling": 1, "pull": 1.0}, "model": "item/crossbow_pulling_2"},
                                                 {"predicate": {"charged": 1}, "model": "item/crossbow_arrow"}]})
    for n in ("crossbow_pulling_0", "crossbow_pulling_1", "crossbow_pulling_2", "crossbow_arrow"):
        write("models/item/" + n, {"parent": "item/crossbow", "textures": {"layer0": "items/" + n}})
    compass = json.load(open(ROOT + "/models/item/compass.json"))
    overrides = []
    for o in compass["overrides"]:
        m = o["model"].replace("item/compass_", "item/recovery_compass_").replace("item/compass", "item/recovery_compass")
        overrides.append({"predicate": o["predicate"], "model": m})
    write("models/item/recovery_compass", {"parent": "item/generated", "textures": {"layer0": "items/recovery_compass_16"}, "overrides": overrides})
    for i in range(32):
        if i != 16:
            write("models/item/recovery_compass_%02d" % i, {"parent": "item/generated", "textures": {"layer0": "items/recovery_compass_%02d" % i}})


def item_lang_and_sounds():
    names = {"netherite_" + p: "Netherite " + p.capitalize() for p in TOOLS + ARMOR}
    for n in ("turtle_helmet", "mace", "spear", "trident", "crossbow", "wind_charge", "brush", "spyglass", "recovery_compass", "warped_fungus_on_a_stick",
              "wolf_armor", "sweet_berries", "glow_berries", "dried_kelp", "suspicious_stew", "honey_bottle", "ominous_bottle", "torchflower_seeds",
              "pitcher_pod", "powder_snow_bucket", "mangrove_boat", "cherry_boat", "pale_oak_boat"):
        names[n] = pretty(n)
    names["turtle_helmet"] = "Turtle Shell"
    lines = ["", "# Sythix: items from Minecraft 1.13-1.21 part 2"]
    for k, v in names.items():
        lines.append(f"item.m.{k}.name={v}")
    for disc, desc in DISCS.items():
        lines.append(f"item.m.music_disc_{disc}.name=Music Disc")
        lines.append(f"item.record.{disc}.desc={desc}")
    path = ROOT + "/lang/en_us.lang"
    text = open(path, encoding="utf-8").read()
    marker = "\n# Sythix: items from Minecraft 1.13-1.21 part 2"
    if marker in text:
        text = text[:text.index(marker)].rstrip("\n") + "\n"
    with open(path, "w", encoding="utf-8") as f:
        f.write(text.rstrip("\n") + "\n" + "\n".join(lines) + "\n")
    sounds = json.load(open(ROOT + "/sounds.json"))
    for disc in DISCS:
        sounds["record." + disc] = {"category": "record", "sounds": [{"name": "records/" + disc, "stream": True}]}
    with open(ROOT + "/sounds.json", "w") as f:
        json.dump(sounds, f, indent=2)


def item_recipes():
    shaped("crossbow", ["SIS", "T#T", " S "], {"S": item("stick"), "I": item("iron_ingot"), "T": item("string"), "#": item("tripwire_hook")}, "crossbow")
    shaped("spyglass", ["A", "C", "C"], {"A": item("amethyst_shard"), "C": item("copper_ingot")}, "spyglass")
    shaped("brush", ["F", "C", "S"], {"F": item("feather"), "C": item("copper_ingot"), "S": item("stick")}, "brush")
    shaped("recovery_compass", ["EEE", "ECE", "EEE"], {"E": item("echo_shard"), "C": item("compass")}, "recovery_compass")
    shaped("mace", ["B", "S"], {"B": item("iron_block"), "S": item("stick")}, "mace")
    shaped("spear", ["  I", " S ", "S  "], {"I": item("iron_ingot"), "S": item("stick")}, "spear")
    shaped("trident", [" PP", " SP", "S  "], {"P": item("prismarine_shard"), "S": item("stick")}, "trident")
    shaped("wind_charge", ["SS", "SS"], {"S": item("snowball")}, "wind_charge", 2)
    for wood in ("mangrove", "cherry", "pale_oak"):
        shaped(wood + "_boat", ["P P", "PPP"], {"P": item(wood + "_planks")}, wood + "_boat")
    shapeless("suspicious_stew", [item("bowl"), item("red_mushroom"), item("brown_mushroom"), item("red_flower", 0)], "suspicious_stew")
    shapeless("powder_snow_bucket", [item("bucket"), item("snow")], "powder_snow_bucket")
    shaped("turtle_helmet", ["NNN", "N N"], {"N": item("nautilus_shell")}, "turtle_helmet")


# ---- part 3: effects, enchantments, potions ------------------------------------------------------

def effects_lang():
    effects = {"slow_falling": "Slow Falling", "conduit_power": "Conduit Power", "dolphins_grace": "Dolphin's Grace", "bad_omen": "Bad Omen",
               "hero_of_the_village": "Hero of the Village", "darkness": "Darkness", "trial_omen": "Trial Omen", "raid_omen": "Raid Omen",
               "wind_charged": "Wind Charged", "weaving": "Weaving", "oozing": "Oozing", "infested": "Infested"}
    enchants = {"swift_sneak": "Swift Sneak", "soul_speed": "Soul Speed", "density": "Density", "breach": "Breach", "wind_burst": "Wind Burst",
                "multishot": "Multishot", "piercing": "Piercing", "quick_charge": "Quick Charge", "impaling": "Impaling", "loyalty": "Loyalty",
                "riptide": "Riptide", "channeling": "Channeling"}
    potions = ["slow_falling", "wind_charged", "weaving", "oozing", "infested"]
    lines = ["", "# Sythix: effects and enchantments from Minecraft 1.13-1.21"]
    for k, v in effects.items():
        lines.append(f"effect.m.{k}={v}")
    for k, v in enchants.items():
        lines.append(f"enchantment.m.{k}={v}")
    for k in potions:
        v = effects[k]
        lines += [f"potion.effect.{k}=Potion of {v}", f"splash_potion.effect.{k}=Splash Potion of {v}",
                  f"lingering_potion.effect.{k}=Lingering Potion of {v}", f"tipped_arrow.effect.{k}=Arrow of {v}"]
    path = ROOT + "/lang/en_us.lang"
    text = open(path, encoding="utf-8").read()
    marker = "\n# Sythix: effects and enchantments from Minecraft 1.13-1.21"
    if marker in text:
        text = text[:text.index(marker)].rstrip("\n") + "\n"
    with open(path, "w", encoding="utf-8") as f:
        f.write(text.rstrip("\n") + "\n" + "\n".join(lines) + "\n")


if __name__ == "__main__":
    textures()
    models()
    lang()
    recipes()
    item_textures()
    item_models()
    item_lang_and_sounds()
    item_recipes()
    effects_lang()
    print("modern assets written")
