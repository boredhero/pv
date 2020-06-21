/*
    Project Vinum - PVFuels.java
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
package io.vinum.fuels;

import io.vinum.block.PVBlocks;
import io.vinum.config.PVConfig;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PVFuels {
    public static final PVFuels instance = new PVFuels();

    private PVFuels(){}

    @SubscribeEvent
    public void onFurnaceFuelBurnTimeEvent(FurnaceFuelBurnTimeEvent event){
        ItemStack fuelStack = event.getItemStack();
        Item fuel = fuelStack.getItem();
        
        //Get the values from the config file!
        final int cinnamon_log = PVConfig.cinnamon_log, stripped_cinnamon_log = PVConfig.stripped_cinnamon_log, cinnamon_wood = PVConfig.cinnamon_wood,
        stripped_cinnamon_wood = PVConfig.stripped_cinnamon_wood, cinnamon_planks = PVConfig.cinnamon_planks, cinnamon_leaves = PVConfig.cinnamon_leaves, cinnamon_sign = PVConfig.cinnamon_sign,
        cinnamon_sapling = PVConfig.cinnamon_sapling;

        if(fuel == PVBlocks.CINNAMON_LOG.get().asItem())
            event.setBurnTime(cinnamon_log);
        else if(fuel == PVBlocks.STRIPPED_CINNAMON_LOG.get().asItem())
            event.setBurnTime(stripped_cinnamon_log);
        else if(fuel == PVBlocks.CINNAMON_WOOD.get().asItem())
            event.setBurnTime(cinnamon_wood);
        else if(fuel == PVBlocks.STRIPPED_CINNAMON_WOOD.get().asItem())
            event.setBurnTime(stripped_cinnamon_wood);
        else if(fuel == PVBlocks.CINNAMON_PLANKS.get().asItem())
            event.setBurnTime(cinnamon_planks);
        else if(fuel == PVBlocks.CINNAMON_LEAVES.get().asItem())
            event.setBurnTime(cinnamon_leaves);
        else if(fuel == PVBlocks.CINNAMON_SIGN.get().asItem())
            event.setBurnTime(cinnamon_sign);
        else if(fuel == PVBlocks.CINNAMON_SAPLING.get().asItem())
            event.setBurnTime(cinnamon_sapling);

    }
}