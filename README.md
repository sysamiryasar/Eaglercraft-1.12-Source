# Sythix Client

Sythix Client is a customised PvP client built on top of this Eaglercraft 1.12 source (desktop Java build, OptiFine included). It has no cheats: every module is visual, informational or a comfort feature.

## Using it

- **Mod menu:** press **Right Shift** in game (rebind it in Controls or the Keybind Manager) or click **Sythix Mods** on the main menu. Search, filter by category or favourites, switch modules on/off and click the gear for settings. Every setting has a description and its own reset arrow.
- **HUD editor:** button in the mod menu. Drag elements, scroll to resize, click one to edit it. Hold Shift to place freely.
- **Sythix Settings:** Options > *Sythix Settings...* (replaces the Snooper button). All video, OptiFine detail/quality/animation/performance options in one tabbed screen.
- **Profile:** **Profile** on the main menu. Change your offline name, skin (put PNGs in `sythix/skins`), model (classic/slim) and cape (built-in or your own PNG in `sythix/capes`). Skins and capes are shown on your screen only.
- **Help Center** (mod menu > Client) has guides and troubleshooting.

## The 65 modules

| Section | Modules |
|---|---|
| A. Performance | Performance Optimizer, Animation Controller, Particle Controller, Graphics Controller, Resource Manager, Performance Monitor |
| B. PvP & combat | Keystrokes, CPS Counter, Crosshair, Combat HUD, Combat Animations, Viewmodel, Armor HUD, Potion HUD, Combat Timers, Movement Controls |
| C. Movement | Camera Tools (zoom, freelook, perspective), Movement HUD, Coordinates HUD, Compass HUD, Waypoints (one module with a manager screen) |
| D. Inventory | Inventory HUD, Item Counters, Item Information, Inventory Enhancements, World Information |
| E. HUD & interface | HUD Editor, Scoreboard, Tab List, Chat, Notifications, Array List, Interface Theme, Main Menu |
| F. Visuals | Environment Visuals, Block Visuals, Texture Pack Manager, Cosmetics, Sound Controller |
| G. Multiplayer | Server Manager, Connection Monitor, Friends Manager, Party Tools, Server Profiles, Chat Utilities |
| H. Recording & stats | Screenshot Manager, Recording Tools, Replay System, Statistics Tracker, Practice Tools |
| I. Client | Keybind Manager, Profile Manager, Settings Manager, Search and Discovery, Accessibility, Help Center |
| J. Advanced | Diagnostics, Error Handler, Safe Mode, Update Manager, Plugin Framework, Developer Tools, Privacy Controls, Browser Compatibility, Localization |

Honest limits:
- **Recording Tools** prepares the game for OBS etc. (REC indicator, clean view, FPS lock) but does not record video itself. **Replay System** stores bookmarks and a timeline, not playable replays.
- **Modern (1.21) look:** Sythix runs Minecraft 1.12.2. Resource packs made for 1.12 can give existing blocks, items and mobs a newer look, but blocks, mobs, items and terrain that do not exist in 1.12 cannot be added by a client.
- **Browser:** this source builds the desktop client only; there is no web build yet.
- **Update Manager** and **Plugin Framework** are off by default and only do something if you set them up.

## Files

Everything Sythix stores is in the `sythix/` folder of the game directory: `config.json` (all settings), `profiles/`, `backups/` (automatic, newest 10 kept), `waypoints.json`, `match-history.json` (statistics), `profile.json`, `skins/`, `capes/` and `plugins/`. Nothing is sent over the internet by default. If something breaks, use **Safe Mode** or restore a backup in **Settings Manager**.

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
