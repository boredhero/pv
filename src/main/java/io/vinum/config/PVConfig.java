/*
    Project Vinum - PVConfig.java
    Copyright (C) 2020 Noah Martino and Tiller Eaton

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package io.vinum.config;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import io.vinum.common.Defines;

public class PVConfig {

    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;
    static{
        final Pair<Common, ForgeConfigSpec> specPair = new Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    //Spawn Chances
    public static int agave_spawn_chances;
    //Fuels
    public static int cinnamon_log, stripped_cinnamon_log, cinnamon_wood, stripped_cinnamon_wood, cinnamon_planks, cinnamon_leaves, cinnamon_sign, cinnamon_sapling;
    
    //This needs to be called or else this doesn't work!
    public static void bakeConfig(){
        //Spawn Chances
        agave_spawn_chances = COMMON.agave_spawn_chances.get();
        //Fuels
        cinnamon_log = COMMON.cinnamon_log.get();
        stripped_cinnamon_log = COMMON.stripped_cinnamon_log.get();
        cinnamon_wood = COMMON.cinnamon_wood.get();
        stripped_cinnamon_wood = COMMON.stripped_cinnamon_wood.get();
        cinnamon_planks = COMMON.cinnamon_planks.get();
        cinnamon_leaves = COMMON.cinnamon_leaves.get();
        cinnamon_sign = COMMON.cinnamon_sign.get();
        cinnamon_sapling = COMMON.cinnamon_sapling.get();
    }
//Internal class
public static class Common {

    //Spawn Chances
    public final ConfigValue<Integer> agave_spawn_chances;
    //Fuels
    public final ConfigValue<Integer> cinnamon_log, stripped_cinnamon_log, cinnamon_wood, stripped_cinnamon_wood, cinnamon_planks, cinnamon_leaves, cinnamon_sign, cinnamon_sapling;

    public Common(Builder builder){
        builder.push("General");

        //Spawn Chances
        agave_spawn_chances = builder
        .comment("Set spawn chances for Agave Plants in the Mesa/Desert Biome (Default:15)")
        .translation(Defines.MODID + ".config." + "agave_spawn_chances")
        .define("agave_spawn_chances", 15);

        //So...I used the same fuels logic as I use in More Fuels Mod for these
        //They follow my *correct* math, not Mojang's janky math where 300/4 = 300 lol
        //https://github.com/boredhero/morefuelsmod/blob/1.15.2/src/main/java/io/morefuelsmod/config/MFMConfig.java\
    
        //Fuels
        cinnamon_log = builder
        .comment("Set fuel burn time in ticks for Cinnamon Log")
        .translation(Defines.MODID + ".config." + "cinnamon_log")
        .define("cinnamon_log", 300);

        stripped_cinnamon_log = builder
        .comment("Set fuel burn time in ticks for Stripped Cinnamon Log")
        .translation(Defines.MODID + ".config." + "stripped_cinnamon_log")
        .define("stripped_cinnamon_log", 300);

        cinnamon_wood = builder
        .comment("Set fuel burn time in ticks for Cinnamon Wood")
        .translation(Defines.MODID + ".config." + "cinnamon_wood")
        .define("cinnamon_wood", 300);

        stripped_cinnamon_wood = builder
        .comment("Set fuel burn time in ticks for Stripped Cinnamon Wood")
        .translation(Defines.MODID + ".config." + "stripped_cinnamon_wood")
        .define("stripped_cinnamon_wood", 300);

        cinnamon_planks = builder
        .comment("Set fuel burn time in ticks for Cinnamon Planks")
        .translation(Defines.MODID + ".config." + "cinnamon_planks")
        .define("cinnamon_planks", 75);

        cinnamon_leaves = builder
        .comment("Set fuel burn time in ticks for Cinnamon Leaves")
        .translation(Defines.MODID + ".config." + "cinnamon_leaves")
        .define("cinnamon_leaves", 75);

        cinnamon_sign = builder
        .comment("Set fuel burn time in ticks for Cinnamon Sign")
        .translation(Defines.MODID + ".config." + "cinnamon_sign")
        .define("cinnamon_sign", 163);

        cinnamon_sapling = builder
        .comment("Set fuel burn time in ticks for Cinnamon Sapling")
        .translation(Defines.MODID + ".config." + "cinnamon_sapling")
        .define("cinnamon_sapling", 100);

        }
    }
}