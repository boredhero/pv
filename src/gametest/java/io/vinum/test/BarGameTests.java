package io.vinum.test;

import io.vinum.bar.*;
import io.vinum.barrel.*;
import io.vinum.beverage.WineCatalog;
import io.vinum.beverage.BottleGeometry;
import io.vinum.item.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class BarGameTests {
    private static ItemStack wine() {
        var stack=((FluidContainerItem)ModItems.WINE_BOTTLE_EMPTY.get()).filled(BarrelFluidData.withAge(WineCatalog.sample(WineCatalog.varieties().getFirst(),317),12345));
        stack.set(DataComponents.CUSTOM_NAME,Component.literal("Private reserve"));return stack;
    }
    private static BarBlockEntity place(GameTestHelper h,BlockPos pos) {
        h.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        var player=h.makeMockPlayer(GameType.SURVIVAL);var item=new ItemStack(BarContent.ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,item);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false);
        h.assertTrue(item.useOn(new UseOnContext(h.getLevel(),player,InteractionHand.MAIN_HAND,item,hit)).consumesAction(),"Bar places normally");
        return (BarBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="barrel_space")
    public static void barPersistsFiveExactBottlesAndDropsOnce(GameTestHelper h) {
        BlockPos pos=h.absolutePos(new BlockPos(3,1,3));var bar=place(h,pos);var bottle=wine();
        for(int slot=0;slot<5;slot++)h.assertTrue(bar.insert(slot,bottle),"Five independent bottle slots");
        h.assertTrue(!bar.insert(0,bottle)&&!bar.insert(5,bottle)&&!bar.insert(-1,bottle),"Cannot overwrite or exceed five");
        var restored=new BarBlockEntity(pos,bar.getBlockState());restored.loadWithComponents(bar.saveWithoutMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        for(int slot=0;slot<5;slot++)h.assertTrue(ItemStack.isSameItemSameComponents(restored.bottle(slot),bottle),"Name, cork, chemistry, volume and age survive save/load");
        var copy=bar.bottle(0);copy.shrink(1);h.assertTrue(!bar.bottle(0).isEmpty(),"Read accessor cannot mutate storage");
        h.getLevel().destroyBlock(pos,true);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));
        h.assertTrue(drops.stream().filter(e->ItemStack.isSameItemSameComponents(e.getItem(),bottle)).mapToInt(e->e.getItem().getCount()).sum()==5,"Breaking returns all five exact bottles once");
        h.assertTrue(drops.stream().filter(e->e.getItem().is(BarContent.ITEM.get())).mapToInt(e->e.getItem().getCount()).sum()==1,"Breaking returns one counter");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void barClicksTargetAllFiveSlotsInEveryFacing(GameTestHelper h) {
        BlockPos pos=h.absolutePos(new BlockPos(3,1,3));var bar=place(h,pos);var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(Direction facing:Direction.Plane.HORIZONTAL) {
            player.getInventory().clearContent();
            h.getLevel().setBlockAndUpdate(pos,bar.getBlockState().setValue(BarBlock.FACING,facing));
            for(int slot=0;slot<5;slot++) {
                double a=BarBlock.CENTERS[slot],x=a,z=.53;
                switch(facing){case EAST->{x=.47;z=a;}case SOUTH->{x=1-a;z=.47;}case WEST->{x=.53;z=1-a;}default->{}}
                var hit=new BlockHitResult(new Vec3(pos.getX()+x,pos.getY()+1,pos.getZ()+z),Direction.UP,pos,false);
                var state=h.getLevel().getBlockState(pos);player.setItemInHand(InteractionHand.MAIN_HAND,wine());
                h.assertTrue(BarBlock.slot(state,pos,hit)==slot,"Target follows facing");
                h.assertTrue(state.useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,hit).consumesAction(),"Right-click inserts");
                h.assertTrue(player.getMainHandItem().isEmpty()&&!bar.bottle(slot).isEmpty(),"Survival transfers one bottle");
                h.assertTrue(state.getShape(h.getLevel(),pos).max(Direction.Axis.Y)>1,"Occupied positions extend the selection outline above the counter");
                state.useWithoutItem(h.getLevel(),player,hit);
                h.assertTrue(bar.bottle(slot).isEmpty(),"Empty-hand retrieves selected bottle");
                h.assertTrue(player.getInventory().countItem(ModItems.WINE_BOTTLE_EMPTY.get())==1,
                        "Retrieval returns exactly the selected bottle");
                player.getInventory().clearContent();
            }
        }
        h.assertTrue(!bar.insert(0,new ItemStack(ModItems.CARBOY.get()))&&!bar.insert(0,new ItemStack(Items.STICK)),"Large vessels and nonbottles rejected");
        var spirit=((FluidContainerItem)ModItems.FIFTH_BOTTLE_EMPTY.get()).filled(ModItems.spirit(io.vinum.fluid.ModFluids.TEQUILA.source().get(),173));
        h.assertTrue(bar.insert(0,spirit)&&ItemStack.isSameItemSameComponents(bar.remove(0),spirit),"Partially filled spirits transfer exactly");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void bottleLiquidHeightTracksVolumeThroughShoulders(GameTestHelper h) {
        for(var shape:BottleGeometry.values()) {
            double previous=-1;
            for(double fraction:new double[]{0,.001,.1,.25,.5,.75,.999,1}) {
                double height=shape.fillHeight(fraction);
                h.assertTrue(height>=previous&&height<=BottleGeometry.LIQUID_TOP,"Continuous bounded fill height");
                h.assertTrue(Math.abs(shape.volumeBelow(height)/shape.volumeBelow(BottleGeometry.LIQUID_TOP)-fraction)<1e-8,"Liquid mesh represents actual volume even across tapered shoulders");previous=height;
            }
        }
        h.succeed();
    }
}
