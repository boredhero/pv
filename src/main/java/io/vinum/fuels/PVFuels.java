package io.vinum.fuels;

import io.vinum.block.ModBlocks;
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

        if(fuel == ModBlocks.CINNAMON_LOG.get().asItem())
            event.setBurnTime(cinnamon_log);
        else if(fuel == ModBlocks.STRIPPED_CINNAMON_LOG.get().asItem())
            event.setBurnTime(stripped_cinnamon_log);
        else if(fuel == ModBlocks.CINNAMON_WOOD.get().asItem())
            event.setBurnTime(cinnamon_wood);
        else if(fuel == ModBlocks.STRIPPED_CINNAMON_WOOD.get().asItem())
            event.setBurnTime(stripped_cinnamon_wood);
        else if(fuel == ModBlocks.CINNAMON_PLANKS.get().asItem())
            event.setBurnTime(cinnamon_planks);
        else if(fuel == ModBlocks.CINNAMON_LEAVES.get().asItem())
            event.setBurnTime(cinnamon_leaves);
        else if(fuel == ModBlocks.CINNAMON_SIGN.get().asItem())
            event.setBurnTime(cinnamon_sign);
        else if(fuel == ModBlocks.CINNAMON_SAPLING.get().asItem())
            event.setBurnTime(cinnamon_sapling);

    }
}