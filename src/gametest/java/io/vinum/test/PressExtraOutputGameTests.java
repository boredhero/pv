package io.vinum.test;

import io.vinum.block.ModBlocks;
import io.vinum.item.ModItems;
import io.vinum.machine.*;
import io.vinum.sugar.SugarContent;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class PressExtraOutputGameTests {
    private static WinePressBlockEntity press(GameTestHelper h) {
        BlockPos pos=new BlockPos(3,2,3);
        h.setBlock(pos,ModBlocks.WINE_PRESS.get());
        ModBlocks.WINE_PRESS.get().setPlacedBy(h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),null,new ItemStack(ModItems.WINE_PRESS.get()));
        return (WinePressBlockEntity)h.getBlockEntity(pos);
    }
    private static void work(GameTestHelper h,WinePressBlockEntity press,int ticks) {
        for(int i=0;i<ticks;i++)WinePressBlockEntity.tick(h.getLevel(),press.getBlockPos(),press.getBlockState(),press);
    }
    @GameTest(template="barrel_space")
    public static void fullExtraOutputPausesBeforeConsumingWorkAndResumesAtomically(GameTestHelper h) {
        var press=press(h);press.attachMotor();press.energy.generate(2000);
        press.inventory.setStackInSlot(0,new ItemStack(Items.SUGAR_CANE,4));
        work(h,press,199);
        h.assertTrue(press.energy.getEnergyStored()==10&&press.data.get(1)==1990,"Fixture almost completes a batch");
        press.inventory.setStackInSlot(3,new ItemStack(SugarContent.BAGASSE.get(),64));
        work(h,press,20);
        h.assertTrue(press.energy.getEnergyStored()==10&&press.data.get(1)==1990&&press.inventory.getStackInSlot(0).getCount()==4&&press.fluids().getFluidInTank(0).isEmpty(),
            "Full Extra Out freezes progress without consuming input, power, or creating liquid");
        var bottom=press.items(Direction.DOWN);
        h.assertTrue(bottom.extractItem(1,1,true).getCount()==1&&press.inventory.getStackInSlot(3).getCount()==64,"Simulated hopper extraction cannot mutate output");
        bottom.extractItem(1,1,false);work(h,press,1);
        h.assertTrue(press.inventory.getStackInSlot(3).getCount()==64&&press.inventory.getStackInSlot(0).isEmpty()&&press.energy.getEnergyStored()==0&&press.fluids().getFluidInTank(0).getAmount()==1000,
            "Making one space permits exact solid and liquid coproduct commit");
        work(h,press,10);h.assertTrue(press.inventory.getStackInSlot(3).getCount()==64,"Idle ticks cannot repeat coproduct");
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void extraOutputRejectsInsertionAndShiftClickUsesNewPlayerIndices(GameTestHelper h) {
        var press=press(h);press.inventory.setStackInSlot(3,new ItemStack(SugarContent.BAGASSE.get(),7));
        h.assertTrue(!press.inventory.insertItem(3,new ItemStack(SugarContent.BAGASSE.get()),false).isEmpty(),"Output-only inventory rejects insertion");
        var sides=press.items(Direction.NORTH);var bottom=press.items(Direction.DOWN);
        h.assertTrue(!sides.insertItem(2,new ItemStack(SugarContent.BAGASSE.get()),false).isEmpty()&&!bottom.insertItem(1,new ItemStack(SugarContent.BAGASSE.get()),false).isEmpty(),"Hoppers cannot insert into Extra Out");
        h.assertTrue(sides.extractItem(2,2,false).getCount()==2&&press.inventory.getStackInSlot(3).getCount()==5,"Side automation extracts bagasse");
        var player=h.makeMockPlayer(GameType.SURVIVAL);var menu=new WinePressMenu(1,player.getInventory(),press);
        h.assertTrue(menu.slots.size()==40&&!menu.slots.get(3).mayPlace(new ItemStack(Items.STONE)),"Menu exposes four machine slots with Extra Out read-only");
        h.assertTrue(menu.quickMoveStack(player,3).getCount()==5&&press.inventory.getStackInSlot(3).isEmpty(),"Shift-click collects Extra Out into player inventory");
        player.getInventory().setItem(0,new ItemStack(Items.SUGAR_CANE,4));
        h.assertTrue(!menu.quickMoveStack(player,31).isEmpty()&&press.inventory.getStackInSlot(0).getCount()==4,"Shift-click hotbar indices still feed the press");
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void extraOutputPersistsAndOldThreeSlotPressesMigrate(GameTestHelper h) {
        var press=press(h);press.inventory.setStackInSlot(0,new ItemStack(Items.SUGAR_CANE,4));
        press.inventory.setStackInSlot(1,new ItemStack(ModItems.CARBOY.get()));
        var old=press.saveWithFullMetadata(h.getLevel().registryAccess());old.getCompound("Inventory").putInt("Size",3);
        var restored=new WinePressBlockEntity(press.getBlockPos(),press.getBlockState());
        restored.loadWithComponents(old,h.getLevel().registryAccess());
        h.assertTrue(restored.inventory.getSlots()==4&&restored.inventory.getStackInSlot(0).getCount()==4&&restored.inventory.getStackInSlot(1).is(ModItems.CARBOY.get())&&restored.inventory.getStackInSlot(3).isEmpty(),"Legacy inventory appends an empty output slot without losing original slots");
        press.inventory.setStackInSlot(3,new ItemStack(SugarContent.BAGASSE.get(),17));
        restored.loadWithComponents(press.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(restored.inventory.getStackInSlot(3).getCount()==17,"New output contents survive save/load and update tag format");
        press.inventory.setStackInSlot(3,new ItemStack(Items.STONE));press.attachMotor();press.energy.generate(2000);work(h,press,10);
        h.assertTrue(press.energy.getEnergyStored()==2000&&press.inventory.getStackInSlot(0).getCount()==4,"Incompatible existing output blocks production without consuming resources");
        h.succeed();
    }
}
