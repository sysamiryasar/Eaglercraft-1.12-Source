package net.sythix.client.eagler;

import java.util.HashMap;
import java.util.Map;

/** 1.8 sound names (like "random.bow") -> 1.12 sound names (like "entity.arrow.shoot"), with a category. */
final class LegacySounds
{
    // 1.12 SoundCategory order
    static final int MASTER = 0;
    static final int RECORDS = 2;
    static final int WEATHER = 3;
    static final int BLOCKS = 4;
    static final int HOSTILE = 5;
    static final int NEUTRAL = 6;
    static final int PLAYERS = 7;
    static final int AMBIENT = 8;

    private static final Map<String, String> NAMES = new HashMap<String, String>();

    private static final String[] TABLE = {
        "ambient.cave.cave", "ambient.cave",
        "ambient.weather.rain", "weather.rain",
        "ambient.weather.thunder", "entity.lightning.thunder",
        "game.player.hurt.fall.big", "entity.player.big_fall",
        "game.player.hurt.fall.small", "entity.player.small_fall",
        "game.neutral.hurt.fall.big", "entity.generic.big_fall",
        "game.neutral.hurt.fall.small", "entity.generic.small_fall",
        "game.hostile.hurt.fall.big", "entity.hostile.big_fall",
        "game.hostile.hurt.fall.small", "entity.hostile.small_fall",
        "game.player.hurt", "entity.player.hurt",
        "game.neutral.hurt", "entity.generic.hurt",
        "game.hostile.hurt", "entity.hostile.hurt",
        "game.player.die", "entity.player.death",
        "game.neutral.die", "entity.generic.death",
        "game.hostile.die", "entity.hostile.death",
        "game.player.swim", "entity.player.swim",
        "game.player.swim.splash", "entity.player.splash",
        "game.neutral.swim", "entity.generic.swim",
        "game.neutral.swim.splash", "entity.generic.splash",
        "game.hostile.swim", "entity.hostile.swim",
        "game.hostile.swim.splash", "entity.hostile.splash",
        "game.tnt.primed", "entity.tnt.primed",
        "creeper.primed", "entity.creeper.primed",
        "dig.cloth", "block.cloth.break",
        "dig.glass", "block.glass.break",
        "dig.grass", "block.grass.break",
        "dig.gravel", "block.gravel.break",
        "dig.sand", "block.sand.break",
        "dig.snow", "block.snow.break",
        "dig.stone", "block.stone.break",
        "dig.wood", "block.wood.break",
        "step.cloth", "block.cloth.step",
        "step.grass", "block.grass.step",
        "step.gravel", "block.gravel.step",
        "step.ladder", "block.ladder.step",
        "step.sand", "block.sand.step",
        "step.snow", "block.snow.step",
        "step.stone", "block.stone.step",
        "step.wood", "block.wood.step",
        "fire.fire", "block.fire.ambient",
        "fire.ignite", "item.flintandsteel.use",
        "item.fireCharge.use", "item.firecharge.use",
        "fireworks.blast", "entity.firework.blast",
        "fireworks.blast_far", "entity.firework.blast_far",
        "fireworks.largeBlast", "entity.firework.large_blast",
        "fireworks.largeBlast_far", "entity.firework.large_blast_far",
        "fireworks.launch", "entity.firework.launch",
        "fireworks.twinkle", "entity.firework.twinkle",
        "fireworks.twinkle_far", "entity.firework.twinkle_far",
        "liquid.lava", "block.lava.ambient",
        "liquid.lavapop", "block.lava.pop",
        "liquid.water", "block.water.ambient",
        "liquid.splash", "entity.generic.splash",
        "liquid.swim", "entity.generic.swim",
        "minecart.base", "entity.minecart.riding",
        "minecart.inside", "entity.minecart.inside",
        "note.bass", "block.note.basedrum",
        "note.bassattack", "block.note.bass",
        "note.bd", "block.note.basedrum",
        "note.harp", "block.note.harp",
        "note.hat", "block.note.hat",
        "note.pling", "block.note.pling",
        "note.snare", "block.note.snare",
        "portal.portal", "block.portal.ambient",
        "portal.travel", "block.portal.travel",
        "portal.trigger", "block.portal.trigger",
        "random.anvil_break", "block.anvil.destroy",
        "random.anvil_land", "block.anvil.land",
        "random.anvil_use", "block.anvil.use",
        "random.bow", "entity.arrow.shoot",
        "random.bowhit", "entity.arrow.hit",
        "random.break", "entity.item.break",
        "random.burp", "entity.player.burp",
        "random.chestclosed", "block.chest.close",
        "random.chestopen", "block.chest.open",
        "gui.button.press", "ui.button.click",
        "random.click", "ui.button.click",
        "random.door_close", "block.wooden_door.close",
        "random.door_open", "block.wooden_door.open",
        "random.drink", "entity.generic.drink",
        "random.eat", "entity.generic.eat",
        "random.explode", "entity.generic.explode",
        "random.fizz", "block.fire.extinguish",
        "random.fuse", "entity.tnt.primed",
        "random.glass", "block.glass.break",
        "random.levelup", "entity.player.levelup",
        "random.orb", "entity.experience_orb.pickup",
        "random.pop", "entity.item.pickup",
        "random.splash", "entity.bobber.splash",
        "random.successful_hit", "entity.arrow.hit_player",
        "random.wood_click", "block.wood_button.click_on",
        "tile.piston.in", "block.piston.contract",
        "tile.piston.out", "block.piston.extend",
        "mob.bat.death", "entity.bat.death", "mob.bat.hurt", "entity.bat.hurt", "mob.bat.idle", "entity.bat.ambient", "mob.bat.loop", "entity.bat.loop", "mob.bat.takeoff", "entity.bat.takeoff",
        "mob.blaze.breathe", "entity.blaze.ambient", "mob.blaze.death", "entity.blaze.death", "mob.blaze.hit", "entity.blaze.hurt",
        "mob.cat.hiss", "entity.cat.hiss", "mob.cat.hitt", "entity.cat.hurt", "mob.cat.meow", "entity.cat.ambient", "mob.cat.purr", "entity.cat.purr", "mob.cat.purreow", "entity.cat.purreow",
        "mob.chicken.hurt", "entity.chicken.hurt", "mob.chicken.plop", "entity.chicken.egg", "mob.chicken.say", "entity.chicken.ambient", "mob.chicken.step", "entity.chicken.step",
        "mob.cow.hurt", "entity.cow.hurt", "mob.cow.say", "entity.cow.ambient", "mob.cow.step", "entity.cow.step",
        "mob.creeper.death", "entity.creeper.death", "mob.creeper.say", "entity.creeper.hurt",
        "mob.enderdragon.end", "entity.enderdragon.death", "mob.enderdragon.growl", "entity.enderdragon.growl", "mob.enderdragon.hit", "entity.enderdragon.hurt", "mob.enderdragon.wings", "entity.enderdragon.flap",
        "mob.endermen.death", "entity.endermen.death", "mob.endermen.hit", "entity.endermen.hurt", "mob.endermen.idle", "entity.endermen.ambient", "mob.endermen.portal", "entity.endermen.teleport", "mob.endermen.scream", "entity.endermen.scream", "mob.endermen.stare", "entity.endermen.stare",
        "mob.ghast.affectionate_scream", "entity.ghast.scream", "mob.ghast.charge", "entity.ghast.warn", "mob.ghast.death", "entity.ghast.death", "mob.ghast.fireball", "entity.ghast.shoot", "mob.ghast.moan", "entity.ghast.ambient", "mob.ghast.scream", "entity.ghast.hurt",
        "mob.guardian.hit", "entity.guardian.hurt", "mob.guardian.idle", "entity.guardian.ambient", "mob.guardian.death", "entity.guardian.death", "mob.guardian.elder.hit", "entity.elder_guardian.hurt", "mob.guardian.elder.idle", "entity.elder_guardian.ambient", "mob.guardian.elder.death", "entity.elder_guardian.death", "mob.guardian.curse", "entity.elder_guardian.curse", "mob.guardian.attack", "entity.guardian.attack",
        "mob.horse.angry", "entity.horse.angry", "mob.horse.armor", "entity.horse.armor", "mob.horse.breathe", "entity.horse.breathe", "mob.horse.death", "entity.horse.death", "mob.horse.gallop", "entity.horse.gallop", "mob.horse.hit", "entity.horse.hurt", "mob.horse.idle", "entity.horse.ambient", "mob.horse.jump", "entity.horse.jump", "mob.horse.land", "entity.horse.land", "mob.horse.leather", "entity.horse.saddle", "mob.horse.soft", "entity.horse.step", "mob.horse.wood", "entity.horse.step_wood",
        "mob.horse.donkey.angry", "entity.donkey.angry", "mob.horse.donkey.death", "entity.donkey.death", "mob.horse.donkey.hit", "entity.donkey.hurt", "mob.horse.donkey.idle", "entity.donkey.ambient",
        "mob.horse.skeleton.death", "entity.skeleton_horse.death", "mob.horse.skeleton.hit", "entity.skeleton_horse.hurt", "mob.horse.skeleton.idle", "entity.skeleton_horse.ambient",
        "mob.horse.zombie.death", "entity.zombie_horse.death", "mob.horse.zombie.hit", "entity.zombie_horse.hurt", "mob.horse.zombie.idle", "entity.zombie_horse.ambient",
        "mob.irongolem.death", "entity.irongolem.death", "mob.irongolem.hit", "entity.irongolem.hurt", "mob.irongolem.throw", "entity.irongolem.attack", "mob.irongolem.walk", "entity.irongolem.step",
        "mob.magmacube.big", "entity.magmacube.squish", "mob.magmacube.jump", "entity.magmacube.jump", "mob.magmacube.small", "entity.small_magmacube.squish",
        "mob.pig.death", "entity.pig.death", "mob.pig.say", "entity.pig.ambient", "mob.pig.step", "entity.pig.step",
        "mob.rabbit.hurt", "entity.rabbit.hurt", "mob.rabbit.idle", "entity.rabbit.ambient", "mob.rabbit.hop", "entity.rabbit.jump", "mob.rabbit.death", "entity.rabbit.death",
        "mob.sheep.say", "entity.sheep.ambient", "mob.sheep.shear", "entity.sheep.shear", "mob.sheep.step", "entity.sheep.step",
        "mob.silverfish.hit", "entity.silverfish.hurt", "mob.silverfish.kill", "entity.silverfish.death", "mob.silverfish.say", "entity.silverfish.ambient", "mob.silverfish.step", "entity.silverfish.step",
        "mob.skeleton.death", "entity.skeleton.death", "mob.skeleton.hurt", "entity.skeleton.hurt", "mob.skeleton.say", "entity.skeleton.ambient", "mob.skeleton.step", "entity.skeleton.step",
        "mob.slime.attack", "entity.slime.attack", "mob.slime.big", "entity.slime.squish", "mob.slime.small", "entity.small_slime.squish",
        "mob.spider.death", "entity.spider.death", "mob.spider.say", "entity.spider.ambient", "mob.spider.step", "entity.spider.step",
        "mob.villager.death", "entity.villager.death", "mob.villager.haggle", "entity.villager.trading", "mob.villager.hit", "entity.villager.hurt", "mob.villager.idle", "entity.villager.ambient", "mob.villager.no", "entity.villager.no", "mob.villager.yes", "entity.villager.yes",
        "mob.wither.death", "entity.wither.death", "mob.wither.hurt", "entity.wither.hurt", "mob.wither.idle", "entity.wither.ambient", "mob.wither.shoot", "entity.wither.shoot", "mob.wither.spawn", "entity.wither.spawn",
        "mob.wolf.bark", "entity.wolf.ambient", "mob.wolf.death", "entity.wolf.death", "mob.wolf.growl", "entity.wolf.growl", "mob.wolf.howl", "entity.wolf.howl", "mob.wolf.hurt", "entity.wolf.hurt", "mob.wolf.panting", "entity.wolf.pant", "mob.wolf.shake", "entity.wolf.shake", "mob.wolf.step", "entity.wolf.step", "mob.wolf.whine", "entity.wolf.whine",
        "mob.zombie.death", "entity.zombie.death", "mob.zombie.hurt", "entity.zombie.hurt", "mob.zombie.infect", "entity.zombie.infect", "mob.zombie.metal", "entity.zombie.attack_iron_door", "mob.zombie.remedy", "entity.zombie_villager.cure", "mob.zombie.say", "entity.zombie.ambient", "mob.zombie.step", "entity.zombie.step", "mob.zombie.unfect", "entity.zombie_villager.converted", "mob.zombie.wood", "entity.zombie.attack_door_wood", "mob.zombie.woodbreak", "entity.zombie.break_door_wood",
        "mob.zombiepig.zpig", "entity.zombie_pig.ambient", "mob.zombiepig.zpigangry", "entity.zombie_pig.angry", "mob.zombiepig.zpigdeath", "entity.zombie_pig.death", "mob.zombiepig.zpighurt", "entity.zombie_pig.hurt",
        "records.11", "record.11", "records.13", "record.13", "records.blocks", "record.blocks", "records.cat", "record.cat", "records.chirp", "record.chirp", "records.far", "record.far", "records.mall", "record.mall", "records.mellohi", "record.mellohi", "records.stal", "record.stal", "records.strad", "record.strad", "records.wait", "record.wait", "records.ward", "record.ward"
    };

    static
    {
        for (int i = 0; i + 1 < TABLE.length; i += 2)
        {
            NAMES.put(TABLE[i], TABLE[i + 1]);
        }
    }

    private LegacySounds()
    {
    }

    /** The 1.12 name, or null when there is no matching sound (it is then left out). */
    static String name(String legacy)
    {
        String n = legacy.startsWith("minecraft:") ? legacy.substring(10) : legacy;
        String found = NAMES.get(n);

        if (found != null)
        {
            return found;
        }

        // a server's own sounds (resource packs) keep their names
        return n.contains(".") && !n.startsWith("mob.") && !n.startsWith("random.") && !n.startsWith("step.") && !n.startsWith("dig.") ? n : null;
    }

    static int category(String legacy)
    {
        if (legacy.startsWith("mob.") || legacy.startsWith("game.hostile") || legacy.startsWith("game.neutral"))
        {
            return legacy.startsWith("mob.zombie") || legacy.startsWith("mob.skeleton") || legacy.startsWith("mob.creeper") || legacy.startsWith("mob.spider") || legacy.startsWith("mob.endermen") || legacy.startsWith("mob.blaze") || legacy.startsWith("mob.ghast") || legacy.startsWith("mob.slime") || legacy.startsWith("mob.wither") || legacy.startsWith("game.hostile") ? HOSTILE : NEUTRAL;
        }

        if (legacy.startsWith("game.player") || legacy.startsWith("random.successful_hit") || legacy.startsWith("random.levelup") || legacy.startsWith("random.burp") || legacy.startsWith("random.eat") || legacy.startsWith("random.drink"))
        {
            return PLAYERS;
        }

        if (legacy.startsWith("dig.") || legacy.startsWith("step.") || legacy.startsWith("tile.") || legacy.startsWith("note.") || legacy.startsWith("fire.") || legacy.startsWith("liquid.") || legacy.startsWith("random.door") || legacy.startsWith("random.chest") || legacy.startsWith("random.anvil"))
        {
            return BLOCKS;
        }

        if (legacy.startsWith("ambient.weather"))
        {
            return WEATHER;
        }

        if (legacy.startsWith("ambient."))
        {
            return AMBIENT;
        }

        if (legacy.startsWith("records."))
        {
            return RECORDS;
        }

        return MASTER;
    }
}
