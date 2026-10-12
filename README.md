# Sythix Client

Sythix Client is a customised PvP client built on top of this Eaglercraft 1.12 source (desktop Java build, OptiFine included). It has no cheats: every module is visual, informational or a comfort feature.

## Using it

- **Mod menu:** press **Right Shift** in game (rebind it in Controls or the Keybind Manager) or click **Sythix Mods** on the main menu. Search, filter by category or favourites, switch modules on/off and click the gear for settings. Every setting has a description and its own reset arrow.
- **HUD editor:** button in the mod menu. Drag elements, scroll to resize, click one to edit it. Hold Shift to place freely.
- **Sythix Settings:** Options > *Sythix Settings...* (replaces the Snooper button). All video, OptiFine detail/quality/animation/performance options in one tabbed screen.
- **Profile:** **Profile** on the main menu. Change your offline name, skin (put PNGs in `sythix/skins`), model (classic/slim) and cape (built-in or your own PNG in `sythix/capes`). Skins and capes are shown on your screen only.
- **Help Center** (mod menu > Client) has guides and troubleshooting.

## The 71 modules

| Section | Modules |
|---|---|
| A. Performance | Performance Optimizer, Animation Controller, Particle Controller, Graphics Controller, Resource Manager, Performance Monitor |
| B. PvP & combat | Keystrokes, CPS Counter, Crosshair, Combat HUD, Combat Animations, Viewmodel, Armor HUD, Potion HUD, Combat Timers, Movement Controls, Crystal Flow Analyzer, CPvP Training Lab |
| C. Movement | Camera Tools (zoom, freelook, perspective), Movement HUD, Coordinates HUD, Compass HUD, Waypoints (one module with a manager screen) |
| D. Inventory | Inventory HUD, Item Counters, Item Information, Inventory Enhancements, World Information |
| E. HUD & interface | HUD Editor, Smart HUD Layout, Scoreboard, Tab List, Chat, Notifications, Array List, Interface Theme, Main Menu |
| F. Visuals | Environment Visuals, Block Visuals, Texture Pack Manager, Cosmetics, Sound Controller |
| G. Multiplayer | Server Manager, Connection Monitor, Friends Manager, Party Tools, Server Profiles, Chat Utilities |
| H. Recording & stats | Screenshot Manager, Recording Tools, Replay System, Combat Replay, Statistics Tracker, Duel Analytics, Practice Tools |
| I. Client | Keybind Manager, Profile Manager, Settings Manager, Search and Discovery, Accessibility, Help Center |
| J. Advanced | Diagnostics, Error Handler, Safe Mode, Update Manager, Plugin Framework, Developer Tools, Privacy Controls, Browser Compatibility, Localization, 1.21 Content |

## Phase 6 highlights

- **Performance:** a benchmark that measures your average FPS and 1% low FPS (results history in *Benchmark Results*), an adaptive render distance that holds a target FPS, dynamic chunk updates and a memory clean-up.
- **Cosmetics** (Profile screen or the Cosmetics module): hats (top hat, crown, halo, horns, cat ears, witch hat, cap), shoulder buddies (slime, cube, ghost), particle auras or trails, four wing styles, animated capes (Chroma Flow, Ember, Galaxy) and seven emotes.
- **Animations** (Animation Controller): menu open animations (fade, slide or zoom), a smooth hotbar and smooth chat. Accessibility > Reduced Motion turns them off.
- **Client texture packs** (*Texture Pack Manager > Client Pack*): **Default** is Cowarted Vanilla, the normal Sythix look. **Blue** switches to Fallen Angel and **Red** to Red Revolution (Cowarted Vanilla is turned off while either is chosen, and comes back when you pick Default again). The **Cart Pack** toggle (Harlu) works the same as before and goes on top of whichever client pack is chosen. Your own resource packs still go on top of all of these. Fallen Angel and Red Revolution are third-party packs; files marked "do not distribute" (Red Revolution's clear-glass textures) are left out.
- **Modern textures:** *Texture Pack Manager > Modern Pack Converter* turns a 1.13-1.21 resource pack into a 1.12 pack, so blocks, items and mobs that exist in 1.12 get the modern look.
- **Video recording:** Recording Tools saves `.avi` videos to `recordings/` by itself (video only; use OBS if you also need sound).
- **Replays:** Replay System records your sessions to `replays/` and plays them back with a free camera, pause, 0.25x-8x speed, seeking and bookmark markers.

## Crystal and cart PvP

Five modules for crystal PvP and TNT minecart (cart) PvP. They only read what your client already sees and never place, hit or aim for you, so they are fine on servers that allow PvP clients.

- **Combat Replay** (Rec & Stats, on by default): every fight is summarised when it ends (when someone dies, or after 8 seconds without combat): damage dealt and taken, hits, crystals placed and hit, crystal speed, break delay (how long your crystals sat before you hit them), TNT minecarts and rail-to-cart time, totem pops, a health graph, a timeline of what happened and tips. If *Replay System > Record Replays* is on, **Watch Replay** opens the replay 3 seconds before the fight. Set a *Last Fight Key* to open the latest review in one press.
- **CPvP Training Lab** (PvP & Combat): *Open* creates a separate superflat world, *Sythix Training Lab*, with an obsidian arena, a rail lane, four armoured training dummies (each with a totem) and a full crystal and cart kit. Your own worlds are never changed. Drills with personal bests: **Crystal Speed** (crystals broken per second), **Cart Speed** (carts per minute), **Totem Reaction** (put a totem in your offhand when SWAP appears, 5 rounds) and **Dummy Duel** (take down every dummy). *Refill Kit*, *Reset Arena* and Creative/Survival are in its settings.
- **Smart HUD Layout** (HUD & Interface): *Apply Preset* switches on and places the useful elements for Crystal PvP, Cart PvP or a Minimal screen. *Auto Arrange* moves what is on into free spots, never over the hotbar, health, crosshair, chat or boss bar, and never on top of each other (it leaves room for elements that grow, like the analyzer during a fight). *Undo* puts your old layout back; *Keep Tidy* fixes overlaps by itself.
- **Crystal Flow Analyzer** (PvP & Combat): live crystals placed and broken per second, your best speed, break delay, totem pops, crystals/carts/totems left and the current fight's damage. In *Auto* it adds cart lines (carts placed, lit and set off, rail-to-cart time) once you use TNT minecarts.
- **Duel Analytics** (Rec & Stats, on by default): keeps every duel in `sythix/duel-history.json` and shows win rate, streaks, average fight length and damage, charts of your win rate and crystal speed (or rail-to-cart time, or damage) over your last duels, a trend line ("improved by 12%"), records against each opponent and personal bests. Filter by Crystal, Cart or Melee. Training Lab fights are left out unless you switch on *Include Training*.

## Shaders

Press **Shaders** on the main menu, in the pause menu or in Sythix Settings. Pick a pack and it loads right away; *Options* changes the pack's own settings.

- **Sythix Vibrant** (made for Sythix, fast): richer colours, a soft glow, a vignette, plants and leaves that sway in the wind, and moving water with rolling waves and drifting ripples.
- **Sythix Shadows**: everything in Vibrant plus real sun and moon shadows. Needs a stronger graphics card.
- **Your own packs:** *Add Pack* copies a shader pack (.zip or folder) in, or put it in the `shaderpacks` folder (*Open Folder*). Packs made for OptiFine (Minecraft 1.8-1.12) work best.

Shaders are off by default. Options that do not work with shaders (anti-aliasing, anisotropic filtering, Fast Render, 3D anaglyph) are switched off automatically when you pick a pack.

## Design

Sythix's menus use a smooth font (Poppins), a blurred, slowly turning panorama behind the main menu, frosted glass panels, rounded buttons with a red-to-ember accent, and the game world blurred behind the pause menu. Change it in **Interface Theme** (colours, *Smooth Font*: Menus / Everywhere / Off) and **Main Menu** (*Background*: Panorama / Artwork / Plain, *Blur*).

## 1.21 content (singleplayer and LAN)

Content from Minecraft 1.13-1.21 has been added to the client itself, using ids that 1.12 never uses, so joining normal 1.12 servers is not affected. Settings are in the **1.21 Content** module (Advanced).

- **103 blocks:** deepslate and its ores, tuff, calcite, copper (oxidises; scrape it with an axe), amethyst (geodes, budding amethyst, buds), mangrove/cherry/pale oak wood with saplings, new flowers, mud, moss, sculk, blast furnace, smoker, barrel, scaffolding, lanterns, campfires, froglights, honey, bamboo, powder snow and more.
- **59 items:** netherite tools and armour (smithing upgrade keeps enchantments), copper, raw ores, mace, spear, trident (throwable), crossbow, wind charge, brush, spyglass, recovery compass, new foods, boats, music discs, scutes, glow ink sacs and goat horns.
- **12 effects and 12 enchantments:** slow falling, darkness, conduit power, dolphin's grace, wind charged, weaving, oozing, infested...; density, breach, wind burst, lunge, multishot, piercing, quick charge, loyalty, riptide, channeling, impaling, swift sneak, soul speed. New potions can be brewed.
- **Mace and spear work like 1.21:** the mace smash adds 4 damage per block for the first 3 blocks fallen, 2 per block for the next 5 and 1 after that (Density adds 0.5 per block per level); Breach makes armour 15% less effective per level; Wind Burst launches you about 8/16/24 blocks up after a smash (the landing still hurts, as in 1.21); Lunge (spear, 1.21.11) pushes you forward 1/2/3 blocks on a fully charged jab and costs food and durability (it needs at least 7 food). Attribute swapping works like 1.21: hit with one weapon's damage and switch to the mace in the same tick to also get the smash bonus.
- **World generation** in newly explored chunks: a deepslate layer, copper ore, amethyst geodes, dripstone, sculk, cherry/pale oak/mangrove trees, bamboo and new flowers, nether gold ore and ancient debris.
- **35 mobs** with spawn eggs (Creative > Miscellaneous): drowned, bogged, pillager, ravager, hoglin, zoglin, piglin (give it a gold ingot to barter), piglin brute, phantom (if you skip sleep for 3 nights), warden (step on a sculk shrieker four times), breeze, creaking (only moves when nobody looks), zombie nautilus, cod, salmon, tropical fish, pufferfish, tadpole, dolphin, axolotl, glow squid, turtle, frog, fox, panda, bee, goat, camel (saddle and ride), trader llama, wandering trader (trades), sniffer, armadillo, allay (give it an item to collect), strider (ride it on lava with a warped fungus on a stick) and happy ghast (saddle it and fly).

The textures are placeholders made by recolouring the 1.12 textures. For the real 1.21 look, open **1.21 Content > Real 1.21 Textures** and pick a 1.21 resource pack or your own installed Minecraft version: the converter copies its block, item, armour and boat textures (mob models are different shapes, so mobs keep their placeholder textures).

What is still not possible, and why:
- **1.21 content on servers:** normal 1.12 servers never send the new blocks, items or mobs, so they only exist in singleplayer and LAN worlds.
- **New biomes, structures and the deep (y < 0) world:** 1.12 worlds have a fixed height of 0-255 and a fixed biome list; the new blocks are added to the existing biomes instead. Waterlogging (blocks inside water) also does not exist in 1.12.
- **Music disc sounds and 1.21 mob sounds** are not included (they are Mojang's audio). Discs play once a converted 1.21 pack provides the sounds; mobs use similar 1.12 sounds.
- **Sound in videos:** the game's audio output cannot be captured from inside the game; record with OBS if you need sound.
- **Update Manager** and **Plugin Framework** are off by default and only do something if you set them up.

## Browser version

`./CompileWebRuntime.sh` builds a browser version into `build/web` (about 55 MB). It runs the same client with [CheerpJ](https://cheerpj.com), a Java virtual machine in WebAssembly, plus LWJGL and gl4es compiled to WebAssembly (from Leaning Technologies' Browsercraft). Upload the folder to any web host (GitHub Pages, Netlify, your own server) or test it locally with `./CompileWebRuntime.sh serve` and open http://localhost:8080.

- Worlds and settings are saved in the browser (CheerpJ's `/files` storage).
- Tested in Chrome: the Sythix menus, mouse input, creating a singleplayer world and playing in it all work (screenshots in `web/screenshots/`).
- It is much slower than the desktop version: Java runs as WebAssembly, and the singleplayer server runs inside the same browser tab. A new world takes a few minutes to open. A fast computer and Chrome or Edge work best.
- Speed tuning for the browser: it starts with light settings (render distance 4, Fast graphics, no animated textures, smooth lighting, clouds, custom skies or entity shadows; all can be raised in Video Settings), menu text is drawn in batches, and the singleplayer server never runs several ticks back to back, so it can use at most about half the time and the picture keeps moving (on a slow computer the world then runs a little slower instead of freezing).
- No sound, no shaders, no blur effects (WebGL through gl4es offers no framebuffers here), and no screenshots or video recording (the screen cannot be read back; use your system's screenshot key). The first start downloads about 30 MB.
- Open the page with `#debug` at the end of the address (for example http://localhost:8080/#debug) to log frame times and the busiest parts of the game to the browser console.
- **Multiplayer:** browsers cannot open game connections, so the browser version joins servers through a small WebSocket proxy, `web/proxy/sythix_proxy.py` (one Python file; setup on a free server in `web/proxy/README.md`). Set the proxy for everyone with a `config.json` next to `index.html` (`{"proxy": "wss://your-proxy"}`), or per player on the start page (*Multiplayer and speed*). Links like `index.html?server=play.example.com` join a server straight away. Offline-mode servers only (no Microsoft login yet).
- **Speed:** multiplayer runs about 2.5x faster than singleplayer in the browser (no built-in server sharing the thread). *Resolution* on the start page (75% or 50%) draws fewer pixels and helps a lot on weak computers (menu frames 80 ms -> 36 ms at 50% in testing). After the first visit the game files are kept by the browser (`sw.js`), so the next start skips the 60 MB download, and `preload.json` loads the Java runtime in parallel.
- CheerpJ is free for personal and open-source projects; other companies need a license from Leaning Technologies.

## Hosting on Cloudflare (free)

The browser version is plain static files, served by Cloudflare's free static assets (Workers). In the Cloudflare dashboard, connect this GitHub repository and use:

| Setting | Value |
|---|---|
| Build command | `bash cloudflare/build.sh` |
| Deploy command | `npx wrangler deploy` |
| Non-production branch deploy command (previews) | `npx wrangler versions upload` |
| Root directory | `/` (repository root) |

- `cloudflare/build.sh` downloads JDK 17 if the build machine has none (Cloudflare's has no Java), compiles the client, writes the site to `build/web` and stops the build if any game file is missing or over Cloudflare's 25 MiB file limit. A build takes a few minutes.
- `wrangler.jsonc` tells Cloudflare to serve `build/web`. Its `"name"` must be the Worker's name in the dashboard.
- Cloudflare's static files don't answer HTTP Range requests, which CheerpJ needs; `sw.js` (a service worker) handles them in the browser, so no paid features are needed. Private windows that block service workers get a clear message instead.
- Multiplayer is separate: run `web/proxy/sythix_proxy.py` on any always-on server (`web/proxy/README.md`) and add `web/config.json` with `{"proxy": "wss://your-proxy"}`.
- Local test of the exact Cloudflare setup: `bash cloudflare/build.sh && npx wrangler dev`, then open http://localhost:8787.

## Files

Everything Sythix stores is in the `sythix/` folder of the game directory: `config.json` (all settings), `profiles/`, `backups/` (automatic, newest 10 kept), `waypoints.json`, `match-history.json` (statistics), `fight-reviews.json` and `duel-history.json` (Combat Replay and Duel Analytics), `training-lab.json` (drill bests), `profile.json`, `skins/`, `capes/`, `plugins/` and `benchmarks.json`. Videos go to `recordings/` and replays to `replays/` in the game folder. Nothing is sent over the internet by default. If something breaks, use **Safe Mode** or restore a backup in **Settings Manager**.

## Building

Build with **JDK 17** (the Gradle 8.0 wrapper does not run on Java 21): `./gradlew clean build`, then `./UnpackRuntime.sh` and run `runtime/run.sh`.

Developers can run the automatic test with `JAVA_OPTS="-Dsythix.selftest=true" runtime/run.sh`. It switches on every module, changes all settings, opens every screen on the menu and in a test world, saves screenshots and writes the result to `sythix/selftest.txt`, then quits.

Source layout: `src/main/java/net/sythix/client/` (`module/` = modules by section, `ui/` = menus and theme, `core/Hooks.java` = the small entry points called from Minecraft's code). Branding textures are in `src/main/resources/assets/minecraft/textures/gui/sythix/`; regenerate them with `python3 branding/make_assets.py branding/logo-source.webp src/main/resources/assets/minecraft/textures/gui/sythix` (needs Pillow).

---

# Eaglercraft 1.12 source

[![Java CI with Gradle](https://github.com/catfoolyou/Eaglercraft-1.12/actions/workflows/gradle.yml/badge.svg)](https://github.com/catfoolyou/Eaglercraft-1.12/actions/workflows/gradle.yml)

This repo contains the source code of my Eaglercraft 1.12 project. While I have made a somewhat functioning version of a desktop runtime, the web version has not yet been compiled (And will not be any time soon).
This client comes with Optifine built in.


## Getting Started:

Prerequisites: Java 17 and JDK 17 or higher

### To compile the latest version of the client, on Windows:

1. Make sure you have at least Java 17 installed and added to your PATH
2. Download (clone) this repository to your computer
3. Run gradle's clean and build tasks `gradlew.bat clean build`, then look in build/distributions for the compiled runtime zip.

### To compile the latest version of the client, on Linux/macOS:

1. Make sure you have at least Java 17 installed
2. Download (clone) this repository to your computer
3. Open a terminal in the folder the repository was cloned to
4. Type `chmod +x gradlew` and hit enter
5. Run gradle's clean and build tasks `./gradlew clean build`, then look in build/distributions for the compiled runtime zip.

### Releases can be outdated! To get the latest working version compile this repo yourself

## Singleplayer

Singleplayer worlds are located in the `saves` folder.

You can also import and export your existing vanilla Minecraft 1.12 worlds into Eaglercraft by copying the world folders into the `saves` folder in your compiled desktop runtime. Beware that the inventories of LAN world players are not saved when the world is converted to vanilla, and pets (dogs, cats, horses, etc) might sometimes forget their owners due to the UUID changes.

## Multiplayer

### LAN Worlds

LAN worlds in Eaglercraft 1.12 work identically to the way LAN works on vanilla 1.12. After opening to LAN you can connect the the given port from another computer in the same network, usually to `192.168.1.x`.
Unlike Lax1dude's EaglercraftX there is no need for a special LAN relay, and there is no relay support. One downside of this is that it can be practically impossible to play on LAN on a managed wifi connection, though solutions such as rconn might be helpful.

### Connecting to a server

Eaglercraft 1.12 can connect to any cracked minecraft server that does not require authentication. The 1.12 client can play on any existing Eaglercraft 1.5.2 or 1.8.8 servers.

Eaglercraft 1.12 DOES NOT support websocket `wss://` connections! When connecting to an Eaglercraft server, you CANNOT have `wss://` in the IP. or IT WILL NOT WORK!!

### Making a Server

To make a server for Eaglercraft 1.12 there is a modified version of EaglercraftXBungee ("EaglerXBungee") which is included in this repository in the `server` folder, though its Linux-only. To run a server on Windows use the vanilla 1.12 server jar. Existing EaglercraftX clients CANNOT connect to neither the vanilla server nor the modified EaglerXBungee, please keep that in mind.
If you want to have a server accessible for both Eaglercraft 1.12 clients and EaglercraftX clients, please use Lax1dude's unmodified EaglerXBungee server.

### Web client
As of right now there is no system to compile an offline download, neither is there a web client.

## Pros & Cons

Pros:
- Its a really slimmed down version of the 1.12 client that works
- ITS FREE!
- It has Optifine

Cons:
- No web or offline web client or download
- It's pirated
- No dedicated GPU support on Windows (yet)
